INSERT INTO role (code, name, is_system)
VALUES ('SHIPPING_OPERATOR', '배송 작업자', TRUE)
ON DUPLICATE KEY UPDATE name = VALUES(name), is_system = VALUES(is_system);

INSERT INTO permission (code, name)
VALUES ('SHIPMENT_CONFIGURE', '브랜드 배송 요율·실제 운송비 관리')
ON DUPLICATE KEY UPDATE name = VALUES(name);

INSERT INTO role_permission (role_id, permission_id)
SELECT role.id, permission.id
FROM role
JOIN permission ON permission.code = 'SHIPMENT_CONFIGURE'
WHERE role.code IN ('ADMIN', 'SUPER_ADMIN', 'SHIPPING_MANAGER')
ON DUPLICATE KEY UPDATE permission_id = VALUES(permission_id);

INSERT INTO role_permission (role_id, permission_id)
SELECT role.id, permission.id
FROM role
JOIN permission ON permission.code IN ('SHIPMENT_READ', 'SHIPMENT_WRITE')
WHERE role.code = 'SHIPPING_OPERATOR'
ON DUPLICATE KEY UPDATE permission_id = VALUES(permission_id);

RENAME TABLE brand_shipping_manager_assignment TO brand_shipping_assignment;

ALTER TABLE brand_shipping_assignment
    ADD COLUMN assignment_type VARCHAR(20) NOT NULL DEFAULT 'MANAGER' AFTER account_id,
    ADD CONSTRAINT CK_brand_shipping_assignment_type CHECK (assignment_type IN ('MANAGER', 'OPERATOR'));
