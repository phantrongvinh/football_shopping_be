package com.datn.project.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.datn.project.entity.ForgotPasswordToken;

public interface IForgotPasswordTokenRepository extends JpaRepository<ForgotPasswordToken, Integer> {

    Optional<ForgotPasswordToken> findByToken(String token);

    ForgotPasswordToken findByEmail(String email);
}
