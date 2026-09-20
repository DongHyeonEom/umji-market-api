CREATE TABLE product_image (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    public_id BINARY(16) NOT NULL,
    product_id BIGINT NOT NULL,
    storage_key VARCHAR(500) NOT NULL,
    alt_text VARCHAR(200) NULL,
    display_order INT NOT NULL DEFAULT 0,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    CONSTRAINT UQ_product_image_public_id UNIQUE (public_id),
    CONSTRAINT UQ_product_image_storage_key UNIQUE (storage_key),
    CONSTRAINT FK_product_image_product FOREIGN KEY (product_id) REFERENCES product(id),
    CONSTRAINT CK_product_image_display_order CHECK (display_order >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX IX_product_image_product_display_order ON product_image(product_id, display_order);

CREATE TABLE product_option (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    public_id BINARY(16) NOT NULL,
    product_id BIGINT NOT NULL,
    name VARCHAR(100) NOT NULL,
    display_order INT NOT NULL DEFAULT 0,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    CONSTRAINT UQ_product_option_public_id UNIQUE (public_id),
    CONSTRAINT UQ_product_option_name UNIQUE (product_id, name),
    CONSTRAINT FK_product_option_product FOREIGN KEY (product_id) REFERENCES product(id),
    CONSTRAINT CK_product_option_display_order CHECK (display_order >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX IX_product_option_product_display_order ON product_option(product_id, display_order);

CREATE TABLE product_option_value (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    public_id BINARY(16) NOT NULL,
    product_option_id BIGINT NOT NULL,
    value VARCHAR(100) NOT NULL,
    display_order INT NOT NULL DEFAULT 0,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    CONSTRAINT UQ_product_option_value_public_id UNIQUE (public_id),
    CONSTRAINT UQ_product_option_value_name UNIQUE (product_option_id, value),
    CONSTRAINT FK_product_option_value_option FOREIGN KEY (product_option_id) REFERENCES product_option(id),
    CONSTRAINT CK_product_option_value_display_order CHECK (display_order >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX IX_product_option_value_option_display_order ON product_option_value(product_option_id, display_order);

CREATE TABLE product_sku_option_value (
    product_sku_id BIGINT NOT NULL,
    product_option_value_id BIGINT NOT NULL,
    PRIMARY KEY (product_sku_id, product_option_value_id),
    CONSTRAINT FK_product_sku_option_value_sku FOREIGN KEY (product_sku_id) REFERENCES product_sku(id),
    CONSTRAINT FK_product_sku_option_value_value FOREIGN KEY (product_option_value_id) REFERENCES product_option_value(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
