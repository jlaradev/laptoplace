package com.laptophub.backend.service;

import com.laptophub.backend.model.Cart;
import com.laptophub.backend.model.CartItem;
import com.laptophub.backend.model.Product;
import com.laptophub.backend.model.User;
import com.laptophub.backend.repository.CartItemRepository;
import com.laptophub.backend.repository.CartRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;

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
    
    @Transactional
    @SuppressWarnings("null")
    public Cart getOrCreateCart(Long userId) {
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
    public Cart addToCart(Long userId, Long productId, Integer cantidad) {
        if (cantidad <= 0) {
            throw new RuntimeException("La cantidad debe ser mayor a 0");
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
            cartItemRepository.save(newItem);
        }
        
        return cart;
    }
    
    @Transactional
    @SuppressWarnings("null")
    public CartItem updateQuantity(Long cartItemId, Integer newQuantity) {
        if (newQuantity <= 0) {
            throw new RuntimeException("La cantidad debe ser mayor a 0. Use removeFromCart para eliminar.");
        }
        
        CartItem item = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new RuntimeException("CartItem no encontrado con id: " + cartItemId));
        
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
    public void clearCart(Long userId) {
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
    public Cart getCartByUserId(Long userId) {
        return getOrCreateCart(userId);
    }
}
