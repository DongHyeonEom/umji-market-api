ALTER TABLE account
    ADD COLUMN default_tax_invoice_requested BOOLEAN NOT NULL DEFAULT FALSE AFTER token_version;

ALTER TABLE purchase_order
    ADD COLUMN tax_invoice_requested BOOLEAN NOT NULL DEFAULT FALSE AFTER total_amount,
    ADD COLUMN deposit_bank_name VARCHAR(100) NULL AFTER tax_invoice_requested,
    ADD COLUMN deposit_account_number VARCHAR(100) NULL AFTER deposit_bank_name,
    ADD COLUMN deposit_account_holder VARCHAR(100) NULL AFTER deposit_account_number;
