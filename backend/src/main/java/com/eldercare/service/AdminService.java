package com.eldercare.service;

import com.eldercare.dto.AdminStatsResponse;
import com.eldercare.model.SystemConfig;
import com.eldercare.model.User;
import com.eldercare.model.enums.AlertType;
import com.eldercare.model.enums.MedicationHistoryStatus;
import com.eldercare.model.enums.UserRole;
import com.eldercare.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminService {

    private final UserRepository userRepository;
    private final PrescriptionRepository prescriptionRepository;
    private final CheckInRepository checkInRepository;
    private final AlertRepository alertRepository;
    private final MedicationHistoryRepository medicationHistoryRepository;
    private final SystemConfigRepository systemConfigRepository;
    private final AuditLogService auditLogService;

    @Transactional(readOnly = true)
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Page<User> getAllUsersPaged(Pageable pageable) {
        return userRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));
    }

    @Transactional
    public User updateUser(Long id, Boolean isActive, UserRole role) {
        log.info("Updating user {} - isActive: {}, role: {}", id, isActive, role);
        
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));
        
        log.info("User before update: id={}, email={}, isActive={}, role={}", 
                user.getId(), user.getEmail(), user.getIsActive(), user.getRole());
        
        String changes = "";
        if (isActive != null && !isActive.equals(user.getIsActive())) {
            changes += String.format("isActive: %s -> %s; ", user.getIsActive(), isActive);
            user.setIsActive(isActive);
            log.info("Changing isActive from {} to {}", !isActive, isActive);
        }
        if (role != null && !role.equals(user.getRole())) {
            changes += String.format("role: %s -> %s", user.getRole(), role);
            user.setRole(role);
            log.info("Changing role from {} to {}", user.getRole(), role);
        }
        
        if (changes.isEmpty()) {
            log.warn("No changes to apply for user {}", id);
            return user;
        }
        
        User savedUser = userRepository.save(user);
        log.info("User after save: id={}, email={}, isActive={}, role={}", 
                savedUser.getId(), savedUser.getEmail(), savedUser.getIsActive(), savedUser.getRole());
        
        // Log audit (chạy sau khi transaction commit)
        auditLogService.logAction("UPDATE_USER", "User", id, null, changes);
        log.info("Audit log created for user update: {}", changes);
        
        return savedUser;
    }

    @Transactional
    public void deleteUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));
        
        if (user.getRole() == UserRole.ADMIN) {
            throw new RuntimeException("Không thể xóa tài khoản admin");
        }
        
        auditLogService.logAction("DELETE_USER", "User", id, null, 
                "Deleted user: " + user.getEmail());
        userRepository.delete(user);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalUsers", userRepository.count());
        stats.put("elderlyCount", userRepository.countByRole(UserRole.ELDERLY));
        stats.put("caregiverCount", userRepository.countByRole(UserRole.CAREGIVER));
        stats.put("adminCount", userRepository.countByRole(UserRole.ADMIN));
        stats.put("prescriptionCount", prescriptionRepository.count());
        stats.put("checkInCount", checkInRepository.count());
        stats.put("alertCount", alertRepository.count());
        stats.put("medicationHistoryCount", medicationHistoryRepository.count());
        return stats;
    }

    @Transactional(readOnly = true)
    public AdminStatsResponse getDetailedStats() {
        long startTime = System.currentTimeMillis();
        
        LocalDateTime todayStart = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);
        LocalDateTime todayEnd = LocalDateTime.of(LocalDate.now(), LocalTime.MAX);
        LocalDateTime weekAgo = LocalDateTime.now().minusWeeks(1);
        LocalDateTime monthAgo = LocalDateTime.now().minusMonths(1);

        // User Stats
        AdminStatsResponse.UserStats userStats = AdminStatsResponse.UserStats.builder()
                .totalUsers(userRepository.count())
                .elderlyCount(userRepository.countByRole(UserRole.ELDERLY))
                .caregiverCount(userRepository.countByRole(UserRole.CAREGIVER))
                .adminCount(userRepository.countByRole(UserRole.ADMIN))
                .activeUsers(userRepository.countByIsActive(true))
                .inactiveUsers(userRepository.countByIsActive(false))
                .newUsersToday(userRepository.countByCreatedAtBetween(todayStart, todayEnd))
                .newUsersThisWeek(userRepository.countByCreatedAtAfter(weekAgo))
                .newUsersThisMonth(userRepository.countByCreatedAtAfter(monthAgo))
                .build();

        // System Stats
        AdminStatsResponse.SystemStats systemStats = AdminStatsResponse.SystemStats.builder()
                .totalPrescriptions(prescriptionRepository.count())
                .totalMedications(medicationHistoryRepository.count())
                .totalAlerts(alertRepository.count())
                .unreadAlerts(alertRepository.countByIsRead(false))
                .totalCheckIns(checkInRepository.count())
                .systemHealth(getSystemHealth())
                .build();

        // Activity Stats - Optimize by doing all AlertType counts in one pass
        Map<String, Long> alertsByType = new HashMap<>();
        for (AlertType type : AlertType.values()) {
            alertsByType.put(type.name(), alertRepository.countByAlertType(type));
        }

        AdminStatsResponse.ActivityStats activityStats = AdminStatsResponse.ActivityStats.builder()
                .medicationsTakenToday(medicationHistoryRepository.countByStatusAndCreatedAtBetween(
                        MedicationHistoryStatus.TAKEN, todayStart, todayEnd))
                .medicationsMissedToday(medicationHistoryRepository.countByStatusAndCreatedAtBetween(
                        MedicationHistoryStatus.MISSED, todayStart, todayEnd))
                .alertsByType(alertsByType)
                .build();

        long elapsed = System.currentTimeMillis() - startTime;
        log.info("getDetailedStats completed in {}ms", elapsed);

        return AdminStatsResponse.builder()
                .userStats(userStats)
                .systemStats(systemStats)
                .activityStats(activityStats)
                .build();
    }

    private Map<String, Object> getSystemHealth() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "healthy");
        health.put("database", "connected");
        health.put("timestamp", LocalDateTime.now());
        return health;
    }

    @Transactional(readOnly = true)
    public List<SystemConfig> getAllConfig() {
        return systemConfigRepository.findAll();
    }

    @Transactional(readOnly = true)
    public SystemConfig getConfig(String key) {
        return systemConfigRepository.findByConfigKey(key)
                .orElse(null);
    }

    public String getConfigValue(String key, String defaultValue) {
        return systemConfigRepository.findByConfigKey(key)
                .map(SystemConfig::getConfigValue)
                .orElse(defaultValue);
    }

    public boolean getConfigValueAsBoolean(String key, boolean defaultValue) {
        String value = getConfigValue(key, String.valueOf(defaultValue));
        return Boolean.parseBoolean(value);
    }

    public int getConfigValueAsInt(String key, int defaultValue) {
        try {
            String value = getConfigValue(key, String.valueOf(defaultValue));
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    @Transactional
    public SystemConfig setConfig(String key, String value, String description) {
        SystemConfig config = systemConfigRepository.findByConfigKey(key)
                .orElse(SystemConfig.builder()
                        .configKey(key)
                        .configType("string")
                        .build());
        
        String oldValue = config.getConfigValue();
        config.setConfigValue(value);
        if (description != null && !description.isBlank()) {
            config.setDescription(description);
        }
        
        SystemConfig savedConfig = systemConfigRepository.save(config);
        
        // Log audit
        String displayName = config.getDisplayName() != null ? config.getDisplayName() : key;
        auditLogService.logAction("UPDATE_CONFIG", "SystemConfig", savedConfig.getId(), null,
                String.format("%s: '%s' → '%s'", displayName, oldValue != null ? oldValue : "(empty)", value));
        
        return savedConfig;
    }

    @Transactional
    public void deleteConfig(String key) {
        SystemConfig config = systemConfigRepository.findByConfigKey(key)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy cấu hình"));
        
        auditLogService.logAction("DELETE_CONFIG", "SystemConfig", config.getId(), null,
                "Deleted config: " + key);
        systemConfigRepository.delete(config);
    }

    /**
     * Lấy danh sách users theo role
     */
    @Transactional(readOnly = true)
    public List<User> getUsersByRole(UserRole role) {
        return userRepository.findByRole(role);
    }

    /**
     * Tìm kiếm users
     */
    @Transactional(readOnly = true)
    public Page<User> searchUsers(String keyword, Pageable pageable) {
        return userRepository.findByEmailContainingOrFullNameContaining(
                keyword, keyword, pageable);
    }
}
