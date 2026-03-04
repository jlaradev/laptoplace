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
public class ReviewableProductDTO {
    private Long id;
    private String nombre;
    private BigDecimal precio;
    private boolean hasReview;
    private Long reviewId;  // null si no tiene reseña
}
