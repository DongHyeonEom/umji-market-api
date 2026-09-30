CREATE TABLE shipping_holiday (
    holiday_date DATE PRIMARY KEY,
    description VARCHAR(200) NULL,
    created_by BIGINT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    CONSTRAINT FK_shipping_holiday_creator FOREIGN KEY (created_by) REFERENCES account(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE order_cancellation_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL,
    requested_by BIGINT NOT NULL,
    request_status VARCHAR(30) NOT NULL,
    processed_by BIGINT NULL,
    requested_at DATETIME(3) NOT NULL,
    processed_at DATETIME(3) NULL,
    CONSTRAINT CK_order_cancellation_status CHECK (request_status IN ('CANCELLED', 'PENDING', 'APPROVED', 'REJECTED')),
    CONSTRAINT FK_order_cancellation_order FOREIGN KEY (order_id) REFERENCES purchase_order(id),
    CONSTRAINT FK_order_cancellation_requester FOREIGN KEY (requested_by) REFERENCES account(id),
    CONSTRAINT FK_order_cancellation_processor FOREIGN KEY (processed_by) REFERENCES account(id),
    INDEX IX_order_cancellation_status_requested_at (request_status, requested_at),
    INDEX IX_order_cancellation_order_requested_at (order_id, requested_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

ALTER TABLE order_payment
    DROP CHECK CK_order_payment_status;

ALTER TABLE order_payment
    ADD CONSTRAINT CK_order_payment_status CHECK (status IN (
        'WAITING_FOR_DEPOSIT', 'PARTIAL_PAYMENT_REVIEW_REQUIRED', 'PAYMENT_ISSUE_REVIEW_REQUIRED',
        'PAYMENT_CONFIRMED', 'REFUND_PENDING', 'REFUNDED'
    ));
