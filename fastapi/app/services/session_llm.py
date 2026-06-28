import json
import logging
import re

from app.schemas.session import AskPayload, EndPayload, PersonalizationContext, ProblemOut
from app.services.llm_client import llm_client

logger = logging.getLogger(__name__)

_FALLBACK_PROBLEM = ProblemOut(question="클래스와 인터페이스의 차이점을 설명하세요.", topic_tag="클래스")
_FALLBACK_FEEDBACK = "답변 평가 중 오류가 발생했습니다. 다음 문제로 넘어갑니다."


def _extract_json(text: str) -> dict:
    text = re.sub(r"```(?:json)?\s*|\s*```", "", text).strip()
    match = re.search(r"\{.*\}", text, re.DOTALL)
    if match:
        return json.loads(match.group())
    return json.loads(text)


async def generate_first_question(skill_doc: str, context: PersonalizationContext) -> ProblemOut:
    weak_str = ", ".join(context.weak_topics) if context.weak_topics else "없음"
    prompt = f"""다음 학습자에게 {context.selected_difficulty} 수준의 {context.subject_name} 문제를 하나 출제하세요.

학습자 레벨: {context.effective_level}
학습 목표: {context.goal or "없음"}
약점 토픽: {weak_str}
최근 정답률: {context.recent_correct_rate:.0%}

다음 JSON만 출력하세요:
{{"question": "문제 내용", "topic_tag": "토픽명"}}"""

    try:
        raw = await llm_client.complete(skill_doc, prompt, max_tokens=512)
        data = _extract_json(raw)
        return ProblemOut(question=data["question"], topic_tag=data.get("topic_tag", "일반"))
    except Exception as exc:
        logger.warning("generate_first_question failed (session=%s): %s", context.session_id, exc)
        return _FALLBACK_PROBLEM


async def evaluate_answer(skill_doc: str, payload: AskPayload, session_id: str) -> tuple[bool, str, ProblemOut]:
    prompt = f"""학습자의 답변을 평가하고 다음 문제를 출제하세요.

과목: {payload.subject_name}
문제: {payload.current_question}
학습자 답변: {payload.user_answer}

다음 JSON만 출력하세요:
{{"is_correct": true, "feedback": "피드백 내용", "next_problem": {{"question": "다음 문제", "topic_tag": "토픽명"}}}}"""

    try:
        raw = await llm_client.complete(skill_doc, prompt, max_tokens=1024)
        data = _extract_json(raw)
        np = data.get("next_problem", {})
        return (
            bool(data.get("is_correct", False)),
            data.get("feedback", _FALLBACK_FEEDBACK),
            ProblemOut(
                question=np.get("question", _FALLBACK_PROBLEM.question),
                topic_tag=np.get("topic_tag", "일반"),
            ),
        )
    except Exception as exc:
        logger.warning("evaluate_answer failed (session=%s): %s", session_id, exc)
        return False, _FALLBACK_FEEDBACK, _FALLBACK_PROBLEM


async def generate_recommendation(skill_doc: str, payload: EndPayload, session_id: str) -> str:
    weak_str = ", ".join(payload.weak_topics) if payload.weak_topics else "없음"
    prompt = f"""학습 세션이 종료되었습니다. 다음 학습자를 위한 맞춤 추천을 한두 문장으로 작성하세요.

과목: {payload.subject_name}
정답률: {payload.correct_rate:.0%}
약점 토픽: {weak_str}
목표: {payload.goal or "없음"}"""

    try:
        return await llm_client.complete(skill_doc, prompt, max_tokens=256)
    except Exception as exc:
        logger.warning("generate_recommendation failed (session=%s): %s", session_id, exc)
        return "학습을 꾸준히 이어가세요!"
