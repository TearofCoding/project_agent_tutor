import pytest
from unittest.mock import AsyncMock, MagicMock, patch

from app.services.skillopt.pipeline import _next_version, maybe_trigger


# ── pure function ──────────────────────────────────────────────────────────────

def test_next_version_increments_major():
    assert _next_version("v1.0") == "v2.0"
    assert _next_version("v2.0") == "v3.0"
    assert _next_version("v10.0") == "v11.0"


def test_next_version_fallback_on_bad_input():
    assert _next_version("unknown") == "v2.0"
    assert _next_version("") == "v2.0"
    assert _next_version("bad-version") == "v2.0"


# ── maybe_trigger condition tests ─────────────────────────────────────────────

def _state(last_count: int = 0) -> MagicMock:
    s = MagicMock()
    s.pipeline_session_count_at_last_run = last_count
    return s


@pytest.mark.asyncio
async def test_skips_when_total_sessions_below_threshold():
    payload = MagicMock()
    payload.total_completed_sessions = 0  # min threshold is 1

    with patch("app.services.skillopt.pipeline._get_validation_set", new_callable=AsyncMock) as mock_vs, \
         patch("app.services.skillopt.pipeline._run", new_callable=AsyncMock) as mock_run:
        mock_vs.return_value = [{"question": "Q?", "expected_answer": "A"}]
        await maybe_trigger(payload, _state())
        mock_run.assert_not_called()


@pytest.mark.asyncio
async def test_skips_when_new_sessions_below_threshold():
    payload = MagicMock()
    payload.total_completed_sessions = 3
    # last_count == total → new_sessions = 0, below threshold of 1

    with patch("app.services.skillopt.pipeline._get_validation_set", new_callable=AsyncMock) as mock_vs, \
         patch("app.services.skillopt.pipeline._run", new_callable=AsyncMock) as mock_run:
        mock_vs.return_value = [{"question": "Q?", "expected_answer": "A"}]
        await maybe_trigger(payload, _state(last_count=3))
        mock_run.assert_not_called()


@pytest.mark.asyncio
async def test_skips_when_validation_set_empty():
    payload = MagicMock()
    payload.total_completed_sessions = 5

    with patch("app.services.skillopt.pipeline._get_validation_set", new_callable=AsyncMock) as mock_vs, \
         patch("app.services.skillopt.pipeline._run", new_callable=AsyncMock) as mock_run:
        mock_vs.return_value = []
        await maybe_trigger(payload, _state())
        mock_run.assert_not_called()


@pytest.mark.asyncio
async def test_runs_when_all_conditions_met():
    payload = MagicMock()
    payload.total_completed_sessions = 5

    with patch("app.services.skillopt.pipeline._get_validation_set", new_callable=AsyncMock) as mock_vs, \
         patch("app.services.skillopt.pipeline._run", new_callable=AsyncMock) as mock_run:
        mock_vs.return_value = [{"question": "Q?", "expected_answer": "A"}]
        await maybe_trigger(payload, _state())
        mock_run.assert_called_once()


@pytest.mark.asyncio
async def test_updates_session_count_after_run():
    payload = MagicMock()
    payload.total_completed_sessions = 5
    state = _state(last_count=0)

    with patch("app.services.skillopt.pipeline._get_validation_set", new_callable=AsyncMock) as mock_vs, \
         patch("app.services.skillopt.pipeline._run", new_callable=AsyncMock):
        mock_vs.return_value = [{"question": "Q?", "expected_answer": "A"}]
        await maybe_trigger(payload, state)
        assert state.pipeline_session_count_at_last_run == 5
