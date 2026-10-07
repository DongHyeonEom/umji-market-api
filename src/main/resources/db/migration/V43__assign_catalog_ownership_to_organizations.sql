ALTER TABLE brand
    DROP INDEX UQ_brand_name,
    ADD COLUMN organization_id BIGINT NULL AFTER public_id,
    ADD CONSTRAINT FK_brand_organization FOREIGN KEY (organization_id) REFERENCES organization(id),
    ADD CONSTRAINT UQ_brand_organization_name UNIQUE (organization_id, name),
    ADD INDEX IX_brand_organization (organization_id, deleted_at);

ALTER TABLE product
    ADD COLUMN organization_id BIGINT NULL AFTER public_id,
    ADD CONSTRAINT FK_product_organization FOREIGN KEY (organization_id) REFERENCES organization(id),
    ADD INDEX IX_product_organization_category (organization_id, category_id, deleted_at);

ALTER TABLE product_sku
    DROP INDEX UQ_product_sku_code,
    ADD CONSTRAINT UQ_product_sku_product_code UNIQUE (product_id, sku_code);
