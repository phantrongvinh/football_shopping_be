package com.datn.project.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.datn.project.entity.PaymentMethod;

public interface IPaymentMethodRepository extends JpaRepository<PaymentMethod, Integer> {

}
