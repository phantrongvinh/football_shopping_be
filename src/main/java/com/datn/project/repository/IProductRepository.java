package com.datn.project.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.datn.project.entity.Product;

import jakarta.transaction.Transactional;

public interface IProductRepository extends JpaRepository<Product, Integer>, JpaSpecificationExecutor<Product> {
    List<Product> findTop4ByDeletedAtIsNullOrderByCreatedAtDesc();

    List<Product> findTop5ByDeletedAtIsNullOrderByCreatedAtDesc();

    List<Product> findByDeletedAtIsNull();

    @Query("""
                SELECT p FROM Product p
                JOIN FETCH p.category
                JOIN FETCH p.brand
                JOIN FETCH p.targetAudience
                LEFT JOIN FETCH p.productImages
                LEFT JOIN FETCH p.productVariants pv
                LEFT JOIN FETCH pv.size
                WHERE p.id = :id AND p.deletedAt IS NULL
            """)
    Optional<Product> findDetailById(@Param("id") Integer id);

    // Soft delete
    @Modifying
    @Transactional
    @Query("UPDATE Product p SET p.deletedAt = :now WHERE p.id = :id")
    void softDelete(@Param("id") Integer id, @Param("now") LocalDateTime now);

    @Query("""
                SELECT DISTINCT p
                FROM Product p
                JOIN p.promotions promo
                WHERE promo.startAt <= CURRENT_TIMESTAMP
                  AND promo.endAt >= CURRENT_TIMESTAMP
            """)
    List<Product> findProductsOnSale(Pageable pageable);

    @Query("""
                SELECT DISTINCT p FROM Product p
                LEFT JOIN FETCH p.productImages
                LEFT JOIN FETCH p.category
                LEFT JOIN FETCH p.brand
                LEFT JOIN FETCH p.targetAudience
                WHERE p.id = :id AND p.deletedAt IS NULL
            """)
    Optional<Product> findDetailByIdWithImages(@Param("id") Integer id);

    @Query("""
                SELECT DISTINCT p FROM Product p
                LEFT JOIN FETCH p.productVariants pv
                LEFT JOIN FETCH pv.size
                WHERE p.id = :id
            """)
    Optional<Product> findDetailByIdWithVariants(@Param("id") Integer id);

    
    @Query("""
                SELECT DISTINCT p FROM Product p
                LEFT JOIN FETCH p.productImages
                LEFT JOIN FETCH p.category
                LEFT JOIN FETCH p.brand
                LEFT JOIN FETCH p.targetAudience
                WHERE p.id IN :ids AND p.deletedAt IS NULL
            """)
    List<Product> findAllWithImagesByIds(@Param("ids") List<Integer> ids);

    @Query("""
                SELECT DISTINCT p FROM Product p
                LEFT JOIN FETCH p.productVariants pv
                LEFT JOIN FETCH pv.size
                WHERE p.id IN :ids
            """)
    List<Product> findAllWithVariantsByIds(@Param("ids") List<Integer> ids);
}
