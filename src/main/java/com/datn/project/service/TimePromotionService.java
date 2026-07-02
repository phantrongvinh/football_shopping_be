package com.datn.project.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.datn.project.dto.TimePromotionRequest;
import com.datn.project.entity.DiscountType;
import com.datn.project.entity.TimePromotion;
import com.datn.project.repository.ITimePromotionRepository;

import jakarta.transaction.Transactional;

@Service
public class TimePromotionService implements ITimePromotionService {

    @Autowired
    private ITimePromotionRepository timePromotionRepository;

    // lấy khuyến mãi giờ đang được active
    @Override
    public Optional<TimePromotion> getActiveTimePromotion() {
        LocalTime now = LocalTime.now();
        return timePromotionRepository.findActiveByTime(now);
    }

    // tính toán khoảng giảm giá
    @Override
    public BigDecimal calcDiscount(BigDecimal orderTotal, TimePromotion promotion) {
        if (promotion.getDiscountType() == DiscountType.PERCENT) {
            return orderTotal.multiply(
                    promotion.getDiscountValue().divide(BigDecimal.valueOf(100))).setScale(0, RoundingMode.HALF_UP);
        }
        return promotion.getDiscountValue().min(orderTotal);
    }

    // lấy danh sách khuyến mãi theo khung giờ
    @Override
    public ResponseEntity<?> getAllTimePromotion(int page, int size) {
        Pageable pageable = PageRequest.of(page - 1, size);

        Page<TimePromotion> resPage = timePromotionRepository.findAll(pageable);

        if (resPage.isEmpty()) {
            return ResponseEntity.ok(Map.of("message", "Không có khuyến mãi khung giờ vàng"));
        }

        return ResponseEntity.ok(resPage);
    }

    // tạo khung giờ khuyến mãi
    @Transactional
    @Override
    public ResponseEntity<?> createTimePromotion(TimePromotionRequest request) {
        validateTimeRange(request.getStartTime(), request.getEndTime(), null);

        TimePromotion promotion = new TimePromotion();
        setFields(promotion, request);
        timePromotionRepository.save(promotion);
        return ResponseEntity.ok("Tạo khuyến mãi thành công");
    }

    // cập nhật lại khuyến mãi
    @Override
    @Transactional
    public ResponseEntity<?> updateTimePromotion(Integer id, TimePromotionRequest request) {
        TimePromotion promotion = timePromotionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Time promotion không tồn tại"));

        validateTimeRange(request.getStartTime(), request.getEndTime(), id);

        setFields(promotion, request);
        timePromotionRepository.save(promotion);
        return ResponseEntity.ok("Cập nhật khuyến mãi thành công");
    }

    // active hoặc deactive
    @Transactional
    @Override
    public ResponseEntity<?> toggleActive(Integer id) {
        TimePromotion promotion = timePromotionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Time promotion không tồn tại"));

        // Nếu muốn active lại thì check overlap
        if (!promotion.isActive()) {
            validateTimeRange(promotion.getStartTime(), promotion.getEndTime(), id);
        }

        promotion.setActive(!promotion.isActive());
        timePromotionRepository.save(promotion);
        return ResponseEntity.ok("Cập nhật trạng thái khuyến mãi thành công");
    }

    // ─── Validate overlap ─────────────────────────────────
    private void validateTimeRange(LocalTime startTime, LocalTime endTime, Integer excludeId) {
        if (startTime.isAfter(endTime) || startTime.equals(endTime)) {
            throw new RuntimeException("Giờ bắt đầu phải nhỏ hơn giờ kết thúc");
        }

        List<TimePromotion> overlapping = timePromotionRepository
                .findOverlapping(startTime, endTime, excludeId);

        if (!overlapping.isEmpty()) {
            TimePromotion conflict = overlapping.get(0);
            throw new RuntimeException(
                    String.format("Khung giờ bị trùng với '%s' (%s - %s)",
                            conflict.getName(),
                            conflict.getStartTime(),
                            conflict.getEndTime()));
        }
    }

    private void setFields(TimePromotion promotion, TimePromotionRequest request) {
        promotion.setName(request.getName());
        promotion.setDiscountType(request.getDiscountType());
        promotion.setDiscountValue(request.getDiscountValue());
        promotion.setStartTime(request.getStartTime());
        promotion.setEndTime(request.getEndTime());
        promotion.setActive(request.isActive());
    }
}
