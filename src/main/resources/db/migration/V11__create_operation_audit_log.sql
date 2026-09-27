CREATE TABLE operation_audit_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    actor_public_id BINARY(16) NULL,
    action VARCHAR(200) NOT NULL,
    resource_type VARCHAR(40) NOT NULL,
    resource_public_id BINARY(16) NULL,
    request_trace_id VARCHAR(64) NULL,
    occurred_at DATETIME(3) NOT NULL,
    INDEX IX_operation_audit_log_occurred_at (occurred_at, id),
    INDEX IX_operation_audit_log_actor_occurred_at (actor_public_id, occurred_at),
    INDEX IX_operation_audit_log_resource_occurred_at (resource_type, resource_public_id, occurred_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO permission (code, name)
VALUES ('ADMIN_AUDIT_READ', '운영 감사 로그 조회')
ON DUPLICATE KEY UPDATE name = VALUES(name);

INSERT INTO role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM role r
JOIN permission p ON p.code = 'ADMIN_AUDIT_READ'
WHERE r.code = 'SUPER_ADMIN'
ON DUPLICATE KEY UPDATE permission_id = VALUES(permission_id);
