package com.datn.project.dto.order;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderDetailResponse {
    private Integer productVariantId;
    private String productName;
    private String color;
    private String sizeName;
    private Integer quantity;
    private BigDecimal originalPrice;
    private BigDecimal price;
    private String promotionName;
}
