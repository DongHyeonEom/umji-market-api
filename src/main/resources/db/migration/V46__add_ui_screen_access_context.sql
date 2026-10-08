INSERT INTO permission (code, name)
VALUES
    ('SHIPMENT_READ', '배송 정보 조회'),
    ('BUYER_GROUP_ONBOARDING_READ', '구매자 그룹 onboarding 화면 조회'),
    ('BUYER_GROUP_CREATE', '구매자 그룹 생성 화면 사용'),
    ('BUYER_GROUP_SEARCH', '구매자 그룹 검색 화면 사용'),
    ('BUYER_GROUP_JOIN_REQUEST_CREATE', '구매자 그룹 가입 요청 화면 사용'),
    ('BUYER_GROUP_ORDER_READ', '그룹 주문 화면 조회'),
    ('BUYER_GROUP_ORDER_CREATE', '그룹 주문 생성 화면 사용'),
    ('BUYER_GROUP_ADDRESS_READ', '그룹 배송지 화면 조회'),
    ('BUYER_GROUP_ADDRESS_MANAGE', '그룹 배송지 관리 화면 사용'),
    ('BUYER_GROUP_INVITE', '그룹 구성원 초대 화면 사용'),
    ('BUYER_GROUP_JOIN_REQUEST_MANAGE', '그룹 가입 요청 관리 화면 사용')
ON DUPLICATE KEY UPDATE name = VALUES(name);

INSERT INTO role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM role r
JOIN permission p ON p.code = 'SHIPMENT_READ'
WHERE r.code IN ('ADMIN', 'SUPER_ADMIN', 'SHIPPING_MANAGER')
ON DUPLICATE KEY UPDATE permission_id = VALUES(permission_id);

CREATE TABLE ui_screen (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    screen_code VARCHAR(100) NOT NULL,
    audience VARCHAR(20) NOT NULL,
    route_key VARCHAR(150) NOT NULL,
    permission_match_mode VARCHAR(10) NOT NULL DEFAULT 'ALL',
    active BOOLEAN NOT NULL DEFAULT TRUE,
    display_order INT NOT NULL DEFAULT 0,
    version BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    CONSTRAINT UQ_ui_screen_code UNIQUE (screen_code),
    CONSTRAINT CK_ui_screen_audience CHECK (audience IN ('ADMIN', 'BUYER')),
    CONSTRAINT CK_ui_screen_match_mode CHECK (permission_match_mode IN ('ALL', 'ANY')),
    CONSTRAINT CK_ui_screen_display_order CHECK (display_order >= 0),
    INDEX IX_ui_screen_audience_active_order (audience, active, display_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE ui_screen_permission (
    ui_screen_id BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,
    PRIMARY KEY (ui_screen_id, permission_id),
    CONSTRAINT FK_ui_screen_permission_screen FOREIGN KEY (ui_screen_id) REFERENCES ui_screen(id),
    CONSTRAINT FK_ui_screen_permission_permission FOREIGN KEY (permission_id) REFERENCES permission(id),
    INDEX IX_ui_screen_permission_permission (permission_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE organization_role_permission (
    membership_role VARCHAR(30) NOT NULL,
    permission_id BIGINT NOT NULL,
    PRIMARY KEY (membership_role, permission_id),
    CONSTRAINT FK_organization_role_permission_permission FOREIGN KEY (permission_id) REFERENCES permission(id),
    CONSTRAINT CK_organization_role_permission_role CHECK (membership_role IN ('UNASSIGNED', 'REPRESENTATIVE', 'MEMBER')),
    INDEX IX_organization_role_permission_permission (permission_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO ui_screen (screen_code, audience, route_key, permission_match_mode, display_order)
VALUES
    ('ADMIN_SHIPMENT_LIST', 'ADMIN', 'ADMIN_SHIPMENT_LIST', 'ALL', 10),
    ('ADMIN_SHIPMENT_DETAIL', 'ADMIN', 'ADMIN_SHIPMENT_DETAIL', 'ALL', 20),
    ('ADMIN_SALES_GROUP_LIST', 'ADMIN', 'ADMIN_SALES_GROUP_LIST', 'ALL', 30),
    ('ADMIN_SALES_COMMISSION_LIST', 'ADMIN', 'ADMIN_SALES_COMMISSION_LIST', 'ALL', 40),
    ('ADMIN_ACCOUNT_ROLE_SETTINGS', 'ADMIN', 'ADMIN_ACCOUNT_ROLE_SETTINGS', 'ALL', 50),
    ('BUYER_GROUP_ONBOARDING', 'BUYER', 'BUYER_GROUP_ONBOARDING', 'ALL', 10),
    ('BUYER_ORDERS', 'BUYER', 'BUYER_ORDERS', 'ALL', 20),
    ('BUYER_ORDER_CREATE', 'BUYER', 'BUYER_ORDER_CREATE', 'ALL', 30),
    ('BUYER_ADDRESSES', 'BUYER', 'BUYER_ADDRESSES', 'ALL', 40),
    ('BUYER_GROUP_MEMBERS', 'BUYER', 'BUYER_GROUP_MEMBERS', 'ALL', 50)
ON DUPLICATE KEY UPDATE route_key = VALUES(route_key), active = TRUE, display_order = VALUES(display_order);

INSERT INTO ui_screen_permission (ui_screen_id, permission_id)
SELECT screen.id, permission.id
FROM (
    SELECT 'ADMIN_SHIPMENT_LIST' AS screen_code, 'SHIPMENT_READ' AS permission_code
    UNION ALL SELECT 'ADMIN_SHIPMENT_DETAIL', 'SHIPMENT_READ'
    UNION ALL SELECT 'ADMIN_SALES_GROUP_LIST', 'SALES_GROUP_READ'
    UNION ALL SELECT 'ADMIN_SALES_COMMISSION_LIST', 'SALES_COMMISSION_READ'
    UNION ALL SELECT 'ADMIN_ACCOUNT_ROLE_SETTINGS', 'ADMIN_ACCOUNT_MANAGE'
    UNION ALL SELECT 'BUYER_GROUP_ONBOARDING', 'BUYER_GROUP_ONBOARDING_READ'
    UNION ALL SELECT 'BUYER_ORDERS', 'BUYER_GROUP_ORDER_READ'
    UNION ALL SELECT 'BUYER_ORDER_CREATE', 'BUYER_GROUP_ORDER_CREATE'
    UNION ALL SELECT 'BUYER_ADDRESSES', 'BUYER_GROUP_ADDRESS_READ'
    UNION ALL SELECT 'BUYER_GROUP_MEMBERS', 'BUYER_GROUP_INVITE'
    UNION ALL SELECT 'BUYER_GROUP_MEMBERS', 'BUYER_GROUP_JOIN_REQUEST_MANAGE'
) mapping
JOIN ui_screen screen ON screen.screen_code = mapping.screen_code
JOIN permission ON permission.code = mapping.permission_code
ON DUPLICATE KEY UPDATE permission_id = VALUES(permission_id);

INSERT INTO organization_role_permission (membership_role, permission_id)
SELECT mapping.membership_role, permission.id
FROM (
    SELECT 'UNASSIGNED' AS membership_role, 'BUYER_GROUP_ONBOARDING_READ' AS permission_code
    UNION ALL SELECT 'UNASSIGNED', 'BUYER_GROUP_CREATE'
    UNION ALL SELECT 'UNASSIGNED', 'BUYER_GROUP_SEARCH'
    UNION ALL SELECT 'UNASSIGNED', 'BUYER_GROUP_JOIN_REQUEST_CREATE'
    UNION ALL SELECT 'REPRESENTATIVE', 'BUYER_GROUP_ORDER_READ'
    UNION ALL SELECT 'REPRESENTATIVE', 'BUYER_GROUP_ORDER_CREATE'
    UNION ALL SELECT 'REPRESENTATIVE', 'BUYER_GROUP_ADDRESS_READ'
    UNION ALL SELECT 'REPRESENTATIVE', 'BUYER_GROUP_ADDRESS_MANAGE'
    UNION ALL SELECT 'REPRESENTATIVE', 'BUYER_GROUP_INVITE'
    UNION ALL SELECT 'REPRESENTATIVE', 'BUYER_GROUP_JOIN_REQUEST_CREATE'
    UNION ALL SELECT 'REPRESENTATIVE', 'BUYER_GROUP_JOIN_REQUEST_MANAGE'
    UNION ALL SELECT 'MEMBER', 'BUYER_GROUP_ORDER_READ'
    UNION ALL SELECT 'MEMBER', 'BUYER_GROUP_ORDER_CREATE'
    UNION ALL SELECT 'MEMBER', 'BUYER_GROUP_ADDRESS_READ'
    UNION ALL SELECT 'MEMBER', 'BUYER_GROUP_SEARCH'
    UNION ALL SELECT 'MEMBER', 'BUYER_GROUP_JOIN_REQUEST_CREATE'
) mapping
JOIN permission ON permission.code = mapping.permission_code
ON DUPLICATE KEY UPDATE membership_role = VALUES(membership_role);
