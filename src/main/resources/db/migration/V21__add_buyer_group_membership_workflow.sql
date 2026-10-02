ALTER TABLE buyer_group
    ADD COLUMN representative_account_id BIGINT NULL AFTER display_name,
    ADD CONSTRAINT FK_buyer_group_representative_account FOREIGN KEY (representative_account_id) REFERENCES account(id),
    ADD INDEX IX_buyer_group_representative_account (representative_account_id);

UPDATE buyer_group
JOIN buyer_group_member ON buyer_group_member.buyer_group_id = buyer_group.id
SET buyer_group.representative_account_id = buyer_group_member.account_id;

ALTER TABLE buyer_group_member
    DROP INDEX UQ_buyer_group_member_account,
    ADD COLUMN active_account_id BIGINT GENERATED ALWAYS AS (CASE WHEN status = 'ACTIVE' THEN account_id ELSE NULL END) STORED,
    ADD CONSTRAINT UQ_buyer_group_member_active_account UNIQUE (active_account_id);

CREATE TABLE buyer_group_invitation (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    public_id BINARY(16) NOT NULL,
    buyer_group_id BIGINT NOT NULL,
    phone_normalized VARCHAR(30) NOT NULL,
    invited_by_account_id BIGINT NOT NULL,
    target_account_id BIGINT NULL,
    status VARCHAR(30) NOT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    responded_at DATETIME(3) NULL,
    pending_phone VARCHAR(30) GENERATED ALWAYS AS (CASE WHEN status = 'PENDING' THEN phone_normalized ELSE NULL END) STORED,
    CONSTRAINT UQ_buyer_group_invitation_public_id UNIQUE (public_id),
    CONSTRAINT UQ_buyer_group_invitation_pending_phone UNIQUE (buyer_group_id, pending_phone),
    CONSTRAINT FK_buyer_group_invitation_group FOREIGN KEY (buyer_group_id) REFERENCES buyer_group(id),
    CONSTRAINT FK_buyer_group_invitation_inviter FOREIGN KEY (invited_by_account_id) REFERENCES account(id),
    CONSTRAINT FK_buyer_group_invitation_target FOREIGN KEY (target_account_id) REFERENCES account(id),
    INDEX IX_buyer_group_invitation_phone_status (phone_normalized, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE buyer_group_join_request (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    public_id BINARY(16) NOT NULL,
    buyer_group_id BIGINT NOT NULL,
    account_id BIGINT NOT NULL,
    status VARCHAR(30) NOT NULL,
    requested_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    responded_at DATETIME(3) NULL,
    responded_by_account_id BIGINT NULL,
    pending_account_id BIGINT GENERATED ALWAYS AS (CASE WHEN status = 'PENDING' THEN account_id ELSE NULL END) STORED,
    CONSTRAINT UQ_buyer_group_join_request_public_id UNIQUE (public_id),
    CONSTRAINT UQ_buyer_group_join_request_pending_account UNIQUE (buyer_group_id, pending_account_id),
    CONSTRAINT FK_buyer_group_join_request_group FOREIGN KEY (buyer_group_id) REFERENCES buyer_group(id),
    CONSTRAINT FK_buyer_group_join_request_account FOREIGN KEY (account_id) REFERENCES account(id),
    CONSTRAINT FK_buyer_group_join_request_responder FOREIGN KEY (responded_by_account_id) REFERENCES account(id),
    INDEX IX_buyer_group_join_request_group_status (buyer_group_id, status, requested_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
