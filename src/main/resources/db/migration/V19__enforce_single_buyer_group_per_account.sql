UPDATE buyer_group
SET public_id = UUID_TO_BIN(BIN_TO_UUID(public_id, 1), 0);

ALTER TABLE buyer_group_member
    ADD CONSTRAINT UQ_buyer_group_member_account UNIQUE (account_id);

ALTER TABLE purchase_order
    MODIFY buyer_group_id BIGINT NOT NULL;
