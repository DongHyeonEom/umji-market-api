CREATE TABLE cart (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    public_id BINARY(16) NOT NULL,
    account_id BIGINT NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    CONSTRAINT UQ_cart_public_id UNIQUE (public_id),
    CONSTRAINT UQ_cart_account UNIQUE (account_id),
    CONSTRAINT FK_cart_account FOREIGN KEY (account_id) REFERENCES account(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE cart_item (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    public_id BINARY(16) NOT NULL,
    cart_id BIGINT NOT NULL,
    sku_id BIGINT NOT NULL,
    quantity INT NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    CONSTRAINT UQ_cart_item_public_id UNIQUE (public_id),
    CONSTRAINT UQ_cart_item_sku UNIQUE (cart_id, sku_id),
    CONSTRAINT CK_cart_item_quantity CHECK (quantity > 0),
    CONSTRAINT FK_cart_item_cart FOREIGN KEY (cart_id) REFERENCES cart(id),
    CONSTRAINT FK_cart_item_sku FOREIGN KEY (sku_id) REFERENCES product_sku(id),
    INDEX IX_cart_item_sku (sku_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
