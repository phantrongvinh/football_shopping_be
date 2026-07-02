package com.datn.project.dto.order;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderRequest {
    private List<OrderItemRequest> items;
    private String voucherCode;
    private String shippingAddress;
    private String receiverName;
    private String receiverPhone;
    private Integer paymentMethodId;
}
