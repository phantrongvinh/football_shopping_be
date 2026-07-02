package com.datn.project.controller;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.datn.project.dto.PromotionRequest;
import com.datn.project.dto.TimePromotionRequest;
import com.datn.project.dto.order.OrderFilterDTO;
import com.datn.project.dto.product.AddPromotionToProductsRequest;
import com.datn.project.dto.product.ProductFilterDTO;
import com.datn.project.dto.product.ProductRequest;
import com.datn.project.dto.user.UserFilterDTO;
import com.datn.project.entity.OrderStatus;
import com.datn.project.entity.PaymentStatus;
import com.datn.project.repository.ITimePromotionRepository;
import com.datn.project.service.IOrderService;
import com.datn.project.service.IProductService;
import com.datn.project.service.IPromotionService;
import com.datn.project.service.ITimePromotionService;
import com.datn.project.service.IUserService;

import tools.jackson.databind.ObjectMapper;

@RestController
@RequestMapping(value = "/api/v1/admin")
public class AdminController {

    @Autowired
    private IProductService productService;

    @Autowired
    private IPromotionService promotionService;

    @Autowired
    private IUserService userService;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ITimePromotionService timePromotionService;

    @Autowired
    private ITimePromotionRepository timePromotionRepository;

    @Autowired
    private IOrderService orderService;

    // phần products
    @GetMapping("/products/top5")
    public ResponseEntity<?> getTop5Product() {
        return ResponseEntity.ok(productService.getTop5Product()).getBody();
    }

    @GetMapping("/products/all")
    public ResponseEntity<?> getAllProducts(@RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size, @RequestParam(required = false) List<Integer> audienceIds,
            @RequestParam(required = false) List<Integer> brandIds,
            @RequestParam(required = false) List<Integer> categoryIds, @RequestParam(required = false) String search,
            @RequestParam(required = false) Boolean onSale,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) String sortBy) {
        ProductFilterDTO filter = new ProductFilterDTO(audienceIds, brandIds, categoryIds, search, onSale, minPrice,
                maxPrice, sortBy);

        System.out.println(filter);
        return ResponseEntity.ok(productService.getAllProducts(page, size, filter)).getBody();
    }

    @PostMapping(value = "/products", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> createProduct(
            @RequestParam("data") String data,
            @RequestPart(value = "images", required = false) List<MultipartFile> images) {
        try {
            ProductRequest request = objectMapper.readValue(data, ProductRequest.class);
            return productService.createProduct(request, images);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Invalid data: " + e.getMessage());
        }
    }

    @PutMapping(value = "/products/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> updateProduct(
            @PathVariable Integer id,
            @RequestParam("data") String data,
            @RequestPart(value = "images", required = false) List<MultipartFile> images) {
        try {
            ProductRequest request = objectMapper.readValue(data, ProductRequest.class);
            return productService.updateProduct(id, request, images);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Invalid data: " + e.getMessage());
        }
    }

    @DeleteMapping("/products/{id}")
    public ResponseEntity<?> deactivateProduct(@PathVariable int id) {
        return ResponseEntity.ok(productService.deactivateProduct(id)).getBody();
    }

    // phần khách hàng
    @GetMapping("/users")
    public ResponseEntity<?> getAllUsers(@RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) LocalDate birthDayFrom,
            @RequestParam(required = false) LocalDate birthDayTo,
            @RequestParam(required = false) LocalDateTime createdAtFrom,
            @RequestParam(required = false) LocalDateTime createdAtTo,
            @RequestParam(required = false) String sortBy) {

        UserFilterDTO filterDTO = new UserFilterDTO(search, birthDayFrom, birthDayTo, createdAtFrom, createdAtTo,
                sortBy);

        return ResponseEntity.ok(userService.getAllUser(filterDTO, page, size)).getBody();
    }

    @GetMapping("/users/{id}")
    public ResponseEntity<?> getUserById(@PathVariable(value = "id") int id) {
        return ResponseEntity.ok(userService.getUserById(id)).getBody();
    }

    // phần khuyến mãi
    @PostMapping("/promotions")
    public ResponseEntity<?> createPromotion(@RequestBody PromotionRequest req) {
        return ResponseEntity.ok(promotionService.createPromotion(req));
    }

    @GetMapping("/promotions")
    public ResponseEntity<?> getAllActivePromotion() {
        return ResponseEntity.ok(promotionService.getAllActivePromotioEntity()).getBody();
    }

    @PostMapping("/promotions/assign")
    public ResponseEntity<?> addPromotionToProducts(
            @RequestBody AddPromotionToProductsRequest request) {
        productService.addPromotionToProducts(request);
        return ResponseEntity.ok("Áp dụng khuyến mãi thành công");
    }

    @GetMapping("/promotions/all")
    public ResponseEntity<?> getAllPromotion(@RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(promotionService.getAllPromotion(page, size)).getBody();
    }

    // time promotion
    @GetMapping("/time-promotions/all")
    public ResponseEntity<?> getAllTimePromotion(@RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(timePromotionService.getAllTimePromotion(page, size)).getBody();
    }

    @PutMapping("/time-promotions/{id}")
    public ResponseEntity<?> update(
            @PathVariable Integer id,
            @RequestBody TimePromotionRequest request) {
        return ResponseEntity.ok(timePromotionService.updateTimePromotion(id, request)).getBody();
    }

    @PostMapping("/time-promotions")
    public ResponseEntity<?> createTimePromotion(@RequestBody TimePromotionRequest request) {
        return ResponseEntity.ok(timePromotionService.createTimePromotion(request)).getBody();
    }

    @PatchMapping("/time-promotions/{id}")
    public ResponseEntity<?> toggle(@PathVariable int id) {
        return ResponseEntity.ok(timePromotionService.toggleActive(id)).getBody();
    }

    @DeleteMapping("/time-promotions/{id}")
    public ResponseEntity<?> delete(@PathVariable int id) {
        timePromotionRepository.deleteById(id);
        return ResponseEntity.ok("Xóa thành công");
    }

    // phần order
    @PatchMapping("/orders/{orderId}/status")
    public ResponseEntity<?> updateStatus(
            @PathVariable Integer orderId,
            @RequestParam String status) {
        OrderStatus orderStatus = status !=null? OrderStatus.valueOf(status.toUpperCase()): null;

        orderService.updateOrderStatus(orderId, orderStatus);
        return ResponseEntity.ok("Cập nhật trạng thái thành công");
    }

    @GetMapping("/orders")
    public ResponseEntity<?> getAllOrders(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "5") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false) String paymentStatus,
            @RequestParam(required = false) Integer paymentMethodId,
            @RequestParam(required = false) LocalDateTime dateFrom,
            @RequestParam(required = false) LocalDateTime dateTo, @RequestParam(required = false) String sortBy) {

        PaymentStatus paymentStatusEnum = paymentStatus != null ||  paymentStatus == "" ? PaymentStatus.valueOf(paymentStatus.toUpperCase())
                : null;

        OrderFilterDTO filterDTO = new OrderFilterDTO(search, status,
                paymentStatusEnum, paymentMethodId, dateFrom, dateTo, sortBy);

        return ResponseEntity.ok(orderService.getAllOrders(page, size, filterDTO)).getBody();
    }

    // @GetMapping("/{id}")
    // public ResponseEntity<?> getOrderDetail(@PathVariable Integer id) {
    // return ResponseEntity.ok(orderService.getOrderDetail(id));
    // }
}
