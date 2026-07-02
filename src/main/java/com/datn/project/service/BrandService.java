package com.datn.project.service;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.datn.project.dto.BrandResponse;
import com.datn.project.entity.Brand;
import com.datn.project.repository.IBrandRepository;

@Service
public class BrandService implements IBrandService {
    
    @Autowired
    private IBrandRepository brandRepository;

    @Override
    public ResponseEntity<?> getAllBrand() {
        List<Brand> brands = brandRepository.findAll();

        if (brands.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "Brands not found"));
        }

        List<BrandResponse> responses = brands.stream().map(b -> {
            BrandResponse response = new BrandResponse();

            response.setId(b.getId());
            response.setName(b.getName());

            return response;
        }).toList();

        return ResponseEntity.ok(responses);
    }
    
}
