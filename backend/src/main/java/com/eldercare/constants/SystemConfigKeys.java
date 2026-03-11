package com.eldercare.constants;

/**
 * Các khóa cấu hình hệ thống
 * Chỉ định nghĩa những configs cốt lõi, các configs khác có thể thêm qua UI
 */
public class SystemConfigKeys {
    
    // ==================== CONFIGS CỐT LÕI (Auto-seed) ====================
    
    // Thông báo
    public static final String NOTIFICATION_ENABLED = "notification.enabled";
    
    // Thời gian điểm danh
    public static final String CHECKIN_INTERVAL_HOURS = "checkin.interval.hours";
    
    // Bảo trì
    public static final String APP_MAINTENANCE_MODE = "app.maintenance.mode";
    
    // ==================== CONFIGS BỔ SUNG (Có thể thêm sau) ====================
    
    // Thông báo chi tiết
    public static final String NOTIFICATION_MEDICATION_REMINDER = "notification.medication.reminder.enabled";
    public static final String NOTIFICATION_CHECKIN_REMINDER = "notification.checkin.reminder.enabled";
    public static final String NOTIFICATION_SOS_ALERT = "notification.sos.alert.enabled";
    
    // Thời gian khác
    public static final String CHECKIN_MISS_THRESHOLD_HOURS = "checkin.miss.threshold.hours";
    public static final String MEDICATION_REMINDER_MINUTES = "medication.reminder.minutes.before";
    public static final String INACTIVITY_THRESHOLD_HOURS = "inactivity.threshold.hours";
    
    // Firebase
    public static final String FIREBASE_ENABLED = "firebase.enabled";
    public static final String FIREBASE_PROJECT_ID = "firebase.project.id";
    
    // Bảo mật
    public static final String MAX_LOGIN_ATTEMPTS = "security.max.login.attempts";
    public static final String SESSION_TIMEOUT_MINUTES = "security.session.timeout.minutes";
    public static final String PASSWORD_MIN_LENGTH = "security.password.min.length";
    public static final String REQUIRE_EMAIL_VERIFICATION = "security.email.verification.required";
    
    // Giới hạn
    public static final String MAX_CAREGIVERS_PER_ELDERLY = "limit.max.caregivers.per.elderly";
    public static final String MAX_MEDICATIONS_PER_PRESCRIPTION = "limit.max.medications.per.prescription";
    public static final String MAX_PRESCRIPTIONS_PER_ELDERLY = "limit.max.prescriptions.per.elderly";
    
    // Vị trí
    public static final String LOCATION_TRACKING_ENABLED = "location.tracking.enabled";
    public static final String LOCATION_UPDATE_INTERVAL_MINUTES = "location.update.interval.minutes";
    public static final String GEO_FENCE_RADIUS_METERS = "location.geofence.radius.meters";
    
    // API
    public static final String API_RATE_LIMIT_PER_MINUTE = "api.rate.limit.per.minute";
    public static final String API_TIMEOUT_SECONDS = "api.timeout.seconds";
    
    // Bảo trì
    public static final String REPORT_RETENTION_DAYS = "report.retention.days";
    public static final String AUTO_CLEANUP_ENABLED = "system.auto.cleanup.enabled";
    public static final String AUTO_CLEANUP_DAYS = "system.auto.cleanup.days";
    
    // Ứng dụng
    public static final String APP_VERSION_MIN_REQUIRED = "app.version.min.required";
    public static final String APP_FORCE_UPDATE = "app.force.update";
    
    // Email/SMS
    public static final String EMAIL_NOTIFICATIONS_ENABLED = "email.notifications.enabled";
    public static final String SMS_NOTIFICATIONS_ENABLED = "sms.notifications.enabled";
    public static final String SMTP_HOST = "email.smtp.host";
    public static final String SMTP_PORT = "email.smtp.port";
    
    private SystemConfigKeys() {
        // Prevent instantiation
    }
}
