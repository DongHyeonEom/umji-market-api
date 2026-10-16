ALTER TABLE product
    ADD COLUMN wholesale_minimum_quantity INT NOT NULL DEFAULT 5 AFTER display_order,
    ADD CONSTRAINT CK_product_wholesale_minimum_quantity CHECK (wholesale_minimum_quantity BETWEEN 1 AND 5);

ALTER TABLE sales_offer
    ADD COLUMN retail_sale_price BIGINT NOT NULL DEFAULT 0 AFTER sale_price,
    ADD COLUMN wholesale_sale_price BIGINT NOT NULL DEFAULT 0 AFTER retail_sale_price,
    ADD CONSTRAINT CK_sales_offer_retail_sale_price CHECK (retail_sale_price >= 0),
    ADD CONSTRAINT CK_sales_offer_wholesale_sale_price CHECK (wholesale_sale_price >= 0);

UPDATE sales_offer offer
JOIN sales_channel channel ON channel.id = offer.sales_channel_id
LEFT JOIN sales_channel other_channel
    ON other_channel.code = CASE channel.code WHEN 'WHOLESALE' THEN 'RETAIL' ELSE 'WHOLESALE' END
LEFT JOIN sales_offer other_offer
    ON other_offer.sales_channel_id = other_channel.id
    AND other_offer.product_sku_id = offer.product_sku_id
    AND (other_offer.organization_id = offer.organization_id OR (other_offer.organization_id IS NULL AND offer.organization_id IS NULL))
SET offer.retail_sale_price = CASE
        WHEN channel.code = 'RETAIL' THEN offer.sale_price * COALESCE(other_offer.units_per_sale, 1)
        ELSE COALESCE(other_offer.sale_price * offer.units_per_sale, offer.sale_price)
    END,
    offer.wholesale_sale_price = CASE
        WHEN channel.code = 'WHOLESALE' THEN offer.sale_price
        ELSE COALESCE(other_offer.sale_price, offer.sale_price)
    END;

UPDATE cart_item cart
JOIN sales_offer old_offer ON old_offer.id = cart.sales_offer_id
JOIN sales_channel old_channel ON old_channel.id = old_offer.sales_channel_id AND old_channel.code = 'RETAIL'
JOIN sales_channel wholesale_channel ON wholesale_channel.code = 'WHOLESALE'
JOIN sales_offer wholesale_offer
    ON wholesale_offer.sales_channel_id = wholesale_channel.id
    AND wholesale_offer.product_sku_id = old_offer.product_sku_id
    AND (wholesale_offer.organization_id = old_offer.organization_id OR (wholesale_offer.organization_id IS NULL AND old_offer.organization_id IS NULL))
SET cart.sales_offer_id = wholesale_offer.id;
