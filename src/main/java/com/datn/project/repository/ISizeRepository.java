package com.datn.project.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.datn.project.entity.Size;

public interface ISizeRepository extends JpaRepository<Size, Integer> {

}
