package com.laptophub.backend.service;

import com.laptophub.backend.dto.*;
import com.laptophub.backend.model.*;
import com.laptophub.backend.repository.CartItemRepository;
import com.laptophub.backend.repository.CartRepository;
import com.laptophub.backend.repository.ProductImageRepository;
import com.laptophub.backend.repository.ReviewRepository;
import com.laptophub.backend.exception.ResourceNotFoundException;
import com.laptophub.backend.exception.ValidationException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Servicio para gestionar el carrito de compras.
 * 
 * - Agregar al carrito NO reserva stock
 * - El carrito persiste indefinidamente
 * - Stock se verifica SOLO al hacer checkout
 * - Si no hay stock al checkout, se notifica al usuario
 * 
 */
@Service
@RequiredArgsConstructor
public class CartService {
    
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final UserService userService;
    private final ProductService productService;
    private final ProductImageRepository productImageRepository;
    private final ReviewRepository reviewRepository;
    
    @Transactional
    @SuppressWarnings("null")
    public Cart getOrCreateCart(UUID userId) {
        User user = userService.findById(userId);
        Optional<Cart> existingCart = cartRepository.findByUser(user);
        
        if (existingCart.isPresent()) {
            return existingCart.get();
        }
        
        Cart newCart = Cart.builder()
                .user(user)
                .build();
        return cartRepository.save(newCart);
    }
    
    @Transactional
    @SuppressWarnings("null")
    public Cart addToCart(UUID userId, Long productId, Integer cantidad) {
        if (cantidad <= 0) {
            throw new ValidationException("La cantidad debe ser mayor a 0");
        }
        
        Cart cart = getOrCreateCart(userId);
        Product product = productService.findById(productId);
        
        Optional<CartItem> existingItem = cart.getItems().stream()
                .filter(item -> item.getProduct().getId().equals(productId))
                .findFirst();
        
        if (existingItem.isPresent()) {
            CartItem item = existingItem.get();
            item.setCantidad(item.getCantidad() + cantidad);
            cartItemRepository.save(item);
        } else {
            CartItem newItem = CartItem.builder()
                    .cart(cart)
                    .product(product)
                    .cantidad(cantidad)
                    .build();
            CartItem savedItem = cartItemRepository.save(newItem);
            cart.getItems().add(savedItem);
        }
        
        return cart;
    }
    
    @Transactional
    @SuppressWarnings("null")
    public CartItem updateQuantity(Long cartItemId, Integer newQuantity) {
        if (newQuantity <= 0) {
            throw new ValidationException("La cantidad debe ser mayor a 0. Use removeFromCart para eliminar.");
        }
        
        CartItem item = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new ResourceNotFoundException("CartItem no encontrado con id: " + cartItemId));
        
        item.setCantidad(newQuantity);
        return cartItemRepository.save(item);
    }
    
    @Transactional
    @SuppressWarnings("null")
    public void removeFromCart(Long cartItemId) {
        CartItem item = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new RuntimeException("CartItem no encontrado con id: " + cartItemId));
        cartItemRepository.delete(item);
    }
    
    @Transactional
    @SuppressWarnings("null")
    public void clearCart(UUID userId) {
        Cart cart = getOrCreateCart(userId);
        cartItemRepository.deleteAll(cart.getItems());
    }
    
    @Transactional(readOnly = true)
    @SuppressWarnings("null")
    public BigDecimal calculateTotal(Long cartId) {
        Cart cart = cartRepository.findById(cartId)
                .orElseThrow(() -> new RuntimeException("Cart no encontrado con id: " + cartId));
        
        return cart.getItems().stream()
                .map(item -> item.getProduct().getPrecio()
                        .multiply(BigDecimal.valueOf(item.getCantidad())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
    
    @Transactional
    public Cart getCartByUserId(UUID userId) {
        return getOrCreateCart(userId);
    }
    
    // Métodos que retornan DTOs
    
    @Transactional
    public CartResponseDTO getCartByUserIdDTO(UUID userId) {
        Cart cart = getOrCreateCart(userId);
        return mapCartToDTO(cart);
    }
    
    @Transactional
    public CartResponseDTO addToCartDTO(UUID userId, AddToCartDTO dto) {
        Cart cart = addToCart(userId, dto.getProductId(), dto.getCantidad());
        return mapCartToDTO(cart);
    }
    
    @Transactional
    public CartResponseDTO updateQuantityDTO(Long cartItemId, UpdateCartItemDTO dto) {
        CartItem item = updateQuantity(cartItemId, dto.getCantidad());
        Cart cart = item.getCart();
        return mapCartToDTO(cart);
    }
    
    private CartResponseDTO mapCartToDTO(Cart cart) {
        List<CartItemResponseDTO> items = cart.getItems().stream()
                .map(this::mapCartItemToDTO)
                .collect(Collectors.toList());
        
        BigDecimal total = calculateTotal(cart.getId());
        
        return DTOMapper.toCartResponse(cart, items, total);
    }
    
    private CartItemResponseDTO mapCartItemToDTO(CartItem item) {
        Product product = item.getProduct();
        List<ProductImage> images = productImageRepository.findByProductIdOrderByOrdenAsc(product.getId());
        ProductImage mainImage = images.isEmpty() ? null : images.get(0);
        
        List<Review> reviews = reviewRepository.findByProduct(product);
        Double avgRating = reviews.stream()
                .mapToInt(Review::getRating)
                .average()
                .orElse(0.0);
        
        return DTOMapper.toCartItemResponse(item, mainImage, avgRating);
    }
}
