ALTER TABLE sales_offer
    DROP FOREIGN KEY FK_sales_offer_channel;

ALTER TABLE sales_offer
    DROP INDEX UQ_sales_offer_channel_sku,
    ADD COLUMN organization_id BIGINT NULL AFTER sales_channel_id,
    ADD CONSTRAINT FK_sales_offer_organization FOREIGN KEY (organization_id) REFERENCES organization(id),
    ADD CONSTRAINT UQ_sales_offer_organization_channel_sku UNIQUE (organization_id, sales_channel_id, product_sku_id),
    ADD INDEX IX_sales_offer_channel (sales_channel_id);

ALTER TABLE sales_offer
    ADD CONSTRAINT FK_sales_offer_channel FOREIGN KEY (sales_channel_id) REFERENCES sales_channel(id);

ALTER TABLE cart_item
    DROP FOREIGN KEY FK_cart_item_cart;

ALTER TABLE cart_item
    DROP INDEX UQ_cart_item_sku,
    ADD CONSTRAINT UQ_cart_item_sales_offer UNIQUE (cart_id, sales_offer_id);

ALTER TABLE cart_item
    ADD CONSTRAINT FK_cart_item_cart FOREIGN KEY (cart_id) REFERENCES cart(id);

ALTER TABLE inventory_stock
    DROP FOREIGN KEY FK_inventory_stock_sku;

ALTER TABLE inventory_stock
    DROP INDEX UQ_inventory_stock_sku,
    ADD COLUMN organization_id BIGINT NULL AFTER id,
    ADD CONSTRAINT FK_inventory_stock_organization FOREIGN KEY (organization_id) REFERENCES organization(id),
    ADD CONSTRAINT UQ_inventory_stock_organization_sku UNIQUE (organization_id, sku_id),
    ADD INDEX IX_inventory_stock_sku (sku_id);

ALTER TABLE inventory_stock
    ADD CONSTRAINT FK_inventory_stock_sku FOREIGN KEY (sku_id) REFERENCES product_sku(id);

ALTER TABLE inventory_movement
    ADD COLUMN organization_id BIGINT NULL AFTER id,
    ADD CONSTRAINT FK_inventory_movement_organization FOREIGN KEY (organization_id) REFERENCES organization(id),
    ADD INDEX IX_inventory_movement_organization_sku_occurred_at (organization_id, sku_id, occurred_at DESC);

ALTER TABLE stock_reservation
    ADD COLUMN organization_id BIGINT NULL AFTER id,
    ADD CONSTRAINT FK_stock_reservation_organization FOREIGN KEY (organization_id) REFERENCES organization(id),
    ADD INDEX IX_stock_reservation_organization_sku (organization_id, sku_id);
