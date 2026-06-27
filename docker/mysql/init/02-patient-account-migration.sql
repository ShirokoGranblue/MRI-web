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
VALUES ('PATIENT', '患者')
ON DUPLICATE KEY UPDATE role_name = VALUES(role_name);
