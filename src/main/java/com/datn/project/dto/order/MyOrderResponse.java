package com.datn.project.dto.order;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MyOrderResponse {
    private int id;
    private BigDecimal price;
    private String status;
    private List<OrderDetailResponse> items;
    private String trackingCode;
    private String paymentStatus;
    private LocalDateTime createdAt;
    private String paymentMethod;
}
