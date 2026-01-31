package com.laptophub.backend.service;


import com.laptophub.backend.model.Product;
import com.laptophub.backend.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductService {
    
    private final ProductRepository productRepository;
    
    @Transactional(readOnly = true)
    public List<Product> findAll() {
        return productRepository.findAll();
    }
    
    @Transactional(readOnly = true)
    @SuppressWarnings("null")
    public Product findById(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Producto no encontrado con id: " + id));
    }
    
    @Transactional(readOnly = true)
    public List<Product> searchByName(String nombre) {
        return productRepository.findByNombreContainingIgnoreCase(nombre);
    }
    
    @Transactional(readOnly = true)
    public List<Product> findByBrand(String marca) {
        return productRepository.findByMarca(marca);
    }
    
    @Transactional
    @SuppressWarnings("null")
    public Product createProduct(Product product) {
        return productRepository.save(product);
    }
    
    @Transactional
    @SuppressWarnings("null")
    public Product updateProduct(Long id, Product updatedProduct) {
        Product existingProduct = findById(id);
        
        if (updatedProduct.getNombre() != null) existingProduct.setNombre(updatedProduct.getNombre());
        if (updatedProduct.getDescripcion() != null) existingProduct.setDescripcion(updatedProduct.getDescripcion());
        if (updatedProduct.getPrecio() != null) existingProduct.setPrecio(updatedProduct.getPrecio());
        if (updatedProduct.getStock() != null) existingProduct.setStock(updatedProduct.getStock());
        if (updatedProduct.getMarca() != null) existingProduct.setMarca(updatedProduct.getMarca());
        if (updatedProduct.getProcesador() != null) existingProduct.setProcesador(updatedProduct.getProcesador());
        if (updatedProduct.getRam() != null) existingProduct.setRam(updatedProduct.getRam());
        if (updatedProduct.getAlmacenamiento() != null) existingProduct.setAlmacenamiento(updatedProduct.getAlmacenamiento());
        if (updatedProduct.getPantalla() != null) existingProduct.setPantalla(updatedProduct.getPantalla());
        if (updatedProduct.getGpu() != null) existingProduct.setGpu(updatedProduct.getGpu());
        if (updatedProduct.getPeso() != null) existingProduct.setPeso(updatedProduct.getPeso());
        if (updatedProduct.getImagenUrl() != null) existingProduct.setImagenUrl(updatedProduct.getImagenUrl());
        
        return productRepository.save(existingProduct);
    }
    
    @Transactional
    @SuppressWarnings("null")
    public void deleteProduct(Long id) {
        if (!productRepository.existsById(id)) {
            throw new RuntimeException("Producto no encontrado con id: " + id);
        }
        productRepository.deleteById(id);
    }
}
