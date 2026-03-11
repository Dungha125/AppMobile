-- ElderCare Database Schema - MySQL
CREATE DATABASE IF NOT EXISTS eldercare CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE eldercare;

-- Bảng người dùng
CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(255) NOT NULL,
    phone VARCHAR(20),
    role ENUM('ELDERLY', 'CAREGIVER', 'ADMIN') NOT NULL,
    avatar_url VARCHAR(500),
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Bảng liên kết Người cao tuổi - Người giám hộ
CREATE TABLE IF NOT EXISTS elderly_caregiver (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    elderly_id BIGINT NOT NULL,
    caregiver_id BIGINT NOT NULL,
    linked_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_primary BOOLEAN DEFAULT FALSE,
    FOREIGN KEY (elderly_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (caregiver_id) REFERENCES users(id) ON DELETE CASCADE,
    UNIQUE KEY uk_elderly_caregiver (elderly_id, caregiver_id)
);

-- Bảng thông tin bổ sung người cao tuổi
CREATE TABLE IF NOT EXISTS elderly_profiles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    date_of_birth DATE,
    address VARCHAR(500),
    emergency_contact VARCHAR(255),
    medical_notes TEXT,
    latitude DECIMAL(10, 8),
    longitude DECIMAL(11, 8),
    last_active_at TIMESTAMP,
    last_checkin_at TIMESTAMP,
    fcm_token VARCHAR(500),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Bảng đơn thuốc
CREATE TABLE IF NOT EXISTS prescriptions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    elderly_id BIGINT NOT NULL,
    title VARCHAR(255) NOT NULL,
    doctor_name VARCHAR(255),
    notes TEXT,
    start_date DATE,
    end_date DATE,
    created_by BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (elderly_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE SET NULL
);

-- Bảng chi tiết thuốc trong đơn
CREATE TABLE IF NOT EXISTS medications (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    prescription_id BIGINT NOT NULL,
    name VARCHAR(255) NOT NULL,
    dosage VARCHAR(100),
    unit VARCHAR(50),
    quantity INT DEFAULT 1,
    instructions TEXT,
    FOREIGN KEY (prescription_id) REFERENCES prescriptions(id) ON DELETE CASCADE
);

-- Bảng lịch uống thuốc
CREATE TABLE IF NOT EXISTS medication_schedules (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    medication_id BIGINT NOT NULL,
    time_of_day TIME NOT NULL,
    day_of_week VARCHAR(20) DEFAULT 'ALL',
    is_active BOOLEAN DEFAULT TRUE,
    reminder_minutes_before INT DEFAULT 15,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (medication_id) REFERENCES medications(id) ON DELETE CASCADE
);

-- Bảng lịch sử uống thuốc
CREATE TABLE IF NOT EXISTS medication_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    medication_schedule_id BIGINT NOT NULL,
    scheduled_time TIMESTAMP NOT NULL,
    taken_at TIMESTAMP,
    status ENUM('PENDING', 'TAKEN', 'SKIPPED', 'MISSED') DEFAULT 'PENDING',
    notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (medication_schedule_id) REFERENCES medication_schedules(id) ON DELETE CASCADE
);

-- Bảng điểm danh sức khỏe
CREATE TABLE IF NOT EXISTS check_ins (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    elderly_id BIGINT NOT NULL,
    check_in_type ENUM('ACTIVE', 'PASSIVE') NOT NULL,
    checked_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    notes TEXT,
    latitude DECIMAL(10, 8),
    longitude DECIMAL(11, 8),
    FOREIGN KEY (elderly_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Bảng cảnh báo
CREATE TABLE IF NOT EXISTS alerts (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    elderly_id BIGINT NOT NULL,
    caregiver_id BIGINT NOT NULL,
    alert_type ENUM('SOS', 'MISSED_MEDICATION', 'NO_CHECKIN', 'INACTIVE', 'OTHER') NOT NULL,
    title VARCHAR(255) NOT NULL,
    message TEXT,
    latitude DECIMAL(10, 8),
    longitude DECIMAL(11, 8),
    is_read BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (elderly_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (caregiver_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Bảng cấu hình hệ thống (Admin)
CREATE TABLE IF NOT EXISTS system_config (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    config_key VARCHAR(100) NOT NULL UNIQUE,
    config_value TEXT,
    display_name VARCHAR(255),
    category VARCHAR(50),
    description TEXT,
    config_type VARCHAR(20) DEFAULT 'string',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Bảng thiết bị đăng nhập
CREATE TABLE IF NOT EXISTS user_devices (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    device_token VARCHAR(500),
    device_info VARCHAR(255),
    last_login_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Bảng audit logs (Admin)
CREATE TABLE IF NOT EXISTS audit_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    action VARCHAR(100) NOT NULL,
    entity_type VARCHAR(100),
    entity_id BIGINT,
    user_id BIGINT,
    details TEXT,
    ip_address VARCHAR(50),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL
);

-- Index cho truy vấn nhanh
CREATE INDEX idx_users_role ON users(role);
CREATE INDEX idx_users_is_active ON users(is_active);
CREATE INDEX idx_users_created_at ON users(created_at);
CREATE INDEX idx_elderly_caregiver_elderly ON elderly_caregiver(elderly_id);
CREATE INDEX idx_elderly_caregiver_caregiver ON elderly_caregiver(caregiver_id);
CREATE INDEX idx_prescriptions_elderly ON prescriptions(elderly_id);
CREATE INDEX idx_medications_prescription ON medications(prescription_id);
CREATE INDEX idx_check_ins_elderly ON check_ins(elderly_id);
CREATE INDEX idx_check_ins_checked_at ON check_ins(checked_at);
CREATE INDEX idx_alerts_caregiver ON alerts(caregiver_id);
CREATE INDEX idx_alerts_created_at ON alerts(created_at);
CREATE INDEX idx_alerts_is_read ON alerts(is_read);
CREATE INDEX idx_alerts_alert_type ON alerts(alert_type);
CREATE INDEX idx_medication_history_status ON medication_history(status);
CREATE INDEX idx_medication_history_created_at ON medication_history(created_at);
CREATE INDEX idx_audit_logs_action ON audit_logs(action);
CREATE INDEX idx_audit_logs_user ON audit_logs(user_id);
CREATE INDEX idx_audit_logs_created_at ON audit_logs(created_at);
CREATE INDEX idx_system_config_key ON system_config(config_key);

-- Tài khoản admin: đăng nhập email "admin" / mật khẩu "admin123"
-- Nếu đăng nhập không được, tạo hash mới: trong thư mục backend chạy:
--   mvn exec:java -Dexec.mainClass="com.eldercare.util.BcryptHashGenerator" -Dexec.args="admin123"
-- rồi COPY dòng SQL in ra và chạy trong MySQL, hoặc: UPDATE users SET password_hash='<hash_in_ra>' WHERE email='admin';
INSERT INTO users (email, password_hash, full_name, role, is_active)
VALUES (
  'admin',
  '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
  'Administrator',
  'ADMIN',
  TRUE
)
ON DUPLICATE KEY UPDATE updated_at = CURRENT_TIMESTAMP;
