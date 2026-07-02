package com.datn.project.controller;

import java.time.LocalDateTime;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.datn.project.entity.Order;
import com.datn.project.entity.OrderStatus;
import com.datn.project.entity.PaymentStatus;
import com.datn.project.service.GHNService;
import com.datn.project.service.ICartService;
import com.datn.project.service.IOrderService;
import com.datn.project.service.VNPayService;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping(value = "/api/v1/payment")
public class PaymentController {

    @Autowired
    private VNPayService vnPayService;

    @Autowired
    private GHNService ghnService;

    @Autowired
    private IOrderService orderService;

    @Autowired
    private ICartService cartService;

    // Tạo payment URL sau khi order đã được tạo
    @GetMapping("/vnpay/{orderId}")
    public ResponseEntity<?> vnpayPayment(
            @PathVariable Integer orderId,
            HttpServletRequest request) throws Exception {
        Order order = orderService.findById(orderId);
        String paymentUrl = vnPayService.createPaymentUrl(
                orderId,
                order.getFinalPrice(),
                request.getRemoteAddr());
        return ResponseEntity.ok(Map.of("paymentUrl", paymentUrl));
    }

    // @GetMapping("/momo/{orderId}")
    // public ResponseEntity<?> momoPayment(@PathVariable Integer orderId) throws
    // Exception {
    // Order order = orderService.findById(orderId);
    // String paymentUrl = moMoService.createPaymentUrl(orderId,
    // order.getFinalPrice());
    // return ResponseEntity.ok(Map.of("paymentUrl", paymentUrl));
    // }

    // VNPay callback (redirect từ VNPay về)
    @GetMapping("/vnpay/callback")
    public ResponseEntity<?> vnpayCallback(@RequestParam Map<String, String> params) throws Exception {

        boolean isValid = vnPayService.verifyCallback(params);
        String responseCode = params.get("vnp_ResponseCode");
        String txnRef = params.get("vnp_TxnRef");
        Integer orderId = Integer.parseInt(txnRef.split("_")[0]);

        if (!isValid) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Chữ ký không hợp lệ"));
        }

        if ("00".equals(responseCode)) {
            Order order = orderService.findById(orderId);
            System.out.println("=== order status: " + order.getStatus());
            System.out.println("=== orderDetails size: " + order.getOrderDetails().size());

            if (order.getPaymentStatus() == PaymentStatus.PAID) {
                return ResponseEntity.ok(Map.of("message", "Thanh toán thành công"));
            }

            orderService.confirmPayment(orderId, params.get("vnp_TransactionNo"));

            System.out.println("=== calling GHN createShipment...");
            ghnService.createShipment(orderId);
            System.out.println("=== GHN done");

            Order confirmedOrder = orderService.findById(orderId);
            cartService.clearCart(confirmedOrder.getUser().getId());

            return ResponseEntity.ok(Map.of("message", "Thanh toán thành công"));

        } else {
            // ─── Thanh toán thất bại / User thoát ────────────
            Order order = orderService.findById(orderId);

            // Chỉ cancel nếu chưa được xử lý
            if (order.getStatus() == OrderStatus.PENDING) {
                orderService.cancelOrder(orderId);
            }

            return ResponseEntity.ok(Map.of("message", "Thanh toán thất bại"));
            // Trả 200 thay vì 400 để FE nhận được response

            // return ResponseEntity.ok(
            // Map.of("message", "Thanh toán chưa hoàn tất"));
        }
    }

    @GetMapping("/vnpay/repay/{orderId}")
    public ResponseEntity<?> repayVNPay(
            @PathVariable Integer orderId,
            HttpServletRequest request) throws Exception {
        Order order = orderService.findById(orderId);

        if (order.getCreatedAt().isBefore(LocalDateTime.now().minusMinutes(30))) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Đơn hàng đã hết thời gian thanh toán"));
        }

        if (order.getStatus() != OrderStatus.PENDING) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Đơn hàng không ở trạng thái chờ thanh toán"));
        }

        // if (order.getPaymentStatus() != PaymentStatus.UNPAID) {
        // return ResponseEntity.badRequest()
        // .body(Map.of("message", "Đơn hàng đã được thanh toán"));
        // }

        if (!order.getPaymentMethod().getName().equals("VNPAY"))
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Đơn hàng không thanh toán qua VNPay"));

        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isBlank()) {
            ip = request.getRemoteAddr();
        }

        String paymentUrl = vnPayService.createPaymentUrl(
                order.getId(),
                order.getFinalPrice(),
                ip);

        return ResponseEntity.ok(Map.of(
                "paymentUrl", paymentUrl));
    }

}
