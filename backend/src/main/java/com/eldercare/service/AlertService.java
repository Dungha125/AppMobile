package com.eldercare.service;

import com.eldercare.model.Alert;
import com.eldercare.model.User;
import com.eldercare.model.enums.AlertType;
import com.eldercare.repository.AlertRepository;
import com.eldercare.repository.ElderlyCaregiverRepository;
import com.eldercare.repository.UserRepository;
import com.eldercare.dto.AlertDto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AlertService {

    private final AlertRepository alertRepository;
    private final UserRepository userRepository;
    private final ElderlyCaregiverRepository elderlyCaregiverRepository;
    private final PushService pushService;
    private final SimpMessagingTemplate messagingTemplate;

    public Alert createAlert(Long elderlyId, AlertType type, String title, String message, BigDecimal lat, BigDecimal lng) {
        User elderly = userRepository.findById(elderlyId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người cao tuổi"));

        List<User> caregivers = elderlyCaregiverRepository.findByElderly(elderly).stream()
                .map(ec -> ec.getCaregiver())
                .toList();

        Alert firstAlert = null;
        for (User caregiver : caregivers) {
            Alert alert = Alert.builder()
                    .elderly(elderly)
                    .caregiver(caregiver)
                    .alertType(type)
                    .title(title)
                    .message(message)
                    .latitude(lat)
                    .longitude(lng)
                    .build();
            alert = alertRepository.save(alert);
            if (firstAlert == null) firstAlert = alert;

            // realtime sync for caregiver
            try {
                messagingTemplate.convertAndSend("/topic/alerts/" + caregiver.getId(), AlertDto.fromEntity(alert));
            } catch (Exception ignored) {}
        }

        if (type == AlertType.SOS && !caregivers.isEmpty()) {
            String locMsg = message;
            if (lat != null && lng != null) {
                locMsg += " Vị trí: " + lat.stripTrailingZeros().toPlainString() + ", " + lng.stripTrailingZeros().toPlainString();
            }
            List<Long> caregiverIds = caregivers.stream().map(User::getId).collect(Collectors.toList());
            pushService.sendToUsers(caregiverIds, title, locMsg,
                    Map.of("type", "SOS", "elderlyName", elderly.getFullName(), "elderlyId", elderlyId,
                            "lat", lat != null ? lat.doubleValue() : 0, "lng", lng != null ? lng.doubleValue() : 0));
        }
        return firstAlert;
    }

    public Alert createSosAlert(Long elderlyId, BigDecimal lat, BigDecimal lng) {
        return createAlert(elderlyId, AlertType.SOS, "🆘 SOS Khẩn cấp",
                "Người cao tuổi đã bấm nút khẩn cấp. Vui lòng kiểm tra ngay!", lat, lng);
    }

    public List<Alert> getAlertsByCaregiver(Long caregiverId, int limit) {
        return alertRepository.findByCaregiverIdOrderByCreatedAtDesc(caregiverId, PageRequest.of(0, limit));
    }

    public long getUnreadCount(Long caregiverId) {
        return alertRepository.countByCaregiverIdAndIsReadFalse(caregiverId);
    }

    public void markAsRead(Long alertId) {
        alertRepository.findById(alertId).ifPresent(alert -> {
            alert.setIsRead(true);
            alertRepository.save(alert);
        });
    }
}
