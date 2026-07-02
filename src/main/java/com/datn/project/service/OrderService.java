package com.datn.project.service;

import java.math.BigDecimal;
import java.sql.Date;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.apache.coyote.BadRequestException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.datn.project.dto.order.MyOrderResponse;
import com.datn.project.dto.order.OrderDetailResponse;
import com.datn.project.dto.order.OrderFilterDTO;
import com.datn.project.dto.order.OrderItemRequest;
import com.datn.project.dto.order.OrderRequest;
import com.datn.project.dto.order.OrderResponse;
import com.datn.project.entity.Order;
import com.datn.project.entity.OrderDetail;
import com.datn.project.entity.OrderStatus;
import com.datn.project.entity.PaymentMethod;
import com.datn.project.entity.PaymentStatus;
import com.datn.project.entity.ProductVariant;
import com.datn.project.entity.Promotion;
import com.datn.project.entity.TimePromotion;
import com.datn.project.entity.User;
import com.datn.project.entity.Voucher;
import com.datn.project.repository.IOrderRepository;
import com.datn.project.repository.IPaymentMethodRepository;
import com.datn.project.repository.IProductVariantRepository;
import com.datn.project.repository.IUserRepository;
import com.datn.project.specification.OrderSpecification;

import jakarta.transaction.Transactional;

@Service
public class OrderService implements IOrderService {

        @Autowired
        private IOrderRepository orderRepository;

        @Autowired
        private IProductVariantRepository productVariantRepository;

        @Autowired
        private IPaymentMethodRepository paymentMethodRepository;

        @Autowired
        private IPromotionService promotionService;

        @Autowired
        private ITimePromotionService timePromotionService;

        @Autowired
        private IVoucherService voucherService;

        @Autowired
        private IUserRepository userRepository;

        @Autowired
        private ICartService cartService;

        @Autowired
        private GHNService ghnService;

        @Autowired
        private VNPayService vnPayService;

        @Transactional
        @Override
        public ResponseEntity<?> placeOrder(Integer userId, OrderRequest request) throws BadRequestException {
                // ─── Setup order ──────────────────────────────────────
                Order order = new Order();

                User user = userRepository.findById(userId)
                                .orElseThrow(() -> new RuntimeException("Khách hàng không tồn tại"));
                order.setUser(user);
                order.setShippingAddress(request.getShippingAddress());
                order.setReceiverName(request.getReceiverName());
                order.setReceiverPhone(request.getReceiverPhone());

                PaymentMethod paymentMethod = paymentMethodRepository
                                .findById(request.getPaymentMethodId())
                                .orElseThrow(() -> new RuntimeException("Phương thức thanh toán không tồn tại"));
                order.setPaymentMethod(paymentMethod);

                // ─── Validate voucher trước (chưa check minOrderValue) ───
                Voucher voucher = null;
                if (request.getVoucherCode() != null && !request.getVoucherCode().isBlank()) {
                        voucher = voucherService.validateVoucher(request.getVoucherCode(), BigDecimal.ZERO);
                }
                final Voucher finalVoucher = voucher;

                // ─── 1. Xử lý từng item, tính subtotal ───────────────
                List<OrderDetail> details = new ArrayList<>();
                BigDecimal totalPrice = BigDecimal.ZERO;

                for (OrderItemRequest req : request.getItems()) {
                        ProductVariant variant = productVariantRepository
                                        .findByIdWithProduct(req.getVariantId())
                                        .orElseThrow(() -> new RuntimeException(
                                                        "Variant không tồn tại: " + req.getVariantId()));

                        // trừ stock atomic
                        int updated = productVariantRepository.decreaseStock(variant.getId(), req.getQuantity());
                        if (updated == 0)
                                throw new RuntimeException(
                                                "Sản phẩm " + variant.getProduct().getName() + " không đủ tồn kho");

                        // apply product promotion
                        Optional<Promotion> productPromo = promotionService
                                        .getActivePromotion(variant.getProduct().getId());

                        BigDecimal unitPrice;
                        if (productPromo.isPresent() && finalVoucher != null && !finalVoucher.isStackable()) {
                                // voucher không stackable → dùng giá gốc cho sản phẩm có promotion
                                unitPrice = variant.getPrice();
                        } else {
                                unitPrice = productPromo
                                                .map(p -> promotionService.calcDiscountedPrice(variant.getPrice(), p))
                                                .orElse(variant.getPrice());
                        }

                        totalPrice = totalPrice.add(unitPrice.multiply(BigDecimal.valueOf(req.getQuantity())));

                        OrderDetail detail = new OrderDetail();
                        detail.setOrder(order);
                        detail.setProductVariant(variant);
                        detail.setQuantity(req.getQuantity());
                        detail.setProductName(variant.getProduct().getName());
                        detail.setColor(variant.getColor());
                        detail.setSizeName(variant.getSize().getName());
                        detail.setPrice(unitPrice);
                        detail.setPromotion(
                                        (finalVoucher != null && !finalVoucher.isStackable())
                                                        ? null
                                                        : productPromo.orElse(null));
                        details.add(detail);
                }

                final BigDecimal finalTotalPrice = totalPrice;

                // ─── 2. Apply voucher (validate lại với totalPrice) ──
                BigDecimal discountAmount = BigDecimal.ZERO;
                if (finalVoucher != null) {
                        // validate minOrderValue sau khi có totalPrice
                        if (finalVoucher.getMinOrderValue() != null
                                        && finalTotalPrice.compareTo(finalVoucher.getMinOrderValue()) < 0) {
                                throw new BadRequestException(
                                                "Đơn hàng tối thiểu "
                                                                + NumberFormat.getCurrencyInstance(
                                                                                new Locale("vi", "VN"))
                                                                                .format(finalVoucher.getMinOrderValue())
                                                                + " để sử dụng mã này");
                        }
                        discountAmount = voucherService.calcDiscount(finalTotalPrice, finalVoucher);
                        voucherService.incrementUsedCount(finalVoucher);
                        order.setVoucher(finalVoucher);
                }
                // ─── 3. Apply time promotion ──────────────────────────
                final BigDecimal afterVoucher = finalTotalPrice.subtract(discountAmount);
                Optional<TimePromotion> timePromo = timePromotionService.getActiveTimePromotion();
                BigDecimal timeDiscount = timePromo
                                .map(p -> timePromotionService.calcDiscount(afterVoucher, p))
                                .orElse(BigDecimal.ZERO);

                // ─── 4. Final price ───────────────────────────────────
                BigDecimal finalPrice = finalTotalPrice
                                .subtract(discountAmount)
                                .subtract(timeDiscount)
                                .max(BigDecimal.ZERO);

                // ─── 5. Save order ────────────────────────────────────
                order.setOrderDetails(details);
                order.setTotalPrice(finalTotalPrice);
                order.setDiscountAmount(discountAmount);
                order.setTimeDiscount(timeDiscount);
                order.setFinalPrice(finalPrice);
                order.setTimePromotion(timePromo.orElse(null));
                order.setStatus(OrderStatus.PENDING);
                order.setPaymentStatus(PaymentStatus.PENDING);

                Order savedOrder = orderRepository.save(order);

                // ─── 6. Xử lý theo payment method ────────────────────
                if (request.getPaymentMethodId() == 1) {
                        savedOrder.setPaymentStatus(PaymentStatus.PAID);
                        savedOrder.setStatus(OrderStatus.CONFIRMED);
                        orderRepository.save(savedOrder);
                        ghnService.createShipment(savedOrder.getId());
                        cartService.clearCart(userId);
                }

                return ResponseEntity.ok(savedOrder.getId());
        }

        @Override
        public OrderResponse mapToResponse(Order order) {
                List<OrderDetailResponse> items = order.getOrderDetails().stream()
                                .map(d -> OrderDetailResponse.builder()
                                                .productVariantId(d.getProductVariant().getId())
                                                .productName(d.getProductName())
                                                .color(d.getColor())
                                                .sizeName(d.getSizeName())
                                                .quantity(d.getQuantity())
                                                .originalPrice(d.getProductVariant().getPrice())
                                                .price(d.getPrice())
                                                .promotionName(d.getPromotion() != null ? d.getPromotion().getName()
                                                                : null)
                                                .build())
                                .toList();

                return OrderResponse.builder()
                                .id(order.getId())
                                .items(items)
                                .totalPrice(order.getTotalPrice())
                                .discountAmount(order.getDiscountAmount())
                                .timeDiscount(order.getTimeDiscount())
                                .finalPrice(order.getFinalPrice())
                                .voucherCode(order.getVoucher() != null ? order.getVoucher().getCode() : null)
                                .timePromotionName(order.getTimePromotion() != null ? order.getTimePromotion().getName()
                                                : null)
                                .status(order.getStatus().name())
                                .shippingAddress(order.getShippingAddress())
                                .receiverName(order.getReceiverName())
                                .receiverPhone(order.getReceiverPhone())
                                .createdAt(order.getCreatedAt())
                                .paymentStatus(order.getPaymentStatus().name())
                                .paymentMethod(order.getPaymentMethod().getName())
                                .trackingCode(order.getTrackingCode())
                                .transactionId(order.getPaymentTransactionId())
                                .build();
        }

        @Override
        public Order findById(Integer orderId) {
                return orderRepository.findByIdWithDetails(orderId)
                                .orElseThrow(() -> new RuntimeException("Order không tồn tại: " + orderId));
        }

        @Transactional
        public void confirmPayment(Integer orderId, String transactionId) {
                Order order = findById(orderId);

                if (order.getPaymentStatus() == PaymentStatus.PAID)
                        return;
                if (order.getStatus() == OrderStatus.CANCELLED)
                        throw new RuntimeException("Đơn hàng đã bị hủy");

                order.setPaymentStatus(PaymentStatus.PAID);
                order.setStatus(OrderStatus.CONFIRMED);
                order.setPaymentTransactionId(transactionId);
                orderRepository.save(order);
        }

        @Override
        public void cancelOrder(Integer orderId) {
                Order order = findById(orderId);

                // Chỉ cancel được khi PENDING hoặc CONFIRMED
                if (order.getStatus() == OrderStatus.CANCELLED)
                        return;
                if (order.getStatus() == OrderStatus.SHIPPING ||
                                order.getStatus() == OrderStatus.DELIVERED) {
                        throw new RuntimeException("Không thể hủy đơn hàng đang giao hoặc đã giao");
                }

                // Hoàn lại stock
                order.getOrderDetails().forEach(detail -> productVariantRepository.increaseStock(
                                detail.getProductVariant().getId(),
                                detail.getQuantity()));

                // Hoàn lại lượt dùng voucher
                if (order.getVoucher() != null) {
                        voucherService.decrementUsedCount(order.getVoucher());
                }

                order.setStatus(OrderStatus.CANCELLED);
                order.setPaymentStatus(PaymentStatus.FAILED);
                orderRepository.save(order);
        }

        @Override
        public void cancelOrderByUser(Integer orderId, Integer userId) {
                Order order = findById(orderId);

                if (order.getUser().getId() != userId)
                        throw new RuntimeException("Bạn không có quyền hủy đơn hàng này");

                // Chỉ cancel khi PENDING hoặc CONFIRMED
                if (order.getStatus() == OrderStatus.SHIPPING ||
                                order.getStatus() == OrderStatus.DELIVERED)
                        throw new RuntimeException("Không thể hủy đơn hàng đang giao hoặc đã giao");

                if (order.getStatus() == OrderStatus.CANCELLED)
                        throw new RuntimeException("Đơn hàng đã bị hủy trước đó");

                // Hoàn tiền nếu đã thanh toán VNPay
                if (order.getPaymentStatus() == PaymentStatus.PAID
                                && order.getPaymentMethod().getName().equals("VNPAY")) {
                        try {
                                // String transactionDate = new SimpleDateFormat("yyyyMMddHHmmss")
                                // .format(new Date()); // thực tế nên lưu ngày thanh toán vào DB
                                // boolean refunded = vnPayService.refund(order, transactionDate);
                                // if (!refunded)
                                // throw new RuntimeException("Hoàn tiền thất bại, vui lòng liên hệ hỗ trợ");
                                order.setPaymentStatus(PaymentStatus.REFUNDED);
                        } catch (Exception e) {
                                throw new RuntimeException("Lỗi hoàn tiền: " + e.getMessage());
                        }
                }

                // Hoàn lại stock
                order.getOrderDetails().forEach(detail -> productVariantRepository
                                .increaseStock(detail.getProductVariant().getId(), detail.getQuantity()));

                // Hoàn lại voucher
                if (order.getVoucher() != null)

                {
                        voucherService.decrementUsedCount(order.getVoucher());
                }

                order.setStatus(OrderStatus.CANCELLED);
                // if (order.getPaymentStatus() != PaymentStatus.REFUNDED) {
                // order.setPaymentStatus(PaymentStatus.FAILED);
                // }
                orderRepository.save(order);
        }

        @Override
        public ResponseEntity<?> getOrdersByIdByUser(int id, int userid) {
                Order order = orderRepository.findById(id)
                                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng"));

                if (order.getUser().getId() != userid) {
                        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                                        .body("Bạn không có quyền xem đơn hàng này");
                }

                OrderResponse response = new OrderResponse();

                response.setItems(order.getOrderDetails().stream().map(i -> {
                        OrderDetailResponse item = new OrderDetailResponse(i.getId(), i.getProductName(), i.getColor(),
                                        i.getSizeName(), i.getQuantity(), null, i.getPrice(),
                                        i.getPromotion() != null ? i.getPromotion().getName() : null);

                        return item;

                }).toList());
                response.setCreatedAt(order.getCreatedAt());
                response.setDiscountAmount(order.getDiscountAmount());
                response.setFinalPrice(order.getFinalPrice());
                response.setId(order.getId());
                response.setPaymentStatus(order.getPaymentStatus().name());
                response.setReceiverName(order.getReceiverName());
                response.setReceiverPhone(order.getReceiverPhone());
                response.setShippingAddress(order.getShippingAddress());
                response.setStatus(order.getStatus().name());
                response.setTimeDiscount(order.getTimeDiscount());
                response.setTimePromotionName(
                                order.getTimePromotion() != null ? order.getTimePromotion().getName() : null);
                response.setTotalPrice(order.getTotalPrice());
                response.setTrackingCode(order.getTrackingCode());
                response.setVoucherCode(order.getVoucher() != null ? order.getVoucher().getCode() : null);
                response.setPaymentMethod(order.getPaymentMethod().getName());
                response.setPaymentStatus(order.getPaymentStatus().name());

                return ResponseEntity.ok(response);
        }

        @Override
        public ResponseEntity<?> getAllMyOrder(int userId) {
                List<Order> orders = orderRepository.findByUserIdOrderByCreatedAtDesc(userId);

                if (orders.isEmpty()) {
                        return ResponseEntity.ok(Map.of("message", "Chưa có đơn hàng"));
                }

                List<MyOrderResponse> responses = orders.stream().map(o -> {
                        MyOrderResponse response = new MyOrderResponse();
                        response.setId(o.getId());
                        response.setCreatedAt(o.getCreatedAt());
                        response.setItems(o.getOrderDetails().stream().map(i -> {
                                OrderDetailResponse orderDetailResponse = new OrderDetailResponse(i.getId(),
                                                i.getProductName(), i.getColor(), i.getSizeName(), i.getQuantity(),
                                                null, i.getPrice(),
                                                i.getPromotion() != null ? i.getPromotion().getName() : null);
                                return orderDetailResponse;
                        }).toList());

                        response.setPaymentStatus(o.getPaymentStatus().name());
                        response.setPrice(o.getFinalPrice());
                        response.setStatus(o.getStatus().name());
                        response.setTrackingCode(o.getTrackingCode());
                        response.setPaymentMethod(o.getPaymentMethod().getName());
                        return response;
                }).toList();
                return ResponseEntity.ok(responses);
        }

        @Override
        public void updateOrderStatus(Integer orderId, OrderStatus newStatus) {
                Order order = findById(orderId);

                // Validate chuyển trạng thái hợp lệ
                validateStatusTransition(order.getStatus(), newStatus);

                order.setStatus(newStatus);
                orderRepository.save(order);
        }

        @Override
        public void validateStatusTransition(OrderStatus current, OrderStatus next) {
                Map<OrderStatus, List<OrderStatus>> allowed = Map.of(
                                OrderStatus.PENDING, List.of(OrderStatus.CONFIRMED, OrderStatus.CANCELLED),
                                OrderStatus.CONFIRMED, List.of(OrderStatus.SHIPPING, OrderStatus.CANCELLED),
                                OrderStatus.SHIPPING, List.of(OrderStatus.DELIVERED),
                                OrderStatus.DELIVERED, List.of(),
                                OrderStatus.CANCELLED, List.of());

                if (!allowed.get(current).contains(next)) {
                        throw new RuntimeException(
                                        "Không thể chuyển từ " + current + " sang " + next);
                }
        }

        // lấy tất cả đơn hàng với filter ở admin
        @Override
        public ResponseEntity<?> getAllOrders(int page, int size, OrderFilterDTO filter) {
                Sort sort = switch (filter.getSortBy() == null ? "" : filter.getSortBy()) {
                        case "oldest" -> Sort.by("createdAt").ascending();
                        case "price_asc" -> Sort.by("finalPrice").ascending();
                        case "price_desc" -> Sort.by("finalPrice").descending();
                        default -> Sort.by("createdAt").descending();
                };

                Pageable pageable = PageRequest.of(page - 1, size, sort);

                Page<Integer> orderIds = orderRepository
                                .findAll(OrderSpecification.filter(filter), pageable)
                                .map(Order::getId);
                if (orderIds.isEmpty())
                        return ResponseEntity.ok(Page.empty(pageable));

                List<Order> orders = orderIds.getContent().stream()
                                .map(id -> orderRepository.findByIdWithDetails(id).orElse(null))
                                .filter(Objects::nonNull)
                                .toList();

                List<OrderResponse> responses = orders.stream()
                                .map(this::mapToResponse)
                                .toList();
                return ResponseEntity.ok(Map.of(
                                "content", responses,
                                "hasNext", orderIds.hasNext(),
                                "page", page,
                                "size", size,
                                "totalElements", orderIds.getTotalElements(),
                                "totalPages", orderIds.getTotalPages()));
        }
}
