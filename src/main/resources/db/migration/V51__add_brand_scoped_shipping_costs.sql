INSERT INTO permission (code, name)
VALUES ('SHIPMENT_ASSIGN', '브랜드 배송 담당자 배정')
ON DUPLICATE KEY UPDATE name = VALUES(name);

INSERT INTO role_permission (role_id, permission_id)
SELECT role.id, permission.id
FROM role
JOIN permission ON permission.code = 'SHIPMENT_ASSIGN'
WHERE role.code IN ('ADMIN', 'SUPER_ADMIN')
ON DUPLICATE KEY UPDATE permission_id = VALUES(permission_id);

CREATE TABLE brand_shipping_manager_assignment (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    brand_id BIGINT NOT NULL,
    account_id BIGINT NOT NULL,
    assigned_by_account_id BIGINT NOT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    CONSTRAINT UQ_brand_shipping_manager_brand_account UNIQUE (brand_id, account_id),
    CONSTRAINT FK_brand_shipping_manager_brand FOREIGN KEY (brand_id) REFERENCES brand(id),
    CONSTRAINT FK_brand_shipping_manager_account FOREIGN KEY (account_id) REFERENCES account(id),
    CONSTRAINT FK_brand_shipping_manager_assigner FOREIGN KEY (assigned_by_account_id) REFERENCES account(id),
    INDEX IX_brand_shipping_manager_account (account_id, brand_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE brand_shipping_rate (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    brand_id BIGINT NOT NULL,
    sku_id BIGINT NOT NULL,
    carrier_code VARCHAR(80) NOT NULL,
    amount_per_box BIGINT NOT NULL,
    updated_by_account_id BIGINT NOT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    CONSTRAINT UQ_brand_shipping_rate_brand_sku_carrier UNIQUE (brand_id, sku_id, carrier_code),
    CONSTRAINT CK_brand_shipping_rate_amount CHECK (amount_per_box >= 0),
    CONSTRAINT FK_brand_shipping_rate_brand FOREIGN KEY (brand_id) REFERENCES brand(id),
    CONSTRAINT FK_brand_shipping_rate_sku FOREIGN KEY (sku_id) REFERENCES product_sku(id),
    CONSTRAINT FK_brand_shipping_rate_updater FOREIGN KEY (updated_by_account_id) REFERENCES account(id),
    INDEX IX_brand_shipping_rate_sku_carrier (sku_id, carrier_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE order_shipment_package (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    public_id BINARY(16) NOT NULL,
    order_shipment_id BIGINT NOT NULL,
    brand_id BIGINT NULL,
    carrier_code VARCHAR(80) NOT NULL,
    tracking_number VARCHAR(100) NOT NULL,
    actual_shipping_cost BIGINT NULL,
    status VARCHAR(40) NOT NULL DEFAULT 'IN_TRANSIT',
    processed_by BIGINT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    CONSTRAINT UQ_order_shipment_package_public_id UNIQUE (public_id),
    CONSTRAINT UQ_order_shipment_package_tracking UNIQUE (order_shipment_id, carrier_code, tracking_number),
    CONSTRAINT CK_order_shipment_package_cost CHECK (actual_shipping_cost IS NULL OR actual_shipping_cost >= 0),
    CONSTRAINT CK_order_shipment_package_status CHECK (status IN ('IN_TRANSIT', 'DELIVERED')),
    CONSTRAINT FK_order_shipment_package_shipment FOREIGN KEY (order_shipment_id) REFERENCES order_shipment(id),
    CONSTRAINT FK_order_shipment_package_brand FOREIGN KEY (brand_id) REFERENCES brand(id),
    CONSTRAINT FK_order_shipment_package_processor FOREIGN KEY (processed_by) REFERENCES account(id),
    INDEX IX_order_shipment_package_shipment_status (order_shipment_id, status),
    INDEX IX_order_shipment_package_status_updated (status, updated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO order_shipment_package (
    public_id, order_shipment_id, brand_id, carrier_code, tracking_number, actual_shipping_cost,
    status, processed_by, created_at, updated_at
)
SELECT UUID_TO_BIN(UUID()), shipment.id,
       (
           SELECT CASE WHEN COUNT(DISTINCT brand.id) = 1 THEN MIN(brand.id) ELSE NULL END
           FROM order_item item
           JOIN product_sku sku ON sku.id = item.sku_id
           JOIN product ON product.id = sku.product_id
           LEFT JOIN brand ON brand.id = product.brand_id
           WHERE item.order_id = shipment.order_id
       ),
       shipment.carrier_code, shipment.tracking_number, NULL,
       IF(shipment.status = 'DELIVERED', 'DELIVERED', 'IN_TRANSIT'), shipment.processed_by, shipment.created_at, shipment.updated_at
FROM order_shipment shipment
WHERE shipment.carrier_code IS NOT NULL AND shipment.tracking_number IS NOT NULL;

UPDATE order_shipment
SET carrier_code = NULL, tracking_number = NULL, processed_by = NULL
WHERE carrier_code IS NOT NULL OR tracking_number IS NOT NULL;

CREATE TABLE order_shipment_package_item (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    package_id BIGINT NOT NULL,
    order_item_id BIGINT NOT NULL,
    box_quantity INT NOT NULL,
    amount_per_box_snapshot BIGINT NULL,
    line_shipping_cost_snapshot BIGINT NULL,
    CONSTRAINT UQ_order_shipment_package_item UNIQUE (package_id, order_item_id),
    CONSTRAINT CK_order_shipment_package_item_quantity CHECK (box_quantity > 0),
    CONSTRAINT FK_order_shipment_package_item_package FOREIGN KEY (package_id) REFERENCES order_shipment_package(id),
    CONSTRAINT FK_order_shipment_package_item_order_item FOREIGN KEY (order_item_id) REFERENCES order_item(id),
    INDEX IX_order_shipment_package_item_order_item (order_item_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO order_shipment_package_item (
    package_id, order_item_id, box_quantity, amount_per_box_snapshot, line_shipping_cost_snapshot
)
SELECT shipmentPackage.id, item.id, item.quantity, NULL, NULL
FROM order_shipment_package shipmentPackage
JOIN order_shipment shipment ON shipment.id = shipmentPackage.order_shipment_id
JOIN order_item item ON item.order_id = shipment.order_id;
