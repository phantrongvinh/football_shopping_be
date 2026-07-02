package com.datn.project.service;

import java.math.BigDecimal;
import java.util.Optional;

import org.springframework.http.ResponseEntity;

import com.datn.project.dto.PromotionRequest;
import com.datn.project.entity.Promotion;

public interface IPromotionService {

    ResponseEntity<?> createPromotion(PromotionRequest req);

    Optional<Promotion> getActivePromotion(Integer productId);

    BigDecimal calcDiscountedPrice(BigDecimal originalPrice, Promotion promotion);

    BigDecimal getDiscountAmount(BigDecimal price, Promotion promotion);

    ResponseEntity<?> getAllActivePromotioEntity();

    ResponseEntity<?> getAllPromotion(int page, int size);
} 
