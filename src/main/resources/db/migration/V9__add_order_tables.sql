CREATE TABLE purchase_order (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    public_id BINARY(16) NOT NULL,
    order_number VARCHAR(40) NOT NULL,
    account_id BIGINT NOT NULL,
    status VARCHAR(30) NOT NULL,
    subtotal_amount BIGINT NOT NULL,
    total_amount BIGINT NOT NULL,
    ordered_at DATETIME(3) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    CONSTRAINT UQ_purchase_order_public_id UNIQUE (public_id),
    CONSTRAINT UQ_purchase_order_number UNIQUE (order_number),
    CONSTRAINT CK_purchase_order_amount CHECK (subtotal_amount >= 0 AND total_amount >= 0),
    CONSTRAINT FK_purchase_order_account FOREIGN KEY (account_id) REFERENCES account(id),
    INDEX IX_purchase_order_account_ordered_at (account_id, ordered_at DESC),
    INDEX IX_purchase_order_status_ordered_at (status, ordered_at DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE order_number_sequence (
    order_date DATE PRIMARY KEY,
    sequence_value BIGINT NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE order_item (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    public_id BINARY(16) NOT NULL,
    order_id BIGINT NOT NULL,
    sku_id BIGINT NOT NULL,
    product_name VARCHAR(200) NOT NULL,
    sku_name VARCHAR(200) NOT NULL,
    sku_code VARCHAR(64) NOT NULL,
    unit_price BIGINT NOT NULL,
    quantity INT NOT NULL,
    line_amount BIGINT NOT NULL,
    reservation_key BINARY(16) NOT NULL,
    status VARCHAR(30) NOT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    CONSTRAINT UQ_order_item_public_id UNIQUE (public_id),
    CONSTRAINT UQ_order_item_reservation_key UNIQUE (reservation_key),
    CONSTRAINT CK_order_item_amount CHECK (unit_price >= 0 AND quantity > 0 AND line_amount >= 0),
    CONSTRAINT FK_order_item_order FOREIGN KEY (order_id) REFERENCES purchase_order(id),
    CONSTRAINT FK_order_item_sku FOREIGN KEY (sku_id) REFERENCES product_sku(id),
    INDEX IX_order_item_order (order_id),
    INDEX IX_order_item_sku (sku_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE order_status_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL,
    from_status VARCHAR(30) NULL,
    to_status VARCHAR(30) NOT NULL,
    reason_code VARCHAR(50) NULL,
    memo VARCHAR(500) NULL,
    changed_at DATETIME(3) NOT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    CONSTRAINT FK_order_status_history_order FOREIGN KEY (order_id) REFERENCES purchase_order(id),
    INDEX IX_order_status_history_order_changed_at (order_id, changed_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
