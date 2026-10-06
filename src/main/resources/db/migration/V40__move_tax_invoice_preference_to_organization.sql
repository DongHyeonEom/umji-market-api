ALTER TABLE organization
    ADD COLUMN default_tax_invoice_requested BOOLEAN NOT NULL DEFAULT FALSE AFTER status;

UPDATE organization organization_record
LEFT JOIN account representative
    ON representative.id = organization_record.representative_account_id
LEFT JOIN (
    SELECT member.organization_id,
           account.default_tax_invoice_requested
    FROM organization_member member
    JOIN account ON account.id = member.account_id
    JOIN (
        SELECT organization_id, MIN(account_id) AS account_id
        FROM organization_member
        WHERE status = 'ACTIVE'
        GROUP BY organization_id
    ) first_member
        ON first_member.organization_id = member.organization_id
        AND first_member.account_id = member.account_id
    WHERE member.status = 'ACTIVE'
) member_preference
    ON member_preference.organization_id = organization_record.id
SET organization_record.default_tax_invoice_requested = COALESCE(
    representative.default_tax_invoice_requested,
    member_preference.default_tax_invoice_requested,
    FALSE
);

ALTER TABLE account
    DROP COLUMN default_tax_invoice_requested;
