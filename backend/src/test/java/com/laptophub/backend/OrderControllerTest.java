package com.laptophub.backend;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.laptophub.backend.dto.AddToCartDTO;
import com.laptophub.backend.dto.CreateOrderDTO;
import com.laptophub.backend.model.Product;
import com.laptophub.backend.model.ProductImage;
import com.laptophub.backend.repository.CartItemRepository;
import com.laptophub.backend.repository.CartRepository;
import com.laptophub.backend.repository.OrderItemRepository;
import com.laptophub.backend.repository.OrderRepository;
import com.laptophub.backend.repository.PaymentRepository;
import com.laptophub.backend.repository.ProductImageRepository;
import com.laptophub.backend.repository.ProductRepository;
import com.laptophub.backend.repository.UserRepository;
import com.laptophub.backend.support.TestAuthHelper;
import com.laptophub.backend.support.TestAuthHelper.AuthInfo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;

/**
 * Tests de endpoints CRUD para Order
 * Ejecuta las pruebas en orden específico para mantener consistencia
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@SuppressWarnings("null")
public class OrderControllerTest {
    /**
     * TEST 11: Validar que el payment de la orden creada incluye clientSecret
     */
    @Test
    @org.junit.jupiter.api.Order(11)
    public void test11_OrderPaymentHasClientSecret() throws Exception {
        System.out.println("\n=== TEST 11: Validar clientSecret en payment de orden creada ===");
        AddToCartDTO addToCart = AddToCartDTO.builder()
                .productId(Long.parseLong(productId))
                .cantidad(1)
                .build();
        mockMvc.perform(post("/api/cart/user/" + userId + "/items")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(addToCart)))
                .andExpect(status().isOk());
        CreateOrderDTO orderDTO = CreateOrderDTO.builder()
                .direccionEnvio("Calle Test Secret")
                .build();
        MvcResult result = mockMvc.perform(post("/api/orders/user/" + userId)
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(orderDTO)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.payment").exists())
                .andReturn();
        String response = result.getResponse().getContentAsString();
        com.fasterxml.jackson.databind.JsonNode jsonNode = objectMapper.readTree(response);
        com.fasterxml.jackson.databind.JsonNode paymentNode = jsonNode.get("payment");
        String clientSecret = paymentNode != null && paymentNode.has("clientSecret") ? paymentNode.get("clientSecret").asText() : null;
        System.out.println("clientSecret devuelto en payment: " + clientSecret);
        org.junit.jupiter.api.Assertions.assertNotNull(clientSecret);
        org.junit.jupiter.api.Assertions.assertFalse(clientSecret.isEmpty());
        System.out.println("✅ TEST 11 PASÓ: clientSecret devuelto correctamente en payment de orden\n");
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private CartItemRepository cartItemRepository;

    @Autowired
    private UserRepository userRepository;

        @Autowired
        private PasswordEncoder passwordEncoder;

    @Autowired
    private ProductRepository productRepository;

        @Autowired
        private ProductImageRepository productImageRepository;

    private static String userId;
    private static String productId;
    private static String orderId;
        private static String userToken;
        private static String adminToken;
    
    private static final String UNIQUE_EMAIL = "order.test." + System.currentTimeMillis() + "@laptophub.com";

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
    @org.junit.jupiter.api.Order(1)
    @SuppressWarnings("null")
    public void test1_SetupUserAndProduct() throws Exception {
        // Limpiar BD solo antes del primer test
        orderItemRepository.deleteAll();
        paymentRepository.deleteAll();
        orderRepository.deleteAll();
        cartItemRepository.deleteAll();
        cartRepository.deleteAll();
        
        System.out.println("\n=== TEST 1: Configuración - Crear usuario y producto ===");
        
        AuthInfo authInfo = TestAuthHelper.registerAndLogin(
                mockMvc,
                objectMapper,
                UNIQUE_EMAIL,
                "password123",
                "Order",
                "Tester"
        );
        userId = authInfo.getUserId();
        userToken = authInfo.getToken();
        adminToken = TestAuthHelper.createAdminAndLogin(
                userRepository,
                passwordEncoder,
                mockMvc,
                objectMapper,
                TestAuthHelper.uniqueEmail("order.admin"),
                "admin123"
        );
        
        Product testProduct = Product.builder()
                .nombre("Laptop Acer Nitro 5")
                .descripcion("Laptop gaming con buena relacion precio-rendimiento")
                .precio(new BigDecimal("1099.99"))
                .stock(40)
                .marca("Acer")
                .procesador("Intel Core i7-11800H")
                .ram(16)
                .almacenamiento(512)
                .pantalla("15.6 pulgadas FHD 144Hz")
                .gpu("NVIDIA RTX 3060")
                .peso(new BigDecimal("2.4"))
                .build();
        
        Product savedProduct = productRepository.save(testProduct);
        productId = savedProduct.getId().toString();
        
        ProductImage mainImage = ProductImage.builder()
                .url("https://example.com/acer-nitro-5.jpg")
                .orden(0)
                .descripcion("Imagen principal")
                .product(savedProduct)
                .build();
        productImageRepository.save(mainImage);
        
        System.out.println("✅ TEST 1 PASÓ: Usuario creado con ID: " + userId);
        System.out.println("✅ Producto creado con ID: " + productId + "\n");
    }

    /**
     * TEST 2: Crear orden desde carrito (POST /api/orders/user/{userId})
     */
    @Test
    @org.junit.jupiter.api.Order(2)
    public void test2_CreateOrderFromCart() throws Exception {
        System.out.println("\n=== TEST 2: Crear orden desde carrito (POST /api/orders/user/{userId}) ===");
        
        // Primero agregar item al carrito usando DTO
        AddToCartDTO addToCart = AddToCartDTO.builder()
                .productId(Long.parseLong(productId))
                .cantidad(2)
                .build();
        
        mockMvc.perform(post("/api/cart/user/" + userId + "/items")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(addToCart)))
                .andExpect(status().isOk());
        
        // Crear orden desde carrito usando CreateOrderDTO
        CreateOrderDTO orderDTO = CreateOrderDTO.builder()
                .direccionEnvio("Calle Order 123")
                .build();
        
        MvcResult result = mockMvc.perform(post("/api/orders/user/" + userId)
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(orderDTO)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.estado").value("PENDIENTE_PAGO"))
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.payment").exists())
                .andReturn();

        String response = result.getResponse().getContentAsString();
        com.fasterxml.jackson.databind.JsonNode jsonNode = objectMapper.readTree(response);
        orderId = jsonNode.get("id").asText();
        
        System.out.println("✅ TEST 2 PASÓ: Orden creada con ID: " + orderId + "\n");
    }

    /**
     * TEST 3: Buscar orden por ID (GET /api/orders/{orderId})
     */
    @Test
    @org.junit.jupiter.api.Order(3)
    public void test3_FindOrderById() throws Exception {
        System.out.println("\n=== TEST 3: Buscar orden por ID (GET /api/orders/{orderId}) ===");
        
        mockMvc.perform(get("/api/orders/" + orderId)
                        .header("Authorization", "Bearer " + userToken))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(orderId))
                .andExpect(jsonPath("$.estado").value("PENDIENTE_PAGO"));
        
        System.out.println("✅ TEST 3 PASÓ: Orden encontrada por ID\n");
    }

    /**
     * TEST 4: Buscar órdenes por usuario (GET /api/orders/user/{userId}) con paginación
     */
    @Test
    @org.junit.jupiter.api.Order(4)
    public void test4_FindOrdersByUser() throws Exception {
        System.out.println("\n=== TEST 4: Buscar órdenes por usuario (GET /api/orders/user/{userId}) ===");
        
        mockMvc.perform(get("/api/orders/user/" + userId)
                        .header("Authorization", "Bearer " + userToken)
                        .param("page", "0")
                        .param("size", "10"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].id").exists())
                .andExpect(jsonPath("$.totalElements").exists())
                .andExpect(jsonPath("$.totalPages").exists());
        
        System.out.println("✅ TEST 4 PASÓ: Órdenes encontradas por usuario (paginadas)\n");
    }

    /**
     * TEST 5: Buscar órdenes por estado (GET /api/orders/status/{estado}) con paginación
     */
    @Test
    @org.junit.jupiter.api.Order(5)
    public void test5_FindOrdersByStatus() throws Exception {
        System.out.println("\n=== TEST 5: Buscar órdenes por estado (GET /api/orders/status/{estado}) ===");
        
        mockMvc.perform(get("/api/orders/status/PENDIENTE_PAGO")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("page", "0")
                        .param("size", "10"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].estado").value("PENDIENTE_PAGO"))
                .andExpect(jsonPath("$.totalElements").exists())
                .andExpect(jsonPath("$.totalPages").exists());
        
        System.out.println("✅ TEST 5 PASÓ: Órdenes encontradas por estado (paginadas)\n");
    }

    /**
     * TEST 6: Actualizar estado de la orden (PUT /api/orders/{orderId}/status/{estado})
     */
    @Test
    @org.junit.jupiter.api.Order(6)
    public void test6_UpdateOrderStatus() throws Exception {
        System.out.println("\n=== TEST 6: Actualizar estado de la orden (PUT /api/orders/{orderId}/status/{estado}) ===");
        
        mockMvc.perform(put("/api/orders/" + orderId + "/status/PROCESANDO")
                        .header("Authorization", "Bearer " + adminToken))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(orderId))
                .andExpect(jsonPath("$.estado").value("PROCESANDO"));
        
        System.out.println("✅ TEST 6 PASÓ: Estado actualizado correctamente\n");
    }

    /**
     * TEST 7: Expirar órdenes pendientes (POST /api/orders/expire)
     */
    @Test
    @org.junit.jupiter.api.Order(7)
    public void test7_ExpirePendingOrders() throws Exception {
        System.out.println("\n=== TEST 7: Expirar órdenes pendientes (POST /api/orders/expire) ===");
        
        mockMvc.perform(post("/api/orders/expire")
                        .header("Authorization", "Bearer " + adminToken))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isNumber());
        
        System.out.println("✅ TEST 7 PASÓ: Expiración ejecutada\n");
    }

    /**
     * TEST 8: Listar todas las órdenes (GET /api/orders)
     */
    @Test
    @org.junit.jupiter.api.Order(8)
    public void test8_FindAllOrders() throws Exception {
        System.out.println("\n=== TEST 8: Listar todas las órdenes (GET /api/orders) ===");
        
        mockMvc.perform(get("/api/orders")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("page", "0")
                        .param("size", "10"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].id").exists())
                .andExpect(jsonPath("$.totalElements").exists());
        
        System.out.println("✅ TEST 8 PASÓ: Lista de órdenes obtenida\n");
    }

    /**
     * TEST 9: Cancelar orden (POST /api/orders/{orderId}/cancel)
     */
    @Test
    @org.junit.jupiter.api.Order(9)
    public void test9_CancelOrder() throws Exception {
        System.out.println("\n=== TEST 9: Cancelar orden (POST /api/orders/{orderId}/cancel) ===");
        
        AddToCartDTO addToCart = AddToCartDTO.builder()
                .productId(Long.parseLong(productId))
                .cantidad(1)
                .build();
        
        mockMvc.perform(post("/api/cart/user/" + userId + "/items")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(addToCart)))
                .andExpect(status().isOk());
        
        CreateOrderDTO orderDTO = CreateOrderDTO.builder()
                .direccionEnvio("Calle Cancel 456")
                .build();
        
        MvcResult result = mockMvc.perform(post("/api/orders/user/" + userId)
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(orderDTO)))
                .andExpect(status().isOk())
                .andReturn();

        String response = result.getResponse().getContentAsString();
        com.fasterxml.jackson.databind.JsonNode jsonNode = objectMapper.readTree(response);
        String cancelOrderId = jsonNode.get("id").asText();
        
        mockMvc.perform(post("/api/orders/" + cancelOrderId + "/cancel")
                        .header("Authorization", "Bearer " + userToken))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("CANCELADO"));
        
        System.out.println("✅ TEST 9 PASÓ: Orden cancelada correctamente\n");
    }

    /**
     * TEST 10: Crear orden final para verificación manual
     */
    @Test
    @org.junit.jupiter.api.Order(10)
    public void test10_CreateFinalOrderForVerification() throws Exception {
        System.out.println("\n=== TEST 10: Crear orden final para verificación manual ===");
        
        AddToCartDTO addToCart = AddToCartDTO.builder()
                .productId(Long.parseLong(productId))
                .cantidad(1)
                .build();
        
        mockMvc.perform(post("/api/cart/user/" + userId + "/items")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(addToCart)))
                .andExpect(status().isOk());
        
        CreateOrderDTO orderDTO = CreateOrderDTO.builder()
                .direccionEnvio("Calle Final 789")
                .build();
        
        MvcResult result = mockMvc.perform(post("/api/orders/user/" + userId)
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(orderDTO)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andReturn();

        String response = result.getResponse().getContentAsString();
        com.fasterxml.jackson.databind.JsonNode jsonNode = objectMapper.readTree(response);
        
        System.out.println("✅ TEST 10 PASÓ: Orden final creada con ID: " + jsonNode.get("id").asText());
        System.out.println("📋 Verifica en tu gestor de BD la orden del usuario: order.test@laptophub.com\n");
    }
}
