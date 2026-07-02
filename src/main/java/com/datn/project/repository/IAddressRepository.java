package com.datn.project.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.datn.project.entity.Address;
import com.datn.project.entity.User;

import jakarta.transaction.Transactional;

public interface IAddressRepository extends JpaRepository<Address, Integer> {
    List<Address> findByUserOrderByIsPrimaryDesc(User user);

    @Modifying
    @Transactional
    @Query("UPDATE Address a SET a.isPrimary = false WHERE a.user = :user")
    void clearPrimary(@Param("user") User user);
}
