package com.laptophub.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductResponseDTO {
    private Long id;
    private String nombre;
    private String descripcion;
    private BigDecimal precio;
    private Integer stock;
    private BrandResponseDTO brand;
    private String procesador;
    private Integer ram;
    private Integer almacenamiento;
    private String pantalla;
    private String gpu;
    private BigDecimal peso;
    private List<ProductImageDTO> imagenes;
    private List<ReviewResponseDTO> resenas;
    private Double promedioRating;
    private LocalDateTime createdAt;
}
