package com.eldercare.service;

import com.eldercare.dto.RegisterDeviceRequest;
import com.eldercare.model.DeviceToken;
import com.eldercare.model.SystemConfig;
import com.eldercare.model.User;
import com.eldercare.repository.DeviceTokenRepository;
import com.eldercare.repository.SystemConfigRepository;
import com.eldercare.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DeviceService {

    private final DeviceTokenRepository deviceTokenRepository;
    private final UserRepository userRepository;
    private final SystemConfigRepository systemConfigRepository;

    public List<DeviceToken> listMyDevices(Long userId) {
        return deviceTokenRepository.findByUserIdAndRevokedAtIsNull(userId).stream()
                .sorted(Comparator.comparing(DeviceToken::getLastSeenAt, Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                .toList();
    }

    @Transactional
    public DeviceToken register(Long userId, RegisterDeviceRequest req) {
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));
        String token = req.getToken().trim();

        DeviceToken dt = deviceTokenRepository.findByUserIdAndToken(userId, token)
                .orElse(DeviceToken.builder().user(user).token(token).build());
        dt.setPlatform(req.getPlatform());
        dt.setDeviceInfo(req.getDeviceInfo());
        dt.setLastSeenAt(LocalDateTime.now());
        dt.setRevokedAt(null);
        dt = deviceTokenRepository.save(dt);

        enforceMaxDevices(userId);
        return dt;
    }

    @Transactional
    public void revokeMyDevice(Long userId, Long deviceId) {
        DeviceToken dt = deviceTokenRepository.findByIdAndUserId(deviceId, userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thiết bị"));
        dt.setRevokedAt(LocalDateTime.now());
        deviceTokenRepository.save(dt);
    }

    private void enforceMaxDevices(Long userId) {
        int max = getIntConfig("max_devices_per_user", 5);
        if (max <= 0) return;
        List<DeviceToken> active = deviceTokenRepository.findByUserIdAndRevokedAtIsNull(userId).stream()
                .sorted(Comparator.comparing(DeviceToken::getLastSeenAt, Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                .toList();
        if (active.size() <= max) return;

        List<DeviceToken> toRevoke = active.subList(max, active.size());
        LocalDateTime now = LocalDateTime.now();
        for (DeviceToken dt : toRevoke) {
            dt.setRevokedAt(now);
            deviceTokenRepository.save(dt);
        }
    }

    private int getIntConfig(String key, int defaultVal) {
        try {
            return systemConfigRepository.findByConfigKey(key)
                    .map(SystemConfig::getConfigValue)
                    .map(String::trim)
                    .filter(v -> !v.isEmpty())
                    .map(Integer::parseInt)
                    .orElse(defaultVal);
        } catch (Exception e) {
            return defaultVal;
        }
    }
}

