package com.eldercare.controller;

import com.eldercare.dto.ApiResponse;
import com.eldercare.dto.UserDto;
import com.eldercare.model.ElderlyProfile;
import com.eldercare.model.User;
import com.eldercare.security.CurrentUser;
import com.eldercare.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserDto>> getCurrentUser(@AuthenticationPrincipal CurrentUser currentUser) {
        log.info("GET /users/me called, currentUser: {}", currentUser != null ? currentUser.getUserId() : "null");
        
        if (currentUser == null) {
            log.warn("No authentication found");
            return ResponseEntity.status(401).body(ApiResponse.error("Chưa đăng nhập"));
        }
        
        try {
            User user = userService.findById(currentUser.getUserId());
            log.info("Found user: id={}, email={}, role={}", user.getId(), user.getEmail(), user.getRole());
            return ResponseEntity.ok(ApiResponse.success(UserDto.fromUser(user)));
        } catch (Exception e) {
            log.error("Error in getCurrentUser", e);
            return ResponseEntity.status(500).body(ApiResponse.error("Lỗi server: " + e.getMessage()));
        }
    }

    @GetMapping("/linked-elderly")
    public ResponseEntity<ApiResponse<List<UserDto>>> getLinkedElderly(@RequestParam Long caregiverId) {
        List<User> elderly = userService.getLinkedElderly(caregiverId);
        List<UserDto> dtos = elderly.stream()
                .map(UserDto::fromUser)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(dtos));
    }

    @GetMapping("/linked-caregivers")
    public ResponseEntity<ApiResponse<List<UserDto>>> getLinkedCaregivers(@RequestParam Long elderlyId) {
        List<User> caregivers = userService.getLinkedCaregivers(elderlyId);
        List<UserDto> dtos = caregivers.stream()
                .map(UserDto::fromUser)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(dtos));
    }

    @PostMapping("/link")
    public ResponseEntity<ApiResponse<String>> linkElderlyCaregiver(@RequestBody Map<String, Long> body) {
        Long elderlyId = body.get("elderlyId");
        Long caregiverId = body.get("caregiverId");
        userService.linkElderlyCaregiver(elderlyId, caregiverId);
        return ResponseEntity.ok(ApiResponse.success("Liên kết thành công", "OK"));
    }

    @PostMapping("/link-by-email")
    public ResponseEntity<ApiResponse<String>> linkByEmail(
            @AuthenticationPrincipal CurrentUser currentUser,
            @RequestBody Map<String, String> body) {
        if (currentUser == null) {
            return ResponseEntity.status(401).body(ApiResponse.error("Chưa đăng nhập"));
        }
        String email = body.get("email");
        if (email == null || email.isBlank()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Vui lòng nhập email"));
        }
        userService.linkByEmail(currentUser.getUserId(), email);
        return ResponseEntity.ok(ApiResponse.success("Liên kết thành công", "OK"));
    }

    @PostMapping("/link-by-phone")
    public ResponseEntity<ApiResponse<String>> linkByPhone(
            @AuthenticationPrincipal CurrentUser currentUser,
            @RequestBody Map<String, String> body) {
        if (currentUser == null) {
            return ResponseEntity.status(401).body(ApiResponse.error("Chưa đăng nhập"));
        }
        String phone = body.get("phone");
        if (phone == null || phone.isBlank()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Vui lòng nhập số điện thoại"));
        }
        userService.linkByPhone(currentUser.getUserId(), phone);
        return ResponseEntity.ok(ApiResponse.success("Liên kết thành công", "OK"));
    }

    @GetMapping("/profile/elderly/{userId}")
    public ResponseEntity<ApiResponse<ElderlyProfile>> getElderlyProfile(@PathVariable Long userId) {
        ElderlyProfile profile = userService.getOrCreateElderlyProfile(userId);
        return ResponseEntity.ok(ApiResponse.success(profile));
    }

    @PutMapping("/profile/elderly/{userId}")
    public ResponseEntity<ApiResponse<ElderlyProfile>> updateElderlyProfile(
            @PathVariable Long userId, @RequestBody ElderlyProfile updates) {
        ElderlyProfile profile = userService.updateElderlyProfile(userId, updates);
        return ResponseEntity.ok(ApiResponse.success(profile));
    }
}
