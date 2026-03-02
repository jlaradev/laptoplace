package com.laptophub.backend.controller;

import com.laptophub.backend.dto.ProductCreateDTO;
import com.laptophub.backend.dto.ProductListDTO;
import com.laptophub.backend.dto.ProductResponseDTO;
import com.laptophub.backend.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    /**
     * Búsqueda unificada con filtros y ordenamiento opcionales
     * 
     * Parámetros query opcionales:
     * - nombre: búsqueda parcial en nombre de producto
     * - brandId: filtro por marca (ID)
     * - sortBy: campo para ordenar (name, price, rating, createdAt). Default: createdAt
     * - sort: dirección (asc, desc). Default: desc
     * - page: número de página (default: 0)
     * - size: elementos por página (default: 20)
     * 
     * Ejemplos:
     * GET /api/products
     * GET /api/products?nombre=Dell&sortBy=price&sort=asc
     * GET /api/products?brandId=1&sortBy=rating&sort=desc
     * GET /api/products?nombre=laptop&brandId=2&sortBy=name&sort=asc&page=1&size=10
     */
    @GetMapping
    public Page<ProductListDTO> search(
            @RequestParam(required = false) String nombre,
            @RequestParam(required = false) Long brandId,
            @RequestParam(required = false, defaultValue = "createdAt") String sortBy,
            @RequestParam(required = false, defaultValue = "desc") String sort,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "20") int size
    ) {
        // Determinar si el usuario es ADMIN
        boolean isAdmin = isUserAdmin();
        
        // Crear Pageable con los parámetros de paginación
        Pageable pageable = PageRequest.of(page, size);
        
        // Llamar al servicio con búsqueda unificada
        return productService.search(nombre, brandId, sortBy, sort, pageable, isAdmin);
    }

    /**
     * Obtener los 10 mejores productos valorados (para página principal)
     * GET /api/products/top-rated
     */
    @GetMapping("/top-rated")
    public Page<ProductListDTO> getTopRated() {
        boolean isAdmin = isUserAdmin();
        Pageable pageable = PageRequest.of(0, 10);
        return productService.search(null, null, "rating", "desc", pageable, isAdmin);
    }

    @GetMapping("/{id}")
    public ProductResponseDTO findById(@PathVariable Long id) {
        return productService.findByIdDTO(id);
    }

    @PostMapping
    public ProductResponseDTO create(@Valid @RequestBody ProductCreateDTO dto) {
        return productService.createProduct(dto);
    }

    @PutMapping("/{id}")
    public ProductResponseDTO update(@PathVariable Long id, @Valid @RequestBody ProductCreateDTO dto) {
        return productService.updateProduct(id, dto);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        productService.deleteProduct(id);
    }

    /**
     * Verifica si el usuario autenticado tiene rol ADMIN
     */
    private boolean isUserAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(auth -> auth.equals("ROLE_ADMIN"));
    }
}