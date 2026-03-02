package com.laptophub.backend.repository;

import com.laptophub.backend.model.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    Page<Product> findByNombreContainingIgnoreCase(String nombre, Pageable pageable);
    Page<Product> findByBrand_Id(Long brandId, Pageable pageable);
    Page<Product> findByBrand_Nombre(String brandNombre, Pageable pageable);
    
    /**
     * Bloquea el producto para evitar race conditions al actualizar stock.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Product p WHERE p.id = :id")
    Optional<Product> findByIdWithLock(@Param("id") Long id);
    
    /**
     * Búsqueda unificada ordenada por NOMBRE (A-Z)
     */
    @Query("""
        SELECT p FROM Product p
        WHERE 
            (LOWER(p.nombre) LIKE LOWER(CONCAT('%', :nombre, '%')) OR :nombre IS NULL)
            AND (p.brand.id = :brandId OR :brandId IS NULL)
            AND (p.stock > 0 OR :includeOutOfStock = true)
        ORDER BY p.nombre ASC
        """)
    Page<Product> searchByNameAsc(
        @Param("nombre") String nombre,
        @Param("brandId") Long brandId,
        @Param("includeOutOfStock") boolean includeOutOfStock,
        Pageable pageable
    );
    
    @Query("""
        SELECT p FROM Product p
        WHERE 
            (LOWER(p.nombre) LIKE LOWER(CONCAT('%', :nombre, '%')) OR :nombre IS NULL)
            AND (p.brand.id = :brandId OR :brandId IS NULL)
            AND (p.stock > 0 OR :includeOutOfStock = true)
        ORDER BY p.nombre DESC
        """)
    Page<Product> searchByNameDesc(
        @Param("nombre") String nombre,
        @Param("brandId") Long brandId,
        @Param("includeOutOfStock") boolean includeOutOfStock,
        Pageable pageable
    );
    
    /**
     * Búsqueda ordenada por PRECIO
     */
    @Query("""
        SELECT p FROM Product p
        WHERE 
            (LOWER(p.nombre) LIKE LOWER(CONCAT('%', :nombre, '%')) OR :nombre IS NULL)
            AND (p.brand.id = :brandId OR :brandId IS NULL)
            AND (p.stock > 0 OR :includeOutOfStock = true)
        ORDER BY p.precio ASC
        """)
    Page<Product> searchByPriceAsc(
        @Param("nombre") String nombre,
        @Param("brandId") Long brandId,
        @Param("includeOutOfStock") boolean includeOutOfStock,
        Pageable pageable
    );
    
    @Query("""
        SELECT p FROM Product p
        WHERE 
            (LOWER(p.nombre) LIKE LOWER(CONCAT('%', :nombre, '%')) OR :nombre IS NULL)
            AND (p.brand.id = :brandId OR :brandId IS NULL)
            AND (p.stock > 0 OR :includeOutOfStock = true)
        ORDER BY p.precio DESC
        """)
    Page<Product> searchByPriceDesc(
        @Param("nombre") String nombre,
        @Param("brandId") Long brandId,
        @Param("includeOutOfStock") boolean includeOutOfStock,
        Pageable pageable
    );
    
    /**
     * Búsqueda ordenada por FECHA DE CREACIÓN (recientes primero)
     */
    @Query("""
        SELECT p FROM Product p
        WHERE 
            (LOWER(p.nombre) LIKE LOWER(CONCAT('%', :nombre, '%')) OR :nombre IS NULL)
            AND (p.brand.id = :brandId OR :brandId IS NULL)
            AND (p.stock > 0 OR :includeOutOfStock = true)
        ORDER BY p.createdAt DESC
        """)
    Page<Product> searchByCreatedAtDesc(
        @Param("nombre") String nombre,
        @Param("brandId") Long brandId,
        @Param("includeOutOfStock") boolean includeOutOfStock,
        Pageable pageable
    );
    
    @Query("""
        SELECT p FROM Product p
        WHERE 
            (LOWER(p.nombre) LIKE LOWER(CONCAT('%', :nombre, '%')) OR :nombre IS NULL)
            AND (p.brand.id = :brandId OR :brandId IS NULL)
            AND (p.stock > 0 OR :includeOutOfStock = true)
        ORDER BY p.createdAt ASC
        """)
    Page<Product> searchByCreatedAtAsc(
        @Param("nombre") String nombre,
        @Param("brandId") Long brandId,
        @Param("includeOutOfStock") boolean includeOutOfStock,
        Pageable pageable
    );
    
    /**
     * Búsqueda ordenada por CALIFICACIÓN PROMEDIO (mejores primero)
     * Nota: Esta query devuelve Object[] complejos, así que el ordenamiento
     * por rating se maneja en memoria en ProductService
     */
    @Deprecated
    @Query(value = """
        SELECT p, COALESCE(AVG(CAST(r.rating AS DOUBLE)), 0.0) as avgRating
        FROM Product p
        LEFT JOIN p.reviews r
        WHERE 
            (LOWER(p.nombre) LIKE LOWER(CONCAT('%', :nombre, '%')) OR :nombre IS NULL)
            AND (p.brand.id = :brandId OR :brandId IS NULL)
            AND (p.stock > 0 OR :includeOutOfStock = true)
        GROUP BY p.id
        ORDER BY avgRating DESC, p.createdAt DESC
        """)
    Page<Object[]> searchByRatingDesc(
        @Param("nombre") String nombre,
        @Param("brandId") Long brandId,
        @Param("includeOutOfStock") boolean includeOutOfStock,
        Pageable pageable
    );
    
    @Deprecated
    @Query(value = """
        SELECT p, COALESCE(AVG(CAST(r.rating AS DOUBLE)), 0.0) as avgRating
        FROM Product p
        LEFT JOIN p.reviews r
        WHERE 
            (LOWER(p.nombre) LIKE LOWER(CONCAT('%', :nombre, '%')) OR :nombre IS NULL)
            AND (p.brand.id = :brandId OR :brandId IS NULL)
            AND (p.stock > 0 OR :includeOutOfStock = true)
        GROUP BY p.id
        ORDER BY avgRating ASC, p.createdAt DESC
        """)
    Page<Object[]> searchByRatingAsc(
        @Param("nombre") String nombre,
        @Param("brandId") Long brandId,
        @Param("includeOutOfStock") boolean includeOutOfStock,
        Pageable pageable
    );
}
