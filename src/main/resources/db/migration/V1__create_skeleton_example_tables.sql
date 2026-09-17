-- MySQL 스켈레톤 예시 테이블 생성
-- SoftDeletableExampleEntity.kt, TestJpaEntity.kt 참조

-- Soft Delete 예시 테이블
CREATE TABLE IF NOT EXISTS soft_deletable_example (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(255) NOT NULL,
    created_by  INT NOT NULL DEFAULT 0,
    created_at  DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_by  INT NOT NULL DEFAULT 0,
    updated_at  DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    deleted_at  DATETIME(3) NULL,
    deleted_by  INT NULL,
    INDEX ix_soft_deletable_example_deleted_at (deleted_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Test 테이블
CREATE TABLE IF NOT EXISTS test (
    id   INT AUTO_INCREMENT PRIMARY KEY,
    json JSON NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
