package com.datn.project.dto.product;

import java.math.BigDecimal;
import java.util.List;

import lombok.Data;

@Data
public class ProductRequest {
     private String name;
    private String description;
    private BigDecimal basePrice;
    private Integer categoryId;
    private Integer brandId;
    private Integer targetAudienceId;
    private List<ProductVariantRequest> variants;
    private List<ProductImageRequest> images;
    private Integer promotionId;
}
