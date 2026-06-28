import logging
from contextlib import asynccontextmanager

from fastapi import FastAPI

from app.routers import internal_sessions, internal_skills
from app.services.skill_manager import load_skill_from_springboot

logger = logging.getLogger(__name__)


@asynccontextmanager
async def lifespan(app: FastAPI):
    content, version = await load_skill_from_springboot()
    app.state.skill_doc = content
    app.state.skill_version = version
    app.state.pipeline_session_count_at_last_run = 0
    if not content:
        logger.warning("skill_doc is empty — session start will return 503 until a skill is initialized")
    yield


app = FastAPI(
    title="AI Agent Tutor — FastAPI",
    version="0.1.0",
    docs_url=None,
    redoc_url=None,
    lifespan=lifespan,
)

app.include_router(internal_sessions.router)
app.include_router(internal_skills.router)


@app.get("/internal/health")
async def health() -> dict:
    return {"status": "ok"}
