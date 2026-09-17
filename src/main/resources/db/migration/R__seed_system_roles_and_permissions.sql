INSERT INTO role (code, name, is_system) VALUES
    ('CUSTOMER', '고객', TRUE),
    ('ADMIN', '관리자', TRUE),
    ('PRODUCT_MANAGER', '상품 관리자', TRUE),
    ('ORDER_MANAGER', '주문 관리자', TRUE),
    ('INVENTORY_MANAGER', '재고 관리자', TRUE),
    ('SUPER_ADMIN', '최고 관리자', TRUE)
ON DUPLICATE KEY UPDATE name = VALUES(name), is_system = VALUES(is_system);

INSERT INTO permission (code, name) VALUES
    ('PRODUCT_READ', '상품 조회'), ('PRODUCT_WRITE', '상품 관리'),
    ('ORDER_READ', '주문 조회'), ('ORDER_WRITE', '주문 관리'),
    ('INVENTORY_READ', '재고 조회'), ('INVENTORY_WRITE', '재고 관리'),
    ('ADMIN_ACCOUNT_MANAGE', '관리자 계정 관리')
ON DUPLICATE KEY UPDATE name = VALUES(name);
