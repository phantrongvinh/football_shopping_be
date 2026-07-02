package com.datn.project.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.datn.project.dto.TimePromotionRequest;
import com.datn.project.repository.ITimePromotionRepository;
import com.datn.project.service.ITimePromotionService;

@RestController
@RequestMapping(value = "/api/v1/time-promotions")
public class TimePromotionController {

    @Autowired
    private ITimePromotionService timePromotionService;

    @GetMapping("/active")
    public ResponseEntity<?> getActive() {
        return ResponseEntity.ok(timePromotionService.getActiveTimePromotion().orElse(null));
    }

}
