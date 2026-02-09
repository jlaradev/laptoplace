package com.laptophub.backend;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.laptophub.backend.dto.AddToCartDTO;
import com.laptophub.backend.dto.CreateOrderDTO;
import com.laptophub.backend.model.Payment;
import com.laptophub.backend.model.Product;
import com.laptophub.backend.model.ProductImage;
import com.laptophub.backend.model.User;
import com.laptophub.backend.repository.CartItemRepository;
import com.laptophub.backend.repository.CartRepository;
import com.laptophub.backend.repository.OrderItemRepository;
import com.laptophub.backend.repository.OrderRepository;
import com.laptophub.backend.repository.PaymentRepository;
import com.laptophub.backend.repository.ProductImageRepository;
import com.laptophub.backend.repository.ProductRepository;
import com.laptophub.backend.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;

/**
 * Tests de endpoints CRUD para Payment
 * Ejecuta las pruebas en orden específico para mantener consistencia
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@SuppressWarnings("null")
public class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

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
    private static String orderId;
    private static String paymentId;
    
    private static final String UNIQUE_EMAIL = "payment.test." + System.currentTimeMillis() + "@laptophub.com";

    /**
     * Limpia la base de datos una sola vez antes de todos los tests
     */
    @BeforeAll
    public static void setUpDatabase() {
        // La limpieza ocurre una sola vez al inicio
    }

    /**
     * TEST 1: Configuración - Crear usuario, producto y orden de prueba
     */
    @Test
    @org.junit.jupiter.api.Order(1)
    @SuppressWarnings("null")
    public void test1_SetupUserProductAndOrder() throws Exception {
        // Limpiar BD solo antes del primer test
        orderItemRepository.deleteAll();
        paymentRepository.deleteAll();
        orderRepository.deleteAll();
        cartItemRepository.deleteAll();
        cartRepository.deleteAll();
        
        System.out.println("\n=== TEST 1: Configuración - Crear usuario, producto y orden ===");
        
        // Crear usuario
        User testUser = User.builder()
                .email(UNIQUE_EMAIL)
                .password("password123")
                .nombre("Payment")
                .apellido("Tester")
                .telefono("555-0004")
                .direccion("Payment Test Address")
                .build();
        
        User savedUser = userRepository.save(testUser);
        userId = savedUser.getId().toString();
        
        // Crear producto
        Product testProduct = Product.builder()
                .nombre("Laptop MSI GS66 Stealth")
                .descripcion("Laptop gaming ultra portátil")
                .precio(new BigDecimal("1899.99"))
                .stock(20)
                .marca("MSI")
                .procesador("Intel Core i9-11980HK")
                .ram(32)
                .almacenamiento(1024)
                .pantalla("15.6 pulgadas FHD 360Hz")
                .gpu("NVIDIA RTX 3080")
                .peso(new BigDecimal("1.98"))
                .build();
        
        Product savedProduct = productRepository.save(testProduct);
        productId = savedProduct.getId().toString();
        
        ProductImage mainImage = ProductImage.builder()
                .url("https://example.com/msi-gs66.jpg")
                .orden(0)
                .descripcion("Imagen principal")
                .product(savedProduct)
                .build();
        productImageRepository.save(mainImage);
        
        // Agregar producto al carrito usando DTO
        AddToCartDTO addToCart = AddToCartDTO.builder()
                .productId(Long.parseLong(productId))
                .cantidad(1)
                .build();
        
        mockMvc.perform(post("/api/cart/user/" + userId + "/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(addToCart)))
                .andExpect(status().isOk());
        
        // Crear orden usando CreateOrderDTO
        CreateOrderDTO orderDTO = CreateOrderDTO.builder()
                .direccionEnvio("Calle Payment 123")
                .build();
        
        MvcResult result = mockMvc.perform(post("/api/orders/user/" + userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(orderDTO)))
                .andExpect(status().isOk())
                .andReturn();

        String response = result.getResponse().getContentAsString();
        com.fasterxml.jackson.databind.JsonNode jsonNode = objectMapper.readTree(response);
        orderId = jsonNode.get("id").asText();
        
        // Obtener el payment creado automáticamente con la orden
        Payment payment = paymentRepository.findAll().stream()
                .filter(p -> p.getOrder().getId().toString().equals(orderId))
                .findFirst()
                .orElseThrow();
        paymentId = payment.getId().toString();
        
        System.out.println("✅ TEST 1 PASÓ: Usuario, producto, orden y payment creados");
        System.out.println("   Usuario ID: " + userId);
        System.out.println("   Orden ID: " + orderId);
        System.out.println("   Payment ID: " + paymentId + "\n");
    }

    /**
     * TEST 2: Buscar payment por ID (GET /api/payments/{paymentId})
     */
    @Test
    @org.junit.jupiter.api.Order(2)
    public void test2_FindPaymentById() throws Exception {
        System.out.println("\n=== TEST 2: Buscar payment por ID (GET /api/payments/{paymentId}) ===");
        
        mockMvc.perform(get("/api/payments/" + paymentId))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(paymentId))
                .andExpect(jsonPath("$.estado").value("PENDIENTE"))
                .andExpect(jsonPath("$.monto").value(1899.99));
        
        System.out.println("✅ TEST 2 PASÓ: Payment encontrado por ID\n");
    }

    /**
     * TEST 3: Actualizar estado del payment (PUT /api/payments/{paymentId}/status/{estado})
     */
    @Test
    @org.junit.jupiter.api.Order(3)
    public void test3_UpdatePaymentStatus() throws Exception {
        System.out.println("\n=== TEST 3: Actualizar estado del payment (PUT /api/payments/{paymentId}/status/{estado}) ===");
        
        mockMvc.perform(put("/api/payments/" + paymentId + "/status/COMPLETADO"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(paymentId))
                .andExpect(jsonPath("$.estado").value("COMPLETADO"));
        
        System.out.println("✅ TEST 3 PASÓ: Estado del payment actualizado\n");
    }

    /**
     * TEST 4: Asignar Stripe ID al payment (PUT /api/payments/{paymentId}/stripe-id)
     */
    @Test
    @org.junit.jupiter.api.Order(4)
    public void test4_SetStripePaymentId() throws Exception {
        System.out.println("\n=== TEST 4: Asignar Stripe ID al payment (PUT /api/payments/{paymentId}/stripe-id) ===");
        
        String stripeId = "pi_1A2B3C4D5E6F7G8H9I0J";
        
        mockMvc.perform(put("/api/payments/" + paymentId + "/stripe-id")
                        .param("value", stripeId))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(paymentId))
                .andExpect(jsonPath("$.stripePaymentId").value(stripeId));
        
        System.out.println("✅ TEST 4 PASÓ: Stripe ID asignado al payment\n");
    }

    /**
     * TEST 5: Buscar payment por Stripe ID (GET /api/payments/stripe/{stripePaymentId})
     */
    @Test
    @org.junit.jupiter.api.Order(5)
    public void test5_FindPaymentByStripeId() throws Exception {
        System.out.println("\n=== TEST 5: Buscar payment por Stripe ID (GET /api/payments/stripe/{stripePaymentId}) ===");
        
        String stripeId = "pi_1A2B3C4D5E6F7G8H9I0J";
        
        mockMvc.perform(get("/api/payments/stripe/" + stripeId))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stripePaymentId").value(stripeId))
                .andExpect(jsonPath("$.estado").value("COMPLETADO"));
        
        System.out.println("✅ TEST 5 PASÓ: Payment encontrado por Stripe ID\n");
    }

    /**
     * TEST 6: Simular pago exitoso (POST /api/payments/{paymentId}/simulate)
     */
    @Test
    @org.junit.jupiter.api.Order(6)
    public void test6_SimulateSuccessPayment() throws Exception {
        System.out.println("\n=== TEST 6: Simular pago exitoso (POST /api/payments/{paymentId}/simulate) ===");
        
        // Crear una nueva orden y payment para esta prueba
        AddToCartDTO addToCart = AddToCartDTO.builder()
                .productId(Long.parseLong(productId))
                .cantidad(1)
                .build();
        
        mockMvc.perform(post("/api/cart/user/" + userId + "/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(addToCart)))
                .andExpect(status().isOk());
        
        CreateOrderDTO orderDTO = CreateOrderDTO.builder()
                .direccionEnvio("Calle Simulate 456")
                .build();
        
        MvcResult orderResult = mockMvc.perform(post("/api/orders/user/" + userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(orderDTO)))
                .andExpect(status().isOk())
                .andReturn();

        String orderResponse = orderResult.getResponse().getContentAsString();
        com.fasterxml.jackson.databind.JsonNode orderNode = objectMapper.readTree(orderResponse);
        String newOrderId = orderNode.get("id").asText();
        
        // Obtener el payment de la nueva orden
        Payment newPayment = paymentRepository.findAll().stream()
                .filter(p -> p.getOrder().getId().toString().equals(newOrderId))
                .findFirst()
                .orElseThrow();
        
        mockMvc.perform(post("/api/payments/" + newPayment.getId() + "/simulate")
                        .param("success", "true"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("COMPLETADO"));
        
        System.out.println("✅ TEST 6 PASÓ: Pago simulado exitosamente\n");
    }

    /**
     * TEST 7: Simular pago fallido (POST /api/payments/{paymentId}/simulate)
     */
    @Test
    @org.junit.jupiter.api.Order(7)
    public void test7_SimulateFailedPayment() throws Exception {
        System.out.println("\n=== TEST 7: Simular pago fallido (POST /api/payments/{paymentId}/simulate) ===");
        
        // Crear una nueva orden y payment para esta prueba
        AddToCartDTO addToCart = AddToCartDTO.builder()
                .productId(Long.parseLong(productId))
                .cantidad(1)
                .build();
        
        mockMvc.perform(post("/api/cart/user/" + userId + "/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(addToCart)))
                .andExpect(status().isOk());
        
        CreateOrderDTO orderDTO = CreateOrderDTO.builder()
                .direccionEnvio("Calle Failed 789")
                .build();
        
        MvcResult orderResult = mockMvc.perform(post("/api/orders/user/" + userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(orderDTO)))
                .andExpect(status().isOk())
                .andReturn();

        String orderResponse = orderResult.getResponse().getContentAsString();
        com.fasterxml.jackson.databind.JsonNode orderNode = objectMapper.readTree(orderResponse);
        String newOrderId = orderNode.get("id").asText();
        
        // Obtener el payment de la nueva orden
        Payment newPayment = paymentRepository.findAll().stream()
                .filter(p -> p.getOrder().getId().toString().equals(newOrderId))
                .findFirst()
                .orElseThrow();
        
        mockMvc.perform(post("/api/payments/" + newPayment.getId() + "/simulate")
                        .param("success", "false"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("FALLIDO"));
        
        System.out.println("✅ TEST 7 PASÓ: Pago fallido simulado correctamente\n");
    }

    /**
     * TEST 8: Crear payment final para verificación manual
     */
    @Test
    @org.junit.jupiter.api.Order(8)
    public void test8_CreateFinalPaymentForVerification() throws Exception {
        System.out.println("\n=== TEST 8: Crear payment final para verificación manual ===");
        
        // Crear una nueva orden y payment
        AddToCartDTO addToCart = AddToCartDTO.builder()
                .productId(Long.parseLong(productId))
                .cantidad(1)
                .build();
        
        mockMvc.perform(post("/api/cart/user/" + userId + "/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(addToCart)))
                .andExpect(status().isOk());
        
        CreateOrderDTO orderDTO = CreateOrderDTO.builder()
                .direccionEnvio("Calle Final Verification")
                .build();
        
        MvcResult result = mockMvc.perform(post("/api/orders/user/" + userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(orderDTO)))
                .andExpect(status().isOk())
                .andReturn();

        String response = result.getResponse().getContentAsString();
        com.fasterxml.jackson.databind.JsonNode jsonNode = objectMapper.readTree(response);
        String finalOrderId = jsonNode.get("id").asText();
        
        // Obtener el payment
        Payment finalPayment = paymentRepository.findAll().stream()
                .filter(p -> p.getOrder().getId().toString().equals(finalOrderId))
                .findFirst()
                .orElseThrow();
        
        System.out.println("✅ TEST 8 PASÓ: Payment final creado con ID: " + finalPayment.getId());
        System.out.println("📋 Verifica en tu gestor de BD los payments del usuario: payment.test@laptophub.com\n");
    }
}
