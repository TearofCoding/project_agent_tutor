"""internal_sessions.py — LangGraph 연동 FastAPI 라우터."""
import time
import uuid

from fastapi import APIRouter, BackgroundTasks, Depends, HTTPException, Request

from app.core.security import verify_internal_key
from app.schemas.session import AskPayload, EndPayload, PersonalizationContext, ProblemOut, SessionResponse
from app.services.search_context import needs_doc_search
from app.services.session_llm import generate_first_question, generate_recommendation
from app.services.skillopt import pipeline
from app.services.tutor_graph import TutorState, tutor_graph

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
async def session_start(payload: PersonalizationContext, request: Request):
    skill_doc = _skill_doc(request)
    start_ms  = time.time()
    problem   = await generate_first_question(skill_doc, payload)
    return SessionResponse(
        session_id=str(payload.session_id),
        interaction_id=str(uuid.uuid4()),
        next_problem=problem,
        skill_version=_skill_version(request),
        response_time_ms=int((time.time() - start_ms) * 1000),
    )


@router.post("/{session_id}/ask", response_model=SessionResponse)
async def session_ask(session_id: str, payload: AskPayload, request: Request):
    skill_doc     = _skill_doc(request)
    start_ms      = time.time()
    topic         = payload.current_topic_tag or ""
    should_search = needs_doc_search(topic, [payload.subject_name])

    state: TutorState = {
        "session_id":        session_id,
        "user_answer":       payload.user_answer,
        "subject_name":      payload.subject_name,
        "current_question":  payload.current_question,
        "topic_tag":         topic,
        "skill_doc":         skill_doc,
        "consecutive_wrong": payload.consecutive_wrong,
        "should_search":     should_search,
        "action":            "",
        "search_snippets":   [],
        "search_urls":       [],
        "is_correct":        None,
        "feedback":          "",
        "next_question":     "",
        "next_topic_tag":    "",
    }

    result     = await tutor_graph.ainvoke(state)
    elapsed_ms = int((time.time() - start_ms) * 1000)

    return SessionResponse(
        session_id=session_id,
        interaction_id=str(uuid.uuid4()),
        is_correct=result["is_correct"],
        feedback=result["feedback"],
        next_problem=ProblemOut(
            question=result["next_question"],
            topic_tag=result["next_topic_tag"],
        ),
        skill_version=_skill_version(request),
        response_time_ms=elapsed_ms,
    )


@router.post("/{session_id}/end")
async def session_end(
    session_id: str,
    payload: EndPayload,
    request: Request,
    background_tasks: BackgroundTasks,
):
    skill_doc      = _skill_doc(request)
    recommendation = await generate_recommendation(skill_doc, payload, session_id)
    background_tasks.add_task(pipeline.maybe_trigger, payload, request.app.state)
    return {"recommendation": recommendation}
