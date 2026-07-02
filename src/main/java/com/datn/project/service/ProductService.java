package com.datn.project.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.datn.project.dto.PromotionResponse;
import com.datn.project.dto.product.AddPromotionToProductsRequest;
import com.datn.project.dto.product.ProductDetailDTO;
import com.datn.project.dto.product.ProductFilterDTO;
import com.datn.project.dto.product.ProductHomeView;
import com.datn.project.dto.product.ProductImageDTO;
import com.datn.project.dto.product.ProductImageRequest;
import com.datn.project.dto.product.ProductImagesResponse;
import com.datn.project.dto.product.ProductOverview;
import com.datn.project.dto.product.ProductRequest;
import com.datn.project.dto.product.ProductResponse;
import com.datn.project.dto.product.ProductVariantDTO;
import com.datn.project.dto.product.ProductVariantRequest;
import com.datn.project.dto.product.ProductVariantResponse;
import com.datn.project.entity.Product;
import com.datn.project.entity.ProductImage;
import com.datn.project.entity.ProductVariant;
import com.datn.project.entity.Promotion;
import com.datn.project.entity.Size;
import com.datn.project.repository.IBrandRepository;
import com.datn.project.repository.ICategoryRepository;
import com.datn.project.repository.IProductImageRepository;
import com.datn.project.repository.IProductRepository;
import com.datn.project.repository.IProductVariantRepository;
import com.datn.project.repository.IPromotionRepository;
import com.datn.project.repository.ISizeRepository;
import com.datn.project.repository.ITargetAudienceRepository;
import com.datn.project.specification.ProductSpecification;

import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;

@Service
public class ProductService implements IProductService {

        @Autowired
        private IProductRepository productRepository;

        @Autowired
        private IBrandRepository brandRepository;

        @Autowired
        private ICategoryRepository categoryRepository;

        @Autowired
        private IPromotionService promotionService;

        @Autowired
        private IProductVariantRepository productVariantRepository;

        @Autowired
        private IProductImageRepository productImageRepository;

        @Autowired
        private ITargetAudienceRepository targetAudienceRepository;

        @Autowired
        private ISizeRepository sizeRepository;

        @Autowired
        private CloudinaryService cloudinaryService;

        @Autowired
        private IPromotionRepository promotionRepository;

        // config để lấy product và giảm giá tốt nhất
        private Optional<Promotion> getBestPromotion(Product product, BigDecimal price) {

                LocalDateTime now = LocalDateTime.now();

                return product.getPromotions()
                                .stream()
                                .filter(pr -> !pr.getStartAt().isAfter(now)
                                                && !pr.getEndAt().isBefore(now))
                                .min(Comparator.comparing(
                                                pr -> promotionService.calcDiscountedPrice(price, pr)));
        }

        private PromotionResponse toPromotionResponse(Promotion promotion) {

                PromotionResponse response = new PromotionResponse();

                response.setId(promotion.getId());
                response.setName(promotion.getName());
                response.setDiscountType(
                                promotion.getDiscountType().name().toLowerCase());
                response.setDiscountValue(promotion.getDiscountValue());
                response.setStartAt(promotion.getStartAt());
                response.setEndAt(promotion.getEndAt());

                return response;
        }

        private ProductOverview toOverview(Product p) {
                ProductOverview overview = new ProductOverview();

                overview.setId(p.getId());
                overview.setName(p.getName());
                overview.setCategory(p.getCategory().getName());
                overview.setBrand(p.getBrand().getName());
                overview.setBasePrice(p.getBasePrice());
                overview.setStatus(p.getDeletedAt() != null);
                overview.setUpdatedAt(p.getUpdatedAt());
                overview.setVariantCount(p.getProductVariants().size());

                overview.setStock(
                                p.getProductVariants()
                                                .stream()
                                                .mapToInt(ProductVariant::getStock)
                                                .sum());

                return overview;
        }

        // Lấy tất cả product theo filter và không bị vô hiệu hóa
        @Override
        public ResponseEntity<?> getFilterProducts(ProductFilterDTO filterDTO, int page, int size) {
                Sort sort = switch (filterDTO.getSortBy() == null ? "" : filterDTO.getSortBy()) {
                        case "price-asc" -> Sort.by("basePrice").ascending();
                        case "price-desc" -> Sort.by("basePrice").descending();
                        case "newest" -> Sort.by("createdAt").descending();
                        case "oldest" -> Sort.by("createdAt").ascending();
                        default -> Sort.by("createdAt").descending();
                };

                Pageable pageable = PageRequest.of(page, size, sort);

                // ─── 1. Query ids ─────────────────────────────────────────
                Page<Integer> productIds = productRepository
                                .findAll(ProductSpecification.filter(filterDTO), pageable)
                                .map(Product::getId);

                if (productIds.isEmpty()) {
                        return ResponseEntity.ok(Map.of("message", "Không tìm thấy sản phẩm"));
                }

                List<Integer> ids = productIds.getContent();

                // ─── 2. Fetch batch images và variants ───────────────────
                List<Product> withImages = productRepository.findAllWithImagesByIds(ids);
                List<Product> withVariants = productRepository.findAllWithVariantsByIds(ids);

                // ─── 3. Merge variants vào product có images ─────────────
                Map<Integer, List<ProductVariant>> variantMap = withVariants.stream()
                                .collect(Collectors.toMap(
                                                Product::getId,
                                                p -> new ArrayList<>(p.getProductVariants())));

                withImages.forEach(p -> p.setProductVariants(variantMap.getOrDefault(p.getId(), new ArrayList<>())));

                // ─── 4. Giữ đúng thứ tự theo ids ────────────────────────
                Map<Integer, Product> productMap = withImages.stream()
                                .collect(Collectors.toMap(Product::getId, p -> p));

                List<Product> products = ids.stream()
                                .map(productMap::get)
                                .filter(Objects::nonNull)
                                .toList();

                // ─── 5. Map sang response ─────────────────────────────────
                List<ProductHomeView> responseList = products.stream()
                                .map(product -> {
                                        ProductHomeView res = new ProductHomeView();

                                        res.setId(product.getId());
                                        res.setName(product.getName());
                                        res.setCategoryName(product.getCategory().getName());
                                        res.setBrandName(product.getBrand().getName());

                                        res.setImage(product.getProductImages().stream()
                                                        .filter(pi -> Boolean.TRUE.equals(pi.getIsPrimary()))
                                                        .findFirst()
                                                        .map(ProductImage::getImageUrl)
                                                        .orElse(product.getProductImages().stream()
                                                                        .findFirst()
                                                                        .map(ProductImage::getImageUrl)
                                                                        .orElse(null)));

                                        BigDecimal minPrice = product.getProductVariants().stream()
                                                        .map(ProductVariant::getPrice)
                                                        .min(BigDecimal::compareTo)
                                                        .orElse(product.getBasePrice());

                                        Optional<Promotion> promo = getBestPromotion(product, minPrice);
                                        BigDecimal discountedMinPrice = promo
                                                        .map(pr -> promotionService.calcDiscountedPrice(minPrice, pr))
                                                        .orElse(minPrice);

                                        res.setPrice(minPrice);
                                        res.setDiscountPrice(discountedMinPrice);
                                        res.setPromotion(promo.map(this::toPromotionResponse).orElse(null));

                                        return res;
                                })
                                .toList();

                return ResponseEntity.ok(Map.of(
                                "content", responseList,
                                "hasNext", productIds.hasNext(),
                                "page", page,
                                "size", size,
                                "totalElements", productIds.getTotalElements(),
                                "totalPages", productIds.getTotalPages()));
        }

        // lấy 5 product mới nhất để thống kê
        @Override
        public ResponseEntity<?> getTop5Product() {
                return ResponseEntity.ok(
                                productRepository
                                                .findTop5ByDeletedAtIsNullOrderByCreatedAtDesc()
                                                .stream()
                                                .map(this::toOverview)
                                                .toList());
        }

        // Lấy tất cả product có filter và page để quản lý ở admin
        @Override
        public ResponseEntity<?> getAllProducts(int page, int size, ProductFilterDTO filterDTO) {
                Sort sort = switch (filterDTO.getSortBy() == null ? "" : filterDTO.getSortBy()) {
                        case "price_asc" -> Sort.by("basePrice").ascending();
                        case "price_desc" -> Sort.by("basePrice").descending();
                        case "newest" -> Sort.by("createdAt").descending();
                        default -> Sort.by("createdAt").descending();
                };

                Pageable pageable = PageRequest.of(page - 1, size, sort);
                Page<Product> products = productRepository.findAll(
                                ProductSpecification.adminFilter(filterDTO), pageable);

                if (products.isEmpty()) {
                        return ResponseEntity.ok(Map.of("message", "Không tìm thấy sản phẩm"));
                }

                Page<ProductResponse> responses = products.map(p -> {
                        ProductResponse res = new ProductResponse();

                        // ─── Thông tin cơ bản ────────────────────────────
                        res.setId(p.getId());
                        res.setName(p.getName());
                        res.setDescription(p.getDescription());
                        res.setBasePrice(p.getBasePrice());
                        res.setCreatedAt(p.getCreatedAt());
                        res.setCategory(p.getCategory().getName());
                        res.setCategoryId(p.getCategory().getId());
                        res.setBrand(p.getBrand().getName());
                        res.setBrandId(p.getBrand().getId());
                        res.setTargetAudience(p.getTargetAudience().getName());
                        res.setTargetAudienceId(p.getTargetAudience().getId());
                        res.setAccessory(p.getCategory().isAccessory());
                        res.setDeletedAt(p.getDeletedAt());

                        // ─── Ảnh primary ─────────────────────────────────
                        res.setImgs(p.getProductImages().stream().map(pi -> {
                                ProductImagesResponse productImagesResponse = new ProductImagesResponse();

                                productImagesResponse.setId(pi.getId());
                                productImagesResponse.setImageUrl(pi.getImageUrl());
                                productImagesResponse.setPrimary(pi.getIsPrimary());

                                return productImagesResponse;
                        }).toList());

                        // ─── Min / Max price từ variants ─────────────────
                        List<BigDecimal> prices = p.getProductVariants().stream()
                                        .map(ProductVariant::getPrice)
                                        .toList();

                        BigDecimal minPrice = prices.isEmpty()
                                        ? p.getBasePrice()
                                        : Collections.min(prices);

                        BigDecimal maxPrice = prices.isEmpty()
                                        ? p.getBasePrice()
                                        : Collections.max(prices);

                        // ─── Promotion ───────────────────────────────────
                        Optional<Promotion> promo = getBestPromotion(p, minPrice);
                        Promotion activePromo = promo.orElse(null);

                        res.setMinPrice(minPrice);
                        res.setMaxPrice(maxPrice);
                        res.setDiscountedPrice(promo
                                        .map(pr -> promotionService.calcDiscountedPrice(minPrice, pr))
                                        .orElse(minPrice));
                        res.setPromotion(promo.map(this::toPromotionResponse).orElse(null));

                        // ─── Variants ────────────────────────────────────
                        List<ProductVariantResponse> variantResponses = p.getProductVariants().stream()
                                        .map(v -> {
                                                ProductVariantResponse vRes = new ProductVariantResponse();
                                                vRes.setId(v.getId());
                                                vRes.setColor(v.getColor());
                                                vRes.setSize(v.getSize().getName());
                                                vRes.setSizeId(v.getSize().getId());
                                                vRes.setStock(v.getStock());
                                                vRes.setSku(v.getSku());
                                                vRes.setPrice(v.getPrice());
                                                vRes.setDiscountedPrice(activePromo != null
                                                                ? promotionService.calcDiscountedPrice(v.getPrice(),
                                                                                activePromo)
                                                                : v.getPrice());
                                                vRes.setCreatedAt(v.getCreatedAt());
                                                return vRes;
                                        })
                                        .toList();

                        res.setProductVariant(variantResponses);

                        return res;
                });

                return ResponseEntity.ok(responses);

        }

        // Tạo mới product ───────────────────────────────
        @Override
        public ResponseEntity<?> createProduct(ProductRequest request, List<MultipartFile> imageFiles) {
                Product product = new Product();
                setProductFields(product, request);
                Product saved = productRepository.save(product);

                saveVariants(saved, request.getVariants());
                saveImages(saved, request.getImages(), imageFiles); // truyền thêm files

                return getProductDetail(saved.getId());
        }

        // ─── Helper: set fields ───────────────────────────────
        private void setProductFields(Product product, ProductRequest request) {
                product.setName(request.getName());
                product.setDescription(request.getDescription());
                product.setBasePrice(request.getBasePrice());
                product.setCategory(categoryRepository.findById(request.getCategoryId())
                                .orElseThrow(() -> new RuntimeException("Category không tồn tại")));
                product.setBrand(brandRepository.findById(request.getBrandId())
                                .orElseThrow(() -> new RuntimeException("Brand không tồn tại")));
                product.setTargetAudience(targetAudienceRepository.findById(request.getTargetAudienceId())
                                .orElseThrow(() -> new RuntimeException("TargetAudience không tồn tại")));
        }

        // ─── Helper: save variants (tạo mới) ─────────────────
        private void saveVariants(Product product, List<ProductVariantRequest> requests) {
                if (requests == null)
                        return;
                requests.forEach(req -> {
                        ProductVariant variant = new ProductVariant();
                        setVariantFields(variant, product, req);
                        productVariantRepository.save(variant);
                });
        }

        // ─── Helper: update variants ──────────────────────────
        private void updateVariants(Product product, List<ProductVariantRequest> requests) {
                if (requests == null || requests.isEmpty()) {
                        // ✅ Xóa hết variants nếu list rỗng
                        productVariantRepository.deleteByProductId(product.getId());
                        return;
                }

                List<Integer> keepIds = new ArrayList<>();

                requests.forEach(req -> {
                        ProductVariant variant = req.getId() != null
                                        ? productVariantRepository.findById(req.getId())
                                                        .orElseThrow(() -> new RuntimeException(
                                                                        "Variant không tồn tại"))
                                        : new ProductVariant();

                        setVariantFields(variant, product, req);
                        ProductVariant saved = productVariantRepository.save(variant);
                        keepIds.add(saved.getId());
                });

                productVariantRepository.deleteByProductIdAndIdNotIn(product.getId(), keepIds);
                // ✅ Bỏ check !keepIds.isEmpty()
        }

        private void setVariantFields(ProductVariant variant, Product product, ProductVariantRequest req) {
                Size size = sizeRepository.findById(req.getSizeId())
                                .orElseThrow(() -> new RuntimeException("Size không tồn tại"));
                variant.setProduct(product);
                variant.setColor(req.getColor());
                variant.setSize(size);
                variant.setStock(req.getStock());
                variant.setPrice(req.getPrice());
                variant.setSku(req.getSku());
        }

        // ─── Helper: save images (tạo mới) ───────────────────
        private void saveImages(Product product, List<ProductImageRequest> requests, List<MultipartFile> files) {
                if (requests == null || requests.isEmpty())
                        return;

                List<String> uploadedUrls = new ArrayList<>();
                if (files != null && !files.isEmpty()) {
                        uploadedUrls = files.stream()
                                        .map(cloudinaryService::uploadImage)
                                        .toList();
                }

                for (int i = 0; i < requests.size(); i++) {
                        ProductImageRequest req = requests.get(i);
                        ProductImage image = new ProductImage();
                        image.setProduct(product);

                        // Ảnh cũ có sẵn imageUrl (trường hợp đặc biệt)
                        if (req.getImageUrl() != null) {
                                image.setImageUrl(req.getImageUrl());
                        } else if (i < uploadedUrls.size()) {
                                image.setImageUrl(uploadedUrls.get(i));
                        }

                        image.setIsPrimary(Boolean.TRUE.equals(req.getIsPrimary()));
                        productImageRepository.save(image);
                }
        }

        // ─── Helper: update images ────────────────────────────
        private void updateImages(Product product, List<ProductImageRequest> requests, List<MultipartFile> files) {
                if (requests == null || requests.isEmpty()) {
                        // ✅ Xóa hết images nếu list rỗng
                        productImageRepository.deleteByProductId(product.getId());
                        return;
                }

                List<Integer> keepIds = new ArrayList<>();
                int newFileIndex = 0;

                List<String> uploadedUrls = new ArrayList<>();
                if (files != null && !files.isEmpty()) {
                        uploadedUrls = files.stream()
                                        .map(cloudinaryService::uploadImage)
                                        .toList();
                }

                for (ProductImageRequest req : requests) {
                        ProductImage image;

                        if (req.getId() != null) {
                                // Ảnh cũ → giữ lại
                                image = productImageRepository.findById(req.getId())
                                                .orElseThrow(() -> new RuntimeException("Image không tồn tại"));
                        } else {
                                // Ảnh mới → upload Cloudinary
                                image = new ProductImage();
                                image.setProduct(product);
                                if (newFileIndex < uploadedUrls.size()) {
                                        image.setImageUrl(uploadedUrls.get(newFileIndex++));
                                }
                        }

                        image.setIsPrimary(Boolean.TRUE.equals(req.getIsPrimary()));
                        ProductImage saved = productImageRepository.save(image);
                        keepIds.add(saved.getId());
                }

                // ✅ Bỏ check isEmpty
                productImageRepository.deleteByProductIdAndIdNotIn(product.getId(), keepIds);
        }

        // Helper update promotion
        private void updatePromotion(Product product, Integer promotionId) {
                // Xóa product khỏi tất cả promotion hiện tại
                if (product.getPromotions() != null) {
                        product.getPromotions().forEach(promo -> promo.getProducts().remove(product));
                        product.getPromotions().clear();
                        productRepository.save(product);
                }

                // Gán promotion mới nếu có
                if (promotionId != null) {
                        Promotion promotion = promotionRepository.findById(promotionId)
                                        .orElseThrow(() -> new RuntimeException("Promotion không tồn tại"));
                        promotion.getProducts().add(product);
                        product.getPromotions().add(promotion);
                        promotionRepository.save(promotion);
                }
        }

        // Vô hiệu hóa và khôi phục product theo id
        @Override
        @Transactional
        public ResponseEntity<?> deactivateProduct(Integer id) {
                Product product = productRepository.findById(id)
                                .orElseThrow(() -> new RuntimeException("Product không tồn tại"));

                if (product.getDeletedAt() != null) {
                        productRepository.softDelete(id, null);
                } else {
                        productRepository.softDelete(id, LocalDateTime.now());
                }

                return ResponseEntity.ok("Câph nhật thành công");
        }

        // Cập nhật product theo id
        @Override
        @Transactional
        public ResponseEntity<?> updateProduct(Integer id, ProductRequest request, List<MultipartFile> imageFiles) {
                Product product = productRepository.findById(id)
                                .orElseThrow(() -> new RuntimeException("Product không tồn tại"));

                setProductFields(product, request);
                productRepository.save(product);

                updateVariants(product, request.getVariants());
                updateImages(product, request.getImages(), imageFiles);
                updatePromotion(product, request.getPromotionId());

                return getProductDetail(id);
        }

        // lấy product theo id gồm các biến thể và hình ảnh của product
        private ProductDetailDTO toDetailResponse(Product product) {
                List<ProductImageDTO> images = product.getProductImages()
                                .stream()
                                .sorted((i1, i2) -> Boolean.compare(i2.getIsPrimary(), i1.getIsPrimary()))
                                .map(img -> ProductImageDTO.builder()
                                                .id(img.getId())
                                                .imageUrl(img.getImageUrl())
                                                .isPrimary(img.getIsPrimary())
                                                .build())
                                .toList();

                // Min và Max price
                BigDecimal minPrice = product.getProductVariants()
                                .stream()
                                .map(ProductVariant::getPrice)
                                .min(BigDecimal::compareTo)
                                .orElse(product.getBasePrice());

                BigDecimal maxPrice = product.getProductVariants()
                                .stream()
                                .map(ProductVariant::getPrice)
                                .max(BigDecimal::compareTo)
                                .orElse(product.getBasePrice());

                // Promotion tốt nhất
                Optional<Promotion> promo = getBestPromotion(product, minPrice);

                PromotionResponse promoResponse = promo
                                .map(this::toPromotionResponse)
                                .orElse(null);

                BigDecimal discountedMinPrice = promo
                                .map(pr -> promotionService.calcDiscountedPrice(minPrice, pr))
                                .orElse(minPrice);

                // Variants
                List<ProductVariantDTO> variants = product.getProductVariants()
                                .stream()
                                .map(v -> ProductVariantDTO.builder()
                                                .id(v.getId())
                                                .color(v.getColor())
                                                .sizeId(v.getSize().getId())
                                                .sizeName(v.getSize().getName())
                                                .stock(v.getStock())
                                                .price(v.getPrice())
                                                .discountedPrice(
                                                                promo.map(pr -> promotionService
                                                                                .calcDiscountedPrice(v.getPrice(), pr))
                                                                                .orElse(v.getPrice()))
                                                .sku(v.getSku())
                                                .build())
                                .toList();

                return ProductDetailDTO.builder()
                                .id(product.getId())
                                .name(product.getName())
                                .description(product.getDescription())
                                .basePrice(product.getBasePrice())

                                .minPrice(minPrice)
                                .maxPrice(maxPrice)
                                .discountedMinPrice(discountedMinPrice)

                                .promotion(promoResponse)

                                .categoryId(product.getCategory().getId())
                                .categoryName(product.getCategory().getName())

                                .brandId(product.getBrand().getId())
                                .brandName(product.getBrand().getName())
                                .brandLogo(product.getBrand().getLogo())

                                .targetAudienceId(product.getTargetAudience().getId())
                                .targetAudienceName(product.getTargetAudience().getName())

                                .images(images)
                                .variants(variants)

                                .build();
        }

        @Override
        public ResponseEntity<?> getProductDetail(int id) {
                // Query 1: lấy product + images + category + brand
                Product product = productRepository.findDetailByIdWithImages(id)
                                .orElseThrow(() -> new EntityNotFoundException("Product not found: " + id));

                // Query 2: lấy variants + size rồi set vào product
                productRepository.findDetailByIdWithVariants(id)
                                .ifPresent(p -> product.setProductVariants(p.getProductVariants()));

                return ResponseEntity.ok(toDetailResponse(product));
        }

        @Override
        public ResponseEntity<?> getSpotlightProducts() {
                List<ProductHomeView> responses = productRepository.findTop4ByDeletedAtIsNullOrderByCreatedAtDesc()
                                .stream()
                                .map(product -> {
                                        ProductHomeView res = new ProductHomeView();
                                        res.setCategoryName(product.getCategory().getName());
                                        res.setId(product.getId());
                                        res.setName(product.getName());
                                        res.setBrandName(product.getBrand().getName());

                                        ProductImage productImage = product.getProductImages().stream()
                                                        .filter(pi -> Boolean.TRUE.equals(pi.getIsPrimary()))
                                                        .findFirst()
                                                        .orElse(null);
                                        res.setImage(productImage != null ? productImage.getImageUrl() : null);
                                        // Min và Max price
                                        BigDecimal minPrice = product.getProductVariants()
                                                        .stream()
                                                        .map(ProductVariant::getPrice)
                                                        .min(BigDecimal::compareTo)
                                                        .orElse(product.getBasePrice());

                                        // Promotion tốt nhất
                                        Optional<Promotion> promo = getBestPromotion(product, minPrice);
                                        PromotionResponse promoResponse = promo
                                                        .map(this::toPromotionResponse)
                                                        .orElse(null);

                                        BigDecimal discountedMinPrice = promo
                                                        .map(pr -> promotionService.calcDiscountedPrice(minPrice, pr))
                                                        .orElse(minPrice);

                                        res.setPrice(minPrice);
                                        res.setDiscountPrice(discountedMinPrice);
                                        res.setPromotion(promoResponse);
                                        return res;
                                })
                                .toList();
                return ResponseEntity.ok(responses);

        }

        // Lấy product theo chương trình khuyến mãi
        @Override
        public ResponseEntity<?> getProductOnSale() {

                List<ProductHomeView> responses = productRepository.findProductsOnSale(PageRequest.of(0, 4))
                                .stream()
                                .map(product -> {
                                        ProductHomeView res = new ProductHomeView();
                                        res.setCategoryName(product.getCategory().getName());
                                        res.setId(product.getId());
                                        res.setName(product.getName());
                                        res.setBrandName(product.getBrand().getName());

                                        ProductImage productImage = product.getProductImages().stream()
                                                        .filter(pi -> Boolean.TRUE.equals(pi.getIsPrimary()))
                                                        .findFirst()
                                                        .orElse(null);
                                        res.setImage(productImage != null ? productImage.getImageUrl() : null);

                                        // Min và Max price
                                        BigDecimal minPrice = product.getProductVariants()
                                                        .stream()
                                                        .map(ProductVariant::getPrice)
                                                        .min(BigDecimal::compareTo)
                                                        .orElse(product.getBasePrice());

                                        // Promotion tốt nhất
                                        Optional<Promotion> promo = getBestPromotion(product, minPrice);
                                        PromotionResponse promoResponse = promo
                                                        .map(this::toPromotionResponse)
                                                        .orElse(null);
                                        BigDecimal discountedMinPrice = promo
                                                        .map(pr -> promotionService.calcDiscountedPrice(minPrice, pr))
                                                        .orElse(minPrice);

                                        res.setPrice(minPrice);
                                        res.setDiscountPrice(discountedMinPrice);
                                        res.setPromotion(promoResponse);
                                        return res;
                                })
                                .toList();

                return ResponseEntity.ok(responses);
        }

        // thêm khuyến mãi vào products
        @Transactional
        @Override
        public void addPromotionToProducts(AddPromotionToProductsRequest request) {
                // check promotion tồn tại
                promotionRepository.findById(request.getPromotionId())
                                .orElseThrow(() -> new RuntimeException("Promotion không tồn tại"));

                // check products tồn tại
                List<Product> products = productRepository.findAllById(request.getProductIds());
                if (products.isEmpty()) {
                        throw new RuntimeException("Không tìm thấy sản phẩm");
                }

                // 1. Xóa tất cả promotion cũ của các product này
                promotionRepository.removePromotionsByProductIds(request.getProductIds());

                // 2. Gán promotion mới
                request.getProductIds().forEach(productId -> promotionRepository
                                .assignPromotionToProduct(request.getPromotionId(), productId));
        }

}
