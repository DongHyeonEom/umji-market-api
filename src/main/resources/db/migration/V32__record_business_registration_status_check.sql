ALTER TABLE buyer_group_business_profile
    ADD COLUMN business_registration_verified_at DATETIME(3) NULL AFTER business_registration_number;
