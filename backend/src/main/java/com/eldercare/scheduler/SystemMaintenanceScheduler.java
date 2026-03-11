package com.eldercare.scheduler;

import com.eldercare.constants.SystemConfigKeys;
import com.eldercare.service.AdminService;
import com.eldercare.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduled tasks cho bảo trì hệ thống
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SystemMaintenanceScheduler {

    private final AdminService adminService;
    private final AuditLogService auditLogService;

    /**
     * Dọn dẹp audit logs cũ - chạy hàng ngày lúc 2:00 AM
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void cleanupOldAuditLogs() {
        try {
            boolean autoCleanupEnabled = adminService.getConfigValueAsBoolean(
                    SystemConfigKeys.AUTO_CLEANUP_ENABLED, true);
            
            if (!autoCleanupEnabled) {
                log.info("Auto cleanup is disabled");
                return;
            }

            int daysToKeep = adminService.getConfigValueAsInt(
                    SystemConfigKeys.AUTO_CLEANUP_DAYS, 90);
            
            log.info("Starting audit logs cleanup, keeping last {} days", daysToKeep);
            auditLogService.cleanupOldLogs(daysToKeep);
            log.info("Audit logs cleanup completed");
        } catch (Exception e) {
            log.error("Failed to cleanup old audit logs", e);
        }
    }

    /**
     * Log thống kê hệ thống - chạy hàng ngày lúc 0:00 AM
     */
    @Scheduled(cron = "0 0 0 * * ?")
    public void logDailyStats() {
        try {
            var stats = adminService.getStats();
            log.info("Daily Stats - Users: {}, Elderly: {}, Caregivers: {}, Alerts: {}, CheckIns: {}",
                    stats.get("totalUsers"),
                    stats.get("elderlyCount"),
                    stats.get("caregiverCount"),
                    stats.get("alertCount"),
                    stats.get("checkInCount"));
        } catch (Exception e) {
            log.error("Failed to log daily stats", e);
        }
    }

    /**
     * Health check - chạy mỗi 5 phút
     */
    @Scheduled(fixedRate = 300000) // 5 minutes
    public void healthCheck() {
        log.debug("System health check - OK");
    }
}
