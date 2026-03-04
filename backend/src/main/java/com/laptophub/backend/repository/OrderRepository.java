package com.laptophub.backend.repository;

import com.laptophub.backend.model.Order;
import com.laptophub.backend.model.OrderStatus;
import com.laptophub.backend.model.User;
import com.laptophub.backend.dto.ReviewableProductDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import jakarta.persistence.LockModeType;

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

    /**
     * Bloquea la orden para evitar race conditions al expirar y restaurar stock.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM Order o WHERE o.id = :id")
    Optional<Order> findByIdWithLock(@Param("id") Long id);

    /**
     * Verifica si un usuario ha comprado un producto específico (orden ENTREGADA).
     * @param userId ID del usuario
     * @param productId ID del producto
     * @return true si el usuario ha comprado el producto, false en caso contrario
     */
    @Query("SELECT COUNT(oi) > 0 FROM Order o " +
           "JOIN o.orderItems oi " +
           "WHERE o.user.id = :userId AND oi.product.id = :productId AND o.estado = 'ENTREGADO'")
    boolean hasUserPurchasedProduct(@Param("userId") UUID userId, @Param("productId") Long productId);

    /**
     * Obtiene los productos que un usuario puede reseñar (asociados a órdenes ENTREGADAS).
     * Incluye información sobre si el producto ya ha sido reseñado.
     * @param userId ID del usuario
     * @return Lista de productos reseñables con estado de reseña
     */
    @Query("SELECT new com.laptophub.backend.dto.ReviewableProductDTO(" +
           "p.id, p.nombre, p.precio, " +
           "CASE WHEN (SELECT COUNT(r) FROM Review r WHERE r.product.id = p.id AND r.user.id = :userId) > 0 THEN true ELSE false END, " +
           "(SELECT r.id FROM Review r WHERE r.product.id = p.id AND r.user.id = :userId)) " +
           "FROM Order o " +
           "JOIN o.orderItems oi " +
           "JOIN oi.product p " +
           "WHERE o.user.id = :userId AND o.estado = 'ENTREGADO' " +
           "GROUP BY p.id, p.nombre, p.precio")
    List<ReviewableProductDTO> getReviewableProducts(@Param("userId") UUID userId);

    /**
     * Obtiene todas las órdenes de un usuario que estén en estados de procesamiento.
     * Retorna órdenes en estado PROCESANDO, ENVIADO o ENTREGADO.
     * @param userId ID del usuario
     * @param pageable Información de paginación
     * @return Página de órdenes en los estados especificados
     */
    @EntityGraph(attributePaths = {"orderItems", "orderItems.product", "payment"})
    @Query("SELECT o FROM Order o WHERE o.user.id = :userId AND o.estado IN ('PROCESANDO', 'ENVIADO', 'ENTREGADO') ORDER BY o.createdAt DESC")
    Page<Order> findUserOrdersByActiveStatuses(@Param("userId") UUID userId, Pageable pageable);
}
