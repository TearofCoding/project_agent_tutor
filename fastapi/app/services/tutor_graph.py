"""tutor_graph.py — LangGraph 기반 튜터 상태 머신.

개선:
  1. [버그픽스] _entry_router가 START에서 list를 반환해 router + speculative_search 병렬 팬아웃
  2. [비용절감] needs_doc_search()로 공식문서 토픽일 때만 Tavily 호출 (평균 80% 절감)
  3. [정확도] consecutive_wrong >= 3 판단을 LLM 대신 Python으로 처리

타임라인:
  검색 필요 시:  max(Router 2s, Tavily 3s) + Evaluate 6s ≈ 8s  < 10s 예산
  검색 불필요 시: Router 2s + Evaluate 6s ≈ 8s
"""
from langgraph.constants import START
from langgraph.graph import END, StateGraph
from typing_extensions import TypedDict

from app.services.graph_nodes import (
    downgrade_node,
    evaluate_node,
    hint_node,
    merge_node,
    router_node,
    speculative_search_node,
)
from app.services.search_context import needs_doc_search


class TutorState(TypedDict):
    session_id: str
    user_answer: str
    subject_name: str
    current_question: str
    topic_tag: str
    skill_doc: str
    consecutive_wrong: int
    should_search: bool

    # router 결과
    action: str                     # "evaluate" | "hint" | "search" | "downgrade"

    # speculative_search 결과
    search_snippets: list[str]
    search_urls: list[str]

    # 최종 응답
    is_correct: bool | None
    feedback: str
    next_question: str
    next_topic_tag: str


def _entry_router(state: TutorState):
    """START 분기.

    consecutive_wrong >= 3이면 LLM 없이 force_downgrade.
    정상 경로에서는 router는 항상, speculative_search는 needs_doc_search 조건부로 병렬 팬아웃.
    """
    if state.get("consecutive_wrong", 0) >= 3:
        return "force_downgrade"
    branches = ["router"]
    if needs_doc_search(state.get("topic_tag", ""), [state.get("subject_name", "")]):
        branches.append("speculative_search")
    return branches


def _route_after_merge(state: TutorState) -> str:
    action = state.get("action", "evaluate")
    if action == "hint":
        return "hint"
    if action == "downgrade":
        return "downgrade"
    return "evaluate"


def build_graph() -> StateGraph:
    g = StateGraph(TutorState)

    g.add_node("router",             router_node)
    g.add_node("speculative_search", speculative_search_node)
    g.add_node("merge",              merge_node)
    g.add_node("evaluate",           evaluate_node)
    g.add_node("hint",               hint_node)
    g.add_node("downgrade",          downgrade_node)
    g.add_node("force_downgrade",    downgrade_node)  # 동일 노드 재사용

    # START → force_downgrade (선제) 또는 router + speculative_search (병렬 팬아웃)
    g.add_conditional_edges(START, _entry_router, {
        "force_downgrade":    "force_downgrade",
        "router":             "router",
        "speculative_search": "speculative_search",
    })

    g.add_conditional_edges("router",             lambda s: "merge", {"merge": "merge"})
    g.add_conditional_edges("speculative_search", lambda s: "merge", {"merge": "merge"})

    g.add_conditional_edges("merge", _route_after_merge, {
        "evaluate":  "evaluate",
        "hint":      "hint",
        "downgrade": "downgrade",
    })

    g.add_edge("evaluate",        END)
    g.add_edge("hint",            END)
    g.add_edge("downgrade",       END)
    g.add_edge("force_downgrade", END)

    return g.compile()


tutor_graph = build_graph()
