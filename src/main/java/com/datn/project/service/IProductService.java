package com.datn.project.service;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import com.datn.project.dto.product.AddPromotionToProductsRequest;
import com.datn.project.dto.product.ProductFilterDTO;
import com.datn.project.dto.product.ProductRequest;

public interface IProductService {

    ResponseEntity<?> getFilterProducts(ProductFilterDTO filterDTO, int page, int size);

    ResponseEntity<?> getSpotlightProducts();

    ResponseEntity<?> getTop5Product();

    ResponseEntity<?> getAllProducts(int page, int size, ProductFilterDTO filterDTO);

    ResponseEntity<?> deactivateProduct(Integer id);

    ResponseEntity<?> updateProduct(Integer id, ProductRequest request, List<MultipartFile> imageFiles);

    ResponseEntity<?> getProductDetail(int id);

    ResponseEntity<?> getProductOnSale();

    ResponseEntity<?> createProduct(ProductRequest request, List<MultipartFile> imageFiles);

   void addPromotionToProducts(AddPromotionToProductsRequest request);

}
