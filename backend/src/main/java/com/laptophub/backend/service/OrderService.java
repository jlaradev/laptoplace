package com.laptophub.backend.service;


import com.laptophub.backend.model.*;
import com.laptophub.backend.model.OrderStatus;
import com.laptophub.backend.model.PaymentStatus;
import com.laptophub.backend.repository.OrderItemRepository;
import com.laptophub.backend.repository.OrderRepository;
import com.laptophub.backend.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderService {
    
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductRepository productRepository;
    private final UserService userService;
    private final CartService cartService;
    private final PaymentService paymentService;
    
    @Transactional
    @SuppressWarnings("null")
    public Order createOrderFromCart(UUID userId, String direccionEnvio) {
        User user = userService.findById(userId);
        Cart cart = cartService.getOrCreateCart(userId);
        
        if (cart.getItems().isEmpty()) {
            throw new RuntimeException("El carrito está vacío");
        }
        
        for (CartItem cartItem : cart.getItems()) {
            Long productId = cartItem.getProduct().getId();
            Product product = productRepository.findByIdWithLock(productId)
                .orElseThrow(() -> new RuntimeException("Producto no encontrado con id: " + productId));
            
            if (product.getStock() < cartItem.getCantidad()) {
                throw new RuntimeException("Stock insuficiente para producto " + product.getId());
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
            paymentService.createPayment(savedOrder, total);
        } catch (com.stripe.exception.StripeException e) {
            throw new RuntimeException("Error al procesar pago con Stripe: " + e.getMessage(), e);
        }
        
        cartService.clearCart(userId);
        
        return savedOrder;
    }
    
    @Transactional(readOnly = true)
    @SuppressWarnings("null")
    public Order findById(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Orden no encontrada con id: " + orderId));
    }
    
    @Transactional(readOnly = true)
    public List<Order> findByUserId(UUID userId) {
        User user = userService.findById(userId);
        return orderRepository.findByUser(user);
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
        
        boolean canCancel = order.getEstado() == OrderStatus.PENDIENTE || 
                           order.getEstado() == OrderStatus.PENDIENTE_PAGO;
        
        if (!canCancel) {
            throw new RuntimeException(
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
        
        if (expired.isEmpty()) {
            return 0;
        }
        
        for (Order order : expired) {
            restoreOrderStock(order);
            order.setEstado(OrderStatus.EXPIRADO);
            
            if (order.getPayment() != null) {
                order.getPayment().setEstado(PaymentStatus.FALLIDO);
            }
            
            orderRepository.save(order);
        }
        
        return expired.size();
    }
    
    @Transactional(readOnly = true)
    public List<Order> findAll() {
        return orderRepository.findAll();
    }
    
    private void restoreOrderStock(Order order) {
        for (OrderItem item : order.getOrderItems()) {
            Product product = item.getProduct();
            product.setStock(product.getStock() + item.getCantidad());
            productRepository.save(product);
        }
    }
}
