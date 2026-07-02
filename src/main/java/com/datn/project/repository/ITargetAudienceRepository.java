package com.datn.project.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.datn.project.entity.TargetAudience;

public interface ITargetAudienceRepository extends JpaRepository<TargetAudience, Integer> {

}
