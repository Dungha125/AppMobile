package com.eldercare.dto;

import com.eldercare.model.Alert;
import com.eldercare.model.enums.AlertType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO cho Alert để tránh lazy loading issues
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertDto {
    private Long id;
    private AlertType alertType;
    private String title;
    private String message;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private Boolean isRead;
    private LocalDateTime createdAt;
    
    // Elderly info
    private Long elderlyId;
    private String elderlyName;
    private String elderlyEmail;
    
    // Caregiver info
    private Long caregiverId;
    private String caregiverName;

    public static AlertDto fromAlert(Alert alert) {
        if (alert == null) return null;
        
        return AlertDto.builder()
                .id(alert.getId())
                .alertType(alert.getAlertType())
                .title(alert.getTitle())
                .message(alert.getMessage())
                .latitude(alert.getLatitude())
                .longitude(alert.getLongitude())
                .isRead(alert.getIsRead())
                .createdAt(alert.getCreatedAt())
                .elderlyId(alert.getElderly() != null ? alert.getElderly().getId() : null)
                .elderlyName(alert.getElderly() != null ? alert.getElderly().getFullName() : null)
                .elderlyEmail(alert.getElderly() != null ? alert.getElderly().getEmail() : null)
                .caregiverId(alert.getCaregiver() != null ? alert.getCaregiver().getId() : null)
                .caregiverName(alert.getCaregiver() != null ? alert.getCaregiver().getFullName() : null)
                .build();
    }
}
