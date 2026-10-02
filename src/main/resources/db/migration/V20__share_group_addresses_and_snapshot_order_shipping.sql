RENAME TABLE account_address TO buyer_group_address;

ALTER TABLE buyer_group_address
    DROP FOREIGN KEY FK_account_address_account,
    DROP INDEX IX_account_address_account_default,
    CHANGE COLUMN account_id created_by_account_id BIGINT NOT NULL,
    ADD COLUMN public_id BINARY(16) NULL AFTER id,
    ADD COLUMN buyer_group_id BIGINT NULL AFTER public_id;

UPDATE buyer_group_address address
JOIN buyer_group_member member ON member.account_id = address.created_by_account_id
SET address.public_id = UUID_TO_BIN(UUID(), 0),
    address.buyer_group_id = member.buyer_group_id;

UPDATE buyer_group_address current_default
JOIN buyer_group_address earlier_default
    ON earlier_default.buyer_group_id = current_default.buyer_group_id
    AND earlier_default.is_default = TRUE
    AND earlier_default.id < current_default.id
SET current_default.is_default = FALSE
WHERE current_default.is_default = TRUE;

UPDATE buyer_group_address first_address
LEFT JOIN buyer_group_address earlier_address
    ON earlier_address.buyer_group_id = first_address.buyer_group_id
    AND earlier_address.id < first_address.id
LEFT JOIN buyer_group_address existing_default
    ON existing_default.buyer_group_id = first_address.buyer_group_id
    AND existing_default.is_default = TRUE
SET first_address.is_default = TRUE
WHERE first_address.is_default = FALSE
    AND earlier_address.id IS NULL
    AND existing_default.id IS NULL;

ALTER TABLE buyer_group_address
    MODIFY COLUMN public_id BINARY(16) NOT NULL,
    MODIFY COLUMN buyer_group_id BIGINT NOT NULL,
    ADD CONSTRAINT UQ_buyer_group_address_public_id UNIQUE (public_id),
    ADD INDEX IX_buyer_group_address_group_default (buyer_group_id, is_default),
    ADD CONSTRAINT FK_buyer_group_address_group FOREIGN KEY (buyer_group_id) REFERENCES buyer_group(id),
    ADD CONSTRAINT FK_buyer_group_address_creator FOREIGN KEY (created_by_account_id) REFERENCES account(id);

ALTER TABLE purchase_order
    ADD COLUMN shipping_recipient_name VARCHAR(100) NULL,
    ADD COLUMN shipping_recipient_phone VARCHAR(30) NULL,
    ADD COLUMN shipping_postal_code VARCHAR(20) NULL,
    ADD COLUMN shipping_address1 VARCHAR(255) NULL,
    ADD COLUMN shipping_address2 VARCHAR(255) NULL;
