package com.laptophub.backend.service;


import com.laptophub.backend.dto.*;
import com.laptophub.backend.model.Brand;
import com.laptophub.backend.model.Product;
import com.laptophub.backend.model.ProductImage;
import com.laptophub.backend.model.Review;
import com.laptophub.backend.repository.BrandRepository;
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
import java.util.stream.Collectors;
import org.springframework.data.domain.PageImpl;

@Service
@RequiredArgsConstructor
public class ProductService {
    
    private final ProductRepository productRepository;
    private final ProductImageRepository productImageRepository;
    private final ReviewRepository reviewRepository;
    private final BrandRepository brandRepository;
    private final CloudinaryService cloudinaryService;
    
    /**
     * Búsqueda unificada con filtros opcionales y ordenamiento dinámico
     * @param nombre Búsqueda parcial en nombre (optional)
     * @param brandId Filtro por marca (optional)
     * @param sortBy Campo para ordenar: name, price, rating, createdAt (default: createdAt)
     * @param sort Dirección: asc, desc (default: desc)
     * @param pageable Paginación
     * @param isAdmin Si es admin, muestra productos sin stock
     */
    @Transactional(readOnly = true)
    public Page<ProductListDTO> search(
            String nombre,
            Long brandId,
            String sortBy,
            String sort,
            @NonNull Pageable pageable,
            boolean isAdmin
    ) {
        boolean includeOutOfStock = isAdmin;
        
        // Para rating, usar lógica especial que ordena en memoria
        if ("rating".equalsIgnoreCase(sortBy)) {
            return searchByRating(nombre, brandId, includeOutOfStock, sort, pageable);
        }
        
        // Para otros ordenamientos, usar queries de base de datos
        Page<Product> results;
        
        if ("price".equalsIgnoreCase(sortBy)) {
            results = "asc".equalsIgnoreCase(sort)
                    ? productRepository.searchByPriceAsc(nombre, brandId, includeOutOfStock, pageable)
                    : productRepository.searchByPriceDesc(nombre, brandId, includeOutOfStock, pageable);
        } else if ("name".equalsIgnoreCase(sortBy)) {
            results = "asc".equalsIgnoreCase(sort)
                    ? productRepository.searchByNameAsc(nombre, brandId, includeOutOfStock, pageable)
                    : productRepository.searchByNameDesc(nombre, brandId, includeOutOfStock, pageable);
        } else {
            // Default: createdAt
            results = "asc".equalsIgnoreCase(sort)
                    ? productRepository.searchByCreatedAtAsc(nombre, brandId, includeOutOfStock, pageable)
                    : productRepository.searchByCreatedAtDesc(nombre, brandId, includeOutOfStock, pageable);
        }
        
        return mapProductsToDTO(results);
    }
    
    /**
     * Búsqueda por rating: ordena en memoria después de calcular ratings
     * Nota: Para mejor performance con muchos productos, considerar usar BD-level ordering
     */
    private Page<ProductListDTO> searchByRating(
            String nombre,
            Long brandId,
            boolean includeOutOfStock,
            String sortDirection,
            Pageable pageable
    ) {
        // Traer TODOS los productos sin paginación de BD (con filtros pero sin limit)
        // Usando un Pageable con tamaño muy grande para obtener todos
        Pageable allData = org.springframework.data.domain.PageRequest.of(0, 10000);
        Page<Product> allProducts = productRepository.searchByCreatedAtDesc(nombre, brandId, includeOutOfStock, allData);
        
        // Mapear a DTO y calcular ratings
        List<ProductListDTO> dtos = allProducts.getContent().stream()
                .map(product -> {
                    List<ProductImage> images = productImageRepository.findByProductIdOrderByOrdenAsc(product.getId());
                    ProductImage mainImage = images.isEmpty() ? null : images.get(0);
                    Double avgRating = getAverageRatingForProduct(product.getId());
                    return DTOMapper.toProductListDTO(product, mainImage, avgRating);
                })
                .collect(Collectors.toList());
        
        // Ordenar por rating
        if ("asc".equalsIgnoreCase(sortDirection)) {
            dtos.sort((a, b) -> Double.compare(a.getPromedioRating(), b.getPromedioRating()));
        } else {
            dtos.sort((a, b) -> Double.compare(b.getPromedioRating(), a.getPromedioRating()));
        }
        
        // Aplicar paginación manualmente
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), dtos.size());
        List<ProductListDTO> pageContent = dtos.subList(start, end);
        
        @SuppressWarnings("null")
        PageImpl<ProductListDTO> result = new PageImpl<>(pageContent, pageable, dtos.size());
        return result;
    }
    
    /**
     * Mapea Page<Product> a Page<ProductListDTO>
     */
    private Page<ProductListDTO> mapProductsToDTO(Page<Product> page) {
        return page.map(product -> {
            List<ProductImage> images = productImageRepository.findByProductIdOrderByOrdenAsc(product.getId());
            ProductImage mainImage = images.isEmpty() ? null : images.get(0);
            Double avgRating = getAverageRatingForProduct(product.getId());
            return DTOMapper.toProductListDTO(product, mainImage, avgRating);
        });
    }
    
    @Transactional(readOnly = true)
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
    public Page<ProductListDTO> findByBrand(Long brandId, @NonNull Pageable pageable) {
        return productRepository.findByBrand_Id(brandId, pageable).map(product -> {
            List<ProductImage> images = productImageRepository.findByProductIdOrderByOrdenAsc(product.getId());
            ProductImage mainImage = images.isEmpty() ? null : images.get(0);
            Double avgRating = getAverageRatingForProduct(product.getId());
            return DTOMapper.toProductListDTO(product, mainImage, avgRating);
        });
    }
    
    @Transactional
    @SuppressWarnings("null")
    public ProductResponseDTO createProduct(ProductCreateDTO dto) {
        Brand brand = brandRepository.findById(dto.getBrandId())
                .orElseThrow(() -> new ResourceNotFoundException("Marca no encontrada con id: " + dto.getBrandId()));
        
        Product product = DTOMapper.toProduct(dto, brand);
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
        if (dto.getBrandId() != null) {
            Brand brand = brandRepository.findById(dto.getBrandId())
                    .orElseThrow(() -> new ResourceNotFoundException("Marca no encontrada con id: " + dto.getBrandId()));
            existingProduct.setBrand(brand);
        }
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

        List<ProductImage> images = productImageRepository.findByProductIdOrderByOrdenAsc(id);
        for (ProductImage image : images) {
            try {
                cloudinaryService.deleteImage(image.getUrl());
            } catch (java.io.IOException e) {
                throw new RuntimeException("Error al eliminar imagen en Cloudinary", e);
            }
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
