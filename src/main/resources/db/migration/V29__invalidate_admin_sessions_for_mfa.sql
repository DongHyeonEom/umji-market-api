UPDATE account a
JOIN account_role ar ON ar.account_id = a.id
JOIN role r ON r.id = ar.role_id
SET a.token_version = a.token_version + 1
WHERE r.code IN ('ADMIN', 'SUPER_ADMIN', 'PRODUCT_MANAGER', 'ORDER_MANAGER', 'INVENTORY_MANAGER');

UPDATE refresh_token rt
JOIN account_role ar ON ar.account_id = rt.account_id
JOIN role r ON r.id = ar.role_id
SET rt.revoked_at = CURRENT_TIMESTAMP(3)
WHERE rt.revoked_at IS NULL
  AND r.code IN ('ADMIN', 'SUPER_ADMIN', 'PRODUCT_MANAGER', 'ORDER_MANAGER', 'INVENTORY_MANAGER');
