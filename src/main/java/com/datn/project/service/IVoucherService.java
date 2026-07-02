package com.datn.project.service;

import java.math.BigDecimal;

import org.apache.coyote.BadRequestException;

import com.datn.project.entity.Voucher;

public interface IVoucherService {
    Voucher validateVoucher(String code, BigDecimal orderTotal) throws BadRequestException;

    BigDecimal calcDiscount(BigDecimal orderTotal, Voucher voucher);

    void incrementUsedCount(Voucher voucher);

    void decrementUsedCount(Voucher voucher);
}
