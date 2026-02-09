package com.laptophub.backend.repository;

import com.laptophub.backend.model.Order;
import com.laptophub.backend.model.OrderStatus;
import com.laptophub.backend.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    @EntityGraph(attributePaths = {"orderItems", "orderItems.product", "payment"})
    @Override
    @NonNull Optional<Order> findById(@NonNull Long id);
    
    @EntityGraph(attributePaths = {"orderItems", "orderItems.product", "payment"})
    Page<Order> findByUser(User user, Pageable pageable);
    
    @EntityGraph(attributePaths = {"orderItems", "orderItems.product", "payment"})
    List<Order> findByUser(User user);
    
    @EntityGraph(attributePaths = {"orderItems", "orderItems.product", "payment"})
    Page<Order> findByEstado(OrderStatus estado, Pageable pageable);
    
    @EntityGraph(attributePaths = {"orderItems", "orderItems.product", "payment"})
    List<Order> findByEstado(OrderStatus estado);
    
    List<Order> findByEstadoAndExpiresAtBefore(OrderStatus estado, LocalDateTime expiresAt);
}
