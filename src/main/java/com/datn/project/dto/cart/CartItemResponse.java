package com.datn.project.dto.cart;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CartItemResponse {
    private int productVariantId;
    private String name;
    private String sku;
    private String image;
    private String size;
    private String color;
    private int quantity;
    private BigDecimal price;
    private BigDecimal originalPrice;
}
