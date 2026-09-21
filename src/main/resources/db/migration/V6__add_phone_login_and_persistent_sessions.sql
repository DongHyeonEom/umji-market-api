ALTER TABLE account
    ADD COLUMN phone_normalized VARCHAR(30) NULL AFTER phone;

UPDATE account
SET phone_normalized =
    CASE
        WHEN REGEXP_REPLACE(phone, '[^0-9]', '') = '' THEN NULL
        WHEN REGEXP_REPLACE(phone, '[^0-9]', '') LIKE '82%'
            THEN CONCAT('0', SUBSTRING(REGEXP_REPLACE(phone, '[^0-9]', ''), 3))
        ELSE REGEXP_REPLACE(phone, '[^0-9]', '')
    END
WHERE phone IS NOT NULL;

ALTER TABLE account
    ADD CONSTRAINT UQ_account_phone_normalized UNIQUE (phone_normalized);

ALTER TABLE refresh_token
    MODIFY expires_at DATETIME(3) NULL,
    ADD COLUMN last_used_at DATETIME(3) NULL AFTER revoked_at;
