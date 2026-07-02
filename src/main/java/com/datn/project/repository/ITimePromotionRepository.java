package com.datn.project.repository;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.datn.project.entity.TimePromotion;

public interface ITimePromotionRepository extends JpaRepository<TimePromotion, Integer> {

    @Query("""
                SELECT t FROM TimePromotion t
                WHERE t.isActive = true
                AND :now BETWEEN t.startTime AND t.endTime
            """)
    Optional<TimePromotion> findActiveByTime(@Param("now") LocalTime now);

    @Query("""
                SELECT t FROM TimePromotion t
                WHERE t.isActive = true
                AND (
                    (t.startTime <= :startTime AND t.endTime > :startTime)
                    OR (t.startTime < :endTime AND t.endTime >= :endTime)
                    OR (t.startTime >= :startTime AND t.endTime <= :endTime)
                )
                AND (:excludeId IS NULL OR t.id <> :excludeId)
            """)
    List<TimePromotion> findOverlapping(
            @Param("startTime") LocalTime startTime,
            @Param("endTime") LocalTime endTime,
            @Param("excludeId") Integer excludeId);
}
