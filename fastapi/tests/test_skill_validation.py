from app.services.skill_manager import REQUIRED_SECTIONS, validate_skill_content


def _valid_content() -> str:
    return (
        "# 교수 원칙 (Teaching Principles)\n"
        "질문 후 학생이 스스로 생각하도록 충분한 시간을 준다. 적극적으로 응답을 유도한다.\n\n"
        "# 난이도별 전략 (Difficulty Strategy)\n"
        "초급: 개념 이해 중심, 중급: 응용 문제, 고급: 설계 문제를 출제한다.\n\n"
        "# 약점 토픽 대응 전략 (Weak Topic Strategy)\n"
        "약점 토픽은 반복 출제하여 보강하고, 관련 개념을 함께 설명한다.\n\n"
        "# 피드백 규칙 (Feedback Rules)\n"
        "오답 시 힌트 제공, 정답 시 심화 질문으로 연결한다. 항상 격려한다."
    )


def test_valid_content_passes():
    content = _valid_content()
    assert len(content) >= 200
    assert validate_skill_content(content) is True


def test_too_short_fails():
    content = "\n".join(REQUIRED_SECTIONS)
    assert len(content) < 200
    assert validate_skill_content(content) is False


def test_exactly_200_chars_passes():
    base = "\n".join(REQUIRED_SECTIONS) + "\n"
    content = base + "X" * (200 - len(base))
    assert len(content) == 200
    assert validate_skill_content(content) is True


def test_199_chars_fails():
    base = "\n".join(REQUIRED_SECTIONS) + "\n"
    content = base + "X" * (199 - len(base))
    assert len(content) == 199
    assert validate_skill_content(content) is False


def test_missing_one_section_fails():
    content = _valid_content().replace("# 교수 원칙", "# 교수 방법론")
    assert validate_skill_content(content) is False


def test_missing_all_sections_fails():
    assert validate_skill_content("X" * 300) is False


def test_empty_string_fails():
    assert validate_skill_content("") is False
