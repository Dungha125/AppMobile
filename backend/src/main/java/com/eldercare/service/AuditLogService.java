package com.eldercare.service;

import com.eldercare.dto.AuditLogDto;
import com.eldercare.model.AuditLog;
import com.eldercare.model.User;
import com.eldercare.repository.AuditLogRepository;
import com.eldercare.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;

    /**
     * Ghi log hành động (async để không ảnh hưởng performance)
     */
    @Async
    @Transactional
    public void logAction(String action, String entityType, Long entityId, 
                         Long userId, String details) {
        try {
            User user = userId != null ? userRepository.findById(userId).orElse(null) : null;
            String ipAddress = getClientIpAddress();

            AuditLog auditLog = AuditLog.builder()
                    .action(action)
                    .entityType(entityType)
                    .entityId(entityId)
                    .performedBy(user)
                    .details(details)
                    .ipAddress(ipAddress)
                    .build();

            auditLogRepository.save(auditLog);
            log.debug("Audit log created: {} - {} - {}", action, entityType, entityId);
        } catch (Exception e) {
            log.error("Failed to create audit log", e);
        }
    }

    /**
     * Lấy logs với phân trang
     */
    public Page<AuditLogDto> getLogs(Pageable pageable) {
        return auditLogRepository.findAll(pageable).map(this::toDto);
    }

    /**
     * Lấy logs của một user
     */
    public Page<AuditLogDto> getLogsByUser(Long userId, Pageable pageable) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return auditLogRepository.findByPerformedByOrderByCreatedAtDesc(user, pageable)
                .map(this::toDto);
    }

    /**
     * Lấy logs theo action
     */
    public Page<AuditLogDto> getLogsByAction(String action, Pageable pageable) {
        return auditLogRepository.findByActionOrderByCreatedAtDesc(action, pageable)
                .map(this::toDto);
    }

    /**
     * Lấy logs theo khoảng thời gian
     */
    public Page<AuditLogDto> getLogsByDateRange(LocalDateTime startDate, 
                                                 LocalDateTime endDate, 
                                                 Pageable pageable) {
        return auditLogRepository.findByCreatedAtBetweenOrderByCreatedAtDesc(
                startDate, endDate, pageable).map(this::toDto);
    }

    /**
     * Lấy 100 logs gần nhất
     */
    public List<AuditLogDto> getRecentLogs() {
        return auditLogRepository.findTop100ByOrderByCreatedAtDesc()
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Xóa logs cũ hơn một khoảng thời gian
     */
    @Transactional
    public void cleanupOldLogs(int daysToKeep) {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(daysToKeep);
        auditLogRepository.deleteByCreatedAtBefore(cutoffDate);
        log.info("Cleaned up audit logs older than {} days", daysToKeep);
    }

    /**
     * Lấy IP address của client
     */
    private String getClientIpAddress() {
        try {
            ServletRequestAttributes attributes = 
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                String xForwardedFor = request.getHeader("X-Forwarded-For");
                if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
                    return xForwardedFor.split(",")[0].trim();
                }
                return request.getRemoteAddr();
            }
        } catch (Exception e) {
            log.warn("Failed to get client IP address", e);
        }
        return "unknown";
    }

    /**
     * Convert AuditLog to DTO
     */
    private AuditLogDto toDto(AuditLog log) {
        return AuditLogDto.builder()
                .id(log.getId())
                .action(log.getAction())
                .entityType(log.getEntityType())
                .entityId(log.getEntityId())
                .performedBy(log.getPerformedBy() != null ? 
                        log.getPerformedBy().getFullName() : "System")
                .performedByEmail(log.getPerformedBy() != null ? 
                        log.getPerformedBy().getEmail() : null)
                .details(log.getDetails())
                .ipAddress(log.getIpAddress())
                .timestamp(log.getCreatedAt())
                .build();
    }
}
