ALTER TABLE order_payment
    DROP CHECK CK_order_payment_status;

ALTER TABLE order_payment
    ADD CONSTRAINT CK_order_payment_status
    CHECK (status IN (
        'WAITING_FOR_DEPOSIT',
        'PARTIAL_PAYMENT_REVIEW_REQUIRED',
        'PAYMENT_ISSUE_REVIEW_REQUIRED',
        'PAYMENT_CONFIRMED'
    ));
