package com.laptophub.backend.dto;

import com.laptophub.backend.model.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Mapper para convertir entre entidades y DTOs
 */
public class DTOMapper {

    // USER
    public static UserResponseDTO toUserResponse(User user) {
        return UserResponseDTO.builder()
                .id(user.getId())
                .email(user.getEmail())
                .nombre(user.getNombre())
                .apellido(user.getApellido())
                .telefono(user.getTelefono())
                .direccion(user.getDireccion())
                .createdAt(user.getCreatedAt())
                .build();
    }

    public static User toUser(UserRegisterDTO dto) {
        return User.builder()
                .email(dto.getEmail())
                .password(dto.getPassword())
                .nombre(dto.getNombre())
                .apellido(dto.getApellido())
                .telefono(dto.getTelefono())
                .direccion(dto.getDireccion())
                .build();
    }

    // PRODUCT IMAGE
    public static ProductImageDTO toProductImageDTO(ProductImage image) {
        return ProductImageDTO.builder()
                .id(image.getId())
                .url(image.getUrl())
                .orden(image.getOrden())
                .descripcion(image.getDescripcion())
                .build();
    }

    // REVIEW
    public static ReviewResponseDTO toReviewResponse(Review review) {
        return ReviewResponseDTO.builder()
                .id(review.getId())
                .productId(review.getProduct().getId())
                .userId(review.getUser().getId())
                .userNombre(review.getUser().getNombre() + " " + review.getUser().getApellido())
                .rating(review.getRating())
                .comentario(review.getComentario())
                .createdAt(review.getCreatedAt())
                .build();
    }

    // BRAND
    public static BrandResponseDTO toBrandResponse(Brand brand) {
        if (brand == null) return null;
        return BrandResponseDTO.builder()
                .id(brand.getId())
                .nombre(brand.getNombre())
                .descripcion(brand.getDescripcion())
                .imageUrl(brand.getImageUrl())
                .createdAt(brand.getCreatedAt())
                .build();
    }

    // PRODUCT
    public static ProductResponseDTO toProductResponse(Product product, List<ProductImage> images, 
                                                       List<Review> reviews, Double avgRating) {
        return ProductResponseDTO.builder()
                .id(product.getId())
                .nombre(product.getNombre())
                .descripcion(product.getDescripcion())
                .precio(product.getPrecio())
                .stock(product.getStock())
                .brand(toBrandResponse(product.getBrand()))
                .procesador(product.getProcesador())
                .ram(product.getRam())
                .almacenamiento(product.getAlmacenamiento())
                .pantalla(product.getPantalla())
                .gpu(product.getGpu())
                .peso(product.getPeso())
                .imagenes(images.stream().map(DTOMapper::toProductImageDTO).collect(Collectors.toList()))
                .resenas(reviews.stream().map(DTOMapper::toReviewResponse).collect(Collectors.toList()))
                .promedioRating(avgRating)
                .createdAt(product.getCreatedAt())
                .build();
    }

    public static ProductListDTO toProductListDTO(Product product, ProductImage mainImage, Double avgRating) {
        return ProductListDTO.builder()
                .id(product.getId())
                .nombre(product.getNombre())
                .precio(product.getPrecio())
                .stock(product.getStock())
                .brand(product.getBrand() != null ? toBrandResponse(product.getBrand()) : null)
                .imagenPrincipal(mainImage != null ? toProductImageDTO(mainImage) : null)
                .promedioRating(avgRating)
                .build();
    }

    public static Product toProduct(ProductCreateDTO dto, Brand brand) {
        return Product.builder()
                .nombre(dto.getNombre())
                .descripcion(dto.getDescripcion())
                .precio(dto.getPrecio())
                .stock(dto.getStock())
                .brand(brand)
                .procesador(dto.getProcesador())
                .ram(dto.getRam())
                .almacenamiento(dto.getAlmacenamiento())
                .pantalla(dto.getPantalla())
                .gpu(dto.getGpu())
                .peso(dto.getPeso())
                .build();
    }

    // CART
    public static CartItemResponseDTO toCartItemResponse(CartItem item, ProductImage mainImage, Double avgRating) {
        return CartItemResponseDTO.builder()
                .id(item.getId())
                .product(toProductListDTO(item.getProduct(), mainImage, avgRating))
                .cantidad(item.getCantidad())
                .build();
    }

    public static CartResponseDTO toCartResponse(Cart cart, List<CartItemResponseDTO> items, 
                                                 java.math.BigDecimal total) {
        return CartResponseDTO.builder()
                .id(cart.getId())
                .userId(cart.getUser().getId())
                .items(items)
                .total(total)
                .createdAt(cart.getCreatedAt())
                .build();
    }

    // ORDER
    public static OrderItemResponseDTO toOrderItemResponse(OrderItem item, ProductImage mainImage, Double avgRating) {
        return OrderItemResponseDTO.builder()
                .id(item.getId())
                .product(toProductListDTO(item.getProduct(), mainImage, avgRating))
                .cantidad(item.getCantidad())
                .precioUnitario(item.getPrecioUnitario())
                .build();
    }

    public static OrderResponseDTO toOrderResponse(Order order, List<OrderItemResponseDTO> items, 
                                                   PaymentResponseDTO payment) {
        return OrderResponseDTO.builder()
                .id(order.getId())
                .userId(order.getUser().getId())
                .total(order.getTotal())
                .estado(order.getEstado())
                .direccionEnvio(order.getDireccionEnvio())
                .expiresAt(order.getExpiresAt())
                .items(items)
                .payment(payment)
                .createdAt(order.getCreatedAt())
                .build();
    }

    // PAYMENT
    public static PaymentResponseDTO toPaymentResponse(Payment payment) {
        return PaymentResponseDTO.builder()
                .id(payment.getId())
                .orderId(payment.getOrder().getId())
                .stripePaymentId(payment.getStripePaymentId())
                .clientSecret(null) // Se setea en PaymentService
                .monto(payment.getMonto())
                .estado(payment.getEstado())
                .createdAt(payment.getCreatedAt())
                .build();
    }
}
