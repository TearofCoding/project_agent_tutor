from fastapi import APIRouter, BackgroundTasks, Depends, Request

from app.core.security import verify_internal_key
from app.services.skillopt import pipeline

router = APIRouter(
    prefix="/internal/skills",
    dependencies=[Depends(verify_internal_key)],
)


@router.post("/pipeline/trigger")
async def force_trigger_pipeline(request: Request, background_tasks: BackgroundTasks) -> dict:
    background_tasks.add_task(pipeline.force_run, request.app.state)
    return {"status": "pipeline triggered"}
