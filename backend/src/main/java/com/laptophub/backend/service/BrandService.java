package com.laptophub.backend.service;

import com.laptophub.backend.dto.BrandCreateDTO;
import com.laptophub.backend.dto.BrandResponseDTO;
import com.laptophub.backend.dto.DTOMapper;
import com.laptophub.backend.exception.ConflictException;
import com.laptophub.backend.exception.ResourceNotFoundException;
import com.laptophub.backend.model.Brand;
import com.laptophub.backend.repository.BrandRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BrandService {
    
    private final BrandRepository brandRepository;
    
    @Transactional(readOnly = true)
    public Page<BrandResponseDTO> findAll(@NonNull Pageable pageable) {
        return brandRepository.findAll(pageable).map(DTOMapper::toBrandResponse);
    }
    
    @Transactional(readOnly = true)
    @SuppressWarnings("null")
    public BrandResponseDTO findById(Long id) {
        Brand brand = brandRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Marca no encontrada con id: " + id));
        return DTOMapper.toBrandResponse(brand);
    }
    
    @Transactional(readOnly = true)
    public BrandResponseDTO findByNombre(String nombre) {
        Brand brand = brandRepository.findByNombre(nombre)
                .orElseThrow(() -> new ResourceNotFoundException("Marca no encontrada: " + nombre));
        return DTOMapper.toBrandResponse(brand);
    }
    
    @Transactional
    @SuppressWarnings("null")
    public BrandResponseDTO createBrand(BrandCreateDTO dto) {
        // Validar que la marca no exista ya
        if (brandRepository.findByNombre(dto.getNombre()).isPresent()) {
            throw new ConflictException("La marca '" + dto.getNombre() + "' ya existe");
        }
        
        Brand brand = Brand.builder()
                .nombre(dto.getNombre())
                .descripcion(dto.getDescripcion())
                .imageUrl(dto.getImageUrl())
                .build();
        
        Brand saved = brandRepository.save(brand);
        return DTOMapper.toBrandResponse(saved);
    }
    
    @Transactional
    @SuppressWarnings("null")
    public BrandResponseDTO updateBrand(Long id, BrandCreateDTO dto) {
        Brand brand = brandRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Marca no encontrada con id: " + id));
        
        if (dto.getNombre() != null) {
            // Validar que el nuevo nombre no esté en uso por otra marca
            if (brandRepository.findByNombre(dto.getNombre()).isPresent() 
                    && !brandRepository.findByNombre(dto.getNombre()).get().getId().equals(id)) {
                throw new ConflictException("La marca '" + dto.getNombre() + "' ya existe");
            }
            brand.setNombre(dto.getNombre());
        }
        if (dto.getDescripcion() != null) {
            brand.setDescripcion(dto.getDescripcion());
        }
        if (dto.getImageUrl() != null) {
            brand.setImageUrl(dto.getImageUrl());
        }
        
        Brand saved = brandRepository.save(brand);
        return DTOMapper.toBrandResponse(saved);
    }
    
    @Transactional
    @SuppressWarnings("null")
    public void deleteBrand(Long id) {
        if (!brandRepository.existsById(id)) {
            throw new ResourceNotFoundException("Marca no encontrada con id: " + id);
        }
        brandRepository.deleteById(id);
    }
}
