package com.laptophub.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BrandResponseDTO {
    private Long id;
    private String nombre;
    private String descripcion;
    private String imageUrl;
    private LocalDateTime createdAt;
}
