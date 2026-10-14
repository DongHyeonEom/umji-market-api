ALTER TABLE order_item
    ADD COLUMN purchase_cost_snapshot BIGINT NULL AFTER line_amount;

UPDATE order_shipment_package_item item
JOIN (
    SELECT source.id, package.actual_shipping_cost,
           ROW_NUMBER() OVER (PARTITION BY source.package_id ORDER BY source.order_item_id) row_no,
           COUNT(*) OVER (PARTITION BY source.package_id) row_count
    FROM order_shipment_package_item source
    JOIN order_shipment_package package ON package.id = source.package_id
    WHERE package.actual_shipping_cost IS NOT NULL
) allocation ON allocation.id = item.id
SET item.line_shipping_cost_snapshot = FLOOR(allocation.actual_shipping_cost / allocation.row_count)
    + IF(allocation.row_no <= MOD(allocation.actual_shipping_cost, allocation.row_count), 1, 0);

CREATE TABLE inventory_cost_lot (
    id BIGINT NOT NULL AUTO_INCREMENT,
    organization_id BIGINT NULL,
    sku_id BIGINT NOT NULL,
    container_reference VARCHAR(120) NOT NULL,
    received_quantity INT NOT NULL,
    remaining_quantity INT NOT NULL,
    reserved_quantity INT NOT NULL DEFAULT 0,
    total_purchase_cost BIGINT NULL,
    remaining_cost_amount BIGINT NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT CK_inventory_cost_lot_quantities CHECK (received_quantity > 0 AND remaining_quantity >= 0 AND reserved_quantity >= 0 AND reserved_quantity <= remaining_quantity),
    CONSTRAINT CK_inventory_cost_lot_cost CHECK (total_purchase_cost IS NULL OR total_purchase_cost >= 0),
    CONSTRAINT FK_inventory_cost_lot_organization FOREIGN KEY (organization_id) REFERENCES organization(id),
    CONSTRAINT FK_inventory_cost_lot_sku FOREIGN KEY (sku_id) REFERENCES product_sku(id),
    INDEX IX_inventory_cost_lot_scope_fifo (organization_id, sku_id, id)
);

CREATE TABLE stock_reservation_lot_allocation (
    id BIGINT NOT NULL AUTO_INCREMENT,
    reservation_id BIGINT NOT NULL,
    lot_id BIGINT NOT NULL,
    quantity INT NOT NULL,
    cost_amount BIGINT NULL,
    status VARCHAR(20) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT CK_stock_reservation_lot_allocation_quantity CHECK (quantity > 0),
    CONSTRAINT FK_stock_reservation_lot_allocation_reservation FOREIGN KEY (reservation_id) REFERENCES stock_reservation(id),
    CONSTRAINT FK_stock_reservation_lot_allocation_lot FOREIGN KEY (lot_id) REFERENCES inventory_cost_lot(id),
    INDEX IX_stock_reservation_lot_allocation_reservation (reservation_id, id)
);

INSERT INTO inventory_cost_lot (organization_id, sku_id, container_reference, received_quantity, remaining_quantity, reserved_quantity)
SELECT organization_id, sku_id, CONCAT('OPENING-', id), on_hand_quantity, on_hand_quantity, reserved_quantity
FROM inventory_stock WHERE on_hand_quantity > 0;

INSERT INTO stock_reservation_lot_allocation (reservation_id, lot_id, quantity, status)
SELECT reservation.id, lot.id, reservation.quantity, 'RESERVED'
FROM stock_reservation reservation
JOIN inventory_stock stock ON stock.sku_id = reservation.sku_id AND stock.organization_id <=> reservation.organization_id
JOIN inventory_cost_lot lot ON lot.container_reference = CONCAT('OPENING-', stock.id)
WHERE reservation.status = 'RESERVED';
