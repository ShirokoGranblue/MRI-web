USE mri_cloud;

SET @column_exists = (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'mri_exam_order' AND COLUMN_NAME = 'risk_level'
);
SET @alter_sql = IF(
  @column_exists = 0,
  'ALTER TABLE mri_exam_order ADD COLUMN risk_level VARCHAR(16) NOT NULL DEFAULT ''UNKNOWN'' AFTER status',
  'SELECT 1'
);
PREPARE alter_stmt FROM @alter_sql;
EXECUTE alter_stmt;
DEALLOCATE PREPARE alter_stmt;

SET @column_exists = (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'mri_exam_order' AND COLUMN_NAME = 'risk_summary'
);
SET @alter_sql = IF(
  @column_exists = 0,
  'ALTER TABLE mri_exam_order ADD COLUMN risk_summary VARCHAR(1024) NULL AFTER risk_level',
  'SELECT 1'
);
PREPARE alter_stmt FROM @alter_sql;
EXECUTE alter_stmt;
DEALLOCATE PREPARE alter_stmt;

SET @column_exists = (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'mri_exam_order' AND COLUMN_NAME = 'risk_evaluated_at'
);
SET @alter_sql = IF(
  @column_exists = 0,
  'ALTER TABLE mri_exam_order ADD COLUMN risk_evaluated_at DATETIME NULL AFTER risk_summary',
  'SELECT 1'
);
PREPARE alter_stmt FROM @alter_sql;
EXECUTE alter_stmt;
DEALLOCATE PREPARE alter_stmt;

SET @column_exists = (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'mri_exam_order' AND COLUMN_NAME = 'risk_confirmed_by'
);
SET @alter_sql = IF(
  @column_exists = 0,
  'ALTER TABLE mri_exam_order ADD COLUMN risk_confirmed_by VARCHAR(64) NULL AFTER risk_evaluated_at',
  'SELECT 1'
);
PREPARE alter_stmt FROM @alter_sql;
EXECUTE alter_stmt;
DEALLOCATE PREPARE alter_stmt;

SET @column_exists = (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'mri_exam_order' AND COLUMN_NAME = 'risk_confirmed_at'
);
SET @alter_sql = IF(
  @column_exists = 0,
  'ALTER TABLE mri_exam_order ADD COLUMN risk_confirmed_at DATETIME NULL AFTER risk_confirmed_by',
  'SELECT 1'
);
PREPARE alter_stmt FROM @alter_sql;
EXECUTE alter_stmt;
DEALLOCATE PREPARE alter_stmt;

SET @column_exists = (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'mri_schedule' AND COLUMN_NAME = 'duration_minutes'
);
SET @alter_sql = IF(
  @column_exists = 0,
  'ALTER TABLE mri_schedule ADD COLUMN duration_minutes INT NOT NULL DEFAULT 30 AFTER scheduled_at',
  'SELECT 1'
);
PREPARE alter_stmt FROM @alter_sql;
EXECUTE alter_stmt;
DEALLOCATE PREPARE alter_stmt;

SET @column_exists = (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'mri_download_log' AND COLUMN_NAME = 'file_id'
);
SET @alter_sql = IF(
  @column_exists = 0,
  'ALTER TABLE mri_download_log ADD COLUMN file_id BIGINT NULL AFTER study_id',
  'SELECT 1'
);
PREPARE alter_stmt FROM @alter_sql;
EXECUTE alter_stmt;
DEALLOCATE PREPARE alter_stmt;

SET @column_exists = (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'mri_download_log' AND COLUMN_NAME = 'download_type'
);
SET @alter_sql = IF(
  @column_exists = 0,
  'ALTER TABLE mri_download_log ADD COLUMN download_type VARCHAR(32) NOT NULL DEFAULT ''LEGACY'' AFTER file_id',
  'SELECT 1'
);
PREPARE alter_stmt FROM @alter_sql;
EXECUTE alter_stmt;
DEALLOCATE PREPARE alter_stmt;
