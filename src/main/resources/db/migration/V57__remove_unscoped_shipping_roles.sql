UPDATE account account
JOIN (
    SELECT DISTINCT account_role.account_id
    FROM account_role
    JOIN role ON role.id = account_role.role_id
    WHERE (role.code = 'SHIPPING_MANAGER' AND NOT EXISTS (
        SELECT 1 FROM brand_shipping_assignment assignment
        WHERE assignment.account_id = account_role.account_id AND assignment.assignment_type = 'MANAGER'
    )) OR (role.code = 'SHIPPING_OPERATOR' AND NOT EXISTS (
        SELECT 1 FROM brand_shipping_assignment assignment
        WHERE assignment.account_id = account_role.account_id AND assignment.assignment_type = 'OPERATOR'
    ))
) stale_shipping_roles ON stale_shipping_roles.account_id = account.id
SET account.token_version = account.token_version + 1;

DELETE account_role
FROM account_role
JOIN role ON role.id = account_role.role_id
WHERE (role.code = 'SHIPPING_MANAGER' AND NOT EXISTS (
    SELECT 1 FROM brand_shipping_assignment assignment
    WHERE assignment.account_id = account_role.account_id AND assignment.assignment_type = 'MANAGER'
)) OR (role.code = 'SHIPPING_OPERATOR' AND NOT EXISTS (
    SELECT 1 FROM brand_shipping_assignment assignment
    WHERE assignment.account_id = account_role.account_id AND assignment.assignment_type = 'OPERATOR'
));
