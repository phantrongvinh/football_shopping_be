package com.datn.project.controller;

import java.nio.file.attribute.UserPrincipal;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.datn.project.dto.cart.CartItemRequest;
import com.datn.project.dto.cart.MergeCartRequest;
import com.datn.project.entity.User;
import com.datn.project.repository.IUserRepository;
import com.datn.project.security.CustomUserDetail;
import com.datn.project.service.ICartService;

@RestController
@RequestMapping(value = "/api/v1/cart")
public class CartController {

    @Autowired
    private ICartService cartService;

    @Autowired
    private IUserRepository userRepository;

    private Integer getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        CustomUserDetail customUserDetail = (CustomUserDetail) authentication.getPrincipal();

        String email =  customUserDetail.getUsername();

        User user = userRepository.findByEmailWithRoles(email).orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản"));
        return user.getId();
    }

    @GetMapping
    public ResponseEntity<?> getCart() {
        return ResponseEntity.ok(cartService.getCart(getCurrentUserId())).getBody();
    }

    @PostMapping("/items")
    public ResponseEntity<?> addItem(@RequestBody CartItemRequest req) {
        return ResponseEntity.ok(cartService.addItem(getCurrentUserId(), req)).getBody();
    }

    @PostMapping("/merge")
    public ResponseEntity<?> mergeCart(@RequestBody MergeCartRequest req) {
        return ResponseEntity.ok(cartService.mergeCart(getCurrentUserId(), req)).getBody();
    }

    @DeleteMapping("/items/{variantId}")
    public ResponseEntity<?> removeItem(@PathVariable Integer variantId) {
        return ResponseEntity.ok(cartService.removeItem(getCurrentUserId(), variantId)).getBody();
    }

    @PatchMapping("/items/{variantId}")
    public ResponseEntity<?> updateQuantity(
            @PathVariable Integer variantId,
            @RequestParam Integer quantity) {
        return ResponseEntity.ok(cartService.updateQuantity(getCurrentUserId(), variantId, quantity)).getBody();
    }

}
