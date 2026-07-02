package com.datn.project.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.datn.project.entity.Cart;

public interface ICartRepository extends JpaRepository<Cart, Integer> {

    Optional<Cart> findByUserId(int userId);

    
}
