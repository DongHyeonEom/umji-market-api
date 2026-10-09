ALTER TABLE purchase_order
    ADD COLUMN order_source VARCHAR(30) NOT NULL DEFAULT 'CUSTOMER',
    ADD COLUMN created_by_account_id BIGINT NULL;

UPDATE purchase_order
SET created_by_account_id = account_id;

ALTER TABLE purchase_order
    MODIFY COLUMN created_by_account_id BIGINT NOT NULL,
    ADD CONSTRAINT FK_purchase_order_creator FOREIGN KEY (created_by_account_id) REFERENCES account(id),
    ADD CONSTRAINT CK_purchase_order_source CHECK (order_source IN ('CUSTOMER', 'ADMIN_PHONE'));

ALTER TABLE purchase_order_tax_invoice
    DROP CHECK CK_purchase_order_tax_invoice_snapshot_status,
    ADD CONSTRAINT CK_purchase_order_tax_invoice_snapshot_status
        CHECK (status IN ('LEGACY', 'WAITING_FOR_SHIPMENT', 'READY_FOR_ISSUANCE', 'MANUALLY_ISSUED'));

ALTER TABLE purchase_order_tax_invoice
    ADD COLUMN invoice_approval_number VARCHAR(100) NULL,
    ADD COLUMN issued_at DATE NULL,
    ADD COLUMN supply_amount BIGINT NULL,
    ADD COLUMN tax_amount BIGINT NULL,
    ADD COLUMN total_amount BIGINT NULL,
    ADD COLUMN issued_by_account_id BIGINT NULL,
    ADD CONSTRAINT FK_purchase_order_tax_invoice_issuer FOREIGN KEY (issued_by_account_id) REFERENCES account(id),
    ADD CONSTRAINT CK_purchase_order_tax_invoice_amounts CHECK (
        (supply_amount IS NULL AND tax_amount IS NULL AND total_amount IS NULL)
        OR (supply_amount >= 0 AND tax_amount >= 0 AND total_amount = supply_amount + tax_amount)
    );

CREATE TABLE purchase_order_tax_invoice_event (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL,
    event_type VARCHAR(30) NOT NULL,
    invoice_approval_number VARCHAR(100) NOT NULL,
    issued_at DATE NOT NULL,
    written_date DATE NULL,
    supply_date DATE NULL,
    supply_amount BIGINT NOT NULL,
    tax_amount BIGINT NOT NULL,
    total_amount BIGINT NOT NULL,
    processed_by_account_id BIGINT NOT NULL,
    reason VARCHAR(500) NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    CONSTRAINT UQ_purchase_order_tax_invoice_event_approval UNIQUE (invoice_approval_number),
    CONSTRAINT FK_purchase_order_tax_invoice_event_order FOREIGN KEY (order_id) REFERENCES purchase_order(id),
    CONSTRAINT FK_purchase_order_tax_invoice_event_processor FOREIGN KEY (processed_by_account_id) REFERENCES account(id),
    CONSTRAINT CK_purchase_order_tax_invoice_event_type CHECK (event_type IN ('MANUAL_ISSUED')),
    CONSTRAINT CK_purchase_order_tax_invoice_event_amounts CHECK (
        supply_amount >= 0 AND tax_amount >= 0 AND total_amount = supply_amount + tax_amount
    ),
    INDEX IX_purchase_order_tax_invoice_event_order_created (order_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
