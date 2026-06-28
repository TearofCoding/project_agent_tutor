# AI Agent Tutor — Claude Code 구현 지침서

**근거 문서:** 기술 명세서 TS-2025-001 v1.9  
**구현 범위:** 1단계 (인증 + 학습 세션 + SkillOpt 파이프라인)  
**Python 환경:** uv 가상환경

---

## 0. AI Engineering Rules

모든 소프트웨어 엔지니어링 작업(구현, 디버깅, 코드 리뷰, 리팩토링, 아키텍처 설계, 테스트, 문서화, 분석)을 시작하기 전에 아래 Skill 선택 정책을 따른다.  
**번들 Skill은 항상 ad-hoc 추론보다 우선한다.**

### Skill Selection Policy

#### Phase 1 — Engineering Reasoning (항상 적용)

모든 프로그래밍 작업은 **karpathy-skillss**로 시작한다.

- 문제를 이해한다.
- 가정과 제약을 파악한다.
- 태스크를 분해한다.
- 구현 전에 계획을 수립한다.
- 가장 단순한 정답 접근법을 선택한다.
- 섣부른 최적화를 피한다.
- 코딩 전에 추론을 우선한다.

> karpathy-skillss을 모든 프로그래밍 태스크의 **기본 엔지니어링 워크플로우**로 간주한다.

#### Phase 2 — Implementation Quality (항상 적용)

구현 계획 수립 후 반드시 **ponytail**을 적용한다.

- 가독성 향상
- 유지보수성 향상
- 불필요한 복잡도 제거
- 네이밍 개선
- 프로젝트 일관성 확보
- 클린코드 원칙 준수
- 모듈 설계 장려
- 합리적 범위 내 기술 부채 제거

> ponytail을 모든 코드의 **기본 품질 검토 단계**로 간주한다.

#### Phase 3 — Context-Specific Skills

기본 워크플로우 이후, 상황에 맞는 추가 번들 Skill을 판단하여 적용한다.

**`/debug` — 자동 적용 조건:**

- 컴파일 에러 / 런타임 에러 / 테스트 실패
- 예상치 못한 동작 / 변경 후 회귀 / 성능 저하
- 크래시 / 잘못된 출력
- 사용자가 명시적으로 디버깅 요청

워크플로우:
1. 이슈 재현
2. 증거 수집
3. 근본 원인 파악
4. 이슈 발생 이유 설명
5. 가장 작고 정확한 수정 적용
6. 수정 검증
7. 회귀 없음 확인

> **추측하지 않는다. 항상 진단 후 수정한다.**

**`/simplify` — 자동 적용 조건:**

- 솔루션이 불필요하게 복잡한 경우
- 중복 로직이 존재하는 경우
- 구현을 더 짧게 만들 수 있는 경우
- 가독성 향상이 가능한 경우
- 리팩토링이 요청된 경우
- 더 단순한 설계가 존재하는 경우
- 다수의 추상화가 거의 가치를 제공하지 않는 경우
- 안전하게 보일러플레이트를 제거할 수 있는 경우

목표: 인지 부하 감소 / 명시적 코드 선호 / 직관적 제어 흐름 / 불필요한 추상화 제거 / 데드 코드 제거 / 동작 유지

> **정확성이 보장될 때만 단순화한다.**

### Recommended Skill Order

| 작업 유형 | 순서 |
|-----------|------|
| 일반 구현 | karpathy-skills → ponytail |
| 버그 수정 | karpathy-skills → /debug → ponytail → /simplify (필요 시) |
| 리팩토링 | karpathy-skills → ponytail → /simplify |
| 코드 리뷰 | karpathy-skills → ponytail → /simplify (필요 시) |
| 아키텍처/설계 | karpathy-skills → ponytail |

### Coding Philosophy

- 코딩 전에 생각한다.
- 가정이 아닌 증거를 우선한다.
- 단순한 솔루션을 선호한다.
- 읽기 쉬운 코드를 작성한다.
- 함수는 한 가지에 집중한다.
- 불필요한 의존성을 최소화한다.
- 의도적 변경이 아니면 기존 동작을 유지한다.
- 자명하지 않은 중요한 설계 결정은 설명을 남긴다.
- 여러 Skill이 결과를 개선할 수 있다면 함께 사용한다.
- 적용 가능한 번들 Skill이 없으면 일반 추론을 계속한다.

### Testing Philosophy

- 구현 완료 후 각 Phase 끝에서 **smoke test**(curl 또는 pytest)를 실행한다.
- smoke test를 통과한 뒤 다음 Phase로 넘어간다.
- 버그 수정 시 수정 전 실패 → 수정 후 통과를 반드시 확인한다.
- 회귀가 없음을 검증한다.

### Planning Workflow

1. karpathy-skills로 문제를 이해하고 태스크를 분해한다.
2. 구현 순서(Phase)를 계획한다.
3. 각 Phase에 적합한 Skill을 사전에 선택한다.
4. Phase별 smoke test 기준을 명확히 정의한다.
5. 완료 기준을 충족한 후에만 다음 Phase로 진행한다.

---

## 1. Project Overview

**프로젝트:** AI Agent Tutor  
**목표:** 개인화된 AI 학습 튜터 시스템 구축  
**구현 범위:** 1단계 — 인증 + 학습 세션 + SkillOpt 파이프라인  
**근거 문서:** 기술 명세서 TS-2025-001 v1.9

**핵심 아키텍처:**
- **SpringBoot** (Java 17 / Spring 3.x) — 인증, 세션 관리, DB 접근
- **FastAPI** (Python 3.11) — AI 추론, SkillOpt 파이프라인
- **MySQL** — 유일한 데이터 저장소 (FastAPI는 직접 접근 불가)
- **Nginx** — 리버스 프록시, /internal/* 외부 차단

---

## 2. Absolute Project Rules

- **1단계 항목만 구현한다.** 명세서에 "2단계"로 표기된 항목(Kakao OAuth, 비밀번호 재설정, api_logs, email_verifications 테이블, problems 테이블, LogCleanupScheduler 등)은 코드를 작성하지 않는다. 클래스 껍데기도 만들지 않는다.
- **FastAPI는 MySQL에 직접 접근하지 않는다.** 모든 데이터 저장은 SpringBoot → MySQL 경유.
- **환경변수는 하드코딩하지 않는다.** 모든 시크릿은 `.env` 파일과 Docker Compose `environment` 블록으로만 관리한다.
- 구현 완료 후 각 Phase 끝에서 **smoke test**(curl 또는 pytest)를 실행하여 통과를 확인한 뒤 다음 Phase로 넘어간다.

---

## 3. Development Environment

### Python 환경 (uv)

FastAPI 작업 전 반드시 실행:

```bash
# uv 설치 (최초 1회)
curl -LsSf https://astral.sh/uv/install.sh | sh

# fastapi/ 디렉토리에서 가상환경 생성 및 의존성 설치
cd fastapi/
uv venv --python 3.11
uv pip install fastapi uvicorn anthropic httpx python-dotenv pytest pytest-asyncio
```

`fastapi/pyproject.toml`을 생성하여 의존성을 고정한다:

```toml
[project]
name = "ai-agent-tutor-fastapi"
version = "0.1.0"
requires-python = ">=3.11"
dependencies = [
    "fastapi>=0.111.0",
    "uvicorn[standard]>=0.29.0",
    "anthropic>=0.28.0",
    "httpx>=0.27.0",
    "python-dotenv>=1.0.0",
]

[project.optional-dependencies]
dev = ["pytest>=8.0.0", "pytest-asyncio>=0.23.0", "httpx>=0.27.0"]
```

이후 의존성 추가 시:

```bash
cd fastapi/
uv pip install <패키지명>
uv pip freeze > requirements.txt   # Docker 빌드용
```

---

## 4. Project Structure

```
ai-agent-tutor/
├── springboot/          # SpringBoot 백엔드 (Java 17 / Spring 3.x)
├── fastapi/             # FastAPI AI 서버 (Python 3.11)
├── nginx/               # Nginx 설정
│   ├── nginx.conf       # 공통
│   ├── nginx.dev.conf   # 개발 (Rate Limiting 비활성)
│   └── nginx.prod.conf  # 운영 (Rate Limiting 활성)
├── docker-compose.yml
├── docker-compose.dev.yml
└── .env.example
```

---

## 5. Implementation Workflow

### Phase 1 — 인프라 골격

**목표:** 4개 컨테이너가 기동 순서대로 뜨고 헬스체크를 통과한다.

**작업 목록:**
1. `docker-compose.yml` 작성 — mysql → springboot → fastapi → nginx 순서, `healthcheck` 포함
2. `docker-compose.dev.yml` 작성 — mailhog, nginx.dev.conf 마운트, 개발 환경변수 오버라이드
3. `.env.example` 작성 — 아래 환경변수 전체 포함

**필수 환경변수 목록:**

| 변수명 | 설명 |
|--------|------|
| `MYSQL_ROOT_PASSWORD` | MySQL root 비밀번호 |
| `MYSQL_DATABASE` | DB명 (`ai_tutor`) |
| `SPRING_DATASOURCE_URL` | `jdbc:mysql://mysql:3306/ai_tutor?...` |
| `SPRING_DATASOURCE_USERNAME` | DB 사용자 |
| `SPRING_DATASOURCE_PASSWORD` | DB 비밀번호 |
| `JWT_SECRET` | Access Token 서명 키 (32자 이상) |
| `JWT_ACCESS_EXPIRY_MS` | `1800000` (30분) |
| `JWT_REFRESH_EXPIRY_MS` | `604800000` (7일) |
| `INTERNAL_API_KEY` | SpringBoot↔FastAPI 내부 인증 키 (32자 이상) |
| `FASTAPI_BASE_URL` | `http://fastapi:8000` |
| `ANTHROPIC_API_KEY` | Claude API 키 (FastAPI 컨테이너에만 주입) |
| `LLM_MODEL` | `claude-sonnet-4-6` |
| `EMAIL_VERIFICATION_ENABLED` | `false` (1단계) |
| `GOOGLE_CLIENT_ID` | Google OAuth 클라이언트 ID |
| `GOOGLE_CLIENT_SECRET` | Google OAuth 클라이언트 시크릿 |
| `SKILLOPT_MIN_TOTAL_SESSIONS` | `1` (개발) / `5` (운영) |
| `SKILLOPT_MIN_NEW_SESSIONS` | `1` (개발) / `3` (운영) |
| `SKILLOPT_MAX_VALIDATION_SIZE` | `5` (개발) / `30` (운영) |

**Nginx 필수 설정 (nginx.conf):**
```nginx
# /internal/* 외부 차단 — 반드시 포함
location /internal/ {
    deny all;
}
```

**smoke test:**
```bash
docker compose -f docker-compose.yml -f docker-compose.dev.yml up -d
curl http://localhost/actuator/health        # {"status":"UP"}
curl http://localhost:8000/internal/health   # 외부 차단 확인 → Nginx가 deny
```

---

### Phase 2 — DB 스키마 + Seed

**목표:** 1단계 테이블 8개가 생성되고 subjects seed 데이터가 들어간다.

**작업 목록:**
1. `springboot/src/main/resources/db/migration/` 또는 `schema.sql` 작성
2. 1단계 테이블만 생성: `users`, `user_profiles`, `refresh_tokens`, `subjects`, `learning_sessions`, `session_interactions`, `skill_versions`, `skill_validation_set`
3. `subjects` seed 스크립트: Java, Python, MySQL 3개 과목 INSERT
4. JPA Entity 클래스 작성 — `domain` 패키지, `@Entity` + `@Table` 매핑
5. `calculated_level` 산정 로직 구현 (SessionService 또는 별도 LevelCalculator):
   - 최근 3세션 `correct_rate` 평균 ≥ 70% → `level +1`
   - ≤ 40% → `level -1`
   - 세션 수 < 3 → `NULL` (declared_level 사용)
6. 인덱스: `INDEX idx_sessions_timeout (status, last_activity_at)` 포함

**smoke test:**
```bash
# 테이블 존재 확인
docker exec -it mysql mysql -u root -p ai_tutor -e "SHOW TABLES;"
# subjects seed 확인
docker exec -it mysql mysql -u root -p ai_tutor -e "SELECT * FROM subjects;"
```

---

### Phase 3 — SpringBoot 인증 (JWT + Google OAuth)

**목표:** 회원가입 → 로그인 → Access Token 발급 → 갱신 → 로그아웃 흐름이 동작한다.

**작업 목록:**

`security` 패키지:
- `JwtTokenProvider` — HS256, Access(30분) + Refresh(UUID, 7일)
- `SecurityConfig` — Spring Security Filter Chain
- `OAuth2UserService` — Google provider, 최초 로그인 시 계정 자동 생성

`controller` + `service`:
- `AuthController` — register, login, refresh, logout, verify-email
- `UserController` — GET /api/users/me, PUT /api/users/me/profile, GET /api/users/me/history, GET /api/users/me/history/{sessionId}

**구현 규칙:**
- 비밀번호: `BCryptPasswordEncoder(10)`
- `email_verified`: 1단계는 `DEFAULT TRUE` (EMAIL_VERIFICATION_ENABLED=false)
- Refresh Token Rotation: 재발급 시 기존 토큰 revoke, 신규 발급
- `ROLE_ADMIN`은 DB 직접 설정으로만 부여 (API 없음)

**smoke test:**
```bash
# 회원가입
curl -X POST http://localhost/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"test@test.com","password":"Test1234!","name":"테스터"}'

# 로그인
curl -X POST http://localhost/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"test@test.com","password":"Test1234!"}'
# → accessToken, refreshToken 반환 확인

# 내 프로파일 조회
curl http://localhost/api/users/me \
  -H "Authorization: Bearer {accessToken}"
```

---

### Phase 4 — FastAPI 골격 + SkillOpt 초기화

**목표:** FastAPI가 기동 시 SpringBoot에서 `best_skill.md`를 로드하고, `app.state.skill_doc`에 저장한다.

**작업 목록:**

```
fastapi/
├── app/
│   ├── main.py           # lifespan, app 생성
│   ├── core/
│   │   ├── config.py     # 환경변수 로드
│   │   └── security.py   # X-Internal-Api-Key 검증 Depends
│   ├── routers/
│   │   ├── internal_sessions.py
│   │   └── internal_skills.py
│   └── services/
│       ├── skill_manager.py   # best_skill.md 로드/갱신
│       └── llm_client.py      # Anthropic SDK 래퍼
├── pyproject.toml
└── Dockerfile
```

`main.py` lifespan:
1. GET `http://springboot:8080/api/skills/active` 호출 (X-Internal-Api-Key 포함)
2. 성공 → `app.state.skill_doc = skill_content`
3. 실패(최대 3회 재시도, 5초 간격) → `app.state.skill_doc = ""`
4. `skill_doc == ""` 상태에서 세션 시작 요청 시 503 반환

SpringBoot에 추가:
- `GET /api/skills/active` — INTERNAL 권한, `skill_versions` 테이블에서 최신 `SUCCESS` 버전 반환
- `POST /api/skills/initialize` — 초기 `best_skill.md` 등록 (ADMIN)

`best_skill.md` 필수 4개 섹션:
```markdown
# 교수 원칙 (Teaching Principles)
# 난이도별 전략 (Difficulty Strategy)
# 약점 토픽 대응 전략 (Weak Topic Strategy)
# 피드백 규칙 (Feedback Rules)
```
초기화 시 검증: 4개 헤더 존재 + 200자 이상. 미달 시 400.

**smoke test:**
```bash
# 초기 스킬 등록 (ADMIN 토큰 필요)
curl -X POST http://localhost/api/skills/initialize \
  -H "Authorization: Bearer {adminToken}" \
  -H "Content-Type: application/json" \
  -d '{"skill_content": "# 교수 원칙 ...(200자 이상)"}'

# FastAPI 재기동 후 로드 확인
docker compose restart fastapi
docker logs fastapi | grep "skill_doc"
```

---

### Phase 5 — 학습 세션 (SpringBoot ↔ FastAPI 연동)

**목표:** 세션 시작 → 문제 생성 → 답변 제출 → 피드백 → 세션 종료 전체 흐름이 동작한다.

**작업 목록:**

SpringBoot:
- `SessionController` — POST /api/sessions, POST /api/sessions/{id}/interactions, POST /api/sessions/{id}/end, GET /api/sessions/{id}
- `SessionService` — 세션 생성, `AIAgentClient` 호출, 결과 저장, `calculated_level` 재산정
- `AIAgentClient` (`RestClient` 기반):
  - `startSession(...)` → 동기
  - `submitAnswer(...)` → 동기
  - `endSession(...)` → `@Async` fire-and-forget
  - 연결 타임아웃 3초, 응답 타임아웃 10초
  - 타임아웃/5xx → 1회 재시도 후 폴백 응답
- `SessionTimeoutScheduler` — `@Scheduled(fixedDelay=300000)`, 30분 비활성 세션 TIMEOUT 처리

FastAPI:
- `POST /internal/sessions/start` — 개인화 컨텍스트 수신, 초기 문제 생성 (LLM 호출)
- `POST /internal/sessions/{id}/ask` — 답변 평가, 피드백, 다음 문제 반환
- `POST /internal/sessions/{id}/end` — SkillOpt 파이프라인 비동기 트리거, recommendation 반환

**개인화 컨텍스트 페이로드** (SpringBoot → FastAPI):
```json
{
  "user_id": 1,
  "declared_level": "BEGINNER",
  "effective_level": "INTERMEDIATE",
  "selected_difficulty": "INTERMEDIATE",
  "subject_id": 1,
  "subject_name": "Java",
  "weak_topics": ["상속", "인터페이스"],
  "recent_correct_rate": 0.65,
  "total_session_count": 5,
  "last_session_subject": "Java",
  "goal": "백엔드 개발자 취업"
}
```

**응답 포맷** (FastAPI → SpringBoot):
```json
{
  "session_id": "1",
  "interaction_id": "uuid",
  "is_correct": true,
  "feedback": "정답입니다. ...",
  "next_problem": {"question": "...", "topic_tag": "상속"},
  "recommendation": null,
  "skill_version": "v1.0",
  "response_time_ms": 1234
}
```

**smoke test:**
```bash
# 세션 시작
curl -X POST http://localhost/api/sessions \
  -H "Authorization: Bearer {token}" \
  -H "Content-Type: application/json" \
  -d '{"subject_id": 1, "selected_difficulty": "BEGINNER"}'
# → session_id, 첫 문제 반환 확인

# 답변 제출
curl -X POST http://localhost/api/sessions/{id}/interactions \
  -H "Authorization: Bearer {token}" \
  -H "Content-Type: application/json" \
  -d '{"user_answer": "public class A extends B {}"}'
# → is_correct, feedback, next_problem 반환 확인
```

---

### Phase 6 — SkillOpt 파이프라인

**목표:** 세션 종료 후 Rollout → Reflect → Edit → Gate 루프가 백그라운드에서 실행되고 결과가 `skill_versions`에 저장된다.

**작업 목록:**

```
fastapi/app/services/
├── skillopt/
│   ├── pipeline.py       # 4단계 루프 오케스트레이터
│   ├── rollout.py        # validation_set으로 현재 스킬 평가
│   ├── reflect_edit.py   # 실패/성공 미니배치 분석 + bounded edit
│   └── gate.py           # 수정 스킬 재평가, 채택 여부 결정
```

**트리거 조건 판단** (FastAPI `POST /internal/sessions/{id}/end` 수신 후):

```python
# FastAPI가 판단. 미달 시 WARN 로그 후 스킵
conditions = [
    total_completed_sessions >= int(SKILLOPT_MIN_TOTAL_SESSIONS),  # 개발: 1
    new_sessions_since_last_pipeline >= int(SKILLOPT_MIN_NEW_SESSIONS),  # 개발: 1
    session_status in ("COMPLETED", "TIMEOUT"),
    active_validation_count >= 1,
]
if not all(conditions):
    logger.warning("SkillOpt pipeline skipped: conditions not met")
    return
```

**강제 트리거** (데모용):
- `POST /internal/skills/pipeline/trigger` — 조건 검사 건너뜀. `skill_validation_set` 활성 문제 수 ≥ 1건은 유지.

**Gate 결과 처리:**
- `correct_rate_after >= correct_rate_before` → `skill_versions.status = 'SUCCESS'`, `app.state.skill_doc` 교체
- 미달 → `status = 'FAILED'`, 기존 스킬 유지

**SpringBoot 추가 API:**
- `GET /api/skills/versions` — ADMIN, 버전 이력 목록
- `GET /api/skills/versions/{id}` — ADMIN, 상세
- `POST /api/skills/rollback/{versionId}` — ADMIN, 롤백
- `POST /api/skills/validation-set` — ADMIN, 검증 문제 등록
- `GET /api/skills/validation-set` — ADMIN
- `DELETE /api/skills/validation-set/{id}` — ADMIN
- `POST /api/skills/pipeline/trigger` — ADMIN, 강제 트리거 (FastAPI 호출)

**smoke test:**
```bash
# 검증 문제 등록 (1건 이상 필요)
curl -X POST http://localhost/api/skills/validation-set \
  -H "Authorization: Bearer {adminToken}" \
  -H "Content-Type: application/json" \
  -d '{"question": "Java에서 interface와 abstract class의 차이는?", "expected_answer": "...", "difficulty": "INTERMEDIATE"}'

# 강제 트리거
curl -X POST http://localhost/api/skills/pipeline/trigger \
  -H "Authorization: Bearer {adminToken}"

# 60초 후 결과 확인
curl http://localhost/api/skills/versions \
  -H "Authorization: Bearer {adminToken}"
# → SUCCESS 또는 FAILED status 확인
```

---

### Phase 7 — Swagger + 통합 검증

**목표:** dev 프로파일에서 Swagger UI가 열리고, 전체 1단계 API를 문서로 확인할 수 있다.

**작업 목록:**
1. `SwaggerConfig` — `@Profile("dev")`로만 활성화
2. `application-dev.yml` — `spring.profiles.active=dev` 설정
3. 전체 1단계 API 엔드포인트 smoke test 일괄 실행

**실행:**
```bash
docker compose -f docker-compose.yml -f docker-compose.dev.yml up -d
# http://localhost/swagger-ui/index.html 접속 확인
```

---

## 6. Architecture Rules

### SpringBoot 패키지 책임

| 패키지 | 책임 | 금지 |
|--------|------|------|
| `controller` | 요청 수신, DTO 변환 위임, 응답 반환 | 비즈니스 로직, 직접 Repository 호출 |
| `service` | 비즈니스 로직, 트랜잭션, DTO↔Entity 변환 | HTTP 요청 직접 처리 |
| `repository` | JPA 인터페이스 선언 | 로직 구현 |
| `domain` | `@Entity` 클래스만 | DTO 혼용, 비즈니스 로직 |
| `dto` | Request/Response 객체 | Entity 필드 직접 노출 |
| `client` | FastAPI HTTP 호출 | DB 접근, 비즈니스 로직 |

### 공통 응답 포맷

```java
// 모든 API 응답에 사용
ApiResponse<T> {
    boolean success;
    String  message;
    T       data;      // nullable
    String  timestamp; // ISO-8601
}
```

### 예외 처리

```
BaseException → DomainException → UserException
                               → SessionException
                               → SkillException
```
`@ControllerAdvice`에서 HTTP 상태 코드로 매핑:
- 400 BAD_REQUEST / 401 UNAUTHORIZED / 403 FORBIDDEN
- 404 NOT_FOUND / 409 CONFLICT
- 502 BAD_GATEWAY (FastAPI 호출 실패) / 503 SERVICE_UNAVAILABLE (스킬 미초기화) / 504 GATEWAY_TIMEOUT

---

## 7. Coding Standards

### 네이밍 규칙

- **SpringBoot (Java):** 클래스 PascalCase, 메서드·변수 camelCase, 상수 UPPER_SNAKE_CASE
- **FastAPI (Python):** 클래스 PascalCase, 함수·변수 snake_case, 상수 UPPER_SNAKE_CASE
- **API 엔드포인트:** kebab-case (`/api/learning-sessions`)
- **DB 컬럼:** snake_case (`correct_rate`, `last_activity_at`)

### 코드 품질 기준

- 함수/메서드는 단일 책임을 가진다 (20줄 초과 시 분리 검토).
- 매직 넘버는 상수 또는 환경변수로 추출한다.
- 중복 로직은 공통 유틸 또는 서비스로 추출한다.
- 주석은 "무엇"이 아닌 "왜"를 설명한다.
- 불필요한 추상화 계층을 만들지 않는다.

### API 설계 기준

- RESTful 원칙을 따른다 (명사형 리소스, HTTP 메서드로 행위 표현).
- 모든 응답은 `ApiResponse<T>` 공통 포맷을 사용한다.
- 페이지네이션이 필요한 목록은 `page`, `size`, `totalElements`를 포함한다.
- 클라이언트에 내부 구현 세부 사항(Entity 필드, 스택 트레이스)을 노출하지 않는다.

---

## 8. Testing Policy

### Smoke Test 기준

각 Phase 완료 후 해당 Phase의 smoke test를 반드시 통과해야 다음 Phase로 진행한다.  
smoke test 명령어는 각 Phase 섹션 내 정의된 curl/pytest 스크립트를 사용한다.

### 테스트 작성 원칙

- 버그 수정 시 수정 전 실패하는 테스트를 먼저 작성한다.
- 외부 의존성(DB, FastAPI, Anthropic API)은 Mock으로 대체하여 단위 테스트를 격리한다.
- 통합 테스트는 실제 컨테이너 환경에서 수행한다.
- 테스트 데이터는 각 테스트 케이스가 독립적으로 준비·정리한다.

### 테스트 범위

| 레이어 | 도구 | 대상 |
|--------|------|------|
| SpringBoot 단위 | JUnit 5 + Mockito | Service, Domain 로직 |
| SpringBoot 통합 | `@SpringBootTest` + Testcontainers | Controller → DB 흐름 |
| FastAPI 단위 | pytest + pytest-asyncio | Service, 파이프라인 로직 |
| FastAPI 통합 | pytest + httpx | Router → LLM Mock 흐름 |
| E2E | curl smoke test | Phase별 전체 흐름 |

---

## 9. Logging & Security

### 로깅

```java
// MDC에 반드시 포함
MDC.put("requestId", UUID);
MDC.put("userId",    userId);
MDC.put("sessionId", sessionId);
```
민감정보 마스킹 필수: `password`, `token`, `ANTHROPIC_API_KEY`, `INTERNAL_API_KEY`

### FastAPI 보안

```python
# 모든 /internal/* 라우터에 적용
async def verify_internal_key(x_internal_api_key: str = Header(...)):
    if x_internal_api_key != settings.INTERNAL_API_KEY:
        raise HTTPException(status_code=403)
```

---

## 10. Checklist

```
[ ] Phase 1  인프라 골격 — 컨테이너 4개 기동, 헬스체크 통과
[ ] Phase 2  DB 스키마 — 테이블 8개 생성, subjects seed 완료
[ ] Phase 3  인증 — JWT 발급/갱신/로그아웃, Google OAuth 동작
[ ] Phase 4  FastAPI 골격 — 기동 시 best_skill.md 로드 확인
[ ] Phase 5  학습 세션 — 세션 시작→인터랙션→종료 전체 흐름 동작
[ ] Phase 6  SkillOpt 파이프라인 — Gate 결과 DB 저장 확인
[ ] Phase 7  Swagger — dev 프로파일에서 UI 접근 가능
```
