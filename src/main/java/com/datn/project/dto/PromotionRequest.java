package com.datn.project.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.datn.project.entity.DiscountType;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PromotionRequest {
    private String name;
    private DiscountType discountType;
    private BigDecimal discountValue;
    private LocalDateTime startAt;
    private LocalDateTime endAt;
    private List<Integer> productIds ;
}
