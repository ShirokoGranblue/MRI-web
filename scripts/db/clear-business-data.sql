-- 清空 MRI 演示库的业务样例数据（保留登录账号 sys_user / sys_role / sys_user_role）。
-- 用法：
--   docker exec -i mri-mysql mysql -uroot -proot123456 mri_cloud < scripts/db/clear-business-data.sql
-- 或在 MySQL 客户端中先 USE mri_cloud; 再逐条执行下方语句。
-- 执行后业务表为空且自增 ID 重置；登录账号保持可用，可重新登记数据。

USE mri_cloud;
SET FOREIGN_KEY_CHECKS = 0;
TRUNCATE TABLE mri_report_audit_log;
TRUNCATE TABLE mri_report;
TRUNCATE TABLE mri_image_file;
TRUNCATE TABLE mri_series;
TRUNCATE TABLE mri_download_log;
TRUNCATE TABLE mri_study;
TRUNCATE TABLE mri_schedule;
TRUNCATE TABLE mri_contraindication;
TRUNCATE TABLE mri_exam_order;
TRUNCATE TABLE patient;
SET FOREIGN_KEY_CHECKS = 1;
