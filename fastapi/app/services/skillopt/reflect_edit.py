import logging

from app.services.llm_client import llm_client
from app.services.skill_manager import validate_skill_content
from app.services.skillopt.rollout import RolloutResult

logger = logging.getLogger(__name__)


async def run(skill_doc: str, rollout_result: RolloutResult) -> tuple[str, str, int]:
    """Returns (edited_skill, reflect_summary, tokens_used)."""
    failures = rollout_result.failed_questions
    successes = rollout_result.passed_questions

    failures_text = "\n".join(
        f"- 질문: {f['question']}\n  기대: {f['expected']}\n  AI답변: {f['answer']}"
        for f in failures
    ) or "없음"
    successes_text = "\n".join(f"- {s['question']}" for s in successes[:3]) or "없음"

    prompt = f"""현재 스킬 문서를 분석하고 개선하세요.

[현재 스킬 문서]
{skill_doc}

[실패한 문제 ({len(failures)}건)]
{failures_text}

[성공한 문제 ({len(successes)}건 중 최대 3건)]
{successes_text}

실패 문제를 개선하면서 성공 문제는 유지하도록 스킬 문서를 수정하세요.
반드시 아래 4개 섹션을 포함하세요:
# 교수 원칙 (Teaching Principles)
# 난이도별 전략 (Difficulty Strategy)
# 약점 토픽 대응 전략 (Weak Topic Strategy)
# 피드백 규칙 (Feedback Rules)

수정된 스킬 문서만 출력하세요 (설명 없이):"""

    try:
        edited, tokens = await llm_client.complete_with_usage("", prompt, max_tokens=4096)
        if not validate_skill_content(edited):
            logger.warning("reflect_edit produced invalid skill content — keeping original")
            summary = f"편집 실패(검증 불통과). 실패 {len(failures)}건."
            return skill_doc, summary, tokens
        summary = f"실패 {len(failures)}건 분석 완료. 스킬 문서 업데이트됨."
        return edited, summary, tokens
    except Exception as exc:
        logger.error("reflect_edit failed: %s", exc)
        return skill_doc, f"편집 실패: {exc}", 0
