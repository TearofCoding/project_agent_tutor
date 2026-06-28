from anthropic import AsyncAnthropic

from app.core.config import settings


class LLMClient:
    def __init__(self) -> None:
        self._client = AsyncAnthropic(api_key=settings.ANTHROPIC_API_KEY)

    async def complete(self, system: str, user: str, max_tokens: int = 1024) -> str:
        response = await self._client.messages.create(
            model=settings.LLM_MODEL,
            max_tokens=max_tokens,
            system=system,
            messages=[{"role": "user", "content": user}],
        )
        return response.content[0].text

    async def complete_with_usage(self, system: str, user: str, max_tokens: int = 1024) -> tuple[str, int]:
        response = await self._client.messages.create(
            model=settings.LLM_MODEL,
            max_tokens=max_tokens,
            system=system,
            messages=[{"role": "user", "content": user}],
        )
        tokens = response.usage.input_tokens + response.usage.output_tokens
        return response.content[0].text, tokens


llm_client = LLMClient()
