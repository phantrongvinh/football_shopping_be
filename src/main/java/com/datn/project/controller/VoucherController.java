package com.datn.project.controller;

import java.math.BigDecimal;
import java.util.Map;

import org.apache.coyote.BadRequestException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.datn.project.entity.Voucher;
import com.datn.project.service.IVoucherService;

@RestController
@RequestMapping(value = "/api/v1/vouchers")
public class VoucherController {

    @Autowired
    private IVoucherService voucherService;

    @GetMapping("/apply")
    public ResponseEntity<?> applyVoucher(
            @RequestParam String code,
            @RequestParam BigDecimal orderTotal) throws BadRequestException {
        Voucher voucher = voucherService.validateVoucher(code, orderTotal);
        BigDecimal discount = voucherService.calcDiscount(orderTotal, voucher);
        return ResponseEntity.ok(Map.of(
                "code", voucher.getCode(),
                "discountType", voucher.getDiscountType().name().toLowerCase(),
                "discountValue", voucher.getDiscountValue(),
                "discount", discount,
                "minOrderValue", voucher.getMinOrderValue(),
                "maxDiscount", voucher.getMaxDiscount(),
                "isStackable", voucher.isStackable()));
    }
}
