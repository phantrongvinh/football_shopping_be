package com.datn.project.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.datn.project.entity.ProductVariant;

import jakarta.transaction.Transactional;

public interface IProductVariantRepository extends JpaRepository<ProductVariant, Integer> {
    @Query("""
                SELECT pv FROM ProductVariant pv
                JOIN FETCH pv.product p
                JOIN FETCH pv.size s
                WHERE pv.id = :id
            """)
    Optional<ProductVariant> findByIdWithProduct(@Param("id") Integer id);

    @Transactional
    @Modifying(clearAutomatically = true)
    @Query("UPDATE ProductVariant pv SET pv.stock = pv.stock - :qty WHERE pv.id = :id AND pv.stock >= :qty")
    int decreaseStock(@Param("id") Integer id, @Param("qty") Integer qty);

    List<ProductVariant> findByProductId(Integer productId);

    @Modifying
    @Transactional
    @Query("DELETE FROM ProductVariant pv WHERE pv.product.id = :productId AND pv.id NOT IN :keepIds")
    void deleteByProductIdAndIdNotIn(
            @Param("productId") Integer productId,
            @Param("keepIds") List<Integer> keepIds);

    @Modifying
    @Transactional
    @Query("DELETE FROM ProductVariant v WHERE v.product.id = :productId")
    void deleteByProductId(@Param("productId") Integer productId);

    @Transactional
    @Modifying
    @Query("UPDATE ProductVariant pv SET pv.stock = pv.stock + :qty WHERE pv.id = :id")
    void increaseStock(@Param("id") Integer id, @Param("qty") Integer qty);
}
