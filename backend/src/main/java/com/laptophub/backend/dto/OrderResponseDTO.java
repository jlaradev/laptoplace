package com.laptophub.backend.dto;

import com.laptophub.backend.model.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponseDTO {
    private Long id;
    private UUID userId;
    private BigDecimal total;
    private OrderStatus estado;
    private String direccionEnvio;
    private LocalDateTime expiresAt;
    private List<OrderItemResponseDTO> items;
    private PaymentResponseDTO payment;
    private LocalDateTime createdAt;
}
