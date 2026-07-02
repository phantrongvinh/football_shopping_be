package com.datn.project.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.util.Locale;

import org.apache.coyote.BadRequestException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.datn.project.entity.DiscountType;
import com.datn.project.entity.Voucher;
import com.datn.project.repository.IVoucherRepository;

import jakarta.transaction.Transactional;

@Service
public class VoucherService implements IVoucherService {

    @Autowired
    private IVoucherRepository voucherRepository;

    @Override
    public Voucher validateVoucher(String code, BigDecimal orderTotal) throws BadRequestException {
        Voucher voucher = voucherRepository.findByCode(code)
                .orElseThrow(() -> new BadRequestException("Mã giảm giá không tồn tại"));

        LocalDateTime now = LocalDateTime.now();

        if (!voucher.isActive())
            throw new BadRequestException("Mã giảm giá không còn hiệu lực");
        if (voucher.getStartDate() != null && now.isBefore(voucher.getStartDate()))
            throw new BadRequestException("Mã giảm giá chưa đến thời gian sử dụng");
        if (voucher.getEndDate() != null && now.isAfter(voucher.getEndDate()))
            throw new BadRequestException("Mã giảm giá đã hết hạn");
        if (voucher.getQuantity() != null && voucher.getUsedCount() >= voucher.getQuantity())
            throw new BadRequestException("Mã giảm giá đã hết lượt sử dụng");

        // Chỉ check minOrderValue khi orderTotal > 0
        if (orderTotal.compareTo(BigDecimal.ZERO) > 0
                && voucher.getMinOrderValue() != null
                && orderTotal.compareTo(voucher.getMinOrderValue()) < 0) {
            throw new BadRequestException(
                    "Đơn hàng tối thiểu "
                            + NumberFormat.getCurrencyInstance(new Locale("vi", "VN"))
                                    .format(voucher.getMinOrderValue())
                            + " để dùng mã này");
        }
        return voucher;
    }

    @Override
    public BigDecimal calcDiscount(BigDecimal orderTotal, Voucher voucher) {
        BigDecimal discount;
        if (voucher.getDiscountType() == DiscountType.PERCENT) {
            discount = orderTotal
                    .multiply(voucher.getDiscountValue().divide(BigDecimal.valueOf(100)))
                    .setScale(0, RoundingMode.HALF_UP);
            // giới hạn max discount nếu có
            if (voucher.getMaxDiscount() != null) {
                discount = discount.min(voucher.getMaxDiscount());
            }
        } else {
            discount = voucher.getDiscountValue().min(orderTotal);
        }
        return discount;

    }

    @Override
    @Transactional
    public void incrementUsedCount(Voucher voucher) {
        voucher.setUsedCount(voucher.getUsedCount() + 1);
        voucherRepository.save(voucher);
    }

    @Override
    public void decrementUsedCount(Voucher voucher) {
        if (voucher.getUsedCount() > 0) {
            voucher.setUsedCount(voucher.getUsedCount() - 1);
            voucherRepository.save(voucher);
        }
    }

}
