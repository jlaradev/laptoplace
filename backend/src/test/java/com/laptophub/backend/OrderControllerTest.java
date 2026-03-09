package com.laptophub.backend;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.laptophub.backend.dto.AddToCartDTO;
import com.laptophub.backend.dto.CreateOrderDTO;
import com.laptophub.backend.model.Brand;
import com.laptophub.backend.model.Product;
import com.laptophub.backend.model.ProductImage;
import com.laptophub.backend.repository.BrandRepository;
import com.laptophub.backend.repository.CartItemRepository;
import com.laptophub.backend.repository.CartRepository;
import com.laptophub.backend.repository.OrderItemRepository;
import com.laptophub.backend.repository.OrderRepository;
import com.laptophub.backend.repository.PaymentRepository;
import com.laptophub.backend.repository.ProductImageRepository;
import com.laptophub.backend.repository.ProductRepository;
import com.laptophub.backend.repository.ReviewRepository;
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

    /**
     * TEST 12: Verificar que expirar una orden cambia estado, marca pago como EXPIRADO y restaura stock
     */
    @Test
    @org.junit.jupiter.api.Order(12)
    public void test12_ExpireOrderEffects() throws Exception {
        System.out.println("\n=== TEST 12: Verificar efectos de expiración ===");

        // Si las variables estáticas no están inicializadas (ej. ejecutando solo este test), inicializar minimalmente
        if (userId == null || productId == null || userToken == null || adminToken == null) {
            String uniqueEmail = TestAuthHelper.uniqueEmail("order.test");
            AuthInfo authInfo = TestAuthHelper.registerAndLogin(
                    mockMvc,
                    objectMapper,
                    uniqueEmail,
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

            // Crear marca para producto
            Brand testBrand = Brand.builder()
                    .nombre("TestBrand")
                    .descripcion("Test Brand")
                    .build();
            Brand savedBrand = brandRepository.save(testBrand);

            // Crear producto de prueba
            Product testProduct = Product.builder()
                    .nombre("Laptop Test Expire")
                    .descripcion("Producto para test expiracion")
                    .precio(new BigDecimal("99.99"))
                    .stock(10)
                    .brand(savedBrand)
                    .procesador("TestCPU")
                    .ram(4)
                    .almacenamiento(128)
                    .pantalla("13")
                    .gpu("Integrated")
                    .peso(new BigDecimal("1.5"))
                    .build();
            Product savedProduct = productRepository.save(testProduct);
            productId = savedProduct.getId().toString();
        }

        // Crear un producto aislado para esta prueba y asegurar carrito limpio
        Brand isoBrand = Brand.builder()
                .nombre("IsoBrand")
                .descripcion("Iso Brand")
                .build();
        Brand savedIsoBrand = brandRepository.save(isoBrand);

        Product testProductForExpire = Product.builder()
                .nombre("Laptop Expire Isolation")
                .descripcion("Producto aislado para test de expiracion")
                .precio(new BigDecimal("199.99"))
                .stock(5)
                .brand(savedIsoBrand)
                .procesador("IsoCPU")
                .ram(4)
                .almacenamiento(128)
                .pantalla("13")
                .gpu("Integrated")
                .peso(new BigDecimal("1.2"))
                .build();
        Product savedTestProductForExpire = productRepository.save(testProductForExpire);
        Long localProductId = savedTestProductForExpire.getId();

        cartItemRepository.deleteAll();
        cartRepository.deleteAll();

        AddToCartDTO addToCart = AddToCartDTO.builder()
                .productId(localProductId)
                .cantidad(1)
                .build();

        mockMvc.perform(post("/api/cart/user/" + userId + "/items")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(addToCart)))
                .andExpect(status().isOk());

        // stock antes de crear la orden
        Product productBefore = productRepository.findById(localProductId).orElseThrow();
        int stockBefore = productBefore.getStock();

        CreateOrderDTO orderDTO = CreateOrderDTO.builder()
                .direccionEnvio("Calle Expire Test")
                .build();

        MvcResult result = mockMvc.perform(post("/api/orders/user/" + userId)
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(orderDTO)))
                .andExpect(status().isOk())
                .andReturn();

        String response = result.getResponse().getContentAsString();
        com.fasterxml.jackson.databind.JsonNode jsonNode = objectMapper.readTree(response);
        String newOrderId = jsonNode.get("id").asText();

        // verificar stock disminuyó
        Product productAfterOrder = productRepository.findById(localProductId).orElseThrow();
        org.junit.jupiter.api.Assertions.assertEquals(stockBefore - 1, productAfterOrder.getStock());

        // Forzar expiresAt al pasado
        com.laptophub.backend.model.Order order = orderRepository.findById(Long.parseLong(newOrderId)).orElseThrow();
        order.setExpiresAt(java.time.LocalDateTime.now().minusMinutes(30));
        orderRepository.save(order);

        // Llamar al endpoint de expiración
        mockMvc.perform(post("/api/orders/expire")
                        .header("Authorization", "Bearer " + adminToken))
                .andDo(print())
                .andExpect(status().isOk());

        // Verificar orden y pago
        com.laptophub.backend.model.Order expiredOrder = orderRepository.findById(Long.parseLong(newOrderId)).orElseThrow();
        org.junit.jupiter.api.Assertions.assertEquals(com.laptophub.backend.model.OrderStatus.EXPIRADO, expiredOrder.getEstado());
        org.junit.jupiter.api.Assertions.assertNotNull(expiredOrder.getPayment());
        org.junit.jupiter.api.Assertions.assertEquals(com.laptophub.backend.model.PaymentStatus.EXPIRADO, expiredOrder.getPayment().getEstado());

        // Verificar stock restaurado
        Product productAfterExpire = productRepository.findById(localProductId).orElseThrow();
        org.junit.jupiter.api.Assertions.assertEquals(stockBefore, productAfterExpire.getStock());

        System.out.println("✅ TEST 12 PASÓ: Expiración marcó orden y pago como EXPIRADO y restauró stock\n");
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
    private BrandRepository brandRepository;

        @Autowired
        private ProductImageRepository productImageRepository;

    @SuppressWarnings("unused")
    @Autowired
    private ReviewRepository reviewRepository;

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
        
        // Crear marca para producto
        Brand acerBrand = Brand.builder()
                .nombre("Acer")
                .descripcion("Acer Computers")
                .build();
        Brand savedAcerBrand = brandRepository.save(acerBrand);

        Product testProduct = Product.builder()
                .nombre("Laptop Acer Nitro 5")
                .descripcion("Laptop gaming con buena relacion precio-rendimiento")
                .precio(new BigDecimal("1099.99"))
                .stock(40)
                .brand(savedAcerBrand)
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

    /**
     * TEST 13: Verificar que el endpoint de purchased devuelve false si el producto no ha sido comprado
     */
    @Test
    @org.junit.jupiter.api.Order(13)
    public void test13_CheckProductNotPurchased() throws Exception {
        System.out.println("\n╔════════════════════════════════════════════════════════════╗");
        System.out.println("║ TEST 13: Verificar producto no comprado                    ║");
        System.out.println("╚════════════════════════════════════════════════════════════╝");
        
        // Crear un producto que definitivamente no fue comprado
        Brand newBrand = Brand.builder()
                .nombre("UnpurchasedBrand")
                .descripcion("Brand para producto no comprado")
                .build();
        Brand savedBrand = brandRepository.save(newBrand);
        System.out.println("✓ Brand creada: " + savedBrand.getNombre() + " (ID: " + savedBrand.getId() + ")");
        
        Product unpurchasedProduct = Product.builder()
                .nombre("Laptop Never Purchased")
                .descripcion("Producto que nunca será comprado")
                .precio(new BigDecimal("1999.99"))
                .stock(10)
                .brand(savedBrand)
                .procesador("Intel i9")
                .ram(32)
                .almacenamiento(512)
                .pantalla("16")
                .gpu("RTX 4090")
                .peso(new BigDecimal("2.0"))
                .build();
        Product savedProduct = productRepository.save(unpurchasedProduct);
        System.out.println("✓ Producto creado: " + savedProduct.getNombre() + " (ID: " + savedProduct.getId() + ")");
        System.out.println("\n📡 Realizando GET /api/orders/user/" + userId + "/product/" + savedProduct.getId() + "/purchased");
        
        MvcResult result = mockMvc.perform(get("/api/orders/user/" + userId + "/product/" + savedProduct.getId() + "/purchased")
                        .header("Authorization", "Bearer " + userToken))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.purchased").exists())
                .andExpect(jsonPath("$.hasReview").exists())
                .andExpect(jsonPath("$.reviewId").hasJsonPath())
                .andReturn();
        
        String response = result.getResponse().getContentAsString();
        System.out.println("\n📥 RESPUESTA JSON:");
        System.out.println("────────────────────────────────────────────────────────────");
        System.out.println(response);
        System.out.println("────────────────────────────────────────────────────────────");
        
        com.fasterxml.jackson.databind.JsonNode jsonNode = objectMapper.readTree(response);
        boolean purchased = jsonNode.get("purchased").asBoolean();
        boolean hasReview = jsonNode.get("hasReview").asBoolean();
        
        System.out.println("\n✓ Campo 'purchased': " + purchased);
        System.out.println("✓ Campo 'hasReview': " + hasReview);
        System.out.println("✓ Campo 'reviewId': " + (jsonNode.get("reviewId").isNull() ? "null" : jsonNode.get("reviewId").asText()));
        org.junit.jupiter.api.Assertions.assertFalse(purchased, "El producto no fue comprado, debe retornar false");
        org.junit.jupiter.api.Assertions.assertFalse(hasReview, "No debe haber reseña si no fue comprado");
        org.junit.jupiter.api.Assertions.assertTrue(jsonNode.get("reviewId").isNull(), "reviewId debe ser null");
        System.out.println("\n✅ TEST 13 PASÓ: El endpoint retorna correctamente 'purchased': false, 'hasReview': false, 'reviewId': null\n");
    }

    /**
     * TEST 14: Verificar que reviewable-products devuelve página vacía si no hay órdenes ENTREGADO
     */
    @Test
    @org.junit.jupiter.api.Order(14)
    public void test14_GetReviewableProductsEmpty() throws Exception {
        System.out.println("\n╔════════════════════════════════════════════════════════════╗");
        System.out.println("║ TEST 14: Verificar página vacía de productos reseñables    ║");
        System.out.println("╚════════════════════════════════════════════════════════════╝");
        
        // Crear un nuevo usuario sin órdenes entregadas
        String newUserEmail = TestAuthHelper.uniqueEmail("review.test");
        System.out.println("\n✓ Creando nuevo usuario: " + newUserEmail);
        AuthInfo newUserAuth = TestAuthHelper.registerAndLogin(
                mockMvc,
                objectMapper,
                newUserEmail,
                "password123",
                "Review",
                "Tester"
        );
        System.out.println("✓ Usuario creado (ID: " + newUserAuth.getUserId() + ")");
        
        System.out.println("\n📡 Realizando GET /api/orders/user/" + newUserAuth.getUserId() + "/reviewable-products?page=0&size=20");
        
        MvcResult result = mockMvc.perform(get("/api/orders/user/" + newUserAuth.getUserId() + "/reviewable-products")
                        .param("page", "0")
                        .param("size", "20")
                        .param("sort", "nombre,asc")
                        .header("Authorization", "Bearer " + newUserAuth.getToken()))
                .andDo(print())
                .andExpect(status().isOk())
                .andReturn();
        
        String response = result.getResponse().getContentAsString();
        System.out.println("\n📥 RESPUESTA JSON:");
        System.out.println("────────────────────────────────────────────────────────────");
        System.out.println(response);
        System.out.println("────────────────────────────────────────────────────────────");
        
        com.fasterxml.jackson.databind.JsonNode jsonNode = objectMapper.readTree(response);
        
        org.junit.jupiter.api.Assertions.assertTrue(jsonNode.has("content"), "Response debe tener field 'content'");
        com.fasterxml.jackson.databind.JsonNode content = jsonNode.get("content");
        org.junit.jupiter.api.Assertions.assertTrue(content.isArray(), "Content debe ser un array");
        System.out.println("✓ Es un Page response: true");
        System.out.println("✓ Tamaño del contenido: " + content.size());
        System.out.println("✓ Total de elementos: " + jsonNode.get("totalElements").asInt());
        System.out.println("✓ Página actual: " + jsonNode.get("number").asInt());
        System.out.println("✓ Tamaño de página: " + jsonNode.get("size").asInt());
        org.junit.jupiter.api.Assertions.assertEquals(0, content.size(), "Content debe estar vacío para usuario sin órdenes entregadas");
        System.out.println("\n✅ TEST 14 PASÓ: Endpoint retorna correctamente una página vacía\n");
    }

    /**
     * TEST 15: Crear orden entregada y verificar que aparece en reviewable-products
     */
    @Test
    @org.junit.jupiter.api.Order(15)
    public void test15_GetReviewableProductsAfterDelivery() throws Exception {
        System.out.println("\n╔════════════════════════════════════════════════════════════╗");
        System.out.println("║ TEST 15: Verificar productos reseñables después de entrega  ║");
        System.out.println("╚════════════════════════════════════════════════════════════╝");
        
        // Crear nuevo usuario
        String deliveryUserEmail = TestAuthHelper.uniqueEmail("delivery.test");
        System.out.println("\n✓ Creando usuario para test de entrega: " + deliveryUserEmail);
        AuthInfo deliveryUserAuth = TestAuthHelper.registerAndLogin(
                mockMvc,
                objectMapper,
                deliveryUserEmail,
                "password123",
                "Delivery",
                "User"
        );
        System.out.println("✓ Usuario creado (ID: " + deliveryUserAuth.getUserId() + ")");
        
        // Crear producto de prueba
        Brand deliveryBrand = Brand.builder()
                .nombre("DeliveryBrand")
                .descripcion("Brand para test de entrega")
                .build();
        Brand savedBrand = brandRepository.save(deliveryBrand);
        System.out.println("✓ Brand creada (ID: " + savedBrand.getId() + ")");
        
        Product deliveryProduct = Product.builder()
                .nombre("Laptop Delivered")
                .descripcion("Producto para verificar reseñability")
                .precio(new BigDecimal("1499.99"))
                .stock(10)
                .brand(savedBrand)
                .procesador("Intel i7")
                .ram(16)
                .almacenamiento(256)
                .pantalla("15")
                .gpu("RTX 3060")
                .peso(new BigDecimal("1.8"))
                .build();
        Product savedProduct = productRepository.save(deliveryProduct);
        System.out.println("✓ Producto creado (ID: " + savedProduct.getId() + ", Precio: $" + savedProduct.getPrecio() + ")");
        
        // Agregar producto al carrito
        AddToCartDTO addToCart = AddToCartDTO.builder()
                .productId(savedProduct.getId())
                .cantidad(1)
                .build();
        System.out.println("\n✓ Agregando producto al carrito");
        mockMvc.perform(post("/api/cart/user/" + deliveryUserAuth.getUserId() + "/items")
                        .header("Authorization", "Bearer " + deliveryUserAuth.getToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(addToCart)))
                .andExpect(status().isOk());
        
        // Crear orden
        System.out.println("✓ Creando orden");
        CreateOrderDTO orderDTO = CreateOrderDTO.builder()
                .direccionEnvio("Calle Entrega 123")
                .build();
        MvcResult orderResult = mockMvc.perform(post("/api/orders/user/" + deliveryUserAuth.getUserId())
                        .header("Authorization", "Bearer " + deliveryUserAuth.getToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(orderDTO)))
                .andExpect(status().isOk())
                .andReturn();
        
        String orderResponse = orderResult.getResponse().getContentAsString();
        com.fasterxml.jackson.databind.JsonNode orderJsonNode = objectMapper.readTree(orderResponse);
        Long orderId = orderJsonNode.get("id").asLong();
        System.out.println("✓ Orden creada (ID: " + orderId + ", Estado: PENDIENTE_PAGO)");
        
        // Cambiar orden a ENTREGADO (usando admin)
        System.out.println("✓ Cambiando estado de la orden a ENTREGADO");
        mockMvc.perform(put("/api/orders/" + orderId + "/status/ENTREGADO")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
        System.out.println("✓ Orden actualizada a ENTREGADO");
        
        // ENDPOINT 1: Verificar que el producto aparece en reviewable-products
        System.out.println("\n📡 ENDPOINT 1: GET /api/orders/user/" + deliveryUserAuth.getUserId() + "/reviewable-products?page=0&size=20");
        MvcResult reviewableResult = mockMvc.perform(get("/api/orders/user/" + deliveryUserAuth.getUserId() + "/reviewable-products")
                        .param("page", "0")
                        .param("size", "20")
                        .param("sort", "nombre,asc")
                        .header("Authorization", "Bearer " + deliveryUserAuth.getToken()))
                .andDo(print())
                .andExpect(status().isOk())
                .andReturn();
        
        String reviewableResponse = reviewableResult.getResponse().getContentAsString();
        System.out.println("\n📥 RESPUESTA JSON:");
        System.out.println("────────────────────────────────────────────────────────────");
        System.out.println(reviewableResponse);
        System.out.println("────────────────────────────────────────────────────────────");
        
        com.fasterxml.jackson.databind.JsonNode reviewableJsonNode = objectMapper.readTree(reviewableResponse);
        
        org.junit.jupiter.api.Assertions.assertTrue(reviewableJsonNode.has("content"), "Response debe tener field 'content'");
        com.fasterxml.jackson.databind.JsonNode content = reviewableJsonNode.get("content");
        org.junit.jupiter.api.Assertions.assertTrue(content.isArray(), "Content debe ser un array");
        System.out.println("✓ Es un Page response: true");
        System.out.println("✓ Tamaño del contenido: " + content.size());
        System.out.println("✓ Total de elementos: " + reviewableJsonNode.get("totalElements").asInt());
        System.out.println("✓ Página actual: " + reviewableJsonNode.get("number").asInt());
        System.out.println("✓ Tamaño de página: " + reviewableJsonNode.get("size").asInt());
        org.junit.jupiter.api.Assertions.assertTrue(content.size() > 0, "Content debe contener al menos un producto");
        
        // Verificar que el producto está en la lista
        boolean foundProduct = false;
        for (com.fasterxml.jackson.databind.JsonNode product : content) {
            Long productId = product.get("id").asLong();
            String nombre = product.get("nombre").asText();
            BigDecimal precio = new BigDecimal(product.get("precio").asText());
            boolean hasReview = product.get("hasReview").asBoolean();
            String reviewId = product.get("reviewId").isNull() ? "null" : product.get("reviewId").asText();
            
            System.out.println("\n✓ Producto encontrado:");
            System.out.println("  - ID: " + productId);
            System.out.println("  - Nombre: " + nombre);
            System.out.println("  - Precio: $" + precio);
            System.out.println("  - Tiene reseña: " + hasReview);
            System.out.println("  - Review ID: " + reviewId);
            
            if (productId == savedProduct.getId()) {
                foundProduct = true;
                org.junit.jupiter.api.Assertions.assertFalse(hasReview, "El producto no debe tener reseña aún");
                org.junit.jupiter.api.Assertions.assertTrue(product.get("reviewId").isNull(), "reviewId debe ser null");
            }
        }
        org.junit.jupiter.api.Assertions.assertTrue(foundProduct, "El producto debe estar en la lista de reseñables");
        
        // ENDPOINT 2: Verificar que el check de purchased ahora retorna true
        System.out.println("\n📡 ENDPOINT 2: GET /api/orders/user/" + deliveryUserAuth.getUserId() + "/product/" + savedProduct.getId() + "/purchased");
        MvcResult purchasedResult = mockMvc.perform(get("/api/orders/user/" + deliveryUserAuth.getUserId() + "/product/" + savedProduct.getId() + "/purchased")
                        .header("Authorization", "Bearer " + deliveryUserAuth.getToken()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.purchased").exists())
                .andExpect(jsonPath("$.hasReview").exists())
                .andExpect(jsonPath("$.reviewId").hasJsonPath())
                .andReturn();
        
        String purchasedResponse = purchasedResult.getResponse().getContentAsString();
        System.out.println("\n📥 RESPUESTA JSON:");
        System.out.println("────────────────────────────────────────────────────────────");
        System.out.println(purchasedResponse);
        System.out.println("────────────────────────────────────────────────────────────");
        
        com.fasterxml.jackson.databind.JsonNode purchasedJsonNode = objectMapper.readTree(purchasedResponse);
        boolean purchased = purchasedJsonNode.get("purchased").asBoolean();
        boolean hasReview = purchasedJsonNode.get("hasReview").asBoolean();
        System.out.println("✓ Campo 'purchased': " + purchased);
        System.out.println("✓ Campo 'hasReview': " + hasReview);
        System.out.println("✓ Campo 'reviewId': " + (purchasedJsonNode.get("reviewId").isNull() ? "null" : purchasedJsonNode.get("reviewId").asText()));
        org.junit.jupiter.api.Assertions.assertTrue(purchased, "El producto debe estar marcado como comprado");
        org.junit.jupiter.api.Assertions.assertFalse(hasReview, "No debe tener reseña aún (no se ha creado ninguna)");
        org.junit.jupiter.api.Assertions.assertTrue(purchasedJsonNode.get("reviewId").isNull(), "reviewId debe ser null pues no hay reseña");
        
        System.out.println("\n✅ TEST 15 PASÓ: Ambos endpoints funcionan correctamente después de entregar la orden\n");
    }

    /**
     * TEST 16: Obtener órdenes activas de un usuario (PROCESANDO, ENVIADO, ENTREGADO)
     * Crea 5 órdenes: solo 3 con estados válidos, 2 con estados inválidos
     * Valida que el endpoint filtra correctamente y solo retorna las 3 órdenes válidas
     */
    @Test
    @org.junit.jupiter.api.Order(16)
    public void test16_GetUserActiveOrders() throws Exception {
        System.out.println("\n╔════════════════════════════════════════════════════════════╗");
        System.out.println("║ TEST 16: Filtro de órdenes activas (5 órdenes, retorna 3)  ║");
        System.out.println("╚════════════════════════════════════════════════════════════╝");
        
        // Crear nuevo usuario para test de órdenes activas
        String activeUserEmail = TestAuthHelper.uniqueEmail("active.orders.test");
        System.out.println("\n✓ Creando usuario: " + activeUserEmail);
        AuthInfo activeUserAuth = TestAuthHelper.registerAndLogin(
                mockMvc,
                objectMapper,
                activeUserEmail,
                "password123",
                "Active",
                "User"
        );
        System.out.println("✓ Usuario creado (ID: " + activeUserAuth.getUserId() + ")");
        
        // Crear 5 productos
        Brand activeBrand = Brand.builder()
                .nombre("ActiveBrand")
                .descripcion("Brand para test de órdenes activas")
                .build();
        Brand savedBrand = brandRepository.save(activeBrand);
        System.out.println("✓ Brand creada");
        
        Product[] products = new Product[5];
        String[] productNames = {
            "Laptop Pendiente", "Laptop Procesando", "Laptop Enviado", 
            "Laptop Entregado", "Laptop Cancelado"
        };
        
        for (int i = 0; i < 5; i++) {
            products[i] = Product.builder()
                    .nombre(productNames[i])
                    .descripcion("Producto " + (i+1) + " para test de filtrado de órdenes")
                    .precio(new BigDecimal(900 + (i * 300)))
                    .stock(10)
                    .brand(savedBrand)
                    .procesador("Intel i" + (5 + i))
                    .ram(8 + (i * 2))
                    .almacenamiento(256 * (i + 1))
                    .pantalla("14")
                    .gpu("Integrated")
                    .peso(new BigDecimal("1.5"))
                    .build();
            products[i] = productRepository.save(products[i]);
        }
        System.out.println("✓ 5 productos creados");
        
        // Crear 5 órdenes con diferentes estados
        System.out.println("\n✓ Creando 5 órdenes con diferentes estados:");
        String[] estados = {"PENDIENTE_PAGO", "PROCESANDO", "ENVIADO", "ENTREGADO", "CANCELADO"};
        Long[] orderIds = new Long[5];
        
        for (int i = 0; i < 5; i++) {
            AddToCartDTO addToCart = AddToCartDTO.builder()
                    .productId(products[i].getId())
                    .cantidad(1)
                    .build();
            mockMvc.perform(post("/api/cart/user/" + activeUserAuth.getUserId() + "/items")
                            .header("Authorization", "Bearer " + activeUserAuth.getToken())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(addToCart)))
                    .andExpect(status().isOk());
            
            CreateOrderDTO orderDTO = CreateOrderDTO.builder()
                    .direccionEnvio("Calle Test " + (i+1))
                    .build();
            MvcResult result = mockMvc.perform(post("/api/orders/user/" + activeUserAuth.getUserId())
                            .header("Authorization", "Bearer " + activeUserAuth.getToken())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(orderDTO)))
                    .andExpect(status().isOk())
                    .andReturn();
            
            orderIds[i] = objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
            
            // Actualizar estado si no es PENDIENTE_PAGO (que es el estado por defecto)
            if (!estados[i].equals("PENDIENTE_PAGO")) {
                mockMvc.perform(put("/api/orders/" + orderIds[i] + "/status/" + estados[i])
                                .header("Authorization", "Bearer " + adminToken))
                        .andExpect(status().isOk());
            }
            
            String validTag = (estados[i].equals("PROCESANDO") || estados[i].equals("ENVIADO") || estados[i].equals("ENTREGADO")) 
                    ? "✓ VÁLIDA" : "✗ INVÁLIDA";
            System.out.println("  - Orden " + (i+1) + ": ID=" + orderIds[i] + ", Estado=" + estados[i] + " " + validTag);
        }
        
        // Validar conteo antes del filtro
        System.out.println("\n📊 Resumen de órdenes creadas:");
        System.out.println("  - Total creada: 5");
        System.out.println("  - Válidas (PROCESANDO, ENVIADO, ENTREGADO): 3");
        System.out.println("  - Inválidas (PENDIENTE_PAGO, CANCELADO): 2");
        
        // Llamar al endpoint y validar que SOLO retorna las 3 órdenes válidas
        System.out.println("\n📡 GET /api/orders/user/" + activeUserAuth.getUserId() + "/active?page=0&size=20");
        MvcResult activeOrdersResult = mockMvc.perform(get("/api/orders/user/" + activeUserAuth.getUserId() + "/active")
                        .param("page", "0")
                        .param("size", "20")
                        .header("Authorization", "Bearer " + activeUserAuth.getToken()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andReturn();
        
        String activeOrdersResponse = activeOrdersResult.getResponse().getContentAsString();
        com.fasterxml.jackson.databind.JsonNode activeOrdersJson = objectMapper.readTree(activeOrdersResponse);
        
        org.junit.jupiter.api.Assertions.assertTrue(activeOrdersJson.has("content"), "Response debe tener 'content'");
        com.fasterxml.jackson.databind.JsonNode content = activeOrdersJson.get("content");
        org.junit.jupiter.api.Assertions.assertTrue(content.isArray(), "Content debe ser un array");
        
        System.out.println("\n✓ Información de paginación:");
        System.out.println("  - Tamaño del contenido: " + content.size());
        System.out.println("  - Total de elementos: " + activeOrdersJson.get("totalElements").asInt());
        System.out.println("  - Total de páginas: " + activeOrdersJson.get("totalPages").asInt());
        System.out.println("  - Página actual: " + activeOrdersJson.get("number").asInt());
        System.out.println("  - Tamaño de página: " + activeOrdersJson.get("size").asInt());
        
        // VALIDACIÓN CRÍTICA: Solo debe retornar 3 órdenes (las válidas)
        org.junit.jupiter.api.Assertions.assertEquals(3, content.size(), 
                "ERROR: Se crearon 5 órdenes pero el endpoint debe retornar SOLO 3 (estados válidos: PROCESANDO, ENVIADO, ENTREGADO). " +
                "Estados inválidos (PENDIENTE_PAGO, CANCELADO) deben ser excluidos. Retornadas: " + content.size());
        
        System.out.println("\n✓ Órdenes retornadas (filtradas correctamente):");
        java.util.Set<String> allowedStatuses = java.util.Set.of("PROCESANDO", "ENVIADO", "ENTREGADO");
        for (int i = 0; i < content.size(); i++) {
            com.fasterxml.jackson.databind.JsonNode order = content.get(i);
            Long id = order.get("id").asLong();
            String estado = order.get("estado").asText();
            String direccion = order.get("direccionEnvio").asText();
            System.out.println("  - Orden retornada " + (i+1) + ": ID=" + id + ", Estado=" + estado + ", Dirección=" + direccion);
            
            // Validar que todos los estados son PROCESANDO, ENVIADO o ENTREGADO
            org.junit.jupiter.api.Assertions.assertTrue(allowedStatuses.contains(estado), 
                    "Estado '" + estado + "' NO es válido. Solo se permiten: PROCESANDO, ENVIADO, ENTREGADO");
        }
        
        System.out.println("\n✅ TEST 16 PASÓ: Endpoint /active filtra correctamente");
        System.out.println("   - De 5 órdenes creadas, retornó solamente 3 (las válidas)");
        System.out.println("   - Excluyó correctamente PENDIENTE_PAGO (1 orden) y CANCELADO (1 orden)\n");
    }
}

