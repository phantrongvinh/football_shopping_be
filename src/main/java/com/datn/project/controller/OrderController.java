package com.datn.project.controller;

import org.apache.coyote.BadRequestException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.datn.project.dto.order.OrderRequest;
import com.datn.project.entity.OrderStatus;
import com.datn.project.entity.User;
import com.datn.project.repository.IUserRepository;
import com.datn.project.security.CustomUserDetail;
import com.datn.project.service.IOrderService;

import jakarta.transaction.Transactional;

@RestController
@RequestMapping(value = "/api/v1/orders")
public class OrderController {

    @Autowired
    private IOrderService orderService;
    @Autowired
    private IUserRepository userRepository;

    private Integer getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        CustomUserDetail customUserDetail = (CustomUserDetail) authentication.getPrincipal();

        String email = customUserDetail.getUsername();

        User user = userRepository.findByEmailWithRoles(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản"));
        return user.getId();
    }

    @PostMapping
    @Transactional
    public ResponseEntity<?> placeOrder(@RequestBody OrderRequest request) throws BadRequestException {
        return ResponseEntity.ok(orderService.placeOrder(getCurrentUserId(), request)).getBody();
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getMyOrders(@PathVariable(value = "id") int id) {
        return ResponseEntity.ok(orderService.getOrdersByIdByUser(id, getCurrentUserId())).getBody();
    }

    @GetMapping()
    public ResponseEntity<?> getAllMyOrders() {
        return ResponseEntity.ok(orderService.getAllMyOrder(getCurrentUserId())).getBody();
    }

    @PatchMapping("/{orderId}/cancel")
    public ResponseEntity<?> cancelOrder(@PathVariable Integer orderId) {
        orderService.cancelOrderByUser(orderId, getCurrentUserId());
        return ResponseEntity.ok("Hủy đơn hàng thành công");
    }

}
