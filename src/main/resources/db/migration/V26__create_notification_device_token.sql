CREATE TABLE notification_device_token (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    public_id BINARY(16) NOT NULL,
    account_id BIGINT NOT NULL,
    platform VARCHAR(20) NOT NULL,
    token_value VARCHAR(4096) NOT NULL,
    token_hash BINARY(32) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    last_registered_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT UQ_notification_device_token_public_id UNIQUE (public_id),
    CONSTRAINT UQ_notification_device_token_hash UNIQUE (token_hash),
    CONSTRAINT CK_notification_device_token_platform CHECK (platform IN ('ANDROID_FCM', 'IOS_APNS')),
    CONSTRAINT CK_notification_device_token_status CHECK (status IN ('ACTIVE', 'INACTIVE')),
    CONSTRAINT FK_notification_device_token_account FOREIGN KEY (account_id) REFERENCES account(id),
    INDEX IX_notification_device_token_account (account_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
