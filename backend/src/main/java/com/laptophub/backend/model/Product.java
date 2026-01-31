package com.laptophub.backend.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "products")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String nombre;
    
    @Column(columnDefinition = "TEXT")
    private String descripcion;
    
    @Column(nullable = false)
    private BigDecimal precio;
    
    @Column(nullable = false)
    private Integer stock;
    
    private String marca;
    
    private String procesador;
    
    private Integer ram;
    
    private Integer almacenamiento;
    
    private String pantalla;
    
    private String gpu;
    
    private java.math.BigDecimal peso;
    
    @Column(length = 500)
    private String imagenUrl;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CartItem> cartItems;
    
    @OneToMany(mappedBy = "product")
    private List<OrderItem> orderItems;
    
    @OneToMany(mappedBy = "product")
    private List<Review> reviews;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
