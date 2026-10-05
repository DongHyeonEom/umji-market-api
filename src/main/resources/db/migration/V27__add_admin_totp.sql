ALTER TABLE account
    ADD COLUMN admin_totp_secret_encrypted VARBINARY(512) NULL,
    ADD COLUMN admin_totp_enabled BOOLEAN NOT NULL DEFAULT FALSE;
