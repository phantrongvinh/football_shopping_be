package com.datn.project.specification;

import java.time.LocalDateTime;

import org.springframework.data.jpa.domain.Specification;

import com.datn.project.dto.order.OrderFilterDTO;
import com.datn.project.entity.Order;
import com.datn.project.entity.OrderStatus;
import com.datn.project.entity.PaymentMethod;
import com.datn.project.entity.PaymentStatus;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;

public class OrderSpecification {
    public static Specification<Order> filter(OrderFilterDTO filter) {
        return Specification
                .where(hasSearch(filter.getSearch()))
                .and(hasStatus(filter.getStatus()))
                .and(hasPaymentStatus(filter.getPaymentStatus()))
                .and(hasPaymentMethod(filter.getPaymentMethodId()))
                .and(hasDateFrom(filter.getDateFrom()))
                .and(hasDateTo(filter.getDateTo()));
    }

    // Tìm theo tên người nhận, SĐT, hoặc mã đơn
    private static Specification<Order> hasSearch(String search) {
        return (root, query, cb) -> {
            if (search == null || search.isBlank())
                return null;
            String keyword = "%" + search.toLowerCase() + "%";
            return cb.or(
                    cb.like(cb.lower(root.get("receiverName")), keyword),
                    cb.like(root.get("receiverPhone"), keyword),
                    cb.like(cb.toString(root.get("id")), keyword));
        };
    }

    private static Specification<Order> hasStatus(OrderStatus status) {
        return (root, query, cb) -> status == null ? null : cb.equal(root.get("status"), status);
    }

    private static Specification<Order> hasPaymentStatus(PaymentStatus paymentStatus) {
        return (root, query, cb) -> paymentStatus == null ? null : cb.equal(root.get("paymentStatus"), paymentStatus);
    }

    private static Specification<Order> hasPaymentMethod(Integer paymentMethodId) {
        return (root, query, cb) -> {
            if (paymentMethodId == null)
                return null;
            Join<Order, PaymentMethod> join = root.join("paymentMethod", JoinType.LEFT);
            return cb.equal(join.get("id"), paymentMethodId);
        };
    }

    private static Specification<Order> hasDateFrom(LocalDateTime dateFrom) {
        return (root, query, cb) -> dateFrom == null ? null : cb.greaterThanOrEqualTo(root.get("createdAt"), dateFrom);
    }

    private static Specification<Order> hasDateTo(LocalDateTime dateTo) {
        return (root, query, cb) -> dateTo == null ? null : cb.lessThanOrEqualTo(root.get("createdAt"), dateTo);
    }
}
