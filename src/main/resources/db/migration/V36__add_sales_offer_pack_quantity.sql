ALTER TABLE sales_offer
    ADD COLUMN units_per_sale INT NOT NULL DEFAULT 1 AFTER sale_price,
    ADD CONSTRAINT CK_sales_offer_units_per_sale CHECK (units_per_sale > 0);

ALTER TABLE order_item
    ADD COLUMN units_per_sale INT NOT NULL DEFAULT 1 AFTER quantity,
    ADD CONSTRAINT CK_order_item_units_per_sale CHECK (units_per_sale > 0);
