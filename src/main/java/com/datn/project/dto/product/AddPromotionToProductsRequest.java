package com.datn.project.dto.product;

import java.util.List;

import lombok.Data;

@Data
public class AddPromotionToProductsRequest {
    private Integer promotionId;
    private List<Integer> productIds;
}
