package com.datn.project.dto.product;

import lombok.Data;

@Data
public class ProductImagesResponse {
    private int id;
    private String imageUrl;
    private boolean isPrimary;
}
