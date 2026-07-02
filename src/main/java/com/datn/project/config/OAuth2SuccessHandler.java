package com.datn.project.config;

import java.io.IOException;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import com.datn.project.entity.CustomOAuth2User;
import com.datn.project.entity.User;
import com.datn.project.repository.IUserRepository;
import com.datn.project.service.JwtService;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    @Autowired
    private JwtService jwtService;

    @Autowired
    private IUserRepository userRepository;

    private static final String FRONTEND_URL = "http://localhost:5173/oauth2/callback";

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
            Authentication authentication) throws IOException, ServletException {
        CustomOAuth2User oAuth2User = (CustomOAuth2User) authentication.getPrincipal();

        User user = userRepository.findByEmailWithRoles(oAuth2User.getEmail())
                .orElseThrow(() -> new RuntimeException("Email khong ton tai"));

        List<GrantedAuthority> roles = user.getRoles().stream()
                .map(r -> (GrantedAuthority) new SimpleGrantedAuthority("ROLE_" + r.getName()))
                .toList();

        String token = jwtService.generateToken(user.getEmail(), roles);

        String redirectUrl = FRONTEND_URL + "?token=" + token;
        getRedirectStrategy().sendRedirect(request, response, redirectUrl);
    }

}
