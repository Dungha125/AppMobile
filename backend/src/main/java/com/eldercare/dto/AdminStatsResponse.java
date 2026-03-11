package com.eldercare.dto;

import lombok.*;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminStatsResponse {
    private UserStats userStats;
    private SystemStats systemStats;
    private ActivityStats activityStats;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserStats {
        private Long totalUsers;
        private Long elderlyCount;
        private Long caregiverCount;
        private Long adminCount;
        private Long activeUsers;
        private Long inactiveUsers;
        private Long newUsersToday;
        private Long newUsersThisWeek;
        private Long newUsersThisMonth;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SystemStats {
        private Long totalPrescriptions;
        private Long activePrescriptions;
        private Long totalMedications;
        private Long totalAlerts;
        private Long unreadAlerts;
        private Long totalCheckIns;
        private Long checkInsToday;
        private Map<String, Object> systemHealth;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ActivityStats {
        private Long medicationsTakenToday;
        private Long medicationsMissedToday;
        private Long sosAlertsToday;
        private Long missedCheckInsToday;
        private Map<String, Long> alertsByType;
        private Map<String, Long> usersByRole;
    }
}
