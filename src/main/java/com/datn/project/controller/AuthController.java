package com.datn.project.controller;

import java.io.IOException;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.datn.project.dto.auth.LoginRequest;
import com.datn.project.dto.auth.RegisterRequest;
import com.datn.project.dto.auth.ResendMailRequest;
import com.datn.project.service.IAuthService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@RestController
@RequestMapping(value = "/api/v1/auth")
public class AuthController {

    @Autowired
    private IAuthService authService;

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authService.register(request)).getBody();
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request)).getBody();
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request) {
        return ResponseEntity.ok(authService.logout(request)).getBody();
    }

    @GetMapping("/activate")
    public void activate(@RequestParam String token, HttpServletResponse response) throws IOException {
        try {

            authService.activate(token);

            response.sendRedirect(
                    "http://localhost:5173/activate-success");

        } catch (Exception e) {

            String message = e.getMessage();

            if (message.equals("EXPIRED")) {

                response.sendRedirect(
                        "http://localhost:5173/activation-error?type=expired");

            } else {

                response.sendRedirect(
                        "http://localhost:5173/activation-error?type=invalid");
            }
        }
    }

    @PostMapping("/resend-activation")
    public ResponseEntity<?> resendActivation(
            @RequestBody ResendMailRequest request) {

        authService.resendActivation(request.getEmail());

        return ResponseEntity.ok(
                Map.of(
                        "message",
                        "Mã kích hoạt đã được gửi đến mail của bạn"));
    }

    @GetMapping("/me")
    public ResponseEntity<?> me() {
        return ResponseEntity.ok(authService.me()).getBody();
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody Map<String, String> body){
        return ResponseEntity.ok(authService.forgotPassword(body.get("email"))).getBody();
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody Map<String, String> body){
        System.out.println(body);
        return ResponseEntity.ok(authService.resetPassword(body.get("token"), body.get("password"))).getBody();
    }
}