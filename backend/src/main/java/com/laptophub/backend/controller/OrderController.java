package com.laptophub.backend.controller;

import com.laptophub.backend.dto.CreateOrderDTO;
import com.laptophub.backend.dto.OrderResponseDTO;
import com.laptophub.backend.dto.PurchasedResponseDTO;
import com.laptophub.backend.dto.ReviewableProductDTO;
import com.laptophub.backend.model.OrderStatus;
import com.laptophub.backend.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping("/user/{userId}")
    public OrderResponseDTO createFromCart(
            @PathVariable UUID userId,
            @Valid @RequestBody CreateOrderDTO dto
    ) {
        return orderService.createOrderFromCartDTO(userId, dto);
    }

    @GetMapping
    public Page<OrderResponseDTO> findAll(@NonNull Pageable pageable) {
        return orderService.findAllDTO(pageable);
    }

    @GetMapping("/{orderId}")
    public OrderResponseDTO findById(@PathVariable Long orderId) {
        return orderService.findByIdDTO(orderId);
    }

    @GetMapping("/user/{userId}")
    public Page<OrderResponseDTO> findByUser(@PathVariable UUID userId, @NonNull Pageable pageable) {
        return orderService.findByUserIdDTO(userId, pageable);
    }

    @GetMapping("/status/{estado}")
    public Page<OrderResponseDTO> findByStatus(@PathVariable OrderStatus estado, @NonNull Pageable pageable) {
        return orderService.findByStatusDTO(estado, pageable);
    }

    @PutMapping("/{orderId}/status/{estado}")
    public OrderResponseDTO updateStatus(
            @PathVariable Long orderId,
            @PathVariable OrderStatus estado
    ) {
        return orderService.updateOrderStatusDTO(orderId, estado);
    }

    @PostMapping("/{orderId}/cancel")
    public OrderResponseDTO cancel(@PathVariable Long orderId) {
        return orderService.cancelOrderDTO(orderId);
    }

    @PostMapping("/expire")
    public int expirePending() {
        return orderService.expirePendingPaymentOrders();
    }

    /**
     * Verifica si un usuario ha comprado un producto específico.
     * @param userId ID del usuario
     * @param productId ID del producto
     * @return Response con boolean indicando si el producto fue comprado
     */
    @GetMapping("/user/{userId}/product/{productId}/purchased")
    public PurchasedResponseDTO checkProductPurchased(
            @PathVariable UUID userId,
            @PathVariable Long productId
    ) {
        boolean purchased = orderService.isProductPurchasedByUser(userId, productId);
        return PurchasedResponseDTO.builder()
                .purchased(purchased)
                .build();
    }

    /**
     * Obtiene los productos que un usuario puede reseñar.
     * Solo retorna productos de órdenes con estado ENTREGADO.
     * @param userId ID del usuario
     * @param pageable Información de paginación (página y tamaño)
     * @return Página de productos reseñables con estado de reseña
     */
    @GetMapping("/user/{userId}/reviewable-products")
    public Page<ReviewableProductDTO> getReviewableProducts(
            @PathVariable UUID userId,
            @NonNull Pageable pageable
    ) {
        return orderService.getReviewableProducts(userId, pageable);
    }

    /**
     * Obtiene todas las órdenes activas de un usuario (PROCESANDO, ENVIADO, ENTREGADO).
     * Incluye información detallada de los items y pagos.
     * @param userId ID del usuario
     * @param pageable Información de paginación (página y tamaño)
     * @return Página de órdenes en estados activos
     */
    @GetMapping("/user/{userId}/active")
    public Page<OrderResponseDTO> getUserActiveOrders(
            @PathVariable UUID userId,
            @NonNull Pageable pageable
    ) {
        return orderService.getUserActiveOrders(userId, pageable);
    }
}