package com.eldercare.service;

import com.eldercare.model.AppActivityLog;
import com.eldercare.model.SystemConfig;
import com.eldercare.model.User;
import com.eldercare.model.enums.CheckInType;
import com.eldercare.model.enums.UserRole;
import com.eldercare.repository.AppActivityLogRepository;
import com.eldercare.repository.CheckInRepository;
import com.eldercare.repository.SystemConfigRepository;
import com.eldercare.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class PassiveCheckInTrackerService {

    private static final int DEFAULT_THRESHOLD = 5;

    private final UserRepository userRepository;
    private final AppActivityLogRepository appActivityLogRepository;
    private final CheckInRepository checkInRepository;
    private final CheckInService checkInService;
    private final SystemConfigRepository systemConfigRepository;

    public void recordUserActivity(Long userId, String method, String endpoint) {
        try {
            User user = userRepository.findById(userId).orElse(null);
            if (user == null || user.getRole() != UserRole.ELDERLY) return;

            AppActivityLog logRow = AppActivityLog.builder()
                    .user(user)
                    .httpMethod(method)
                    .endpoint(endpoint)
                    .build();
            appActivityLogRepository.save(logRow);

            int threshold = resolveThreshold();
            if (threshold <= 0) threshold = DEFAULT_THRESHOLD;

            LocalDate today = LocalDate.now();
            LocalDateTime startOfDay = today.atStartOfDay();
            LocalDateTime endOfDay = today.atTime(LocalTime.MAX);

            long todayActions = appActivityLogRepository.countByUserIdAndCreatedAtBetween(userId, startOfDay, endOfDay);
            if (todayActions % threshold != 0) return;

            boolean alreadyCheckedInToday = checkInRepository
                    .findFirstByElderlyIdAndCheckedAtBetweenOrderByCheckedAtDesc(userId, startOfDay, endOfDay)
                    .isPresent();
            if (alreadyCheckedInToday) return;

            checkInService.createCheckIn(
                    userId,
                    CheckInType.PASSIVE,
                    "Điểm danh thụ động tự động sau " + todayActions + " thao tác trong ứng dụng.",
                    null,
                    null
            );
            log.info("Auto passive check-in created for elderlyId={} after {} actions", userId, todayActions);
        } catch (Exception e) {
            log.warn("PassiveCheckInTrackerService failed: {}", e.getMessage());
        }
    }

    private int resolveThreshold() {
        try {
            return systemConfigRepository.findByConfigKey("passive_checkin_action_threshold")
                    .map(SystemConfig::getConfigValue)
                    .map(String::trim)
                    .filter(v -> !v.isBlank())
                    .map(Integer::parseInt)
                    .orElse(DEFAULT_THRESHOLD);
        } catch (Exception e) {
            return DEFAULT_THRESHOLD;
        }
    }
}
