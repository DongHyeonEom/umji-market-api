CREATE TABLE order_payment (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL,
    payment_method VARCHAR(40) NOT NULL,
    status VARCHAR(40) NOT NULL,
    updated_at DATETIME(3) NOT NULL,
    CONSTRAINT UQ_order_payment_order UNIQUE (order_id),
    CONSTRAINT CK_order_payment_status CHECK (status IN ('WAITING_FOR_DEPOSIT', 'PARTIAL_PAYMENT_REVIEW_REQUIRED', 'PAYMENT_CONFIRMED')),
    CONSTRAINT FK_order_payment_order FOREIGN KEY (order_id) REFERENCES purchase_order(id),
    INDEX IX_order_payment_status_updated_at (status, updated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE order_payment_status_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    payment_id BIGINT NOT NULL,
    from_status VARCHAR(40) NULL,
    to_status VARCHAR(40) NOT NULL,
    processed_by BIGINT NULL,
    changed_at DATETIME(3) NOT NULL,
    CONSTRAINT FK_order_payment_history_payment FOREIGN KEY (payment_id) REFERENCES order_payment(id),
    CONSTRAINT FK_order_payment_history_processor FOREIGN KEY (processed_by) REFERENCES account(id),
    INDEX IX_order_payment_history_payment_changed_at (payment_id, changed_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO order_payment (order_id, payment_method, status, updated_at)
SELECT id, 'BANK_TRANSFER', IF(status = 'PAID', 'PAYMENT_CONFIRMED', 'WAITING_FOR_DEPOSIT'), ordered_at
FROM purchase_order;

INSERT INTO order_payment_status_history (payment_id, from_status, to_status, changed_at)
SELECT payment.id, NULL, payment.status, purchase_order.ordered_at
FROM order_payment payment
JOIN purchase_order ON purchase_order.id = payment.order_id;
