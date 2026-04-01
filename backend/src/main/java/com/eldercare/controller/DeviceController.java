package com.eldercare.controller;

import com.eldercare.dto.ApiResponse;
import com.eldercare.dto.RegisterDeviceRequest;
import com.eldercare.model.DeviceToken;
import com.eldercare.security.CurrentUser;
import com.eldercare.service.DeviceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/devices")
@RequiredArgsConstructor
public class DeviceController {

    private final DeviceService deviceService;

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<List<DeviceToken>>> listMy(@AuthenticationPrincipal CurrentUser currentUser) {
        if (currentUser == null) return ResponseEntity.status(401).body(ApiResponse.error("Chưa đăng nhập"));
        return ResponseEntity.ok(ApiResponse.success(deviceService.listMyDevices(currentUser.getUserId())));
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<DeviceToken>> register(
            @AuthenticationPrincipal CurrentUser currentUser,
            @Valid @RequestBody RegisterDeviceRequest req
    ) {
        if (currentUser == null) return ResponseEntity.status(401).body(ApiResponse.error("Chưa đăng nhập"));
        DeviceToken dt = deviceService.register(currentUser.getUserId(), req);
        return ResponseEntity.ok(ApiResponse.success(dt));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<String>> revokeMy(
            @AuthenticationPrincipal CurrentUser currentUser,
            @PathVariable Long id
    ) {
        if (currentUser == null) return ResponseEntity.status(401).body(ApiResponse.error("Chưa đăng nhập"));
        deviceService.revokeMyDevice(currentUser.getUserId(), id);
        return ResponseEntity.ok(ApiResponse.success("OK"));
    }
}

