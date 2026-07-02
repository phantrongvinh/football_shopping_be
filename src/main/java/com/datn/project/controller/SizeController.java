package com.datn.project.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.datn.project.service.ISizeService;

@RestController
@RequestMapping(value = "/api/v1/sizes")
public class SizeController {
    
    @Autowired
    private ISizeService sizeService;

    @GetMapping
    public ResponseEntity<?> getAllSize(){
        return ResponseEntity.ok(sizeService.getAllSize()).getBody();
    }
}
