CREATE TABLE order_shipment (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL,
    status VARCHAR(40) NOT NULL,
    carrier_code VARCHAR(80) NULL,
    tracking_number VARCHAR(100) NULL,
    processed_by BIGINT NULL,
    created_at DATETIME(3) NOT NULL,
    updated_at DATETIME(3) NOT NULL,
    CONSTRAINT UQ_order_shipment_order UNIQUE (order_id),
    CONSTRAINT CK_order_shipment_status CHECK (status IN ('READY_TO_SHIP', 'PREPARING', 'IN_TRANSIT')),
    CONSTRAINT FK_order_shipment_order FOREIGN KEY (order_id) REFERENCES purchase_order(id),
    CONSTRAINT FK_order_shipment_processor FOREIGN KEY (processed_by) REFERENCES account(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO order_shipment (order_id, status, created_at, updated_at)
SELECT id, 'READY_TO_SHIP', ordered_at, ordered_at
FROM purchase_order;
