package com.datn.project.controller;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.datn.project.dto.product.ProductFilterDTO;
import com.datn.project.service.IProductService;

@RestController
@RequestMapping(value = "/api/v1/products")
public class ProductController {

    @Autowired
    private IProductService productService;

    @GetMapping()
    public ResponseEntity<?> getFilterProducts(
            @RequestParam(required = false) List<Integer> audienceIds,
            @RequestParam(required = false) List<Integer> brandIds,
            @RequestParam(required = false) List<Integer> categoryIds,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Boolean onSale,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) String sortBy,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        ProductFilterDTO filter = new ProductFilterDTO(brandIds, categoryIds, audienceIds, search, onSale, minPrice,
                maxPrice, sortBy);

        return ResponseEntity.ok(productService.getFilterProducts(filter, page, size)).getBody();
    }

    @GetMapping("/spotlight")
    public ResponseEntity<?> getSpotlightProducts() {
        return ResponseEntity.ok(productService.getSpotlightProducts()).getBody();
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getProductById(@PathVariable(name = "id") int id) {
        return ResponseEntity.ok(productService.getProductDetail(id)).getBody();
    }

    @GetMapping("/sales")
    public ResponseEntity<?> getProductOnSale() {
        return ResponseEntity.ok(productService.getProductOnSale()).getBody();
    }
}
