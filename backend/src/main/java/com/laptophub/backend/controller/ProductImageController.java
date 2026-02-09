package com.laptophub.backend.controller;

import com.laptophub.backend.dto.ProductImageDTO;
import com.laptophub.backend.model.Product;
import com.laptophub.backend.model.ProductImage;
import com.laptophub.backend.repository.ProductImageRepository;
import com.laptophub.backend.repository.ProductRepository;
import com.laptophub.backend.service.CloudinaryService;
import jakarta.validation.constraints.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/products")
public class ProductImageController {

    @Autowired
    private ProductImageRepository productImageRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CloudinaryService cloudinaryService;

    /**
     * Agregar imagen a un producto
     * POST /api/products/{productId}/images
     */
    @PostMapping("/{productId}/images")
    @SuppressWarnings("null")
    public ResponseEntity<ProductImageDTO> addImage(
            @PathVariable Long productId,
            @RequestParam @NotNull MultipartFile file,
            @RequestParam @NotNull Integer orden,
            @RequestParam(required = false) String descripcion) throws IOException {
        
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));
        
        String imageUrl = cloudinaryService.uploadImage(file, "laptophub/products");
        
        ProductImage image = ProductImage.builder()
                .url(imageUrl)
                .orden(orden)
                .descripcion(descripcion)
                .product(product)
                .build();
        
        ProductImage saved = productImageRepository.save(image);
        return ResponseEntity.ok(mapToDTO(saved));
    }

    /**
     * Obtener todas las imágenes de un producto
     * GET /api/products/{productId}/images
     */
    @GetMapping("/{productId}/images")
    public ResponseEntity<List<ProductImageDTO>> getImagesByProduct(@PathVariable Long productId) {
        List<ProductImage> images = productImageRepository.findByProductIdOrderByOrdenAsc(productId);
        List<ProductImageDTO> dtos = images.stream().map(this::mapToDTO).collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    /**
     * Obtener imagen específica por ID
     * GET /api/products/images/{imageId}
     */
    @GetMapping("/images/{imageId}")
    @SuppressWarnings("null")
    public ResponseEntity<ProductImageDTO> getImageById(@PathVariable Long imageId) {
        return productImageRepository.findById(imageId)
                .map(image -> ResponseEntity.ok(mapToDTO(image)))
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Actualizar imagen (orden, url, descripción)
     * PUT /api/products/images/{imageId}
     */
    @PutMapping("/images/{imageId}")
    @SuppressWarnings("null")
    public ResponseEntity<ProductImageDTO> updateImage(
            @PathVariable Long imageId,
            @RequestParam(required = false) String url,
            @RequestParam(required = false) Integer orden,
            @RequestParam(required = false) String descripcion) {
        
        ProductImage image = productImageRepository.findById(imageId)
                .orElseThrow(() -> new RuntimeException("Image not found"));
        
        if (url != null) image.setUrl(url);
        if (orden != null) image.setOrden(orden);
        if (descripcion != null) image.setDescripcion(descripcion);
        
        ProductImage saved = productImageRepository.save(image);
        return ResponseEntity.ok(mapToDTO(saved));
    }

    /**
     * Eliminar imagen
     * DELETE /api/products/images/{imageId}
     */
    @DeleteMapping("/images/{imageId}")
    @SuppressWarnings("null")
    public ResponseEntity<Void> deleteImage(@PathVariable Long imageId) {
        if (!productImageRepository.existsById(imageId)) {
            return ResponseEntity.notFound().build();
        }
        productImageRepository.deleteById(imageId);
        return ResponseEntity.ok().build();
    }

    /**
     * Eliminar todas las imágenes de un producto
     * DELETE /api/products/{productId}/images
     */
    @DeleteMapping("/{productId}/images")
    public ResponseEntity<Void> deleteAllImagesByProduct(@PathVariable Long productId) {
        productImageRepository.deleteByProductId(productId);
        return ResponseEntity.ok().build();
    }
    
    private ProductImageDTO mapToDTO(ProductImage image) {
        return ProductImageDTO.builder()
                .id(image.getId())
                .url(image.getUrl())
                .orden(image.getOrden())
                .descripcion(image.getDescripcion())
                .build();
    }
}
