package com.datn.project.service;

import org.springframework.http.ResponseEntity;

import com.datn.project.dto.auth.LoginRequest;
import com.datn.project.dto.auth.RegisterRequest;
import com.datn.project.entity.User;

import jakarta.servlet.http.HttpServletRequest;

public interface IAuthService {

    ResponseEntity<?> register(RegisterRequest request);

    ResponseEntity<?> login(LoginRequest request);

    ResponseEntity<?> logout(HttpServletRequest request);

    // void sendVerificationEmail(User user, String token);

    void activate(String token);

    void resendActivation(String email);

    ResponseEntity<?> me();

    ResponseEntity<?> forgotPassword(String email);

    ResponseEntity<?> resetPassword(String token, String newPassword);
}
