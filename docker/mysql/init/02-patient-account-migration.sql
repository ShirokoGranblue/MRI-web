USE mri_cloud;

SET @account_column_exists = (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'patient'
    AND COLUMN_NAME = 'account_username'
);
SET @add_account_column = IF(
  @account_column_exists = 0,
  'ALTER TABLE patient ADD COLUMN account_username VARCHAR(64) NULL AFTER patient_no',
  'SELECT 1'
);
PREPARE add_account_column_stmt FROM @add_account_column;
EXECUTE add_account_column_stmt;
DEALLOCATE PREPARE add_account_column_stmt;

SET @account_index_exists = (
  SELECT COUNT(*)
  FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'patient'
    AND INDEX_NAME = 'uk_patient_account_username'
);
SET @add_account_index = IF(
  @account_index_exists = 0,
  'ALTER TABLE patient ADD UNIQUE KEY uk_patient_account_username (account_username)',
  'SELECT 1'
);
PREPARE add_account_index_stmt FROM @add_account_index;
EXECUTE add_account_index_stmt;
DEALLOCATE PREPARE add_account_index_stmt;

INSERT INTO sys_role (role_code, role_name)
VALUES ('PATIENT', CONVERT(0xE682A3E88085 USING utf8mb4))
ON DUPLICATE KEY UPDATE role_name = VALUES(role_name);

UPDATE sys_role
SET role_name = CASE role_code
  WHEN 'ADMIN' THEN CONVERT(0xE7AEA1E79086E59198 USING utf8mb4)
  WHEN 'REGISTRAR' THEN CONVERT(0xE799BBE8AEB0E4BABAE59198 USING utf8mb4)
  WHEN 'TECHNICIAN' THEN CONVERT(0xE68A80E5B888 USING utf8mb4)
  WHEN 'RADIOLOGIST' THEN CONVERT(0xE8AF8AE696ADE58CBBE7949F USING utf8mb4)
  WHEN 'AUDITOR' THEN CONVERT(0xE5AEA1E6A0B8E58CBBE7949F USING utf8mb4)
  WHEN 'PATIENT' THEN CONVERT(0xE682A3E88085 USING utf8mb4)
  ELSE role_name
END
WHERE role_code IN ('ADMIN', 'REGISTRAR', 'TECHNICIAN', 'RADIOLOGIST', 'AUDITOR', 'PATIENT');

UPDATE sys_user
SET display_name = CONVERT(0xE7B3BBE7BB9FE7AEA1E79086E59198 USING utf8mb4)
WHERE username = 'admin';
