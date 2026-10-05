CREATE TABLE web_login_attempt (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    phone_hash BINARY(32) NOT NULL,
    remote_address_hash BINARY(32) NOT NULL,
    window_started_at DATETIME(3) NOT NULL,
    failure_count INT NOT NULL,
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    CONSTRAINT UQ_web_login_attempt_phone_address UNIQUE (phone_hash, remote_address_hash),
    INDEX IX_web_login_attempt_phone_window (phone_hash, window_started_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
