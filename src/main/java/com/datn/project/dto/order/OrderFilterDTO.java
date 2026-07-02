package com.datn.project.dto.order;

import java.time.LocalDateTime;

import com.datn.project.entity.OrderStatus;
import com.datn.project.entity.PaymentStatus;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderFilterDTO {
    private String search;
    private OrderStatus status;
    private PaymentStatus paymentStatus;
    private Integer paymentMethodId;
    private LocalDateTime dateFrom;
    private LocalDateTime dateTo;
    private String sortBy;
}