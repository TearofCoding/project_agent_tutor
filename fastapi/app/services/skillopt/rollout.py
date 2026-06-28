import json
import logging
import re
from dataclasses import dataclass, field

from app.services.llm_client import llm_client

logger = logging.getLogger(__name__)


@dataclass
class RolloutResult:
    correct_count: int = 0
    total_count: int = 0
    tokens_used: int = 0
    passed_questions: list = field(default_factory=list)
    failed_questions: list = field(default_factory=list)

    @property
    def correct_rate(self) -> float:
        return self.correct_count / self.total_count if self.total_count > 0 else 0.0


def _extract_json(text: str) -> dict:
    text = re.sub(r"```(?:json)?\s*|\s*```", "", text).strip()
    match = re.search(r"\{.*\}", text, re.DOTALL)
    return json.loads(match.group() if match else text)


async def evaluate(skill_doc: str, validation_set: list[dict]) -> RolloutResult:
    result = RolloutResult(total_count=len(validation_set))

    for item in validation_set:
        question = item.get("question", "")
        expected = item.get("expectedAnswer", "")
        is_correct, answer, tokens = await _evaluate_one(skill_doc, question, expected)
        result.tokens_used += tokens
        record = {"question": question, "expected": expected, "answer": answer}
        if is_correct:
            result.correct_count += 1
            result.passed_questions.append(record)
        else:
            result.failed_questions.append(record)

    return result


async def _evaluate_one(skill_doc: str, question: str, expected: str) -> tuple[bool, str, int]:
    prompt = f"""다음 질문에 답변하고, 기대 정답과 일치하는지 판단하세요.

질문: {question}
기대 정답: {expected}

JSON만 출력하세요:
{{"answer": "답변 내용", "is_correct": true}}"""

    try:
        raw, tokens = await llm_client.complete_with_usage(skill_doc, prompt, max_tokens=512)
        data = _extract_json(raw)
        return bool(data.get("is_correct", False)), data.get("answer", ""), tokens
    except Exception as exc:
        logger.warning("rollout _evaluate_one failed: %s", exc)
        return False, "", 0
