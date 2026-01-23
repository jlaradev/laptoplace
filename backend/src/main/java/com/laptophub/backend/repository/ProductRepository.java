package com.laptophub.backend.repository;

import com.laptophub.backend.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    List<Product> findByNombreContainingIgnoreCase(String nombre);
    List<Product> findByMarca(String marca);
}
