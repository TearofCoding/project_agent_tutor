from fastapi import Header, HTTPException

from app.core.config import settings


async def verify_internal_key(x_internal_api_key: str = Header(...)) -> None:
    """Dependency: validates X-Internal-Api-Key on all /internal/* routes."""
    if x_internal_api_key != settings.INTERNAL_API_KEY:
        raise HTTPException(status_code=403, detail="Forbidden")
