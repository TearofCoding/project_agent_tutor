CREATE TABLE IF NOT EXISTS users (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    email          VARCHAR(255) NOT NULL UNIQUE,
    password_hash  VARCHAR(255),
    name           VARCHAR(100) NOT NULL,
    role           ENUM('ROLE_USER','ROLE_ADMIN') NOT NULL DEFAULT 'ROLE_USER',
    provider       ENUM('LOCAL','GOOGLE') NOT NULL DEFAULT 'LOCAL',
    provider_id    VARCHAR(255),
    email_verified BOOLEAN NOT NULL DEFAULT TRUE,
    is_deleted     BOOLEAN NOT NULL DEFAULT FALSE,
    created_at     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS user_profiles (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id           BIGINT NOT NULL UNIQUE,
    declared_level    ENUM('BEGINNER','INTERMEDIATE','ADVANCED'),
    calculated_level  ENUM('BEGINNER','INTERMEDIATE','ADVANCED'),
    goal              VARCHAR(500),
    created_at        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS subjects (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(100) NOT NULL UNIQUE,
    description TEXT,
    is_active   BOOLEAN NOT NULL DEFAULT TRUE,
    created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS refresh_tokens (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id    BIGINT NOT NULL,
    token      VARCHAR(500) NOT NULL UNIQUE,
    expires_at DATETIME NOT NULL,
    revoked    BOOLEAN NOT NULL DEFAULT FALSE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS learning_sessions (
    id                 BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id            BIGINT NOT NULL,
    subject_id         BIGINT NOT NULL,
    skill_version      VARCHAR(50),
    selected_difficulty ENUM('BEGINNER','INTERMEDIATE','ADVANCED'),
    status             ENUM('IN_PROGRESS','COMPLETED','TIMEOUT') NOT NULL DEFAULT 'IN_PROGRESS',
    score              DECIMAL(5,2),
    correct_rate       DECIMAL(5,2),
    duration_sec       INT,
    last_activity_at   DATETIME,
    recommendation     TEXT,
    started_at         DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ended_at           DATETIME,
    created_at         DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at         DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (subject_id) REFERENCES subjects(id),
    INDEX idx_sessions_timeout (status, last_activity_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS session_interactions (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    session_id        BIGINT NOT NULL,
    interaction_order INT,
    question          TEXT NOT NULL,
    topic_tag         VARCHAR(100),
    difficulty        ENUM('BEGINNER','INTERMEDIATE','ADVANCED'),
    user_answer       TEXT,
    is_correct        BOOLEAN,
    feedback          TEXT,
    response_time_ms  INT,
    created_at        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (session_id) REFERENCES learning_sessions(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS skill_versions (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    version             VARCHAR(50) NOT NULL,
    skill_content       TEXT NOT NULL,
    status              ENUM('IN_PROGRESS','SUCCESS','FAILED','ROLLBACK') NOT NULL DEFAULT 'IN_PROGRESS',
    trigger_session_id  BIGINT,
    correct_rate_before DECIMAL(5,2),
    correct_rate_after  DECIMAL(5,2),
    reflect_summary     TEXT,
    tokens_used         INT,
    budget_exceeded     BOOLEAN NOT NULL DEFAULT FALSE,
    rollback_reason     TEXT,
    rolled_back_by      BIGINT,
    created_at          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS skill_validation_set (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    question        TEXT NOT NULL,
    expected_answer TEXT NOT NULL,
    difficulty      ENUM('BEGINNER','INTERMEDIATE','ADVANCED'),
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
