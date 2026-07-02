package com.datn.project.dto.product;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductVariantResponse {

    private int id;
    private String color;
    private String size;
    private int sizeId;
    private int stock;
    private BigDecimal price;
    private BigDecimal discountedPrice;
    private String sku;
    private LocalDateTime createdAt;
}
