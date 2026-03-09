package com.laptophub.backend.repository;

import com.laptophub.backend.model.Brand;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BrandRepository extends JpaRepository<Brand, Long> {
    Optional<Brand> findByNombre(String nombre);
    Optional<Brand> findByNombreAndDeletedAtIsNull(String nombre);
    Page<Brand> findAllByDeletedAtIsNull(Pageable pageable);
    Page<Brand> findAllByDeletedAtIsNotNull(Pageable pageable);
}
