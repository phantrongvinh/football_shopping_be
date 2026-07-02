package com.datn.project.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.datn.project.entity.User;
import com.datn.project.entity.VerificationToken;

public interface IVerificationTokenRepository extends JpaRepository<VerificationToken, Integer> {

    Optional<VerificationToken> findByToken(String token);

    @Modifying
    @Query("""
                delete from VerificationToken v
                where v.user = :user
            """)
    void deleteAllByUser(
            @Param("user") User user);
}
