package com.datn.project.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.datn.project.entity.Promotion;

import jakarta.transaction.Transactional;

public interface IPromotionRepository extends JpaRepository<Promotion, Integer> {
    @Query("""
                SELECT p FROM Promotion p
                JOIN p.products pr
                WHERE pr.id = :productId
                AND :now BETWEEN p.startAt AND p.endAt
                ORDER BY p.discountValue DESC
            """)
    Optional<Promotion> findActiveByProductId(
            @Param("productId") Integer productId,
            @Param("now") LocalDateTime now);

    @Query("""
                SELECT p FROM Promotion p
                WHERE p.startAt <= :now AND p.endAt >= :now
            """)
    List<Promotion> findActivePromotion(@Param("now") LocalDateTime now);

    @Modifying
    @Transactional
    @Query(value = "DELETE FROM promotion_products WHERE product_id IN :productIds", nativeQuery = true)
    void removePromotionsByProductIds(@Param("productIds") List<Integer> productIds);

    @Modifying
    @Transactional
    @Query(value = """
                INSERT INTO promotion_products (promotion_id, product_id)
                VALUES (:promotionId, :productId)
                ON DUPLICATE KEY UPDATE promotion_id = :promotionId
            """, nativeQuery = true)
    void assignPromotionToProduct(
            @Param("promotionId") Integer promotionId,
            @Param("productId") Integer productId);
}
