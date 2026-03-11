package com.eldercare.config;

import com.eldercare.constants.SystemConfigKeys;
import com.eldercare.model.SystemConfig;
import com.eldercare.repository.SystemConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Seed dữ liệu cấu hình hệ thống mặc định
 */
@Component
@Order(2) // Chạy sau AdminSeeder
@RequiredArgsConstructor
@Slf4j
public class SystemConfigSeeder implements CommandLineRunner {

    private final SystemConfigRepository systemConfigRepository;

    @Override
    public void run(String... args) {
        log.info("Đang kiểm tra và seed cấu hình hệ thống...");
        
        Map<String, ConfigValue> defaultConfigs = getDefaultConfigs();
        
        int seededCount = 0;
        int updatedCount = 0;
        
        for (Map.Entry<String, ConfigValue> entry : defaultConfigs.entrySet()) {
            String key = entry.getKey();
            ConfigValue configValue = entry.getValue();
            
            var existingConfig = systemConfigRepository.findByConfigKey(key);
            
            if (existingConfig.isEmpty()) {
                // Tạo mới
                SystemConfig config = SystemConfig.builder()
                        .configKey(key)
                        .configValue(configValue.value)
                        .displayName(configValue.displayName)
                        .category(configValue.category)
                        .description(configValue.description)
                        .configType(configValue.configType)
                        .build();
                systemConfigRepository.save(config);
                seededCount++;
                log.debug("Đã tạo cấu hình: {} ({})", key, configValue.displayName);
            } else {
                // Cập nhật displayName, category, configType nếu thiếu
                SystemConfig config = existingConfig.get();
                boolean needsUpdate = false;
                
                if (config.getDisplayName() == null || config.getDisplayName().isEmpty()) {
                    config.setDisplayName(configValue.displayName);
                    needsUpdate = true;
                }
                if (config.getCategory() == null || config.getCategory().isEmpty()) {
                    config.setCategory(configValue.category);
                    needsUpdate = true;
                }
                if (config.getConfigType() == null || config.getConfigType().isEmpty()) {
                    config.setConfigType(configValue.configType);
                    needsUpdate = true;
                }
                
                if (needsUpdate) {
                    systemConfigRepository.save(config);
                    updatedCount++;
                    log.debug("Đã cập nhật metadata: {} ({})", key, configValue.displayName);
                }
            }
        }
        
        if (seededCount > 0 || updatedCount > 0) {
            log.info("✓ Đã seed {} cấu hình mới, cập nhật {} cấu hình", seededCount, updatedCount);
        } else {
            log.info("✓ Tất cả cấu hình hệ thống đã được thiết lập đầy đủ");
        }
    }

    private Map<String, ConfigValue> getDefaultConfigs() {
        Map<String, ConfigValue> configs = new HashMap<>();
        
        // Chỉ seed 3 cấu hình quan trọng nhất
        
        // 1. Bật/tắt thông báo (quan trọng nhất)
        configs.put(SystemConfigKeys.NOTIFICATION_ENABLED, 
                new ConfigValue("true", "Bật Thông Báo Hệ Thống", "NOTIFICATION", 
                        "Bật hoặc tắt tất cả các loại thông báo đẩy (push notifications) trong hệ thống", "boolean"));
        
        // 2. Chu kỳ điểm danh (core feature)
        configs.put(SystemConfigKeys.CHECKIN_INTERVAL_HOURS, 
                new ConfigValue("12", "Chu Kỳ Điểm Danh (giờ)", "TIMING", 
                        "Người cao tuổi cần điểm danh mỗi X giờ. Ví dụ: 12 giờ = điểm danh 2 lần/ngày", "integer"));
        
        // 3. Chế độ bảo trì (admin control)
        configs.put(SystemConfigKeys.APP_MAINTENANCE_MODE, 
                new ConfigValue("false", "Chế Độ Bảo Trì", "APPLICATION", 
                        "Bật chế độ bảo trì để tạm ngưng người dùng sử dụng app (khi nâng cấp hệ thống)", "boolean"));
        
        // Có thể thêm configs khác qua UI admin sau này
        
        return configs;
    }

    private static class ConfigValue {
        String value;
        String displayName;
        String category;
        String description;
        String configType;

        ConfigValue(String value, String displayName, String category, String description, String configType) {
            this.value = value;
            this.displayName = displayName;
            this.category = category;
            this.description = description;
            this.configType = configType;
        }
    }
}
