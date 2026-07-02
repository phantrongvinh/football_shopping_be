package com.datn.project.service;

import org.springframework.http.ResponseEntity;

public interface ICategoryService {
    
    ResponseEntity<?> getAllCategories();

    ResponseEntity<?> getAllAccessories();
}
