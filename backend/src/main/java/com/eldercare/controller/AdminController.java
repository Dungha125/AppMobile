package com.eldercare.controller;

import com.eldercare.dto.*;
import com.eldercare.model.SystemConfig;
import com.eldercare.model.User;
import com.eldercare.model.enums.UserRole;
import com.eldercare.service.AdminService;
import com.eldercare.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Slf4j
public class AdminController {

    private final AdminService adminService;
    private final AuditLogService auditLogService;

    // ==================== USER MANAGEMENT ====================

    /**
     * Lấy danh sách tất cả users
     */
    @GetMapping("/users")
    public ResponseEntity<ApiResponse<List<UserDto>>> getAllUsers() {
        List<User> users = adminService.getAllUsers();
        List<UserDto> dtos = users.stream()
                .map(UserDto::fromUser)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(dtos));
    }

    /**
     * Lấy danh sách users với phân trang
     */
    @GetMapping("/users/paged")
    public ResponseEntity<ApiResponse<Page<UserDto>>> getAllUsersPaged(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String direction) {
        Sort sort = direction.equalsIgnoreCase("ASC") ? 
                Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<UserDto> users = adminService.getAllUsersPaged(pageable)
                .map(UserDto::fromUser);
        return ResponseEntity.ok(ApiResponse.success(users));
    }

    /**
     * Lấy thông tin chi tiết một user
     */
    @GetMapping("/users/{id}")
    public ResponseEntity<ApiResponse<UserDto>> getUserById(@PathVariable Long id) {
        User user = adminService.getUserById(id);
        return ResponseEntity.ok(ApiResponse.success(UserDto.fromUser(user)));
    }

    /**
     * Lấy danh sách users theo role
     */
    @GetMapping("/users/by-role/{role}")
    public ResponseEntity<ApiResponse<List<UserDto>>> getUsersByRole(@PathVariable UserRole role) {
        List<User> users = adminService.getUsersByRole(role);
        List<UserDto> dtos = users.stream()
                .map(UserDto::fromUser)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(dtos));
    }

    /**
     * Tìm kiếm users
     */
    @GetMapping("/users/search")
    public ResponseEntity<ApiResponse<Page<UserDto>>> searchUsers(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<UserDto> users = adminService.searchUsers(keyword, pageable)
                .map(UserDto::fromUser);
        return ResponseEntity.ok(ApiResponse.success(users));
    }

    /**
     * Cập nhật user (khóa/mở khóa tài khoản, đổi role)
     */
    @PutMapping("/users/{id}")
    public ResponseEntity<ApiResponse<UserDto>> updateUser(
            @PathVariable Long id,
            @RequestBody UpdateUserRequest request) {
        log.info("AdminController.updateUser called: userId={}, isActive={}, role={}", 
                id, request.getIsActive(), request.getRole());
        User user = adminService.updateUser(id, request.getIsActive(), request.getRole());
        return ResponseEntity.ok(ApiResponse.success(UserDto.fromUser(user)));
    }

    /**
     * Xóa user
     */
    @DeleteMapping("/users/{id}")
    public ResponseEntity<ApiResponse<String>> deleteUser(@PathVariable Long id) {
        adminService.deleteUser(id);
        return ResponseEntity.ok(ApiResponse.success("Đã xóa người dùng"));
    }

    // ==================== STATISTICS ====================

    /**
     * Lấy thống kê cơ bản
     */
    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getStats() {
        return ResponseEntity.ok(ApiResponse.success(adminService.getStats()));
    }

    /**
     * Lấy thống kê chi tiết
     */
    @GetMapping("/stats/detailed")
    public ResponseEntity<ApiResponse<AdminStatsResponse>> getDetailedStats() {
        return ResponseEntity.ok(ApiResponse.success(adminService.getDetailedStats()));
    }

    // ==================== SYSTEM CONFIG ====================

    /**
     * Lấy tất cả cấu hình hệ thống
     */
    @GetMapping("/config")
    public ResponseEntity<ApiResponse<List<SystemConfig>>> getConfig() {
        return ResponseEntity.ok(ApiResponse.success(adminService.getAllConfig()));
    }

    /**
     * Lấy một cấu hình theo key
     */
    @GetMapping("/config/{key}")
    public ResponseEntity<ApiResponse<SystemConfig>> getConfigByKey(@PathVariable String key) {
        SystemConfig config = adminService.getConfig(key);
        if (config == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(ApiResponse.success(config));
    }

    /**
     * Cập nhật hoặc tạo mới cấu hình
     */
    @PutMapping("/config")
    public ResponseEntity<ApiResponse<SystemConfig>> setConfig(@RequestBody ConfigUpdateRequest request) {
        log.info("Setting config: key={}, value={}", request.getConfigKey(), request.getConfigValue());
        
        if (request.getConfigKey() == null || request.getConfigKey().isBlank()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Thiếu configKey"));
        }
        
        SystemConfig config = adminService.setConfig(
                request.getConfigKey(), 
                request.getConfigValue(), 
                request.getDescription());
        
        log.info("Config saved: key={}, value={}", config.getConfigKey(), config.getConfigValue());
        return ResponseEntity.ok(ApiResponse.success(config));
    }

    /**
     * Xóa một cấu hình
     */
    @DeleteMapping("/config/{key}")
    public ResponseEntity<ApiResponse<String>> deleteConfig(@PathVariable String key) {
        adminService.deleteConfig(key);
        return ResponseEntity.ok(ApiResponse.success("Đã xóa cấu hình"));
    }

    // ==================== AUDIT LOGS ====================

    /**
     * Lấy logs gần đây
     */
    @GetMapping("/logs/recent")
    public ResponseEntity<ApiResponse<List<AuditLogDto>>> getRecentLogs() {
        return ResponseEntity.ok(ApiResponse.success(auditLogService.getRecentLogs()));
    }

    /**
     * Lấy logs với phân trang
     */
    @GetMapping("/logs")
    public ResponseEntity<ApiResponse<Page<AuditLogDto>>> getLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(ApiResponse.success(auditLogService.getLogs(pageable)));
    }

    /**
     * Lấy logs của một user
     */
    @GetMapping("/logs/user/{userId}")
    public ResponseEntity<ApiResponse<Page<AuditLogDto>>> getLogsByUser(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(ApiResponse.success(auditLogService.getLogsByUser(userId, pageable)));
    }

    /**
     * Lấy logs theo action
     */
    @GetMapping("/logs/action/{action}")
    public ResponseEntity<ApiResponse<Page<AuditLogDto>>> getLogsByAction(
            @PathVariable String action,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(ApiResponse.success(auditLogService.getLogsByAction(action, pageable)));
    }

    /**
     * Dọn dẹp logs cũ
     */
    @DeleteMapping("/logs/cleanup")
    public ResponseEntity<ApiResponse<String>> cleanupOldLogs(
            @RequestParam(defaultValue = "90") int daysToKeep) {
        auditLogService.cleanupOldLogs(daysToKeep);
        return ResponseEntity.ok(ApiResponse.success("Đã dọn dẹp logs cũ"));
    }
}
