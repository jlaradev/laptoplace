package com.laptophub.backend.controller;

import com.laptophub.backend.dto.BrandCreateDTO;
import com.laptophub.backend.dto.BrandResponseDTO;
import com.laptophub.backend.service.BrandService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/brands")
@RequiredArgsConstructor
public class BrandController {
    
    private final BrandService brandService;
    
    @GetMapping
    public Page<BrandResponseDTO> findAll(@NonNull Pageable pageable) {
        return brandService.findAll(pageable);
    }

    @GetMapping("/inactive")
    @PreAuthorize("hasRole('ADMIN')")
    public Page<BrandResponseDTO> findAllInactive(@NonNull Pageable pageable) {
        return brandService.findAllInactive(pageable);
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
    public void deactivate(@PathVariable Long id) {
        brandService.deactivateBrand(id);
    }

    @PutMapping("/{id}/reactivate")
    @PreAuthorize("hasRole('ADMIN')")
    public BrandResponseDTO reactivate(@PathVariable Long id) {
        return brandService.reactivateBrand(id);
    }

    @PostMapping("/{id}/image")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BrandResponseDTO> uploadImage(
            @PathVariable Long id,
            @RequestParam MultipartFile file) throws IOException {
        return ResponseEntity.ok(brandService.uploadImage(id, file));
    }
}
