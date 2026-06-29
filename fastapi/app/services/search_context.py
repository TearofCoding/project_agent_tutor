"""Tavily 검색 트리거 조건 판별 및 쿼리 생성 모듈.

session_llm.py 의 라인 수를 줄이기 위해 분리.
"""

# 공식 문서 검색이 의미 있는 토픽 키워드 목록
_OFFICIAL_DOC_KEYWORDS: set[str] = {
    "record", "sealed", "pattern matching", "switch expression",
    "virtual thread", "stream api", "optional", "generic",
    "annotation", "reflection", "module system",
    "쿼리 최적화", "인덱스", "트랜잭션", "실행 계획",
    "asyncio", "type hint", "dataclass", "pydantic",
}


def needs_doc_search(topic_tag: str, weak_topics: list[str]) -> bool:
    """공식 문서 검색 여부를 결정한다.

    topic_tag 또는 weak_topics 중 하나라도 _OFFICIAL_DOC_KEYWORDS 에 포함되면 True.
    """
    tag_lower = topic_tag.lower()
    combined = {tag_lower} | {t.lower() for t in (weak_topics or [])}
    return bool(combined & _OFFICIAL_DOC_KEYWORDS)


def build_doc_query(topic_tag: str, subject_name: str) -> str:
    """문제 생성용 검색 쿼리."""
    return f"{subject_name} {topic_tag} 공식 문서 예시 site:docs.oracle.com OR site:dev.java OR site:baeldung.com"


def build_feedback_query(topic_tag: str, subject_name: str) -> str:
    """오답 피드백용 검색 쿼리."""
    return f"{subject_name} {topic_tag} 개념 설명 예시 초보자"


def format_search_block(context_text: str, urls: list[str]) -> str:
    """LLM 프롬프트에 삽입할 검색 결과 블록."""
    if not context_text:
        return ""
    url_line = f"\n참고: {urls[0]}" if urls else ""
    return f"\n\n[웹 검색 참고자료]{url_line}\n{context_text}\n"
