package com.laptophub.backend;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.laptophub.backend.dto.AddToCartDTO;
import com.laptophub.backend.model.Cart;
import com.laptophub.backend.model.Product;
import com.laptophub.backend.model.ProductImage;
import com.laptophub.backend.model.User;
import com.laptophub.backend.repository.CartItemRepository;
import com.laptophub.backend.repository.CartRepository;
import com.laptophub.backend.repository.ProductImageRepository;
import com.laptophub.backend.repository.ProductRepository;
import com.laptophub.backend.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;

/**
 * Tests de endpoints CRUD para Cart
 * Ejecuta las pruebas en orden específico para mantener consistencia
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@SuppressWarnings("null")
public class CartControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private CartItemRepository cartItemRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductRepository productRepository;

        @Autowired
        private ProductImageRepository productImageRepository;

    private static String userId;
    private static String productId;
    private static String cartItemId;

    /**
     * Limpia la base de datos una sola vez antes de todos los tests
     */
    @BeforeAll
    public static void setUpDatabase() {
        // La limpieza ocurre una sola vez al inicio
    }

    /**
     * TEST 1: Configuración - Crear usuario y producto de prueba
     */
    @Test
    @Order(1)
    @SuppressWarnings("null")
    public void test1_SetupUserAndProduct() throws Exception {
        // Limpiar BD solo antes del primer test
        cartItemRepository.deleteAll();
        cartRepository.deleteAll();
        
        System.out.println("\n=== TEST 1: Configuración - Crear usuario y producto ===");
        
        // Crear usuario de prueba
        User testUser = User.builder()
                .email("cart.test@laptophub.com")
                .password("password123")
                .nombre("Test")
                .apellido("Cart")
                .telefono("555-0001")
                .direccion("Cart Test Address")
                .build();
        
        User savedUser = userRepository.save(testUser);
        userId = savedUser.getId().toString();
        
        // Crear producto de prueba
        Product testProduct = Product.builder()
                .nombre("Laptop Lenovo ThinkPad")
                .descripcion("Laptop empresarial de alta calidad")
                .precio(new BigDecimal("999.99"))
                .stock(50)
                .marca("Lenovo")
                .procesador("Intel Core i5-1135G7")
                .ram(16)
                .almacenamiento(512)
                .pantalla("14 pulgadas FHD")
                .gpu("Intel Iris Xe")
                .peso(new BigDecimal("1.4"))
                .build();
        
        Product savedProduct = productRepository.save(testProduct);
        productId = savedProduct.getId().toString();
        
        ProductImage mainImage = ProductImage.builder()
                .url("https://example.com/lenovo-thinkpad.jpg")
                .orden(0)
                .descripcion("Imagen principal")
                .product(savedProduct)
                .build();
        productImageRepository.save(mainImage);
        
        System.out.println("✅ TEST 1 PASÓ: Usuario creado con ID: " + userId);
        System.out.println("✅ Producto creado con ID: " + productId + "\n");
    }

    /**
     * TEST 2: Obtener o crear carrito por userId (GET /api/cart/user/{userId})
     */
    @Test
    @Order(2)
    public void test2_GetOrCreateCartByUser() throws Exception {
        System.out.println("\n=== TEST 2: Obtener/crear carrito por usuario (GET /api/cart/user/{userId}) ===");
        
        mockMvc.perform(get("/api/cart/user/" + userId))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.userId").value(userId))
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.total").exists());
        
        System.out.println("✅ TEST 2 PASÓ: Carrito obtenido/creado para el usuario\n");
    }

    /**
     * TEST 3: Agregar producto al carrito (POST /api/cart/user/{userId}/items)
     */
    @Test
    @Order(3)
    public void test3_AddItemToCart() throws Exception {
        System.out.println("\n=== TEST 3: Agregar producto al carrito (POST /api/cart/user/{userId}/items) ===");
        
        AddToCartDTO addToCart = AddToCartDTO.builder()
                .productId(Long.parseLong(productId))
                .cantidad(2)
                .build();
        
        mockMvc.perform(post("/api/cart/user/" + userId + "/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(addToCart)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.items[0].product.id").value(productId))
                .andExpect(jsonPath("$.items[0].cantidad").value(2));
        
        // Buscar el CartItem recién creado
        cartItemId = cartItemRepository.findAll().stream()
                .filter(ci -> ci.getProduct().getId().toString().equals(productId))
                .findFirst()
                .orElseThrow()
                .getId().toString();
        
        System.out.println("✅ TEST 3 PASÓ: Producto agregado al carrito con CartItem ID: " + cartItemId + "\n");
    }

    /**
     * TEST 4: Actualizar cantidad de un item (PUT /api/cart/items/{cartItemId})
     */
    @Test
    @Order(4)
    public void test4_UpdateItemQuantity() throws Exception {
        System.out.println("\n=== TEST 4: Actualizar cantidad de item (PUT /api/cart/items/{cartItemId}) ===");
        
        com.laptophub.backend.dto.UpdateCartItemDTO updateDTO = com.laptophub.backend.dto.UpdateCartItemDTO.builder()
                .cantidad(5)
                .build();
        
        mockMvc.perform(put("/api/cart/items/" + cartItemId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDTO)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray());
        
        System.out.println("✅ TEST 4 PASÓ: Cantidad actualizada correctamente\n");
    }

    /**
     * TEST 5: Calcular total del carrito (GET /api/cart/{cartId}/total)
     */
    @Test
    @Order(5)
    public void test5_CalculateCartTotal() throws Exception {
        System.out.println("\n=== TEST 5: Calcular total del carrito (GET /api/cart/{cartId}/total) ===");
        
        // Obtener el cartId
        Cart cart = cartRepository.findAll().stream().findFirst().orElseThrow();
        String cartId = cart.getId().toString();
        
        mockMvc.perform(get("/api/cart/" + cartId + "/total"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isNumber());
        
        System.out.println("✅ TEST 5 PASÓ: Total del carrito calculado\n");
    }

    /**
     * TEST 6: Remover item del carrito (DELETE /api/cart/items/{cartItemId})
     */
    @Test
    @Order(6)
    public void test6_RemoveItemFromCart() throws Exception {
        System.out.println("\n=== TEST 6: Remover item del carrito (DELETE /api/cart/items/{cartItemId}) ===");
        
        mockMvc.perform(delete("/api/cart/items/" + cartItemId))
                .andDo(print())
                .andExpect(status().isOk());
        
        System.out.println("✅ TEST 6 PASÓ: Item removido del carrito\n");
    }

    /**
     * TEST 7: Agregar item nuevamente y limpiar carrito (DELETE /api/cart/user/{userId}/clear)
     */
    @Test
    @Order(7)
    public void test7_ClearCart() throws Exception {
        System.out.println("\n=== TEST 7: Limpiar carrito completo (DELETE /api/cart/user/{userId}/clear) ===");
        
        // Primero agregar un item
        AddToCartDTO addToCart = AddToCartDTO.builder()
                .productId(Long.parseLong(productId))
                .cantidad(3)
                .build();
        
        mockMvc.perform(post("/api/cart/user/" + userId + "/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(addToCart)))
                .andExpect(status().isOk());
        
        // Luego limpiar el carrito
        mockMvc.perform(delete("/api/cart/user/" + userId + "/clear"))
                .andDo(print())
                .andExpect(status().isOk());
        
        System.out.println("✅ TEST 7 PASÓ: Carrito limpiado correctamente\n");
    }

    /**
     * TEST 8: Crear carrito final para verificación manual
     */
    @Test
    @Order(8)
    public void test8_CreateFinalCartForVerification() throws Exception {
        System.out.println("\n=== TEST 8: Crear carrito final para verificación manual ===");
        
        // Agregar items al carrito para verificación manual
        AddToCartDTO addToCart = AddToCartDTO.builder()
                .productId(Long.parseLong(productId))
                .cantidad(2)
                .build();
        
        mockMvc.perform(post("/api/cart/user/" + userId + "/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(addToCart)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists());
        
        System.out.println("✅ TEST 8 PASÓ: Carrito final creado");
        System.out.println("📋 Verifica en tu gestor de BD el carrito del usuario: cart.test@laptophub.com\n");
    }
}
