package com.datn.project.service;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.datn.project.dto.adress.AddressRequest;
import com.datn.project.dto.adress.AddressResponse;
import com.datn.project.entity.Address;
import com.datn.project.entity.User;
import com.datn.project.repository.IAddressRepository;
import com.datn.project.repository.IUserRepository;

@Service
public class AddressService implements IAddressService {

    @Autowired
    private IUserRepository userRepository;

    @Autowired
    private IAddressRepository addressRepository;

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Vui lòng đăng nhập"));
        return user;
    }

    @Override
    public ResponseEntity<?> addAddress(AddressRequest request) {

        User user = getCurrentUser();

        // Nếu isPrimary thì bỏ primary cũ
        if (request.isPrimary()) {
            addressRepository.clearPrimary(user);
        }

        Address address = new Address();
        address.setUser(user);
        address.setAddress(request.getAddress());
        address.setReceiverName(request.getReceiverName());
        address.setReceiverPhone(request.getReceiverPhone());
        address.setPrimary(request.isPrimary());

        addressRepository.save(address);
        return ResponseEntity.ok(Map.of("message", "Thêm địa chỉ thành công"));
    }

    @Override
    public ResponseEntity<?> deleteAddress(int id) {
        User user = getCurrentUser();
        Address address = addressRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy địa chỉ"));
        if (address.getUser().getId() != user.getId()) {
            return ResponseEntity.status(403).body(Map.of("message", "Không có quyền"));
        }
        addressRepository.delete(address);
        return ResponseEntity.ok(Map.of("message", "Xoá thành công"));
    }

    @Override
    public ResponseEntity<?> getAddresses() {
        User user = getCurrentUser();
        List<AddressResponse> list = addressRepository.findByUserOrderByIsPrimaryDesc(user)
                .stream()
                .map(a -> new AddressResponse(a.getId(),a.getAddress(),a.getReceiverName(),a.getReceiverPhone(),a.isPrimary()))
                .toList();
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<?> setPrimary(int id) {
        User user = getCurrentUser();
        addressRepository.clearPrimary(user);
        Address address = addressRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy địa chỉ"));
        address.setPrimary(true);
        addressRepository.save(address);
        return ResponseEntity.ok(Map.of("message", "Cập nhật thành công"));
    }
}
