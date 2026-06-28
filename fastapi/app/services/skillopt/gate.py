from app.services.skillopt.rollout import RolloutResult, evaluate as rollout_evaluate


async def evaluate(skill_doc: str, validation_set: list[dict]) -> RolloutResult:
    """Re-evaluate edited skill against the same validation set."""
    return await rollout_evaluate(skill_doc, validation_set)
