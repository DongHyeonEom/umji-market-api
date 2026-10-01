CREATE TABLE buyer_group (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    public_id BINARY(16) NOT NULL,
    legacy_account_id BIGINT NULL,
    group_type VARCHAR(30) NOT NULL,
    display_name VARCHAR(200) NOT NULL,
    status VARCHAR(30) NOT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    CONSTRAINT UQ_buyer_group_public_id UNIQUE (public_id),
    CONSTRAINT UQ_buyer_group_legacy_account_id UNIQUE (legacy_account_id),
    CONSTRAINT CK_buyer_group_type CHECK (group_type IN ('BUSINESS', 'INDIVIDUAL'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE buyer_group_member (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    buyer_group_id BIGINT NOT NULL,
    account_id BIGINT NOT NULL,
    status VARCHAR(30) NOT NULL,
    joined_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    CONSTRAINT UQ_buyer_group_member_group_account UNIQUE (buyer_group_id, account_id),
    CONSTRAINT FK_buyer_group_member_group FOREIGN KEY (buyer_group_id) REFERENCES buyer_group(id),
    CONSTRAINT FK_buyer_group_member_account FOREIGN KEY (account_id) REFERENCES account(id),
    INDEX IX_buyer_group_member_account_status (account_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE buyer_group_business_profile (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    buyer_group_id BIGINT NOT NULL,
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
    CONSTRAINT UQ_buyer_group_business_profile_group UNIQUE (buyer_group_id),
    CONSTRAINT FK_buyer_group_business_profile_group FOREIGN KEY (buyer_group_id) REFERENCES buyer_group(id),
    INDEX IX_buyer_group_business_profile_registration_number (business_registration_number)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO buyer_group (public_id, legacy_account_id, group_type, display_name, status)
SELECT UUID_TO_BIN(UUID(), 1),
       account.id,
       CASE WHEN business_profile.id IS NULL THEN 'INDIVIDUAL' ELSE 'BUSINESS' END,
       CASE WHEN business_profile.id IS NULL THEN account.name ELSE business_profile.business_name END,
       'ACTIVE'
FROM account
LEFT JOIN business_profile ON business_profile.account_id = account.id;

INSERT INTO buyer_group_member (buyer_group_id, account_id, status)
SELECT buyer_group.id, account.id, 'ACTIVE'
FROM account
JOIN buyer_group ON buyer_group.legacy_account_id = account.id;

INSERT INTO buyer_group_business_profile (
    buyer_group_id,
    business_name,
    business_registration_number,
    representative_name,
    business_phone,
    postal_code,
    address1,
    address2,
    status,
    created_at,
    updated_at
)
SELECT buyer_group.id,
       business_profile.business_name,
       business_profile.business_registration_number,
       business_profile.representative_name,
       business_profile.business_phone,
       business_profile.postal_code,
       business_profile.address1,
       business_profile.address2,
       business_profile.status,
       business_profile.created_at,
       business_profile.updated_at
FROM business_profile
JOIN buyer_group ON buyer_group.legacy_account_id = business_profile.account_id;

ALTER TABLE purchase_order
    ADD COLUMN buyer_group_id BIGINT NULL AFTER account_id;

UPDATE purchase_order
JOIN buyer_group ON buyer_group.legacy_account_id = purchase_order.account_id
SET purchase_order.buyer_group_id = buyer_group.id;

ALTER TABLE buyer_group
    DROP INDEX UQ_buyer_group_legacy_account_id,
    DROP COLUMN legacy_account_id;

ALTER TABLE purchase_order
    ADD CONSTRAINT FK_purchase_order_buyer_group FOREIGN KEY (buyer_group_id) REFERENCES buyer_group(id),
    ADD INDEX IX_purchase_order_buyer_group_ordered_at (buyer_group_id, ordered_at DESC);
