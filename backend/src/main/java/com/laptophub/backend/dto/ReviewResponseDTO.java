package com.laptophub.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewResponseDTO {
    private Long id;
    private Long productId;
    private UUID userId;
    private String userNombre;
    private Integer rating;
    private String comentario;
    private LocalDateTime createdAt;
}
