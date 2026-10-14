ALTER TABLE operation_audit_log
    ADD COLUMN before_values_json JSON NULL AFTER occurred_at,
    ADD COLUMN after_values_json JSON NULL AFTER before_values_json,
    ADD COLUMN reason VARCHAR(500) NULL AFTER after_values_json;

CREATE INDEX IX_order_shipment_package_brand_created
    ON order_shipment_package (brand_id, created_at, id);

CREATE INDEX IX_purchase_order_ordered_at
    ON purchase_order (ordered_at, id);
