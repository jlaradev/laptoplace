package com.laptophub.backend.service;


import com.laptophub.backend.dto.*;
import com.laptophub.backend.model.*;
import com.laptophub.backend.model.OrderStatus;
import com.laptophub.backend.model.PaymentStatus;
import com.laptophub.backend.repository.OrderItemRepository;
import com.laptophub.backend.repository.OrderRepository;
import com.laptophub.backend.repository.ProductImageRepository;
import com.laptophub.backend.repository.ProductRepository;
import com.laptophub.backend.repository.ReviewRepository;
import com.laptophub.backend.exception.ResourceNotFoundException;
import com.laptophub.backend.exception.ValidationException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderService {

    private static final Logger logger = LoggerFactory.getLogger(OrderService.class);

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductRepository productRepository;
    private final ProductImageRepository productImageRepository;
    private final ReviewRepository reviewRepository;
    private final UserService userService;
    private final CartService cartService;
    private final PaymentService paymentService;
    
    @Transactional
    @SuppressWarnings("null")
    public Order createOrderFromCart(UUID userId, String direccionEnvio) {
        User user = userService.findById(userId);
        Cart cart = cartService.getOrCreateCart(userId);
        
        if (cart.getItems().isEmpty()) {
            throw new ValidationException("El carrito está vacío");
        }
        
        for (CartItem cartItem : cart.getItems()) {
            Long productId = cartItem.getProduct().getId();
            Product product = productRepository.findByIdWithLock(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado con id: " + productId));
            
            if (product.getStock() < cartItem.getCantidad()) {
                throw new ValidationException("Stock insuficiente para producto " + product.getId());
            }

            cartItem.setProduct(product);
        }
        
        BigDecimal total = cart.getItems().stream()
                .map(item -> item.getProduct().getPrecio()
                        .multiply(BigDecimal.valueOf(item.getCantidad())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        Order order = Order.builder()
                .user(user)
                .total(total)
                .estado(OrderStatus.PENDIENTE_PAGO)
                .direccionEnvio(direccionEnvio)
                .expiresAt(LocalDateTime.now().plusMinutes(15))
                .build();
        
        Order savedOrder = orderRepository.save(order);
        
        for (CartItem cartItem : cart.getItems()) {
            Product product = cartItem.getProduct();
            
            OrderItem orderItem = OrderItem.builder()
                    .order(savedOrder)
                    .product(product)
                    .cantidad(cartItem.getCantidad())
                    .precioUnitario(product.getPrecio())
                    .build();
            
            orderItemRepository.save(orderItem);
            
            Integer newStock = product.getStock() - cartItem.getCantidad();
            product.setStock(newStock);
            productRepository.save(product);
        }
        
        try {
            Payment payment = paymentService.createPayment(savedOrder, total);
            savedOrder.setPayment(payment);
        } catch (com.stripe.exception.StripeException e) {
            throw new ValidationException("Error al procesar pago con Stripe: " + e.getMessage());
        }
        
        cartService.clearCart(userId);
        
        return savedOrder;
    }
    
    @Transactional(readOnly = true)
    @SuppressWarnings("null")
    public Order findById(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Orden no encontrada con id: " + orderId));
    }
    
    @Transactional(readOnly = true)
    public Page<Order> findByUserId(UUID userId, @NonNull Pageable pageable) {
        User user = userService.findById(userId);
        return orderRepository.findByUser(user, pageable);
    }
    
    @Transactional(readOnly = true)
    public List<Order> findByUserId(UUID userId) {
        User user = userService.findById(userId);
        return orderRepository.findByUser(user);
    }
    
    @Transactional(readOnly = true)
    public Page<Order> findByStatus(OrderStatus estado, @NonNull Pageable pageable) {
        return orderRepository.findByEstado(estado, pageable);
    }
    
    @Transactional(readOnly = true)
    public List<Order> findByStatus(OrderStatus estado) {
        return orderRepository.findByEstado(estado);
    }
    
    @Transactional
    public Order updateOrderStatus(Long orderId, OrderStatus newStatus) {
        Order order = findById(orderId);
        order.setEstado(newStatus);
        return orderRepository.save(order);
    }
    
    @Transactional
    public Order cancelOrder(Long orderId) {
        Order order = findById(orderId);
        
        boolean canCancel = order.getEstado() == OrderStatus.PENDIENTE_PAGO;
        
        if (!canCancel) {
            throw new ValidationException(
                    "Solo se pueden cancelar órdenes pendientes. Estado actual: " + order.getEstado());
        }
        
        restoreOrderStock(order);
        order.setEstado(OrderStatus.CANCELADO);
        return orderRepository.save(order);
    }

    @Transactional
    public int expirePendingPaymentOrders() {
        LocalDateTime now = LocalDateTime.now();
        List<Order> expired = orderRepository.findByEstadoAndExpiresAtBefore(OrderStatus.PENDIENTE_PAGO, now);

        logger.info("[OrderService] [{}] Órdenes candidatas a expirar: {}", now, expired.size());

        if (expired.isEmpty()) {
            return 0;
        }

        int count = 0;
        for (Order order : expired) {
            Order lockedOrder = orderRepository.findByIdWithLock(order.getId()).orElse(null);
            if (lockedOrder == null) continue;

            if (lockedOrder.getEstado() != OrderStatus.PENDIENTE_PAGO || lockedOrder.getExpiresAt().isAfter(now)) {
                continue;
            }

            restoreOrderStock(lockedOrder);
            lockedOrder.setEstado(OrderStatus.EXPIRADO);

            if (lockedOrder.getPayment() != null &&
                lockedOrder.getPayment().getEstado() != PaymentStatus.COMPLETADO) {
                lockedOrder.getPayment().setEstado(PaymentStatus.EXPIRADO);
            }

            orderRepository.save(lockedOrder);
            count++;
        }

        logger.info("[OrderService] [{}] Órdenes expiradas correctamente: {}", now, count);
        return count;
    }
    
    @Transactional(readOnly = true)
    public List<Order> findAll() {
        return orderRepository.findAll();
    }
    
    public void restoreOrderStock(Order order) {
        for (OrderItem item : order.getOrderItems()) {
            Product product = item.getProduct();
            product.setStock(product.getStock() + item.getCantidad());
            productRepository.save(product);
        }
    }
    
    // Métodos que retornan DTOs
    
    @Transactional
    public OrderResponseDTO createOrderFromCartDTO(UUID userId, CreateOrderDTO dto) {
        Order order = createOrderFromCart(userId, dto.getDireccionEnvio());
        return mapOrderToDTO(order);
    }
    
    @Transactional(readOnly = true)
    public OrderResponseDTO findByIdDTO(Long orderId) {
        Order order = findById(orderId);
        return mapOrderToDTO(order);
    }
    
    @Transactional(readOnly = true)
    public Page<OrderResponseDTO> findAllDTO(@NonNull Pageable pageable) {
        return orderRepository.findAll(pageable).map(this::mapOrderToDTO);
    }
    
    @Transactional(readOnly = true)
    public Page<OrderResponseDTO> findByUserIdDTO(UUID userId, @NonNull Pageable pageable) {
        return findByUserId(userId, pageable).map(this::mapOrderToDTO);
    }
    
    @Transactional(readOnly = true)
    public Page<OrderResponseDTO> findByStatusDTO(OrderStatus estado, @NonNull Pageable pageable) {
        return findByStatus(estado, pageable).map(this::mapOrderToDTO);
    }
    
    @Transactional
    public OrderResponseDTO updateOrderStatusDTO(Long orderId, OrderStatus newStatus) {
        Order order = updateOrderStatus(orderId, newStatus);
        return mapOrderToDTO(order);
    }
    
    @Transactional
    public OrderResponseDTO cancelOrderDTO(Long orderId) {
        Order order = cancelOrder(orderId);
        return mapOrderToDTO(order);
    }
    
    private OrderResponseDTO mapOrderToDTO(Order order) {
        List<OrderItemResponseDTO> items = order.getOrderItems().stream()
                .map(this::mapOrderItemToDTO)
                .collect(Collectors.toList());

        PaymentResponseDTO payment = null;
        if (order.getPayment() != null) {
            payment = DTOMapper.toPaymentResponse(order.getPayment());
            if (order.getPayment().getStripePaymentId() != null) {
                String clientSecret = paymentService.getClientSecret(order.getPayment().getStripePaymentId());
                payment.setClientSecret(clientSecret);
            }
        }
        return DTOMapper.toOrderResponse(order, items, payment);
    }
    
    private OrderItemResponseDTO mapOrderItemToDTO(OrderItem item) {
        Product product = item.getProduct();
        List<ProductImage> images = productImageRepository.findByProductIdOrderByOrdenAsc(product.getId());
        ProductImage mainImage = images.isEmpty() ? null : images.get(0);
        
        List<Review> reviews = reviewRepository.findByProduct(product);
        Double avgRating = reviews.stream()
                .mapToInt(Review::getRating)
                .average()
                .orElse(0.0);
        
        return DTOMapper.toOrderItemResponse(item, mainImage, avgRating);
    }

    /**
     * Progresa órdenes en estado PROCESANDO a ENVIADO
     * Se ejecuta automáticamente by scheduler cada 2 minutos
     */
    @Transactional
    public int progressProcessingToShipped() {
        logger.info("[OrderService] Buscando órdenes en estado PROCESANDO para cambiar a ENVIADO...");
        List<Order> processingOrders = orderRepository.findByEstado(OrderStatus.PROCESANDO);
        
        int count = 0;
        for (Order order : processingOrders) {
            order.setEstado(OrderStatus.ENVIADO);
            orderRepository.save(order);
            logger.info("[OrderService] Orden {} movida a ENVIADO", order.getId());
            count++;
        }
        return count;
    }

    /**
     * Progresa órdenes en estado ENVIADO a ENTREGADO
     * Se ejecuta automáticamente by scheduler cada 2 minutos
     */
    @Transactional
    public int progressShippedToDelivered() {
        logger.info("[OrderService] Buscando órdenes en estado ENVIADO para cambiar a ENTREGADO...");
        List<Order> shippedOrders = orderRepository.findByEstado(OrderStatus.ENVIADO);
        
        int count = 0;
        for (Order order : shippedOrders) {
            order.setEstado(OrderStatus.ENTREGADO);
            orderRepository.save(order);
            logger.info("[OrderService] Orden {} movida a ENTREGADO", order.getId());
            count++;
        }
        return count;
    }

    /**
     * Cambia una orden específica de PROCESANDO a ENVIADO (Endpoint admin)
     */
    @Transactional
    public Order shipOrder(Long orderId) {
        Order order = findById(orderId);
        
        if (order.getEstado() != OrderStatus.PROCESANDO) {
            throw new ValidationException(
                "La orden no puede ser enviada. Estado actual: " + order.getEstado().getValue() +
                ". Solo se pueden enviar órdenes en estado PROCESANDO."
            );
        }
        
        order.setEstado(OrderStatus.ENVIADO);
        Order updated = orderRepository.save(order);
        logger.info("[OrderService] Orden {} cambió a ENVIADO por acción admin", orderId);
        return updated;
    }

    /**
     * Cambia una orden específica de PROCESANDO a ENVIADO (DTO version)
     */
    @Transactional
    public OrderResponseDTO shipOrderDTO(Long orderId) {
        Order order = shipOrder(orderId);
        return mapOrderToDTO(order);
    }

    /**
     * Cambia una orden específica de ENVIADO a ENTREGADO (Endpoint admin)
     */
    @Transactional
    public Order deliverOrder(Long orderId) {
        Order order = findById(orderId);
        
        if (order.getEstado() != OrderStatus.ENVIADO) {
            throw new ValidationException(
                "La orden no puede ser entregada. Estado actual: " + order.getEstado().getValue() +
                ". Solo se pueden entregar órdenes en estado ENVIADO."
            );
        }
        
        order.setEstado(OrderStatus.ENTREGADO);
        Order updated = orderRepository.save(order);
        logger.info("[OrderService] Orden {} cambió a ENTREGADO por acción admin", orderId);
        return updated;
    }

    /**
     * Cambia una orden específica de ENVIADO a ENTREGADO (DTO version)
     */
    @Transactional
    public OrderResponseDTO deliverOrderDTO(Long orderId) {
        Order order = deliverOrder(orderId);
        return mapOrderToDTO(order);
    }
}
