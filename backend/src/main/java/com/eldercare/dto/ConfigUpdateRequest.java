package com.eldercare.dto;

import lombok.Data;

@Data
public class ConfigUpdateRequest {
    private String configKey;
    private String configValue;
    private String description;
    private String displayName;
    private String category;
    private String configType;
}
