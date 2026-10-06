ALTER TABLE buyer_group_business_profile
    ADD COLUMN business_industry VARCHAR(100) NULL AFTER business_phone,
    ADD COLUMN business_item VARCHAR(100) NULL AFTER business_industry,
    ADD COLUMN tax_invoice_email VARCHAR(255) NULL AFTER business_item;

ALTER TABLE purchase_order
    ADD COLUMN tax_invoice_status VARCHAR(30) NULL AFTER tax_invoice_requested,
    ADD COLUMN tax_invoice_written_date DATE NULL AFTER tax_invoice_status,
    ADD COLUMN tax_invoice_supply_date DATE NULL AFTER tax_invoice_written_date,
    ADD COLUMN tax_invoice_supplier_registration_number VARCHAR(30) NULL AFTER tax_invoice_supply_date,
    ADD COLUMN tax_invoice_supplier_business_name VARCHAR(200) NULL AFTER tax_invoice_supplier_registration_number,
    ADD COLUMN tax_invoice_supplier_name VARCHAR(100) NULL AFTER tax_invoice_supplier_business_name,
    ADD COLUMN tax_invoice_supplier_address VARCHAR(500) NULL AFTER tax_invoice_supplier_name,
    ADD COLUMN tax_invoice_supplier_industry VARCHAR(100) NULL AFTER tax_invoice_supplier_address,
    ADD COLUMN tax_invoice_supplier_item VARCHAR(100) NULL AFTER tax_invoice_supplier_industry,
    ADD COLUMN tax_invoice_supplier_email VARCHAR(255) NULL AFTER tax_invoice_supplier_item,
    ADD COLUMN tax_invoice_buyer_registration_number VARCHAR(30) NULL AFTER tax_invoice_supplier_email,
    ADD COLUMN tax_invoice_buyer_business_name VARCHAR(200) NULL AFTER tax_invoice_buyer_registration_number,
    ADD COLUMN tax_invoice_buyer_name VARCHAR(100) NULL AFTER tax_invoice_buyer_business_name,
    ADD COLUMN tax_invoice_buyer_postal_code VARCHAR(20) NULL AFTER tax_invoice_buyer_name,
    ADD COLUMN tax_invoice_buyer_address1 VARCHAR(255) NULL AFTER tax_invoice_buyer_postal_code,
    ADD COLUMN tax_invoice_buyer_address2 VARCHAR(255) NULL AFTER tax_invoice_buyer_address1,
    ADD COLUMN tax_invoice_buyer_industry VARCHAR(100) NULL AFTER tax_invoice_buyer_address2,
    ADD COLUMN tax_invoice_buyer_item VARCHAR(100) NULL AFTER tax_invoice_buyer_industry,
    ADD COLUMN tax_invoice_buyer_email VARCHAR(255) NULL AFTER tax_invoice_buyer_item,
    ADD CONSTRAINT CK_purchase_order_tax_invoice_status CHECK (
        tax_invoice_status IS NULL OR tax_invoice_status IN ('LEGACY', 'WAITING_FOR_SHIPMENT', 'READY_FOR_ISSUANCE')
    );

UPDATE purchase_order
SET tax_invoice_status = 'LEGACY'
WHERE tax_invoice_requested = TRUE;
