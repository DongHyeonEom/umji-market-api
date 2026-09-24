CREATE TABLE category (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    public_id BINARY(16) NOT NULL,
    parent_id BIGINT NULL,
    name VARCHAR(100) NOT NULL,
    path VARCHAR(1000) NOT NULL,
    depth INT NOT NULL,
    display_order INT NOT NULL DEFAULT 0,
    display_status VARCHAR(30) NOT NULL,
    deleted_at DATETIME(3) NULL,
    deleted_by BIGINT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    CONSTRAINT UQ_category_public_id UNIQUE (public_id),
    CONSTRAINT FK_category_parent FOREIGN KEY (parent_id) REFERENCES category(id),
    CONSTRAINT CK_category_depth CHECK (depth >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX IX_category_parent_display_order ON category(parent_id, display_order);
CREATE INDEX IX_category_path ON category(path(191));

CREATE TABLE brand (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    public_id BINARY(16) NOT NULL,
    name VARCHAR(100) NOT NULL,
    display_status VARCHAR(30) NOT NULL,
    deleted_at DATETIME(3) NULL,
    deleted_by BIGINT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    CONSTRAINT UQ_brand_public_id UNIQUE (public_id),
    CONSTRAINT UQ_brand_name UNIQUE (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE product (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    public_id BINARY(16) NOT NULL,
    category_id BIGINT NOT NULL,
    brand_id BIGINT NULL,
    name VARCHAR(200) NOT NULL,
    description TEXT NULL,
    display_status VARCHAR(30) NOT NULL,
    sales_status VARCHAR(30) NOT NULL,
    display_order INT NOT NULL DEFAULT 0,
    deleted_at DATETIME(3) NULL,
    deleted_by BIGINT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    CONSTRAINT UQ_product_public_id UNIQUE (public_id),
    CONSTRAINT FK_product_category FOREIGN KEY (category_id) REFERENCES category(id),
    CONSTRAINT FK_product_brand FOREIGN KEY (brand_id) REFERENCES brand(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX IX_product_category_display ON product(category_id, display_status, display_order);
CREATE INDEX IX_product_brand ON product(brand_id);

CREATE TABLE product_sku (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    public_id BINARY(16) NOT NULL,
    product_id BIGINT NOT NULL,
    sku_code VARCHAR(64) NOT NULL,
    name VARCHAR(200) NOT NULL,
    sale_price BIGINT NOT NULL,
    list_price BIGINT NULL,
    sales_status VARCHAR(30) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    CONSTRAINT UQ_product_sku_public_id UNIQUE (public_id),
    CONSTRAINT UQ_product_sku_code UNIQUE (sku_code),
    CONSTRAINT CK_product_sku_sale_price CHECK (sale_price >= 0),
    CONSTRAINT CK_product_sku_list_price CHECK (list_price IS NULL OR list_price >= 0),
    CONSTRAINT FK_product_sku_product FOREIGN KEY (product_id) REFERENCES product(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX IX_product_sku_product_sales ON product_sku(product_id, sales_status);
