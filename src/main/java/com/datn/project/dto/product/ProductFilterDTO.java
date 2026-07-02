package com.datn.project.dto.product;

import java.math.BigDecimal;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductFilterDTO {
    private List<Integer> brandIds;
    private List<Integer> categoryIds;
    private List<Integer> audienceIds;
    private String search;
    private Boolean onSale;

    private BigDecimal minPrice;
    private BigDecimal maxPrice;

    private String sortBy;
}
