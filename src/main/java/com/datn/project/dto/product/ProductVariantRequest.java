package com.datn.project.dto.product;

import java.math.BigDecimal;

import lombok.Data;

@Data
public class ProductVariantRequest {
    private Integer id;      
    private String color;
    private Integer sizeId;
    private Integer stock;
    private BigDecimal price;
    private String sku;
}
