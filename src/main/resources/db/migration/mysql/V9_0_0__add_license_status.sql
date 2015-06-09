ALTER TABLE `compound_service_provider` ADD COLUMN license_status ENUM('HAS_LICENSE_SURFMARKET', 'HAS_LICENSE_SP', 'NO_LICENSE', 'NOT_NEEDED', 'UNKNOWN') DEFAULT 'NOT_NEEDED' NOT NULL;
UPDATE `compound_service_provider` SET `license_status` = 'HAS_LICENSE_SURFMARKET' WHERE  `lmng_id` IS NOT NULL;
