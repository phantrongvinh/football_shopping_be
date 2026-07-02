package com.datn.project.service;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.datn.project.dto.CategoryResponse;
import com.datn.project.entity.Category;
import com.datn.project.repository.ICategoryRepository;

@Service
public class CategoryService implements ICategoryService {
    
    @Autowired
    private ICategoryRepository categoryRepository;

    @Override
    public ResponseEntity<?> getAllCategories() {
        List<Category> categories = categoryRepository.findAll();

        if (categories.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "Categories not found"));
        }

        List<CategoryResponse> responses = categories.stream().map(c -> {
            CategoryResponse response = new CategoryResponse();

            response.setId(c.getId());
            response.setName(c.getName());

            return response;
        }).toList();

        return ResponseEntity.ok(responses);
    }

    @Override
    public ResponseEntity<?> getAllAccessories() {
        List<Category> categories = categoryRepository.findByIsAccessory(true);

        if (categories.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "Categories not found"));
        }

        List<CategoryResponse> responses = categories.stream().map(c -> {
            CategoryResponse response = new CategoryResponse();

            response.setId(c.getId());
            response.setName(c.getName());

            return response;
        }).toList();

        return ResponseEntity.ok(responses);
    }
}
