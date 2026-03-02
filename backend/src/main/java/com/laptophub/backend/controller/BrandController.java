package com.laptophub.backend.controller;

import com.laptophub.backend.dto.BrandCreateDTO;
import com.laptophub.backend.dto.BrandResponseDTO;
import com.laptophub.backend.service.BrandService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.lang.NonNull;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/brands")
@RequiredArgsConstructor
public class BrandController {
    
    private final BrandService brandService;
    
    @GetMapping
    public Page<BrandResponseDTO> findAll(@NonNull Pageable pageable) {
        return brandService.findAll(pageable);
    }
    
    @GetMapping("/{id}")
    public BrandResponseDTO findById(@PathVariable Long id) {
        return brandService.findById(id);
    }
    
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public BrandResponseDTO create(@Valid @RequestBody BrandCreateDTO dto) {
        return brandService.createBrand(dto);
    }
    
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public BrandResponseDTO update(@PathVariable Long id, @Valid @RequestBody BrandCreateDTO dto) {
        return brandService.updateBrand(id, dto);
    }
    
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public void delete(@PathVariable Long id) {
        brandService.deleteBrand(id);
    }
}
