ALTER TABLE buyer_group_business_profile
    ADD COLUMN business_registration_verification_status VARCHAR(30) NOT NULL DEFAULT 'PENDING' AFTER business_registration_verified_at,
    ADD COLUMN business_registration_confirmed_at DATETIME(3) NULL AFTER business_registration_verification_status;

UPDATE buyer_group_business_profile
SET business_registration_verification_status = 'ACTIVE'
WHERE business_registration_verified_at IS NOT NULL;

UPDATE buyer_group_business_profile
SET business_registration_verification_status = 'NOT_REQUIRED'
WHERE business_registration_number IS NULL OR TRIM(business_registration_number) = '';
