package com.datn.project.service;

import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.datn.project.entity.AuthProvider;
import com.datn.project.entity.CustomOAuth2User;
import com.datn.project.entity.Role;
import com.datn.project.entity.User;
import com.datn.project.repository.IRoleRepository;
import com.datn.project.repository.IUserRepository;

@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    @Autowired
    private IUserRepository userRepository;

    @Autowired
    private IRoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {

        OAuth2User oAuth2User = super.loadUser(userRequest);

        String email = oAuth2User.getAttribute("email");
        String name = oAuth2User.getAttribute("name");

        userRepository.findByEmailWithRoles(email).orElseGet(() -> {
            User newUser = new User();
            newUser.setEmail(email);
            newUser.setFullName(name);
            newUser.setPhone("");
            newUser.setActived(true);
            newUser.setAuthProvider(AuthProvider.GOOGLE);
            newUser.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
            Role userRole = roleRepository.findByName("USER").orElseThrow(() -> new RuntimeException("Role not found"));

            newUser.setRoles(List.of(userRole));
            return userRepository.save(newUser);
        });

        return new CustomOAuth2User(oAuth2User);
    }

}
