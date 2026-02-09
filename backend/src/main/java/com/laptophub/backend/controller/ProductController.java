package com.laptophub.backend.controller;

import com.laptophub.backend.dto.ProductCreateDTO;
import com.laptophub.backend.dto.ProductListDTO;
import com.laptophub.backend.dto.ProductResponseDTO;
import com.laptophub.backend.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @GetMapping
    public Page<ProductListDTO> findAll(@NonNull Pageable pageable) {
        return productService.findAll(pageable);
    }

    @GetMapping("/{id}")
    public ProductResponseDTO findById(@PathVariable Long id) {
        return productService.findByIdDTO(id);
    }

    @GetMapping("/search")
    public Page<ProductListDTO> searchByName(@RequestParam String nombre, @NonNull Pageable pageable) {
        return productService.searchByName(nombre, pageable);
    }

    @GetMapping("/brand")
    public Page<ProductListDTO> findByBrand(@RequestParam String marca, @NonNull Pageable pageable) {
        return productService.findByBrand(marca, pageable);
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
}