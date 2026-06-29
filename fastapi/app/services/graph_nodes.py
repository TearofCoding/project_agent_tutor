"""graph_nodes.py — LangGraph 노드 구현.

개선:
  - speculative_search_node: should_search 플래그 확인 후 스킵 가능
  - merge_node: should_search=False면 snippets 무조건 비움 (이중 방어)
  - _parse: 공통 JSON 파싱 함수
  - router_node: consecutive_wrong 분기 제거 (tutor_graph에서 선제 처리)
"""
import json
import logging
import re

from app.services.llm_client import llm_client
from app.services.search_context import build_feedback_query, format_search_block
from app.services.tavily_client import tavily_client

logger = logging.getLogger(__name__)

# ── 공통 유틸 ──

_FALLBACK = {
    "is_correct":     False,
    "feedback":       "평가 오류. 다음 문제로 넘어갑니다.",
    "next_question":  "클래스와 인터페이스의 차이를 설명하세요.",
    "next_topic_tag": "클래스",
}


def _parse(raw: str) -> dict:
    """LLM 응답에서 JSON 추출. 마크다운 펜스 제거 후 파싱."""
    text = re.sub(r"```(?:json)?\s*|\s*```", "", raw).strip()
    m = re.search(r"\{.*\}", text, re.DOTALL)
    return json.loads(m.group() if m else text)


# ── 1. Router 노드 ──

_ROUTER_PROMPT = """학습자 입력을 분류하세요.

과목: {subject_name}
현재 문제: {current_question}
학습자 입력: {user_answer}

다음 JSON만 출력 (consecutive_wrong 판단은 이미 완료됨):
{{"action": "evaluate" | "hint" | "search"}}

분류 기준:
- evaluate: 정상 답변 제출
- hint: "모르겠어요" / 질문형 / 도움 요청
- search: "공식 문서" / "레퍼런스" / "예시 코드" 명시 요청"""


async def router_node(state: dict) -> dict:
    """LLM으로 action 결정. downgrade는 tutor_graph에서 선제 처리되므로 제외."""
    prompt = _ROUTER_PROMPT.format(**state)
    try:
        raw    = await llm_client.complete("", prompt, max_tokens=64)
        action = _parse(raw).get("action", "evaluate")
        if action not in ("evaluate", "hint", "search"):
            action = "evaluate"
    except Exception as exc:
        logger.warning("router_node failed: %s", exc)
        action = "evaluate"
    return {"action": action}


# ── 2. Speculative Search 노드 ──

async def speculative_search_node(state: dict) -> dict:
    """조건부 Tavily 검색.

    should_search=False(needs_doc_search 미충족)이면 즉시 빈 결과 반환 (이중 방어).
    """
    if not state.get("should_search", False):
        return {"search_snippets": [], "search_urls": []}

    topic   = state.get("topic_tag") or state.get("subject_name", "")
    subject = state.get("subject_name", "")
    try:
        result = await tavily_client.search(
            build_feedback_query(topic, subject), max_results=2
        )
        logger.debug("speculative_search: got %d snippets for '%s'", len(result.snippets), topic)
        return {"search_snippets": result.snippets, "search_urls": result.urls}
    except Exception as exc:
        logger.warning("speculative_search_node failed: %s", exc)
        return {"search_snippets": [], "search_urls": []}


# ── 3. Merge 노드 ──

async def merge_node(state: dict) -> dict:
    """Router + speculative_search 결과 결합.

    action != "search" 또는 should_search=False면 snippets 폐기.
    """
    if state.get("action") != "search" or not state.get("should_search", False):
        return {"search_snippets": [], "search_urls": []}
    return {}   # search이면 speculative_search 결과 그대로 유지


# ── 4. Evaluate 노드 ──

_EVAL_PROMPT = """정오를 판정하고 피드백과 다음 문제를 출제하세요.

과목: {subject_name}  문제: {current_question}  답변: {user_answer}{search_block}

JSON만 출력:
{{"is_correct": true/false, "feedback": "...", "next_problem": {{"question": "...", "topic_tag": "..."}}}}"""


async def evaluate_node(state: dict) -> dict:
    snippets = state.get("search_snippets", [])
    urls     = state.get("search_urls", [])
    sb = format_search_block("\n".join(snippets)[:400], urls) if snippets else ""

    try:
        raw  = await llm_client.complete(
            state.get("skill_doc", ""),
            _EVAL_PROMPT.format(search_block=sb, **state),
            max_tokens=512,
        )
        data = _parse(raw)
        np   = data.get("next_problem", {})
        return {
            "is_correct":     bool(data.get("is_correct", False)),
            "feedback":       data.get("feedback", _FALLBACK["feedback"]),
            "next_question":  np.get("question",  _FALLBACK["next_question"]),
            "next_topic_tag": np.get("topic_tag", _FALLBACK["next_topic_tag"]),
        }
    except Exception as exc:
        logger.warning("evaluate_node failed: %s", exc)
        return _FALLBACK


# ── 5. Hint 노드 ──

_HINT_PROMPT = """학습자가 막혔습니다. 답을 알려주지 말고 힌트만 주세요.

문제: {current_question}   과목: {subject_name}

JSON만 출력: {{"feedback": "힌트 내용"}}"""


async def hint_node(state: dict) -> dict:
    try:
        raw  = await llm_client.complete(
            state.get("skill_doc", ""),
            _HINT_PROMPT.format(**state),
            max_tokens=256,
        )
        feedback = _parse(raw).get("feedback", "힌트를 생성할 수 없습니다.")
    except Exception as exc:
        logger.warning("hint_node failed: %s", exc)
        feedback = "힌트 생성 오류."
    return {
        "is_correct":     None,
        "feedback":       feedback,
        "next_question":  state.get("current_question"),
        "next_topic_tag": state.get("topic_tag", ""),
    }


# ── 6. Downgrade 노드 ──

_DOWNGRADE_PROMPT = """학습자가 연속 {consecutive_wrong}회 틀렸습니다.
더 쉬운 {subject_name} 문제를 BEGINNER 수준으로 출제하세요.

JSON만 출력: {{"feedback": "격려 메시지", "next_problem": {{"question": "...", "topic_tag": "..."}}}}"""


async def downgrade_node(state: dict) -> dict:
    try:
        raw  = await llm_client.complete(
            state.get("skill_doc", ""),
            _DOWNGRADE_PROMPT.format(**state),
            max_tokens=256,
        )
        data = _parse(raw)
        np   = data.get("next_problem", {})
        return {
            "is_correct":     False,
            "feedback":       data.get("feedback", "조금 더 쉬운 문제로 시작해봐요!"),
            "next_question":  np.get("question",  _FALLBACK["next_question"]),
            "next_topic_tag": np.get("topic_tag", state.get("topic_tag", "")),
        }
    except Exception as exc:
        logger.warning("downgrade_node failed: %s", exc)
        return {**_FALLBACK, "feedback": "난이도 조정 오류. 새 문제를 드립니다."}
