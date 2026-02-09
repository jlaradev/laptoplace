package com.laptophub.backend.service;


import com.laptophub.backend.dto.*;
import com.laptophub.backend.model.Product;
import com.laptophub.backend.model.ProductImage;
import com.laptophub.backend.model.Review;
import com.laptophub.backend.repository.ProductImageRepository;
import com.laptophub.backend.repository.ProductRepository;
import com.laptophub.backend.repository.ReviewRepository;
import com.laptophub.backend.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductService {
    
    private final ProductRepository productRepository;
    private final ProductImageRepository productImageRepository;
    private final ReviewRepository reviewRepository;
    
    @Transactional(readOnly = true)
    @SuppressWarnings("null")
    public Page<ProductListDTO> findAll(@NonNull Pageable pageable) {
        return productRepository.findAll(pageable).map(product -> {
            List<ProductImage> images = productImageRepository.findByProductIdOrderByOrdenAsc(product.getId());
            ProductImage mainImage = images.isEmpty() ? null : images.get(0);
            Double avgRating = getAverageRatingForProduct(product.getId());
            return DTOMapper.toProductListDTO(product, mainImage, avgRating);
        });
    }
    
    @Transactional(readOnly = true)
    @SuppressWarnings("null")
    public Product findById(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado con id: " + id));
    }
    
    @Transactional(readOnly = true)
    public ProductResponseDTO findByIdDTO(Long id) {
        Product product = findById(id);
        List<ProductImage> images = productImageRepository.findByProductIdOrderByOrdenAsc(id);
        List<Review> reviews = reviewRepository.findByProduct(product);
        Double avgRating = getAverageRatingForProduct(id);
        return DTOMapper.toProductResponse(product, images, reviews, avgRating);
    }
    
    @Transactional(readOnly = true)
    public Page<ProductListDTO> searchByName(String nombre, @NonNull Pageable pageable) {
        return productRepository.findByNombreContainingIgnoreCase(nombre, pageable).map(product -> {
            List<ProductImage> images = productImageRepository.findByProductIdOrderByOrdenAsc(product.getId());
            ProductImage mainImage = images.isEmpty() ? null : images.get(0);
            Double avgRating = getAverageRatingForProduct(product.getId());
            return DTOMapper.toProductListDTO(product, mainImage, avgRating);
        });
    }
    
    @Transactional(readOnly = true)
    public Page<ProductListDTO> findByBrand(String marca, @NonNull Pageable pageable) {
        return productRepository.findByMarca(marca, pageable).map(product -> {
            List<ProductImage> images = productImageRepository.findByProductIdOrderByOrdenAsc(product.getId());
            ProductImage mainImage = images.isEmpty() ? null : images.get(0);
            Double avgRating = getAverageRatingForProduct(product.getId());
            return DTOMapper.toProductListDTO(product, mainImage, avgRating);
        });
    }
    
    @Transactional
    @SuppressWarnings("null")
    public ProductResponseDTO createProduct(ProductCreateDTO dto) {
        Product product = DTOMapper.toProduct(dto);
        Product saved = productRepository.save(product);
        return DTOMapper.toProductResponse(saved, List.of(), List.of(), 0.0);
    }
    
    @Transactional
    @SuppressWarnings("null")
    public ProductResponseDTO updateProduct(Long id, ProductCreateDTO dto) {
        Product existingProduct = findById(id);
        
        if (dto.getNombre() != null) existingProduct.setNombre(dto.getNombre());
        if (dto.getDescripcion() != null) existingProduct.setDescripcion(dto.getDescripcion());
        if (dto.getPrecio() != null) existingProduct.setPrecio(dto.getPrecio());
        if (dto.getStock() != null) existingProduct.setStock(dto.getStock());
        if (dto.getMarca() != null) existingProduct.setMarca(dto.getMarca());
        if (dto.getProcesador() != null) existingProduct.setProcesador(dto.getProcesador());
        if (dto.getRam() != null) existingProduct.setRam(dto.getRam());
        if (dto.getAlmacenamiento() != null) existingProduct.setAlmacenamiento(dto.getAlmacenamiento());
        if (dto.getPantalla() != null) existingProduct.setPantalla(dto.getPantalla());
        if (dto.getGpu() != null) existingProduct.setGpu(dto.getGpu());
        if (dto.getPeso() != null) existingProduct.setPeso(dto.getPeso());
        
        Product saved = productRepository.save(existingProduct);
        List<ProductImage> images = productImageRepository.findByProductIdOrderByOrdenAsc(id);
        List<Review> reviews = reviewRepository.findByProduct(saved);
        Double avgRating = getAverageRatingForProduct(id);
        return DTOMapper.toProductResponse(saved, images, reviews, avgRating);
    }
    
    @Transactional
    @SuppressWarnings("null")
    public void deleteProduct(Long id) {
        if (!productRepository.existsById(id)) {
            throw new ResourceNotFoundException("Producto no encontrado con id: " + id);
        }
        productRepository.deleteById(id);
    }
    
    private Double getAverageRatingForProduct(Long productId) {
        Product product = findById(productId);
        List<Review> reviews = reviewRepository.findByProduct(product);
        return reviews.stream()
                .mapToInt(Review::getRating)
                .average()
                .orElse(0.0);
    }
}
