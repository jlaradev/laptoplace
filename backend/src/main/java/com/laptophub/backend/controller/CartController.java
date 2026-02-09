package com.laptophub.backend.controller;

import com.laptophub.backend.dto.AddToCartDTO;
import com.laptophub.backend.dto.CartResponseDTO;
import com.laptophub.backend.dto.UpdateCartItemDTO;
import com.laptophub.backend.service.CartService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.UUID;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    @GetMapping("/user/{userId}")
    public CartResponseDTO getCartByUser(@PathVariable UUID userId) {
        return cartService.getCartByUserIdDTO(userId);
    }

    @PostMapping("/user/{userId}/items")
    public CartResponseDTO addToCart(
            @PathVariable UUID userId,
            @Valid @RequestBody AddToCartDTO dto
    ) {
        return cartService.addToCartDTO(userId, dto);
    }

    @PutMapping("/items/{cartItemId}")
    public CartResponseDTO updateQuantity(
            @PathVariable Long cartItemId,
            @Valid @RequestBody UpdateCartItemDTO dto
    ) {
        return cartService.updateQuantityDTO(cartItemId, dto);
    }

    @DeleteMapping("/items/{cartItemId}")
    public void removeFromCart(@PathVariable Long cartItemId) {
        cartService.removeFromCart(cartItemId);
    }

    @DeleteMapping("/user/{userId}/clear")
    public void clearCart(@PathVariable UUID userId) {
        cartService.clearCart(userId);
    }

    @GetMapping("/{cartId}/total")
    public BigDecimal calculateTotal(@PathVariable Long cartId) {
        return cartService.calculateTotal(cartId);
    }
}