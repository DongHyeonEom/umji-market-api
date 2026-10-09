INSERT INTO permission (code, name)
VALUES
    ('SALES_COMMISSION_READ_ALL', '전체 인센티브 내역 조회'),
    ('SALES_COMMISSION_SETTLE', '월말 인센티브 정산·지급')
ON DUPLICATE KEY UPDATE name = VALUES(name);

INSERT INTO role_permission (role_id, permission_id)
SELECT role.id, permission.id
FROM role
JOIN permission ON permission.code IN ('SALES_COMMISSION_READ_ALL', 'SALES_COMMISSION_SETTLE')
WHERE role.code IN ('ADMIN', 'SUPER_ADMIN')
ON DUPLICATE KEY UPDATE permission_id = VALUES(permission_id);

CREATE TABLE sales_commission (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    public_id BINARY(16) NOT NULL,
    order_id BIGINT NOT NULL,
    organization_id BIGINT NOT NULL,
    assignment_id BIGINT NULL,
    sales_account_id BIGINT NULL,
    rate_bps_snapshot INT NULL,
    basis_snapshot VARCHAR(40) NOT NULL,
    basis_amount BIGINT NOT NULL,
    commission_amount BIGINT NOT NULL,
    status VARCHAR(30) NOT NULL,
    settlement_month DATE NULL,
    qualified_at DATETIME(3) NULL,
    paid_at DATETIME(3) NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    CONSTRAINT UQ_sales_commission_public_id UNIQUE (public_id),
    CONSTRAINT UQ_sales_commission_order UNIQUE (order_id),
    CONSTRAINT FK_sales_commission_order FOREIGN KEY (order_id) REFERENCES purchase_order(id),
    CONSTRAINT FK_sales_commission_organization FOREIGN KEY (organization_id) REFERENCES organization(id),
    CONSTRAINT FK_sales_commission_assignment FOREIGN KEY (assignment_id) REFERENCES organization_sales_assignment(id),
    CONSTRAINT FK_sales_commission_sales_account FOREIGN KEY (sales_account_id) REFERENCES account(id),
    CONSTRAINT CK_sales_commission_rate CHECK (rate_bps_snapshot IS NULL OR rate_bps_snapshot BETWEEN 1 AND 10000),
    CONSTRAINT CK_sales_commission_amounts CHECK (basis_amount >= 0 AND commission_amount >= 0),
    CONSTRAINT CK_sales_commission_status CHECK (status IN ('NOT_APPLICABLE', 'WAITING', 'PAYABLE', 'PAID', 'REVERSED')),
    CONSTRAINT CK_sales_commission_basis CHECK (basis_snapshot = 'NET_ITEM_SALES_EX_TAX'),
    INDEX IX_sales_commission_account_status_month (sales_account_id, status, settlement_month),
    INDEX IX_sales_commission_status_period (status, settlement_month, qualified_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE sales_commission_event (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    commission_id BIGINT NOT NULL,
    event_type VARCHAR(30) NOT NULL,
    amount_delta BIGINT NOT NULL,
    idempotency_key VARCHAR(150) NOT NULL,
    processed_by_account_id BIGINT NULL,
    reason_code VARCHAR(50) NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    CONSTRAINT UQ_sales_commission_event_key UNIQUE (idempotency_key),
    CONSTRAINT FK_sales_commission_event_commission FOREIGN KEY (commission_id) REFERENCES sales_commission(id),
    CONSTRAINT FK_sales_commission_event_processor FOREIGN KEY (processed_by_account_id) REFERENCES account(id),
    CONSTRAINT CK_sales_commission_event_type CHECK (event_type IN ('SNAPSHOT', 'ACCRUED', 'REVERSED', 'PAID')),
    INDEX IX_sales_commission_event_commission_created (commission_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
