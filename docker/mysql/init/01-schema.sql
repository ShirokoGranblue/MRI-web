CREATE DATABASE IF NOT EXISTS mri_cloud DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE mri_cloud;

CREATE TABLE IF NOT EXISTS sys_user (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  username VARCHAR(64) NOT NULL UNIQUE,
  password_hash VARCHAR(256) NOT NULL,
  display_name VARCHAR(64) NOT NULL,
  enabled CHAR(1) NOT NULL DEFAULT 'Y',
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT DEFAULT 0,
  version INT DEFAULT 0,
  UNIQUE KEY uk_mri_report_exam_order_id (exam_order_id)
);

CREATE TABLE IF NOT EXISTS sys_role (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  role_code VARCHAR(64) NOT NULL UNIQUE,
  role_name VARCHAR(64) NOT NULL,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT DEFAULT 0,
  version INT DEFAULT 0
);

CREATE TABLE IF NOT EXISTS sys_user_role (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  role_code VARCHAR(64) NOT NULL,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT DEFAULT 0,
  version INT DEFAULT 0
);

CREATE TABLE IF NOT EXISTS patient (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  patient_no VARCHAR(64) NOT NULL UNIQUE,
  name VARCHAR(64) NOT NULL,
  gender VARCHAR(16) NOT NULL,
  birth_date DATE,
  phone VARCHAR(32),
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT DEFAULT 0,
  version INT DEFAULT 0
);

CREATE TABLE IF NOT EXISTS mri_contraindication (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  patient_id BIGINT NOT NULL,
  type VARCHAR(64) NOT NULL,
  description VARCHAR(512),
  severity VARCHAR(32),
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT DEFAULT 0,
  version INT DEFAULT 0
);

CREATE TABLE IF NOT EXISTS mri_exam_order (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  patient_id BIGINT NOT NULL,
  exam_item VARCHAR(128) NOT NULL,
  clinical_diagnosis VARCHAR(256),
  priority VARCHAR(32),
  status VARCHAR(32) NOT NULL,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT DEFAULT 0,
  version INT DEFAULT 0
);

CREATE TABLE IF NOT EXISTS mri_schedule (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  exam_order_id BIGINT NOT NULL,
  scanner_room VARCHAR(64) NOT NULL,
  scheduled_at DATETIME NOT NULL,
  technologist VARCHAR(64),
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT DEFAULT 0,
  version INT DEFAULT 0
);

CREATE TABLE IF NOT EXISTS mri_study (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  exam_order_id BIGINT NOT NULL,
  study_instance_uid VARCHAR(128) NOT NULL UNIQUE,
  description VARCHAR(256),
  status VARCHAR(32) NOT NULL,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT DEFAULT 0,
  version INT DEFAULT 0
);

CREATE TABLE IF NOT EXISTS mri_series (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  study_id BIGINT NOT NULL,
  series_name VARCHAR(128) NOT NULL,
  body_position VARCHAR(64),
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT DEFAULT 0,
  version INT DEFAULT 0
);

CREATE TABLE IF NOT EXISTS mri_image_file (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  series_id BIGINT NOT NULL,
  file_name VARCHAR(256) NOT NULL,
  storage_path VARCHAR(512) NOT NULL,
  checksum VARCHAR(128),
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT DEFAULT 0,
  version INT DEFAULT 0
);

CREATE TABLE IF NOT EXISTS mri_download_log (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  study_id BIGINT NOT NULL,
  operator VARCHAR(64) NOT NULL,
  reason VARCHAR(256),
  downloaded_at DATETIME NOT NULL,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT DEFAULT 0,
  version INT DEFAULT 0
);

CREATE TABLE IF NOT EXISTS mri_report (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  exam_order_id BIGINT NOT NULL,
  study_id BIGINT NOT NULL,
  findings TEXT,
  impression TEXT,
  status VARCHAR(32) NOT NULL,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT DEFAULT 0,
  version INT DEFAULT 0
);

CREATE TABLE IF NOT EXISTS mri_report_audit_log (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  report_id BIGINT NOT NULL,
  action VARCHAR(64) NOT NULL,
  operator VARCHAR(64) NOT NULL,
  comment VARCHAR(256),
  operated_at DATETIME NOT NULL,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT DEFAULT 0,
  version INT DEFAULT 0
);

INSERT INTO sys_role (id, role_code, role_name) VALUES
  (1, 'ADMIN', '管理员'),
  (2, 'REGISTRAR', '登记人员'),
  (3, 'TECHNICIAN', '技师'),
  (4, 'RADIOLOGIST', '诊断医生'),
  (5, 'AUDITOR', '审核医生')
ON DUPLICATE KEY UPDATE role_name = VALUES(role_name);

INSERT INTO sys_user (id, username, password_hash, display_name, enabled) VALUES
  (1, 'admin', 'bXJpLWRlbW8tc2FsdC0wMQ==:RXNbRsnC6O0uocAF8JkAe7ozzmURjU7gnQYBDpcs640=', '系统管理员', 'Y')
ON DUPLICATE KEY UPDATE password_hash = VALUES(password_hash), display_name = VALUES(display_name);

INSERT INTO sys_user_role (id, user_id, role_code) VALUES
  (1, 1, 'ADMIN'),
  (2, 1, 'RADIOLOGIST'),
  (3, 1, 'AUDITOR')
ON DUPLICATE KEY UPDATE role_code = VALUES(role_code);

INSERT INTO patient (id, patient_no, name, gender, birth_date, phone) VALUES
  (1, 'P20260618001', '张三', '男', '1988-05-01', '13800000000')
ON DUPLICATE KEY UPDATE name = VALUES(name), phone = VALUES(phone);

INSERT INTO mri_contraindication (id, patient_id, type, description, severity) VALUES
  (1, 1, '金属植入物', '无心脏起搏器，左膝钛合金内固定需技师确认', 'LOW')
ON DUPLICATE KEY UPDATE description = VALUES(description);

INSERT INTO mri_exam_order (id, patient_id, exam_item, clinical_diagnosis, priority, status) VALUES
  (1, 1, '头颅MRI平扫', '眩晕待查', '普通', 'REQUESTED')
ON DUPLICATE KEY UPDATE status = VALUES(status);

INSERT INTO mri_schedule (id, exam_order_id, scanner_room, scheduled_at, technologist) VALUES
  (1, 1, 'MRI-1', '2026-06-18 15:30:00', '李技师')
ON DUPLICATE KEY UPDATE scanner_room = VALUES(scanner_room);

INSERT INTO mri_study (id, exam_order_id, study_instance_uid, description, status) VALUES
  (1, 1, '1.2.156.112605.20260618.0001', '头颅MRI平扫', 'ARCHIVED')
ON DUPLICATE KEY UPDATE description = VALUES(description);

INSERT INTO mri_series (id, study_id, series_name, body_position) VALUES
  (1, 1, 'T1_AXIAL', 'AXIAL'),
  (2, 1, 'T2_SAGITTAL', 'SAGITTAL')
ON DUPLICATE KEY UPDATE series_name = VALUES(series_name);

INSERT INTO mri_image_file (id, series_id, file_name, storage_path, checksum) VALUES
  (1, 1, 't1-001.dcm', 'storage/mri-images/t1-001.dcm', 'demo-checksum-t1'),
  (2, 2, 't2-001.dcm', 'storage/mri-images/t2-001.dcm', 'demo-checksum-t2')
ON DUPLICATE KEY UPDATE file_name = VALUES(file_name);

INSERT INTO mri_report (id, exam_order_id, study_id, findings, impression, status) VALUES
  (1, 1, 1, '双侧基底节区未见明确异常信号。', '头颅MRI未见明确急性异常。', 'DRAFT')
ON DUPLICATE KEY UPDATE status = VALUES(status);
