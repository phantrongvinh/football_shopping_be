package com.datn.project.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PromotionResponse {
    private Integer id;
    private String name;
    private String discountType;  
    private BigDecimal discountValue;
    private LocalDateTime startAt;
    private LocalDateTime endAt;
}