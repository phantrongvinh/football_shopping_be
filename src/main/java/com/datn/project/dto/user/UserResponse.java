package com.datn.project.dto.user;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {
    private int id;
    private String fullName;
    private String email;
    private String phone;
    private LocalDate birthDay;
    private boolean isActive;
    private LocalDateTime createdAt;
    private List<RoleResponse> role;
}
