package com.laptophub.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductListDTO {
    private Long id;
    private String nombre;
    private BigDecimal precio;
    private Integer stock;
    private String marca;
    private ProductImageDTO imagenPrincipal;
    private Double promedioRating;
}
