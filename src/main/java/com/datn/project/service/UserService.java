package com.datn.project.service;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.datn.project.dto.adress.AddressResponse;
import com.datn.project.dto.user.RoleResponse;
import com.datn.project.dto.user.UserDetailResponse;
import com.datn.project.dto.user.UserFilterDTO;
import com.datn.project.dto.user.UserResponse;
import com.datn.project.entity.Role;
import com.datn.project.entity.User;
import com.datn.project.repository.IUserRepository;
import com.datn.project.specification.UserSpecification;

@Service
public class UserService implements IUserService {

    @Autowired
    private IUserRepository userRepository;

    private UserResponse toUserResponse(User u) {
        UserResponse response = new UserResponse();
        response.setId(u.getId());
        response.setFullName(u.getFullName());
        response.setEmail(u.getEmail());
        response.setPhone(u.getPhone());
        response.setBirthDay(u.getBirthDay());
        response.setActive(u.isActived());
        response.setCreatedAt(u.getCreatedAt());
        List<Role> roles = u.getRoles();

        List<RoleResponse> roleResponses = roles.stream().map(r -> {
            RoleResponse res = new RoleResponse(r.getName());
            return res;
        }).toList();

        response.setRole(roleResponses);
        return response;
    }

    // lấy tất cả user cho admin quản lý
    @Override
    public ResponseEntity<?> getAllUser(UserFilterDTO filterDTO, int page, int size) {

        Sort sort = switch (filterDTO.getSortBy() == null ? "" : filterDTO.getSortBy()) {
            case "newest" -> Sort.by("createdAt").descending();
            default -> Sort.by("createdAt").descending();
        };

        Pageable pageable = PageRequest.of(page - 1, size, sort);

        Page<User> users = userRepository.findAll(UserSpecification.adminFilter(filterDTO), pageable);

        if (users.isEmpty()) {
            return ResponseEntity.ok(Map.of("message", "Không tìm thấy khách hàng"));
        }
        return ResponseEntity.ok(users.stream().map(this::toUserResponse).toList());
    }

    @Override
    public ResponseEntity<?> getUserById(int id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        UserDetailResponse response = new UserDetailResponse();
        response.setId(user.getId());
        response.setFullName(user.getFullName());
        response.setEmail(user.getEmail());
        response.setPhone(user.getPhone());
        response.setBirthDay(user.getBirthDay());
        response.setActived(user.isActived());
        response.setCreatedAt(user.getCreatedAt());
        response.setAuthProvider(user.getAuthProvider());

        response.setAddresses(user.getAddresses().stream()
                .map(a -> new AddressResponse(a.getId(), a.getAddress(), a.getReceiverName(), a.getReceiverPhone(),
                        a.isPrimary()))
                .toList());

        response.setTotalOrders(userRepository.countOrdersByUserId(id));
        response.setTotalSpent(userRepository.sumFinalPriceByUserId(id));

        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<?> updateActiveUser(int id) {
        // TODO Auto-generated method stub
        return null;
    }
}
