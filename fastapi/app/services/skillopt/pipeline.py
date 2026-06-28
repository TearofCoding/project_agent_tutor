import logging

import httpx

from app.core.config import settings
from app.schemas.session import EndPayload
from app.services.skillopt import gate, reflect_edit, rollout

logger = logging.getLogger(__name__)

MAX_TOKEN_BUDGET = 50_000


async def maybe_trigger(payload: EndPayload, state) -> None:
    """Check conditions and run pipeline if met."""
    try:
        validation_set = await _get_validation_set()
        total = payload.total_completed_sessions
        new_sessions = total - getattr(state, "pipeline_session_count_at_last_run", 0)

        if not (
            total >= settings.SKILLOPT_MIN_TOTAL_SESSIONS
            and new_sessions >= settings.SKILLOPT_MIN_NEW_SESSIONS
            and len(validation_set) >= 1
        ):
            logger.warning(
                "SkillOpt skipped: total=%d new=%d validation=%d",
                total, new_sessions, len(validation_set),
            )
            return

        await _run(state, validation_set)
        state.pipeline_session_count_at_last_run = total
    except Exception as exc:
        logger.error("SkillOpt pipeline error: %s", exc, exc_info=True)


async def force_run(state) -> None:
    """Skip condition checks — run unconditionally (requires ≥1 validation question)."""
    try:
        validation_set = await _get_validation_set()
        if not validation_set:
            logger.warning("SkillOpt force run: no active validation questions")
            return
        await _run(state, validation_set)
    except Exception as exc:
        logger.error("SkillOpt force run error: %s", exc, exc_info=True)


async def _run(state, validation_set: list[dict]) -> None:
    current_skill = state.skill_doc
    current_version = getattr(state, "skill_version", "unknown")

    logger.info("SkillOpt starting (version=%s, validation=%d)", current_version, len(validation_set))

    # Stage 1: Rollout — evaluate current skill
    before = await rollout.evaluate(current_skill, validation_set)
    logger.info("Rollout: %.1f%% (%d/%d)", before.correct_rate * 100, before.correct_count, before.total_count)

    # Stage 2: Reflect + Edit
    edited_skill, reflect_summary, edit_tokens = await reflect_edit.run(current_skill, before)

    # Stage 3: Gate — re-evaluate edited skill
    after = await gate.evaluate(edited_skill, validation_set)
    logger.info("Gate: %.1f%% (before=%.1f%%)", after.correct_rate * 100, before.correct_rate * 100)

    total_tokens = before.tokens_used + edit_tokens + after.tokens_used
    adopted = after.correct_rate >= before.correct_rate
    status = "SUCCESS" if adopted else "FAILED"
    next_ver = _next_version(current_version)

    saved_version = await _save_result({
        "version": next_ver,
        "skill_content": edited_skill if adopted else current_skill,
        "status": status,
        "correct_rate_before": before.correct_rate,
        "correct_rate_after": after.correct_rate,
        "reflect_summary": reflect_summary,
        "tokens_used": total_tokens,
        "budget_exceeded": total_tokens > MAX_TOKEN_BUDGET,
    })

    if adopted:
        state.skill_doc = edited_skill
        state.skill_version = saved_version or next_ver
        logger.info("SkillOpt ADOPTED %s (%.1f%% → %.1f%%)",
                    saved_version, before.correct_rate * 100, after.correct_rate * 100)
    else:
        logger.info("SkillOpt REJECTED (%.1f%% → %.1f%%)",
                    before.correct_rate * 100, after.correct_rate * 100)


def _next_version(current: str) -> str:
    try:
        major = int(current.lstrip("v").split(".")[0])
        return f"v{major + 1}.0"
    except (ValueError, IndexError):
        return "v2.0"


async def _get_validation_set() -> list[dict]:
    url = f"{settings.SPRINGBOOT_BASE_URL}/api/skills/validation-set/active"
    headers = {"X-Internal-Api-Key": settings.INTERNAL_API_KEY}
    async with httpx.AsyncClient(timeout=10.0) as client:
        response = await client.get(url, headers=headers)
        response.raise_for_status()
        return response.json().get("data", [])


async def _save_result(result: dict) -> str | None:
    url = f"{settings.SPRINGBOOT_BASE_URL}/api/skills/pipeline/result"
    headers = {"X-Internal-Api-Key": settings.INTERNAL_API_KEY}
    async with httpx.AsyncClient(timeout=15.0) as client:
        response = await client.post(url, headers=headers, json=result)
        if response.status_code in (200, 201):
            return response.json().get("data", {}).get("version")
    logger.warning("_save_result: SpringBoot returned %d", response.status_code)
    return None
