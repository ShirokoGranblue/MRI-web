-- 清空 MRI 运行验证数据，保留 admin 医生账号和系统角色定义。
-- 用法：
--   docker exec -i mri-mysql mysql -uroot -proot123456 mri_cloud < scripts/db/clear-business-data.sql
-- 或在 MySQL 客户端中先 USE mri_cloud; 再逐条执行下方语句。
-- 执行后业务表为空、自增 ID 重置、患者账号删除；admin 登录账号保持可用。

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

CREATE TEMPORARY TABLE patient_account_ids (id BIGINT PRIMARY KEY);
INSERT INTO patient_account_ids (id)
SELECT DISTINCT u.id
FROM sys_user u
JOIN sys_user_role ur ON ur.user_id = u.id
WHERE ur.role_code = 'PATIENT'
  AND u.username <> 'admin';

DELETE ur
FROM sys_user_role ur
JOIN patient_account_ids p ON p.id = ur.user_id;

DELETE u
FROM sys_user u
JOIN patient_account_ids p ON p.id = u.id;

DROP TEMPORARY TABLE patient_account_ids;
SET FOREIGN_KEY_CHECKS = 1;
