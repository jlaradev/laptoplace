package com.laptophub.backend.controller;

import com.laptophub.backend.dto.OrderResponseDTO;
import com.laptophub.backend.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/orders")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminOrderController {

    private static final Logger logger = LoggerFactory.getLogger(AdminOrderController.class);
    private final OrderService orderService;

    /**
     * Cambia una orden de PROCESANDO a ENVIADO
     * Solo administradores pueden usar este endpoint
     */
    @PostMapping("/{orderId}/ship")
    public OrderResponseDTO shipOrder(@PathVariable Long orderId) {
        logger.info("[AdminOrderController] Admin solicitó cambiar orden {} a ENVIADO", orderId);
        return orderService.shipOrderDTO(orderId);
    }

    /**
     * Cambia una orden de ENVIADO a ENTREGADO
     * Solo administradores pueden usar este endpoint
     */
    @PostMapping("/{orderId}/deliver")
    public OrderResponseDTO deliverOrder(@PathVariable Long orderId) {
        logger.info("[AdminOrderController] Admin solicitó cambiar orden {} a ENTREGADO", orderId);
        return orderService.deliverOrderDTO(orderId);
    }
}
