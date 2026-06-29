import os

from dotenv import load_dotenv

load_dotenv()


class Settings:
    INTERNAL_API_KEY: str = os.getenv("INTERNAL_API_KEY", "")
    ANTHROPIC_API_KEY: str = os.getenv("ANTHROPIC_API_KEY", "")
    LLM_MODEL: str = os.getenv("LLM_MODEL", "claude-sonnet-4-6")
    SPRINGBOOT_BASE_URL: str = os.getenv("SPRINGBOOT_BASE_URL", "http://springboot:8080")
    SKILLOPT_MIN_TOTAL_SESSIONS: int = int(os.getenv("SKILLOPT_MIN_TOTAL_SESSIONS", "1"))
    SKILLOPT_MIN_NEW_SESSIONS: int = int(os.getenv("SKILLOPT_MIN_NEW_SESSIONS", "1"))
    SKILLOPT_MAX_VALIDATION_SIZE: int = int(os.getenv("SKILLOPT_MAX_VALIDATION_SIZE", "5"))
    TAVILY_API_KEY: str = os.getenv("TAVILY_API_KEY", "")
    TAVILY_ENABLED: bool = os.getenv("TAVILY_ENABLED", "false").lower() == "true"


settings = Settings()
