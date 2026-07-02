package com.datn.project.dto.auth;

import java.time.LocalDate;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProfileResponse {

    private String email;
    private String phone;
    private String fullName;
    private LocalDate birthDay;
    private List<String> roles;
}