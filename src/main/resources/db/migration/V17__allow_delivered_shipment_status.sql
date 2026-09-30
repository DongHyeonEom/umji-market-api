ALTER TABLE order_shipment
    DROP CHECK CK_order_shipment_status;

ALTER TABLE order_shipment
    ADD CONSTRAINT CK_order_shipment_status
        CHECK (status IN ('READY_TO_SHIP', 'PREPARING', 'IN_TRANSIT', 'DELIVERED'));
