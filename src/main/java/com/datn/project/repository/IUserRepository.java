package com.datn.project.repository;

import java.math.BigDecimal;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.datn.project.entity.User;

public interface IUserRepository extends JpaRepository<User, Integer>, JpaSpecificationExecutor<User> {

    @Query("SELECT u FROM User u JOIN FETCH u.roles WHERE u.email = :email")
    Optional<User> findByEmailWithRoles(@Param("email") String email);

    boolean existsByEmail(String email);

    Optional<User> findByEmail(String email);

    @Query("""
                SELECT COUNT(o) FROM Order o
                WHERE o.user.id = :userId
            """)
    int countOrdersByUserId(@Param("userId") int userId);

    @Query("""
                SELECT COALESCE(SUM(o.finalPrice), 0) FROM Order o
                WHERE o.user.id = :userId
            """)
    BigDecimal sumFinalPriceByUserId(@Param("userId") int userId);
}
