package com.datn.project.service;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.datn.project.dto.AudienceResponse;
import com.datn.project.entity.TargetAudience;
import com.datn.project.repository.ITargetAudienceRepository;

@Service
public class TargetAudienceService implements ITargetAudienceService {
    
    @Autowired
    private ITargetAudienceRepository targetAudienceRepository;

    @Override
    public ResponseEntity<?> getAllAudiences() {
        List<TargetAudience> audiences = targetAudienceRepository.findAll();

        if (audiences.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "gender not found"));
        }

        List<AudienceResponse> responses = audiences.stream().map(a -> {
            AudienceResponse response = new AudienceResponse();

            response.setId(a.getId());
            response.setName(a.getName());

            return response;
        }).toList();


        return ResponseEntity.ok(responses);
    }
}
