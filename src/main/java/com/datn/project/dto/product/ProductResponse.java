package com.datn.project.dto.product;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.datn.project.dto.PromotionResponse;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductResponse {
    
    private int id;
    private String name;
    private String description;
    private BigDecimal basePrice;
    private LocalDateTime createdAt;
    private String category;
    private int categoryId;
    private String brand;
    private int brandId;
    private String targetAudience;
    private int targetAudienceId;
    private boolean isAccessory;
    private List<ProductImagesResponse> imgs;
    private List<ProductVariantResponse> productVariant;
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    private BigDecimal discountedPrice;
    private PromotionResponse promotion;
    private LocalDateTime deletedAt;
}
