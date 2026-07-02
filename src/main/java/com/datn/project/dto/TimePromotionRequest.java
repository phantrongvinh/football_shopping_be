package com.datn.project.dto;

import java.math.BigDecimal;
import java.time.LocalTime;

import com.datn.project.entity.DiscountType;

import lombok.Data;

@Data
public class TimePromotionRequest {
    private String name;
    private DiscountType discountType;
    private BigDecimal discountValue;
    private LocalTime startTime;
    private LocalTime endTime;
    private boolean isActive;
}
