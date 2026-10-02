INSERT INTO role (code, name, is_system)
VALUES
    ('ADMIN', '관리자', TRUE),
    ('SUPER_ADMIN', '최고 관리자', TRUE),
    ('SHIPPING_MANAGER', '배송 관리자', TRUE)
ON DUPLICATE KEY UPDATE name = VALUES(name), is_system = VALUES(is_system);

INSERT INTO permission (code, name)
VALUES ('SHIPMENT_WRITE', '배송 정보 변경')
ON DUPLICATE KEY UPDATE name = VALUES(name);

INSERT INTO role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM role r
JOIN permission p ON p.code = 'SHIPMENT_WRITE'
WHERE r.code IN ('ADMIN', 'SUPER_ADMIN', 'SHIPPING_MANAGER')
ON DUPLICATE KEY UPDATE permission_id = VALUES(permission_id);
