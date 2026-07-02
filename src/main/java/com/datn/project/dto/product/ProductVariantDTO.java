package com.datn.project.dto.product;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ProductVariantDTO {
    private Integer id;
    private String color;
    private Integer sizeId;
    private String sizeName;
    private Integer stock;
    private BigDecimal discountedPrice;
    private BigDecimal price;
    private String sku;
}