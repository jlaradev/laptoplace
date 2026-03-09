package com.laptophub.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductListDTO {
    private Long id;
    private String nombre;
    private BigDecimal precio;
    private Integer stock;
    private BrandResponseDTO brand;
    private ProductImageDTO imagenPrincipal;
    private Double promedioRating;
    private LocalDateTime deletedAt;
}
