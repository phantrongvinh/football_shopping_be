package com.datn.project.service;

import org.springframework.http.ResponseEntity;

import com.datn.project.dto.cart.CartItemRequest;
import com.datn.project.dto.cart.MergeCartRequest;
import com.datn.project.entity.Cart;

public interface ICartService {

    Cart getOrCreateCart(int userId);

    ResponseEntity<?> addItem(int userId, CartItemRequest request );

    ResponseEntity<?> mergeCart(int userId, MergeCartRequest request);

    ResponseEntity<?> getCart(int userId);

    ResponseEntity<?> removeItem(int userId, int variantId);

    ResponseEntity<?> updateQuantity(int userId,int variantId, int quantity);

    void clearCart(int userId);
    
} 
