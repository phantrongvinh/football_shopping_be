package com.datn.project.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.datn.project.entity.Role;

public interface IRoleRepository extends JpaRepository<Role, Integer> {

    Optional<Role> findByName(String name);
}
