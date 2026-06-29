from __future__ import annotations

from pydantic import BaseModel


class PersonalizationContext(BaseModel):
    session_id: int
    user_id: int
    declared_level: str
    effective_level: str
    selected_difficulty: str
    subject_id: int
    subject_name: str
    weak_topics: list[str] = []
    recent_correct_rate: float = 0.0
    total_session_count: int = 0
    last_session_subject: str | None = None
    goal: str | None = None


class AskPayload(BaseModel):
    user_answer: str
    current_question: str
    current_topic_tag: str | None = None
    subject_name: str
    selected_difficulty: str
    effective_level: str
    weak_topics: list[str] = []
    goal: str | None = None
    interaction_count: int = 0
    consecutive_wrong: int = 0


class EndPayload(BaseModel):
    session_id: int
    subject_name: str
    correct_rate: float
    total_interactions: int
    weak_topics: list[str] = []
    goal: str | None = None
    total_completed_sessions: int = 0


class ProblemOut(BaseModel):
    question: str
    topic_tag: str


class SessionResponse(BaseModel):
    session_id: str
    interaction_id: str
    is_correct: bool | None = None
    feedback: str | None = None
    next_problem: ProblemOut | None = None
    recommendation: str | None = None
    skill_version: str
    response_time_ms: int
