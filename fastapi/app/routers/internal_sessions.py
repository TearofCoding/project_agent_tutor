import time
import uuid

from fastapi import APIRouter, BackgroundTasks, Depends, HTTPException, Request

from app.core.security import verify_internal_key
from app.schemas.session import AskPayload, EndPayload, PersonalizationContext, SessionResponse
from app.services import session_llm
from app.services.skillopt import pipeline

router = APIRouter(
    prefix="/internal/sessions",
    dependencies=[Depends(verify_internal_key)],
)


def _skill_doc(request: Request) -> str:
    doc = getattr(request.app.state, "skill_doc", "")
    if not doc:
        raise HTTPException(status_code=503, detail="스킬 문서가 초기화되지 않았습니다.")
    return doc


def _skill_version(request: Request) -> str:
    return getattr(request.app.state, "skill_version", "unknown")


@router.post("/start", response_model=SessionResponse)
async def start_session(context: PersonalizationContext, request: Request):
    skill_doc = _skill_doc(request)
    start_ms = time.time()

    problem = await session_llm.generate_first_question(skill_doc, context)

    return SessionResponse(
        session_id=str(context.session_id),
        interaction_id=str(uuid.uuid4()),
        next_problem=problem,
        skill_version=_skill_version(request),
        response_time_ms=int((time.time() - start_ms) * 1000),
    )


@router.post("/{session_id}/ask", response_model=SessionResponse)
async def ask(session_id: str, payload: AskPayload, request: Request):
    skill_doc = _skill_doc(request)
    start_ms = time.time()

    is_correct, feedback, next_problem = await session_llm.evaluate_answer(
        skill_doc, payload, session_id
    )

    return SessionResponse(
        session_id=session_id,
        interaction_id=str(uuid.uuid4()),
        is_correct=is_correct,
        feedback=feedback,
        next_problem=next_problem,
        skill_version=_skill_version(request),
        response_time_ms=int((time.time() - start_ms) * 1000),
    )


@router.post("/{session_id}/end", response_model=SessionResponse)
async def end_session(
    session_id: str,
    payload: EndPayload,
    request: Request,
    background_tasks: BackgroundTasks,
):
    skill_doc = _skill_doc(request)
    start_ms = time.time()

    recommendation = await session_llm.generate_recommendation(skill_doc, payload, session_id)

    app_state = request.app.state
    background_tasks.add_task(pipeline.maybe_trigger, payload, app_state)

    return SessionResponse(
        session_id=session_id,
        interaction_id=str(uuid.uuid4()),
        recommendation=recommendation,
        skill_version=_skill_version(request),
        response_time_ms=int((time.time() - start_ms) * 1000),
    )
