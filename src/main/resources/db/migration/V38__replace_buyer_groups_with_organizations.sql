RENAME TABLE
    buyer_group TO organization,
    buyer_group_member TO organization_member,
    buyer_group_business_profile TO organization_profile,
    buyer_group_address TO organization_address,
    buyer_group_invitation TO organization_invitation,
    buyer_group_join_request TO organization_join_request;

ALTER TABLE organization
    CHANGE COLUMN group_type organization_type VARCHAR(30) NOT NULL;

ALTER TABLE organization_member
    CHANGE COLUMN buyer_group_id organization_id BIGINT NOT NULL;

ALTER TABLE organization_profile
    CHANGE COLUMN buyer_group_id organization_id BIGINT NOT NULL;

ALTER TABLE organization_address
    CHANGE COLUMN buyer_group_id organization_id BIGINT NOT NULL;

ALTER TABLE organization_invitation
    CHANGE COLUMN buyer_group_id organization_id BIGINT NOT NULL;

ALTER TABLE organization_join_request
    CHANGE COLUMN buyer_group_id organization_id BIGINT NOT NULL;

ALTER TABLE purchase_order
    CHANGE COLUMN buyer_group_id organization_id BIGINT NOT NULL;

INSERT INTO organization_profile (
    organization_id,
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
SELECT member.organization_id,
       profile.business_name,
       profile.business_registration_number,
       profile.representative_name,
       profile.business_phone,
       profile.postal_code,
       profile.address1,
       profile.address2,
       profile.status,
       profile.created_at,
       profile.updated_at
FROM (
    SELECT organization_id, MIN(account_id) AS account_id
    FROM organization_member
    WHERE status = 'ACTIVE'
    GROUP BY organization_id
) member
JOIN business_profile profile ON profile.account_id = member.account_id
LEFT JOIN organization_profile current_profile ON current_profile.organization_id = member.organization_id
WHERE current_profile.id IS NULL;

UPDATE organization_profile current_profile
JOIN (
    SELECT organization_id, MIN(account_id) AS account_id
    FROM organization_member
    WHERE status = 'ACTIVE'
    GROUP BY organization_id
) member ON member.organization_id = current_profile.organization_id
JOIN business_profile legacy_profile
  ON legacy_profile.account_id = member.account_id
SET current_profile.business_registration_number = COALESCE(current_profile.business_registration_number, legacy_profile.business_registration_number),
    current_profile.representative_name = COALESCE(current_profile.representative_name, legacy_profile.representative_name),
    current_profile.business_phone = COALESCE(current_profile.business_phone, legacy_profile.business_phone),
    current_profile.postal_code = COALESCE(current_profile.postal_code, legacy_profile.postal_code),
    current_profile.address1 = COALESCE(current_profile.address1, legacy_profile.address1),
    current_profile.address2 = COALESCE(current_profile.address2, legacy_profile.address2),
    current_profile.status = CASE WHEN current_profile.status = 'INCOMPLETE' THEN legacy_profile.status ELSE current_profile.status END;

CREATE TABLE organization_capability (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    organization_id BIGINT NOT NULL,
    capability_code VARCHAR(30) NOT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    CONSTRAINT UQ_organization_capability_org_code UNIQUE (organization_id, capability_code),
    CONSTRAINT FK_organization_capability_organization FOREIGN KEY (organization_id) REFERENCES organization(id),
    CONSTRAINT CK_organization_capability_code CHECK (capability_code IN ('BUYER', 'SELLER', 'OPERATOR')),
    INDEX IX_organization_capability_code (capability_code, organization_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO organization_capability (organization_id, capability_code)
SELECT id, 'BUYER'
FROM organization;
