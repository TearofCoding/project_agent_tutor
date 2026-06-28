import asyncio
import logging

import httpx

from app.core.config import settings

logger = logging.getLogger(__name__)

REQUIRED_SECTIONS = [
    "# 교수 원칙",
    "# 난이도별 전략",
    "# 약점 토픽 대응 전략",
    "# 피드백 규칙",
]


def validate_skill_content(content: str) -> bool:
    if len(content) < 200:
        return False
    return all(section in content for section in REQUIRED_SECTIONS)


async def load_skill_from_springboot() -> tuple[str, str]:
    """Returns (content, version). Both empty string on failure."""
    url = f"{settings.SPRINGBOOT_BASE_URL}/api/skills/active"
    headers = {"X-Internal-Api-Key": settings.INTERNAL_API_KEY}

    for attempt in range(1, 4):
        try:
            async with httpx.AsyncClient(timeout=10.0) as client:
                response = await client.get(url, headers=headers)
            if response.status_code == 200:
                data = response.json().get("data", {})
                content = data.get("content", "")
                version = data.get("version", "unknown")
                logger.info("skill_doc loaded (version=%s, length=%d)", version, len(content))
                return content, version
            logger.warning(
                "GET /api/skills/active returned %d (attempt %d)",
                response.status_code,
                attempt,
            )
        except Exception as exc:
            logger.warning("skill_doc load failed (attempt %d): %s", attempt, exc)

        if attempt < 3:
            await asyncio.sleep(5)

    logger.warning("skill_doc could not be loaded after 3 attempts — set to empty")
    return "", ""
