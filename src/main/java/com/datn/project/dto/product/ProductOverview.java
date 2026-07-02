package com.datn.project.dto.product;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductOverview {
    
    private int id;
    private String name;
    private String category;
    private String brand;
    private BigDecimal basePrice;
    private int stock;
    private boolean status;
    private LocalDateTime updatedAt;
    private int variantCount;
}
