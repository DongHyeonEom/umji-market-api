CREATE TABLE sales_channel (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    public_id BINARY(16) NOT NULL,
    code VARCHAR(30) NOT NULL,
    name VARCHAR(100) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    CONSTRAINT UQ_sales_channel_public_id UNIQUE (public_id),
    CONSTRAINT UQ_sales_channel_code UNIQUE (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO sales_channel (public_id, code, name)
VALUES
    (UUID_TO_BIN(UUID()), 'WHOLESALE', '도매'),
    (UUID_TO_BIN(UUID()), 'RETAIL', '소매');

ALTER TABLE category
    ADD COLUMN sales_channel_id BIGINT NULL AFTER id;

UPDATE category
SET sales_channel_id = (SELECT id FROM sales_channel WHERE code = 'WHOLESALE');

ALTER TABLE category
    MODIFY COLUMN sales_channel_id BIGINT NOT NULL,
    ADD CONSTRAINT FK_category_sales_channel FOREIGN KEY (sales_channel_id) REFERENCES sales_channel(id),
    ADD INDEX IX_category_channel_parent_display (sales_channel_id, parent_id, display_order);

CREATE TABLE channel_product_listing (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    public_id BINARY(16) NOT NULL,
    sales_channel_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    category_id BIGINT NOT NULL,
    display_status VARCHAR(30) NOT NULL,
    display_order INT NOT NULL DEFAULT 0,
    version BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    CONSTRAINT UQ_channel_product_listing_public_id UNIQUE (public_id),
    CONSTRAINT UQ_channel_product_listing_channel_product UNIQUE (sales_channel_id, product_id),
    CONSTRAINT FK_channel_product_listing_channel FOREIGN KEY (sales_channel_id) REFERENCES sales_channel(id),
    CONSTRAINT FK_channel_product_listing_product FOREIGN KEY (product_id) REFERENCES product(id),
    CONSTRAINT FK_channel_product_listing_category FOREIGN KEY (category_id) REFERENCES category(id),
    INDEX IX_channel_product_listing_category_display (sales_channel_id, category_id, display_status, display_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE sales_offer (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    public_id BINARY(16) NOT NULL,
    sales_channel_id BIGINT NOT NULL,
    product_sku_id BIGINT NOT NULL,
    sale_price BIGINT NOT NULL,
    list_price BIGINT NULL,
    sales_status VARCHAR(30) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    CONSTRAINT UQ_sales_offer_public_id UNIQUE (public_id),
    CONSTRAINT UQ_sales_offer_channel_sku UNIQUE (sales_channel_id, product_sku_id),
    CONSTRAINT CK_sales_offer_sale_price CHECK (sale_price >= 0),
    CONSTRAINT CK_sales_offer_list_price CHECK (list_price IS NULL OR list_price >= 0),
    CONSTRAINT FK_sales_offer_channel FOREIGN KEY (sales_channel_id) REFERENCES sales_channel(id),
    CONSTRAINT FK_sales_offer_sku FOREIGN KEY (product_sku_id) REFERENCES product_sku(id),
    INDEX IX_sales_offer_sku_status (product_sku_id, sales_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO channel_product_listing (public_id, sales_channel_id, product_id, category_id, display_status, display_order)
SELECT UUID_TO_BIN(UUID()), sc.id, p.id, p.category_id, p.display_status, p.display_order
FROM product p
JOIN category c ON c.id = p.category_id
JOIN sales_channel sc ON sc.code = 'WHOLESALE'
WHERE p.deleted_at IS NULL AND c.deleted_at IS NULL;

INSERT INTO sales_offer (public_id, sales_channel_id, product_sku_id, sale_price, list_price, sales_status)
SELECT UUID_TO_BIN(UUID()), sc.id, sku.id, sku.sale_price, sku.list_price, sku.sales_status
FROM product_sku sku
JOIN product p ON p.id = sku.product_id AND p.deleted_at IS NULL
JOIN sales_channel sc ON sc.code = 'WHOLESALE';

ALTER TABLE cart_item
    ADD COLUMN sales_offer_id BIGINT NULL AFTER sku_id;

UPDATE cart_item ci
JOIN sales_offer offer ON offer.product_sku_id = ci.sku_id
JOIN sales_channel channel ON channel.id = offer.sales_channel_id AND channel.code = 'WHOLESALE'
SET ci.sales_offer_id = offer.id;

ALTER TABLE cart_item
    MODIFY COLUMN sales_offer_id BIGINT NOT NULL,
    ADD CONSTRAINT FK_cart_item_sales_offer FOREIGN KEY (sales_offer_id) REFERENCES sales_offer(id),
    ADD INDEX IX_cart_item_sales_offer (sales_offer_id);

ALTER TABLE purchase_order
    ADD COLUMN sales_channel_code VARCHAR(30) NOT NULL DEFAULT 'WHOLESALE' AFTER account_id;

ALTER TABLE order_item
    ADD COLUMN sales_offer_id BIGINT NULL AFTER sku_id;

UPDATE order_item oi
JOIN sales_offer offer ON offer.product_sku_id = oi.sku_id
JOIN sales_channel channel ON channel.id = offer.sales_channel_id AND channel.code = 'WHOLESALE'
SET oi.sales_offer_id = offer.id;

ALTER TABLE order_item
    MODIFY COLUMN sales_offer_id BIGINT NOT NULL,
    ADD CONSTRAINT FK_order_item_sales_offer FOREIGN KEY (sales_offer_id) REFERENCES sales_offer(id),
    ADD INDEX IX_order_item_sales_offer (sales_offer_id);
