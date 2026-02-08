package com.laptophub.backend.service;


import com.laptophub.backend.model.Product;
import com.laptophub.backend.repository.ProductRepository;
import com.laptophub.backend.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProductService {
    
    private final ProductRepository productRepository;
    
    @Transactional(readOnly = true)
    @SuppressWarnings("null")
    public Page<Product> findAll(Pageable pageable) {
        return productRepository.findAll(pageable);
    }
    
    @Transactional(readOnly = true)
    @SuppressWarnings("null")
    public Product findById(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado con id: " + id));
    }
    
    @Transactional(readOnly = true)
    public Page<Product> searchByName(String nombre, Pageable pageable) {
        return productRepository.findByNombreContainingIgnoreCase(nombre, pageable);
    }
    
    @Transactional(readOnly = true)
    public Page<Product> findByBrand(String marca, Pageable pageable) {
        return productRepository.findByMarca(marca, pageable);
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
        
        return productRepository.save(existingProduct);
    }
    
    @Transactional
    @SuppressWarnings("null")
    public void deleteProduct(Long id) {
        if (!productRepository.existsById(id)) {
            throw new ResourceNotFoundException("Producto no encontrado con id: " + id);
        }
        productRepository.deleteById(id);
    }
}
