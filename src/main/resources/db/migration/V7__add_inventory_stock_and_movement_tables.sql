CREATE TABLE inventory_stock (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    sku_id BIGINT NOT NULL,
    on_hand_quantity INT NOT NULL DEFAULT 0,
    reserved_quantity INT NOT NULL DEFAULT 0,
    safety_stock_quantity INT NOT NULL DEFAULT 0,
    version BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    CONSTRAINT UQ_inventory_stock_sku UNIQUE (sku_id),
    CONSTRAINT CK_inventory_stock_non_negative CHECK (on_hand_quantity >= 0 AND reserved_quantity >= 0 AND safety_stock_quantity >= 0),
    CONSTRAINT CK_inventory_stock_reservation CHECK (reserved_quantity <= on_hand_quantity),
    CONSTRAINT FK_inventory_stock_sku FOREIGN KEY (sku_id) REFERENCES product_sku(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE inventory_movement (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    sku_id BIGINT NOT NULL,
    movement_type VARCHAR(30) NOT NULL,
    quantity_delta INT NOT NULL,
    reference_type VARCHAR(50) NULL,
    reference_id BINARY(16) NULL,
    memo VARCHAR(500) NULL,
    occurred_at DATETIME(3) NOT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    CONSTRAINT FK_inventory_movement_sku FOREIGN KEY (sku_id) REFERENCES product_sku(id),
    INDEX IX_inventory_movement_sku_occurred_at (sku_id, occurred_at DESC),
    INDEX IX_inventory_movement_reference (reference_type, reference_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE stock_reservation (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    reservation_key BINARY(16) NOT NULL,
    sku_id BIGINT NOT NULL,
    quantity INT NOT NULL,
    status VARCHAR(30) NOT NULL,
    expires_at DATETIME(3) NULL,
    released_at DATETIME(3) NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    CONSTRAINT UQ_stock_reservation_key UNIQUE (reservation_key),
    CONSTRAINT CK_stock_reservation_quantity CHECK (quantity > 0),
    CONSTRAINT FK_stock_reservation_sku FOREIGN KEY (sku_id) REFERENCES product_sku(id),
    INDEX IX_stock_reservation_status_expires_at (status, expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
