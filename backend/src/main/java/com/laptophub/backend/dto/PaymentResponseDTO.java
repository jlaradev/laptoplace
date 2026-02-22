package com.laptophub.backend.dto;

import com.laptophub.backend.model.PaymentStatus;
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
public class PaymentResponseDTO {
    private Long id;
    private Long orderId;
    private String stripePaymentId;
    private String clientSecret;
    private BigDecimal monto;
    private PaymentStatus estado;
    private LocalDateTime createdAt;
}
