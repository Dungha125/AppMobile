package com.eldercare.dto;

import com.eldercare.model.enums.UserRole;
import lombok.Data;

@Data
public class UpdateUserRequest {
    private Boolean isActive;
    private UserRole role;
}
