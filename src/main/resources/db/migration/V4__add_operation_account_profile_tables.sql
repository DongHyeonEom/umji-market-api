ALTER TABLE account
    MODIFY login_id VARCHAR(100) NULL,
    MODIFY password_hash VARCHAR(255) NULL;

CREATE TABLE business_profile (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    account_id BIGINT NOT NULL,
    business_name VARCHAR(200) NOT NULL,
    business_registration_number VARCHAR(30) NULL,
    representative_name VARCHAR(100) NULL,
    business_phone VARCHAR(30) NULL,
    postal_code VARCHAR(20) NULL,
    address1 VARCHAR(255) NULL,
    address2 VARCHAR(255) NULL,
    status VARCHAR(30) NOT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    CONSTRAINT UQ_business_profile_account UNIQUE (account_id),
    INDEX IX_business_profile_registration_number (business_registration_number),
    CONSTRAINT FK_business_profile_account FOREIGN KEY (account_id) REFERENCES account(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE consent_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    account_id BIGINT NOT NULL,
    consent_type VARCHAR(100) NOT NULL,
    document_version VARCHAR(100) NOT NULL,
    consent_method VARCHAR(30) NOT NULL,
    evidence_reference VARCHAR(500) NULL,
    processed_by BIGINT NULL,
    consented_at DATETIME(3) NOT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    INDEX IX_consent_history_account (account_id, consented_at),
    CONSTRAINT FK_consent_history_account FOREIGN KEY (account_id) REFERENCES account(id),
    CONSTRAINT FK_consent_history_processed_by FOREIGN KEY (processed_by) REFERENCES account(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
