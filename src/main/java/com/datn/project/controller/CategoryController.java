package com.datn.project.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.datn.project.service.ICategoryService;


@RestController
@RequestMapping(value = "/api/v1/categories")
public class CategoryController {
    
    @Autowired
    private ICategoryService categoryService;

    @GetMapping
    public ResponseEntity<?> getAllCategory() {
        return ResponseEntity.ok(categoryService.getAllCategories()).getBody();
    }

    @GetMapping("/accessory")
    public ResponseEntity<?> getAllAccessories() {
        return ResponseEntity.ok(categoryService.getAllAccessories()).getBody();
    }
}
