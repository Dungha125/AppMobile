package com.eldercare.dto;

import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLogDto {
    private Long id;
    private String action;
    private String entityType;
    private Long entityId;
    private String performedBy;
    private String performedByEmail;
    private String details;
    private String ipAddress;
    private LocalDateTime timestamp;
}
