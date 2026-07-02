package com.datn.project.dto.product;

import lombok.Data;

@Data
public class ProductImageRequest {
    private Integer id;      
    private String imageUrl;
    private Boolean isPrimary;
}
