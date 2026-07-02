package com.datn.project.service;

import java.math.BigDecimal;
import java.util.Optional;

import org.springframework.http.ResponseEntity;

import com.datn.project.dto.TimePromotionRequest;
import com.datn.project.entity.TimePromotion;

public interface ITimePromotionService {

    Optional<TimePromotion> getActiveTimePromotion();

    ResponseEntity<?> getAllTimePromotion(int page, int size);

    BigDecimal calcDiscount(BigDecimal orderTotal, TimePromotion promotion);

    ResponseEntity<?> createTimePromotion(TimePromotionRequest request);

    ResponseEntity<?> updateTimePromotion(Integer id, TimePromotionRequest request);

    ResponseEntity<?> toggleActive(Integer id);
}
