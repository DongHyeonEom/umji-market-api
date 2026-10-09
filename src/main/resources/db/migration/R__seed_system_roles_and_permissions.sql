INSERT INTO role (code, name, is_system) VALUES
    ('CUSTOMER', '고객', TRUE),
    ('ADMIN', '관리자', TRUE),
    ('PRODUCT_MANAGER', '상품 관리자', TRUE),
    ('ORDER_MANAGER', '주문 관리자', TRUE),
    ('INVENTORY_MANAGER', '재고 관리자', TRUE),
    ('SHIPPING_MANAGER', '배송 관리자', TRUE),
    ('SALES_MANAGER', '영업 관리자', TRUE),
    ('SUPER_ADMIN', '최고 관리자', TRUE)
ON DUPLICATE KEY UPDATE name = VALUES(name), is_system = VALUES(is_system);

INSERT INTO permission (code, name) VALUES
    ('PRODUCT_READ', '상품 조회'), ('PRODUCT_WRITE', '상품 관리'),
    ('ORDER_READ', '주문 조회'), ('ORDER_WRITE', '주문 관리'),
    ('INVENTORY_READ', '재고 조회'), ('INVENTORY_WRITE', '재고 관리'),
    ('ADMIN_ACCOUNT_MANAGE', '관리자 계정 관리'),
    ('ADMIN_AUDIT_READ', '운영 감사 로그 조회'),
    ('SHIPMENT_WRITE', '배송 정보 변경'),
    ('SALES_GROUP_CREATE', '영업 담당 그룹 생성'),
    ('SALES_GROUP_READ', '영업 담당 그룹 조회'),
    ('SALES_GROUP_ASSIGN', '영업 담당자·인센티브율 배정'),
    ('SALES_COMMISSION_READ', '본인 인센티브 조회'),
    ('SALES_COMMISSION_READ_ALL', '전체 인센티브 내역 조회'),
    ('SALES_COMMISSION_SETTLE', '월말 인센티브 정산·지급')
ON DUPLICATE KEY UPDATE name = VALUES(name);

INSERT INTO role_permission (role_id, permission_id)
SELECT role.id, permission.id
FROM role
JOIN permission ON permission.code = 'SALES_GROUP_ASSIGN'
WHERE role.code IN ('ADMIN', 'SUPER_ADMIN')
ON DUPLICATE KEY UPDATE permission_id = VALUES(permission_id);

INSERT INTO role_permission (role_id, permission_id)
SELECT role.id, permission.id
FROM role
JOIN permission ON permission.code IN ('SALES_COMMISSION_READ_ALL', 'SALES_COMMISSION_SETTLE')
WHERE role.code IN ('ADMIN', 'SUPER_ADMIN')
ON DUPLICATE KEY UPDATE permission_id = VALUES(permission_id);
