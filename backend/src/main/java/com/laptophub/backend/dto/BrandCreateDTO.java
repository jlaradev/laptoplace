package com.laptophub.backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BrandCreateDTO {
    @NotBlank(message = "El nombre de la marca es obligatorio")
    private String nombre;
    
    private String descripcion;
    
    private String imageUrl;
}
