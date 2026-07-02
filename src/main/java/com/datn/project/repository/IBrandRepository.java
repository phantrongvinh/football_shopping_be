package com.datn.project.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.datn.project.entity.Brand;

public interface IBrandRepository extends JpaRepository<Brand, Integer> {

}
