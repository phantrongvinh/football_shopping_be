package com.datn.project.service;

import org.springframework.http.ResponseEntity;

import com.datn.project.dto.user.UserFilterDTO;

public interface IUserService {
    
    ResponseEntity<?> getAllUser(UserFilterDTO filterDTO, int page, int size);

    ResponseEntity<?> updateActiveUser(int id);

    ResponseEntity<?> getUserById(int id);
}
