INSERT INTO permission (code, name)
VALUES ('SALES_GROUP_ASSIGN', '영업 담당자·인센티브율 배정')
ON DUPLICATE KEY UPDATE name = VALUES(name);

INSERT INTO role_permission (role_id, permission_id)
SELECT role.id, permission.id
FROM role
JOIN permission ON permission.code = 'SALES_GROUP_ASSIGN'
WHERE role.code IN ('ADMIN', 'SUPER_ADMIN')
ON DUPLICATE KEY UPDATE permission_id = VALUES(permission_id);

CREATE TABLE organization_sales_assignment (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    public_id BINARY(16) NOT NULL,
    organization_id BIGINT NOT NULL,
    sales_account_id BIGINT NOT NULL,
    commission_rate_bps INT NULL,
    assignment_reason VARCHAR(30) NOT NULL,
    valid_from DATETIME(3) NOT NULL,
    valid_until DATETIME(3) NULL,
    assigned_by_account_id BIGINT NOT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    CONSTRAINT UQ_organization_sales_assignment_public_id UNIQUE (public_id),
    CONSTRAINT FK_organization_sales_assignment_organization FOREIGN KEY (organization_id) REFERENCES organization(id),
    CONSTRAINT FK_organization_sales_assignment_sales_account FOREIGN KEY (sales_account_id) REFERENCES account(id),
    CONSTRAINT FK_organization_sales_assignment_assigned_by FOREIGN KEY (assigned_by_account_id) REFERENCES account(id),
    CONSTRAINT CK_organization_sales_assignment_rate CHECK (commission_rate_bps IS NULL OR commission_rate_bps BETWEEN 1 AND 10000),
    CONSTRAINT CK_organization_sales_assignment_period CHECK (valid_until IS NULL OR valid_until > valid_from),
    INDEX IX_organization_sales_assignment_org_period (organization_id, valid_from, valid_until),
    INDEX IX_organization_sales_assignment_sales_period (sales_account_id, valid_from, valid_until)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
