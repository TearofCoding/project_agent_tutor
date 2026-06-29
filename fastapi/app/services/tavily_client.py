"""Tavily 검색 클라이언트 래퍼.

TAVILY_ENABLED=false 이면 모든 메서드가 빈 결과를 반환한다.
"""
import logging
from dataclasses import dataclass, field

import httpx

from app.core.config import settings

logger = logging.getLogger(__name__)

_BASE_URL = "https://api.tavily.com/search"
_TIMEOUT = 3.0  # 10초 예산 중 Tavily에 할당하는 최대값


@dataclass
class SearchResult:
    snippets: list[str] = field(default_factory=list)
    urls: list[str] = field(default_factory=list)

    def to_context(self, max_chars: int = 400) -> str:
        """스니펫을 LLM 프롬프트에 삽입할 텍스트로 변환."""
        joined = "\n".join(self.snippets)
        return joined[:max_chars] if joined else ""


class TavilyClient:
    def __init__(self) -> None:
        self._enabled = settings.TAVILY_ENABLED
        self._api_key = settings.TAVILY_API_KEY

    async def search(self, query: str, max_results: int = 2) -> SearchResult:
        """비동기 검색. 비활성화·오류 시 빈 SearchResult 반환 (폴백 보장)."""
        if not self._enabled or not self._api_key:
            return SearchResult()

        payload = {
            "api_key": self._api_key,
            "query": query,
            "max_results": max_results,
            "search_depth": "basic",
            "include_answer": False,
        }
        try:
            async with httpx.AsyncClient(timeout=_TIMEOUT) as client:
                resp = await client.post(_BASE_URL, json=payload)
                resp.raise_for_status()
                data = resp.json()
                results = data.get("results", [])
                return SearchResult(
                    snippets=[r.get("content", "")[:300] for r in results],
                    urls=[r.get("url", "") for r in results],
                )
        except Exception as exc:
            logger.warning("Tavily search failed (query=%r): %s", query, exc)
            return SearchResult()


tavily_client = TavilyClient()
