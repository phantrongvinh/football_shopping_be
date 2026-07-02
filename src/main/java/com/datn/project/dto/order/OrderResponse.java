package com.datn.project.dto.order;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderResponse {
    private Integer id;
    private List<OrderDetailResponse> items;
    private BigDecimal totalPrice;
    private BigDecimal discountAmount;
    private BigDecimal timeDiscount;
    private BigDecimal finalPrice;
    private String voucherCode;
    private String timePromotionName;
    private String status;
    private String shippingAddress;
    private String receiverName;
    private String receiverPhone;
    private LocalDateTime createdAt;
    private String paymentStatus;
    private String trackingCode;
    private String paymentMethod;
    private String transactionId;
}
