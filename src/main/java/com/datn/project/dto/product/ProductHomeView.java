package com.datn.project.dto.product;

import java.math.BigDecimal;

import com.datn.project.dto.PromotionResponse;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductHomeView {
    private int id;
    private String image;
    private String name;
    private String categoryName;
    private BigDecimal price;
    private BigDecimal discountPrice;
    private String brandName;
    private PromotionResponse promotion;
}
