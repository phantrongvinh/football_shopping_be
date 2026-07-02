package com.datn.project.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.datn.project.service.ITargetAudienceService;

@RestController
@RequestMapping(value = "/api/v1/audiences")
public class TargetAudienceController {
    

    @Autowired
    private ITargetAudienceService targetAudienceService;

    @GetMapping()
    public ResponseEntity<?> getAllAudiences() {
        return ResponseEntity.ok(targetAudienceService.getAllAudiences()).getBody();
    }
}
