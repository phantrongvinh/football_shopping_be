package com.datn.project.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.datn.project.dto.cart.CartItemRequest;
import com.datn.project.dto.cart.CartItemResponse;
import com.datn.project.dto.cart.MergeCartRequest;
import com.datn.project.entity.Cart;
import com.datn.project.entity.CartItem;
import com.datn.project.entity.Product;
import com.datn.project.entity.ProductImage;
import com.datn.project.entity.ProductVariant;
import com.datn.project.entity.Promotion;
import com.datn.project.entity.User;
import com.datn.project.repository.ICartItemRepository;
import com.datn.project.repository.ICartRepository;
import com.datn.project.repository.IProductVariantRepository;
import com.datn.project.repository.IUserRepository;

import jakarta.transaction.Transactional;

@Service
public class CartService implements ICartService {

    @Autowired
    private ICartRepository cartRepository;

    @Autowired
    private ICartItemRepository cartItemRepository;

    @Autowired
    private IProductVariantRepository productVariantRepository;

    @Autowired
    private IUserRepository userRepository;

    @Autowired
    private IPromotionService promotionService;

    // Thêm item vào cart
    @Override
    public ResponseEntity<?> addItem(int userId, CartItemRequest request) {
        Cart cart = getOrCreateCart(userId);
        ProductVariant variant = productVariantRepository.findById(request.getProductVariantId())
                .orElseThrow(() -> new RuntimeException("Biến thể không tồn tại"));

        CartItem item = cartItemRepository.findByCartIdAndProductVariantId(cart.getId(), variant.getId())
                .orElseGet(() -> {
                    CartItem newItem = new CartItem();
                    newItem.setCart(cart);
                    newItem.setProductVariant(variant);
                    newItem.setQuantity(0);
                    return newItem;
                });

        item.setQuantity(item.getQuantity() + request.getQuantity());
        cartItemRepository.save(item);

        return ResponseEntity.ok("Thêm thành công");
    }

    // fetch cart theo id của user
    @Override
    public ResponseEntity<?> getCart(int userId) {
        Cart cart = getOrCreateCart(userId);
        List<CartItemResponse> cartItemResponses = cart.getCartItems().stream()
                .map(item -> {
                    ProductVariant v = item.getProductVariant();
                    Product p = v.getProduct();

                    // check promotion
                    Optional<Promotion> promo = promotionService.getActivePromotion(p.getId());
                    BigDecimal discountedPrice = promo
                            .map(pr -> promotionService.calcDiscountedPrice(v.getPrice(), pr))
                            .orElse(v.getPrice());
                    return new CartItemResponse(
                            v.getId(),
                            p.getName(),
                            v.getSku(),
                            p.getProductImages().stream()
                                    .filter(img -> img.getIsPrimary())
                                    .findFirst()
                                    .map(ProductImage::getImageUrl)
                                    .orElse(p.getProductImages().stream()
                                            .findFirst()
                                            .map(ProductImage::getImageUrl)
                                            .orElse(null)),
                            v.getColor(),
                            v.getSize().getName(),
                            item.getQuantity(),
                            discountedPrice,

                            v.getPrice());
                })
                .toList();

        return ResponseEntity.ok(cartItemResponses);
    }

    // fetch cart hoặc tạo cart nếu chưa có theo user
    @Override
    public Cart getOrCreateCart(int userId) {
        return cartRepository.findByUserId(userId).orElseGet(() -> {
            Cart newCart = new Cart();
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));
            newCart.setUser(user);
            return cartRepository.save(newCart);
        });

    }

    // Đồng bộ cart trên trang web khi đăng nhập
    @Override
    @Transactional
    public ResponseEntity<?> mergeCart(int userId, MergeCartRequest request) {
        for (CartItemRequest item : request.getItems()) {
            addItem(userId, item);
        }
        return ResponseEntity.ok("Đồng bộ giỏ hàng thành công");
    }

    // Xóa item khỏi cart
    @Override
    public ResponseEntity<?> removeItem(int userId, int variantId) {
        Cart cart = getOrCreateCart(userId);
        cartItemRepository.findByCartIdAndProductVariantId(cart.getId(), variantId)
                .ifPresent(cartItemRepository::delete);
        return ResponseEntity.ok("Xóa thành công");
    }

    // Cập nhật xóa lượng cart
    @Override
    public ResponseEntity<?> updateQuantity(int userId, int variantId, int quantity) {
        Cart cart = getOrCreateCart(userId);
        CartItem item = cartItemRepository
                .findByCartIdAndProductVariantId(cart.getId(), variantId)
                .orElseThrow(() -> new RuntimeException("Item không tồn tại trong giỏ hàng"));

        if (quantity <= 0) {
            cartItemRepository.delete(item);
            return ResponseEntity.ok("Xóa thành công");
        } else {
            item.setQuantity(quantity);
            cartItemRepository.save(item);
            return ResponseEntity.ok("Cập nhật thành công");
        }
    }

    @Override
    @Transactional
    public void clearCart(int userId) {
        Cart cart = getOrCreateCart(userId);
        cartItemRepository.deleteAllByCartId(cart.getId());
    }
}
