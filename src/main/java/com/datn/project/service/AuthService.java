package com.datn.project.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.datn.project.config.JwtFilter;
import com.datn.project.dto.auth.LoginRequest;
import com.datn.project.dto.auth.ProfileResponse;
import com.datn.project.dto.auth.RegisterRequest;
import com.datn.project.entity.AuthProvider;
import com.datn.project.entity.ForgotPasswordToken;
import com.datn.project.entity.Role;
import com.datn.project.entity.User;
import com.datn.project.entity.VerificationToken;
import com.datn.project.repository.IForgotPasswordTokenRepository;
import com.datn.project.repository.IRoleRepository;
import com.datn.project.repository.IUserRepository;
import com.datn.project.repository.IVerificationTokenRepository;

import jakarta.servlet.http.HttpServletRequest;

@Service
public class AuthService implements IAuthService {

    @Autowired
    private MailService mailService;

    @Autowired
    private IUserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private JwtFilter jwtFilter;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private IRoleRepository roleRepository;

    @Autowired
    private JwtBlackListService jwtBlackListService;

    @Autowired
    private IVerificationTokenRepository verificationTokenRepository;

    @Autowired
    private IForgotPasswordTokenRepository forgotPasswordToken;

    AuthService(MailService mailService) {
        this.mailService = mailService;
    }

    @Override
    @Transactional
    public ResponseEntity<?> register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("message", "Tài khoản có email này đã tồn tại"));
        }

        if (!request.getPassword().equals(request.getConfirmPassword())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", "Không đúng mật khẩu"));
        }

        User user = new User();
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setFullName(request.getFullName());
        user.setActived(false);
        user.setAuthProvider(AuthProvider.LOCAL);

        Role role = roleRepository.findByName("ADMIN")
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Role not found"));

        List<Role> roles = new ArrayList<>();
        roles.add(role);

        user.setRoles(roles);

        userRepository.save(user);

        String token = UUID.randomUUID().toString();

        VerificationToken verificationToken = new VerificationToken();
        verificationToken.setToken(token);
        verificationToken.setUser(user);
        verificationToken.setExpiryDate(LocalDateTime.now().plusHours(24));

        verificationTokenRepository.save(verificationToken);

        sendVerificationEmail(user, token);

        return ResponseEntity.ok(Map.of("message","Đăng ký thành công, kiểm tra email để kích hoạt tài khoản"));
    }

    @Override
    public ResponseEntity<?> login(LoginRequest request) {

        Authentication authentication = authenticationManager
                .authenticate(new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();

        List<GrantedAuthority> roles = new ArrayList<>(userDetails.getAuthorities());

        String token = jwtService.generateToken(userDetails.getUsername(), roles);

        return ResponseEntity.ok(
                Map.of("token", token));
    }

    @Override
    public ResponseEntity<?> logout(HttpServletRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Chưa đăng nhập"));
        }

        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {

            String token = authHeader.substring(7);
            jwtBlackListService.blacklistToken(token);
        }

        SecurityContextHolder.clearContext();

        return ResponseEntity.ok(Map.of("message", "Đăng xuất thành công"));
    }

    @Override
    public ResponseEntity<?> me() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();

        User user = userRepository.findByEmailWithRoles(email).orElseThrow(() -> new RuntimeException(
                "Người dùng không tồn tại"));

        ProfileResponse response = new ProfileResponse();

        response.setEmail(email);
        response.setFullName(user.getFullName());
        response.setPhone(user.getPhone());
        response.setBirthDay(user.getBirthDay());

        List<String> roles = user.getRoles().stream().map(r -> r.getName()).toList();

        response.setRoles(roles);

        return ResponseEntity.ok(response);
    }

    private void sendVerificationEmail(User user, String token) {
        String link = "http://localhost:8080/api/v1/auth/activate?token=" + token;

        mailService.sendMessageEmail(user.getEmail(), "Kích hoạt tài khoản", "Bấm vào link dưới đây để kích hoạt: " + link);

    }

    @Override
    public void activate(String token) {
        VerificationToken verificationToken = verificationTokenRepository.findByToken(token)
                .orElseThrow(() -> new RuntimeException(
                        "Mã kích hoạt không hợp lệ"));

        if (verificationToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            throw new RuntimeException(
                    "Mã kích hoạt hết hạn");
        }

        User user = verificationToken.getUser();
        user.setActived(true);
        userRepository.save(user);
        verificationTokenRepository.delete(verificationToken);

    }

    @Override
    @Transactional
    public void resendActivation(
            String email) {

        User user = userRepository
                .findByEmail(email)
                .orElseThrow(() -> new RuntimeException(
                        "Người dùng không tìm thấy"));

        if (user.isActived()) {

            throw new RuntimeException(
                    "Tài khoản đã được kích hoạt");
        }

        verificationTokenRepository.deleteAllByUser(user);

        String token = UUID.randomUUID().toString();

        VerificationToken verificationToken = new VerificationToken();

        verificationToken.setToken(token);

        verificationToken.setUser(user);

        verificationToken.setExpiryDate(
                LocalDateTime.now().plusHours(24));

        verificationTokenRepository
                .save(verificationToken);

        sendVerificationEmail(user, token);
    }

    @Override
    public ResponseEntity<?> forgotPassword(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException(
                        "Tài khoản có email này không tồn tại"));

        ForgotPasswordToken oldResetToken = forgotPasswordToken.findByEmail(user.getEmail());

        if (oldResetToken != null) {
            forgotPasswordToken.delete(oldResetToken);
        }

        String token = UUID.randomUUID().toString();

        ForgotPasswordToken resetToken = new ForgotPasswordToken();
        resetToken.setEmail(email);
        resetToken.setToken(token);
        resetToken.setExpiryDate(LocalDateTime.now().plusMinutes(15)); // hết hạn sau 15 phút
        forgotPasswordToken.save(resetToken);

        // Gửi mail
        sendResetPassword(user, token);
        return ResponseEntity.ok("Email đã được gửi");
    }

    private void sendResetPassword(User user, String token) {
        String link = "http://localhost:5173/reset-password?token=" + token;
        mailService.sendMessageEmail(user.getEmail(), "Khôi phục mật khẩu", "Bấm vào đây để cập nhật mật khẩu mới: " + link);

    }

    @Override
    public ResponseEntity<?> resetPassword(String token, String newPassword) {
        ForgotPasswordToken resetToken = forgotPasswordToken.findByToken(token)
                .orElseThrow(() -> new RuntimeException("Mã khôi phục hết hạng, vui lòng gửi lại yêu cầu"));

        if (resetToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Hết thời gian khôi phục, vui lòng gửi lại yêu cầu");
        }

        User user = userRepository.findByEmail(resetToken.getEmail()).get();
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        forgotPasswordToken.delete(resetToken);
        return ResponseEntity.ok("Đổi mật khẩu thành công");
    }
}
