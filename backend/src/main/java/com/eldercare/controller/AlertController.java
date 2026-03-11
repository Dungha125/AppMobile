package com.eldercare.controller;

import com.eldercare.dto.AlertDto;
import com.eldercare.dto.ApiResponse;
import com.eldercare.model.Alert;
import com.eldercare.service.AlertService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/alerts")
@RequiredArgsConstructor
public class AlertController {

    private final AlertService alertService;

    @PostMapping("/sos")
    public ResponseEntity<ApiResponse<AlertDto>> sos(@RequestBody Map<String, Object> body) {
        Long elderlyId = Long.valueOf(body.get("elderlyId").toString());
        BigDecimal lat = body.get("latitude") != null ? new BigDecimal(body.get("latitude").toString()) : null;
        BigDecimal lng = body.get("longitude") != null ? new BigDecimal(body.get("longitude").toString()) : null;

        Alert alert = alertService.createSosAlert(elderlyId, lat, lng);
        return ResponseEntity.ok(ApiResponse.success(AlertDto.fromAlert(alert)));
    }

    @GetMapping("/caregiver/{caregiverId}")
    public ResponseEntity<ApiResponse<List<AlertDto>>> getByCaregiver(
            @PathVariable Long caregiverId,
            @RequestParam(defaultValue = "50") int limit) {
        List<Alert> list = alertService.getAlertsByCaregiver(caregiverId, limit);
        List<AlertDto> dtos = list.stream()
                .map(AlertDto::fromAlert)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(dtos));
    }

    @GetMapping("/caregiver/{caregiverId}/unread-count")
    public ResponseEntity<ApiResponse<Long>> getUnreadCount(@PathVariable Long caregiverId) {
        long count = alertService.getUnreadCount(caregiverId);
        return ResponseEntity.ok(ApiResponse.success(count));
    }

    @PutMapping("/{id}/read")
    public ResponseEntity<ApiResponse<String>> markAsRead(@PathVariable Long id) {
        alertService.markAsRead(id);
        return ResponseEntity.ok(ApiResponse.success("Đã đánh dấu đọc"));
    }
}
