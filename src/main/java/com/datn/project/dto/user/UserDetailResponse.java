package com.datn.project.dto.user;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import com.datn.project.dto.adress.AddressResponse;
import com.datn.project.entity.AuthProvider;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserDetailResponse {
    private int id;
    private String fullName;
    private String email;
    private String phone;
    private LocalDate birthDay;
    private boolean isActived;
    private LocalDateTime createdAt;
    private AuthProvider authProvider;

    // Địa chỉ
    private List<AddressResponse> addresses;

    // Thống kê đơn hàng
    private int totalOrders;
    private BigDecimal totalSpent;
}
