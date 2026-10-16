ALTER TABLE organization
    ADD COLUMN buyer_type VARCHAR(30) NOT NULL DEFAULT 'GENERAL_BUSINESS' AFTER organization_type,
    ADD COLUMN payment_before_shipment_required BOOLEAN NOT NULL DEFAULT FALSE AFTER default_tax_invoice_requested;

UPDATE organization
SET buyer_type = 'INDIVIDUAL'
WHERE organization_type = 'INDIVIDUAL';

ALTER TABLE organization
    ADD CONSTRAINT CK_organization_buyer_type
        CHECK (buyer_type IN ('WHOLESALE_BUSINESS', 'GENERAL_BUSINESS', 'INDIVIDUAL'));
