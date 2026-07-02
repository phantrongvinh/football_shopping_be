package com.datn.project.specification;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.jpa.domain.Specification;

import com.datn.project.dto.product.ProductFilterDTO;
import com.datn.project.entity.Brand;
import com.datn.project.entity.Category;
import com.datn.project.entity.Product;
import com.datn.project.entity.Promotion;
import com.datn.project.entity.TargetAudience;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;

public class ProductSpecification {

    public static Specification<Product> notDeleted() {
        return (root, query, cb) -> cb.isNull(root.get("deletedAt"));
    }

    public static Specification<Product> filter(ProductFilterDTO filterDTO) {
        return buildFilter(filterDTO, false);
    }

    public static Specification<Product> adminFilter(ProductFilterDTO filterDTO) {
        return buildFilter(filterDTO, true);
    }

    private static Specification<Product> buildFilter(ProductFilterDTO filterDTO, boolean isAdmin) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Chỉ lấy sản phẩm chưa bị xoá (User)
            if (!isAdmin) {
                predicates.add(cb.isNull(root.get("deletedAt")));
            }

            // Filter audience
            if (filterDTO.getAudienceIds() != null && !filterDTO.getAudienceIds().isEmpty()) {
                Join<Product, TargetAudience> audienceJoin = root.join("targetAudience");
                predicates.add(audienceJoin.get("id").in(filterDTO.getAudienceIds()));
            }

            // Filter brand
            if (filterDTO.getBrandIds() != null && !filterDTO.getBrandIds().isEmpty()) {
                Join<Product, Brand> brandJoin = root.join("brand");
                predicates.add(brandJoin.get("id").in(filterDTO.getBrandIds()));
            }

            // Filter category
            if (filterDTO.getCategoryIds() != null && !filterDTO.getCategoryIds().isEmpty()) {
                Join<Product, Category> categoryJoin = root.join("category");
                predicates.add(categoryJoin.get("id").in(filterDTO.getCategoryIds()));
            }

            // Search: name > brand > category > description
            if (filterDTO.getSearch() != null && !filterDTO.getSearch().trim().isEmpty()) {
                String kw = "%" + filterDTO.getSearch().trim().toLowerCase() + "%";
                Predicate byName = cb.like(cb.lower(root.get("name")), kw);
                Predicate byBrand = cb.like(cb.lower(root.get("brand").get("name")), kw);
                Predicate byCategory = cb.like(cb.lower(root.get("category").get("name")), kw);
                Predicate byDescription = cb.like(cb.lower(root.get("description")), kw);
                predicates.add(cb.or(byName, byBrand, byCategory, byDescription));
            }

            // Đang giảm giá
            if (Boolean.TRUE.equals(filterDTO.getOnSale())) {
                LocalDateTime now = LocalDateTime.now();
                Join<Product, Promotion> promoJoin = root.join("promotions", JoinType.INNER);
                predicates.add(cb.lessThanOrEqualTo(promoJoin.get("startAt"), now));
                predicates.add(cb.greaterThanOrEqualTo(promoJoin.get("endAt"), now));
            }

            // Khoảng giá (theo minPrice của variant)
            if (filterDTO.getMinPrice() != null || filterDTO.getMaxPrice() != null) {
                if (filterDTO.getMinPrice() != null) {
                    predicates.add(cb.greaterThanOrEqualTo(root.get("basePrice"), filterDTO.getMinPrice()));
                }
                if (filterDTO.getMaxPrice() != null) {
                    predicates.add(cb.lessThanOrEqualTo(root.get("basePrice"), filterDTO.getMaxPrice()));
                }
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
