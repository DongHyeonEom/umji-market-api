ALTER TABLE notification_outbox
    DROP CHECK CK_notification_outbox_status,
    ADD COLUMN lease_expires_at DATETIME(3) NULL AFTER next_attempt_at,
    ADD CONSTRAINT CK_notification_outbox_status CHECK (status IN ('PENDING', 'PROCESSING', 'SENT', 'FAILED'));

CREATE INDEX IX_notification_outbox_lease ON notification_outbox (status, lease_expires_at, id);
