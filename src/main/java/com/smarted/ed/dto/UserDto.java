package com.smarted.ed.dto;

import com.smarted.ed.enums.RoleType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserDto {
    private Integer id;
    private String email;
    private String fullName;
    private String phone;
    private String avatarUrl;
    private RoleType role;
    private Boolean isActive;
    private Boolean emailVerified;
    private LocalDateTime createdAt;
}
