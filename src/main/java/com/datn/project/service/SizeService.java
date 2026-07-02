package com.datn.project.service;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.datn.project.dto.SizeResponse;
import com.datn.project.entity.Size;
import com.datn.project.repository.ISizeRepository;

@Service
public class SizeService implements ISizeService {

    @Autowired
    private ISizeRepository sizeRepository;

    @Override
    public ResponseEntity<?> getAllSize() {
        List<Size> sizes = sizeRepository.findAll();

        if(sizes.isEmpty()){
            return ResponseEntity.ok(Map.of("message","Không tìm thấy kích cỡ"));
        }

        List<SizeResponse> responses = sizes.stream().map(s ->{

            SizeResponse res = new SizeResponse();
            res.setId(s.getId());
            res.setName(s.getName());

            return res;
        }).toList();

        return ResponseEntity.ok(responses);
    }
    
}
