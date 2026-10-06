INSERT INTO role (code, name, is_system)
VALUES ('SALES_MANAGER', '영업 관리자', TRUE)
ON DUPLICATE KEY UPDATE name = VALUES(name), is_system = VALUES(is_system);

INSERT INTO permission (code, name)
VALUES
    ('SALES_GROUP_CREATE', '영업 담당 그룹 생성'),
    ('SALES_GROUP_READ', '영업 담당 그룹 조회'),
    ('SALES_COMMISSION_READ', '본인 인센티브 조회')
ON DUPLICATE KEY UPDATE name = VALUES(name);

INSERT INTO role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM role r
JOIN permission p ON p.code IN ('SALES_GROUP_CREATE', 'SALES_GROUP_READ', 'SALES_COMMISSION_READ')
WHERE r.code IN ('ADMIN', 'SUPER_ADMIN', 'SALES_MANAGER')
ON DUPLICATE KEY UPDATE permission_id = VALUES(permission_id);
