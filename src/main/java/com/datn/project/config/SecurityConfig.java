package com.datn.project.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.datn.project.service.CustomOAuth2UserService;
import com.datn.project.service.CustomUserDetailService;

import jakarta.servlet.http.HttpServletResponse;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private JwtFilter jwtFilter;

    @Autowired
    private CustomUserDetailService customUserDetailService;

    @Autowired
    private CustomOAuth2UserService customOAuth2UserService;

    @Autowired
    private OAuth2SuccessHandler oAuth2SuccessHandler;

    @Autowired
    private PasswordEncoderConfig passwordEncoderConfig;

    @Bean
    AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) {
        return configuration.getAuthenticationManager();
    }

    @Bean
    AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(customUserDetailService);
        provider.setPasswordEncoder(passwordEncoderConfig.passwordEncoder());
        return provider;
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity httpSecurity) {
        return httpSecurity.cors(Customizer.withDefaults()).csrf(csrf -> csrf.disable())
                .exceptionHandling(ex -> ex
                        // Xử lý 401 - chưa đăng nhập
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.setContentType("application/json");
                            response.getWriter().write("""
                                        {
                                            "status": 401,
                                            "error": "Unauthorized",
                                            "message": "Please login to continue"
                                        }
                                    """);
                        })
                        // Xử lý 403 - không có quyền
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                            response.setContentType("application/json");
                            response.getWriter().write("""
                                        {
                                            "status": 403,
                                            "error": "Forbidden",
                                            "message": "You don't have permission"
                                        }
                                    """);
                        }))
                .authorizeHttpRequests(auth -> {
                    auth
                            // ─── Auth ────────────────────────────────────────
                            .requestMatchers("/api/v1/auth/me").authenticated()
                            .requestMatchers("/api/v1/auth/logout").authenticated()
                            .requestMatchers("/api/v1/auth/**").permitAll()

                            // ─── Public ──────────────────────────────────────
                            .requestMatchers(HttpMethod.GET, "/api/v1/products/**").permitAll()
                            .requestMatchers(HttpMethod.GET, "/api/v1/promotions/**").permitAll()

                            // ─── Admin ───────────────────────────────────────
                            .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")

                            // ─── User (đã login) ─────────────────────────────
                            .requestMatchers("/api/v1/cart/**").authenticated()
                            .requestMatchers("/api/v1/orders/**").authenticated()

                            // ─── Promotion/ Voucher ──────────────────────────
                            .requestMatchers("/api/v1/vouchers/**").authenticated()

                            // ─── Payment ─────────────────────────────────────
                            .requestMatchers("/api/v1/payment/**").permitAll()

                            .anyRequest().permitAll();
                })
                .oauth2Login(oauth2 -> oauth2
                        .userInfoEndpoint(userInfo -> userInfo.userService(customOAuth2UserService))
                        .successHandler(oAuth2SuccessHandler)
                        .failureUrl("/api/auth/oauth2/failure"))

                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
                .authenticationProvider(authenticationProvider())
                .build();
    }
}
