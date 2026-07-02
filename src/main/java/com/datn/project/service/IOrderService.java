package com.datn.project.service;

import org.apache.coyote.BadRequestException;
import org.springframework.http.ResponseEntity;

import com.datn.project.dto.order.OrderFilterDTO;
import com.datn.project.dto.order.OrderRequest;
import com.datn.project.dto.order.OrderResponse;
import com.datn.project.entity.Order;
import com.datn.project.entity.OrderStatus;

public interface IOrderService {
    ResponseEntity<?> placeOrder(Integer userId, OrderRequest request) throws BadRequestException;

    OrderResponse mapToResponse(Order order);

    Order findById(Integer orderId);

    void confirmPayment(Integer orderId, String transactionId);

    ResponseEntity<?> getOrdersByIdByUser(int id, int userid);

    ResponseEntity<?> getAllMyOrder(int userId);

    void cancelOrder(Integer orderId);

    void cancelOrderByUser(Integer orderId, Integer userId);

    void updateOrderStatus(Integer orderId, OrderStatus newStatus);

    void validateStatusTransition(OrderStatus current, OrderStatus next);

    ResponseEntity<?> getAllOrders(int page, int size, OrderFilterDTO filter);
}
