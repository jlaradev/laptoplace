package com.laptophub.backend;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.laptophub.backend.dto.CreateReviewDTO;
import com.laptophub.backend.model.Brand;
import com.laptophub.backend.model.Product;
import com.laptophub.backend.repository.BrandRepository;
import com.laptophub.backend.repository.ProductRepository;
import com.laptophub.backend.repository.ReviewRepository;
import com.laptophub.backend.repository.UserRepository;
import com.laptophub.backend.support.TestAuthHelper;
import com.laptophub.backend.support.TestAuthHelper.AuthInfo;
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
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;

/**
 * Tests de endpoints CRUD para Review
 * Ejecuta las pruebas en orden específico para mantener consistencia
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@SuppressWarnings("null")
public class ReviewControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private BrandRepository brandRepository;

    private static String userId;
    private static String productId;
    private static String reviewId;
        private static String userToken;
    
    private static final String UNIQUE_EMAIL = "review.test." + System.currentTimeMillis() + "@laptophub.com";

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
        reviewRepository.deleteAll();
        userRepository.deleteAll();
        
        System.out.println("\n=== TEST 1: Configuración - Crear usuario y producto ===");
        
        AuthInfo authInfo = TestAuthHelper.registerAndLogin(
                mockMvc,
                objectMapper,
                UNIQUE_EMAIL,
                "password123",
                "Reviewer",
                "Test"
        );
        userId = authInfo.getUserId();
        userToken = authInfo.getToken();
        
        // Crear marca primero
        Brand asusBrand = Brand.builder()
                .nombre("ASUS")
                .descripcion("Marca ASUS")
                .build();
        Brand savedBrand = brandRepository.save(asusBrand);
        
        // Crear producto de prueba (sin imagenUrl deprecated)
        Product testProduct = Product.builder()
                .nombre("Laptop Asus ROG Strix")
                .descripcion("Laptop gaming de alto rendimiento")
                .precio(new BigDecimal("1499.99"))
                .stock(30)
                .brand(savedBrand)
                .procesador("AMD Ryzen 9 5900HX")
                .ram(32)
                .almacenamiento(1024)
                .pantalla("17.3 pulgadas QHD 165Hz")
                .gpu("NVIDIA RTX 3070")
                .peso(new BigDecimal("2.9"))
                .build();
        
        Product savedProduct = productRepository.save(testProduct);
        productId = savedProduct.getId().toString();
        
        System.out.println("✅ TEST 1 PASÓ: Usuario creado con ID: " + userId);
        System.out.println("✅ Producto creado con ID: " + productId + "\n");
    }

    /**
     * TEST 2: Crear review (POST /api/reviews)
     */
    @Test
    @Order(2)
    public void test2_CreateReview() throws Exception {
        System.out.println("\n=== TEST 2: Crear nueva review (POST /api/reviews) ===");
        
        CreateReviewDTO reviewDTO = CreateReviewDTO.builder()
                .productId(Long.parseLong(productId))
                .rating(5)
                .comentario("Excelente laptop, superó mis expectativas")
                .build();
        
        MvcResult result = mockMvc.perform(post("/api/reviews")
                        .header("Authorization", "Bearer " + userToken)
                        .param("userId", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reviewDTO)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.rating").value(5))
                .andExpect(jsonPath("$.comentario").value("Excelente laptop, superó mis expectativas"))
                .andExpect(jsonPath("$.userNombre").exists())
                .andReturn();

        String response = result.getResponse().getContentAsString();
        com.fasterxml.jackson.databind.JsonNode jsonNode = objectMapper.readTree(response);
        reviewId = jsonNode.get("id").asText();
        
        System.out.println("✅ TEST 2 PASÓ: Review creada con ID: " + reviewId + "\n");
    }

    /**
     * TEST 3: Obtener reviews por producto (GET /api/reviews/product/{productId}) con paginación
     */
    @Test
    @Order(3)
    public void test3_GetReviewsByProduct() throws Exception {
        System.out.println("\n=== TEST 3: Obtener reviews por producto (GET /api/reviews/product/{productId}) ===");
        
        mockMvc.perform(get("/api/reviews/product/" + productId)
                        .param("page", "0")
                        .param("size", "10"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].id").exists())
                .andExpect(jsonPath("$.content[0].rating").value(5))
                .andExpect(jsonPath("$.totalElements").exists())
                .andExpect(jsonPath("$.totalPages").exists());
        
        System.out.println("✅ TEST 3 PASÓ: Reviews obtenidas por producto (paginadas)\n");
    }

    /**
     * TEST 4: Obtener review específica de usuario para producto (GET /api/reviews/product/{productId}/user/{userId})
     */
    @Test
    @Order(4)
    public void test4_GetUserReviewForProduct() throws Exception {
        System.out.println("\n=== TEST 4: Obtener review de usuario específico (GET /api/reviews/product/{productId}/user/{userId}) ===");
        
        mockMvc.perform(get("/api/reviews/product/" + productId + "/user/" + userId)
                        .header("Authorization", "Bearer " + userToken))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(reviewId))
                .andExpect(jsonPath("$.rating").value(5))
                .andExpect(jsonPath("$.comentario").value("Excelente laptop, superó mis expectativas"));
        
        System.out.println("✅ TEST 4 PASÓ: Review específica del usuario obtenida\n");
    }

    /**
     * TEST 5: Actualizar review (PUT /api/reviews/{reviewId})
     */
    @Test
    @Order(5)
    public void test5_UpdateReview() throws Exception {
        System.out.println("\n=== TEST 5: Actualizar review (PUT /api/reviews/{reviewId}) ===");
        
        com.laptophub.backend.dto.UpdateReviewDTO updateDTO = com.laptophub.backend.dto.UpdateReviewDTO.builder()
                .rating(4)
                .comentario("Muy buena laptop, solo el ventilador es un poco ruidoso")
                .build();
        
        mockMvc.perform(put("/api/reviews/" + reviewId)
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDTO)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(reviewId))
                .andExpect(jsonPath("$.rating").value(4))
                .andExpect(jsonPath("$.comentario").value("Muy buena laptop, solo el ventilador es un poco ruidoso"));
        
        System.out.println("✅ TEST 5 PASÓ: Review actualizada correctamente\n");
    }

    /**
     * TEST 6: Calcular promedio de ratings (GET /api/reviews/product/{productId}/average)
     */
    @Test
    @Order(6)
    public void test6_CalculateAverageRating() throws Exception {
        System.out.println("\n=== TEST 6: Calcular promedio de ratings (GET /api/reviews/product/{productId}/average) ===");
        
        mockMvc.perform(get("/api/reviews/product/" + productId + "/average"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isNumber());
        
        System.out.println("✅ TEST 6 PASÓ: Promedio de ratings calculado\n");
    }

    /**
     * TEST 7: Eliminar review (DELETE /api/reviews/{reviewId})
     */
    @Test
    @Order(7)
    public void test7_DeleteReview() throws Exception {
        System.out.println("\n=== TEST 7: Eliminar review (DELETE /api/reviews/{reviewId}) ===");
        
        mockMvc.perform(delete("/api/reviews/" + reviewId)
                        .header("Authorization", "Bearer " + userToken))
                .andDo(print())
                .andExpect(status().isOk());
        
        System.out.println("✅ TEST 7 PASÓ: Review eliminada correctamente\n");
    }

    /**
     * TEST 8: Crear review final para verificación manual
     */
    @Test
    @Order(8)
    public void test8_CreateFinalReviewForVerification() throws Exception {
        System.out.println("\n=== TEST 8: Crear review final para verificación manual ===");
        
        CreateReviewDTO finalReviewDTO = CreateReviewDTO.builder()
                .productId(Long.parseLong(productId))
                .rating(5)
                .comentario("Producto verificado - Review final de prueba")
                .build();
        
        MvcResult result = mockMvc.perform(post("/api/reviews")
                        .header("Authorization", "Bearer " + userToken)
                        .param("userId", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(finalReviewDTO)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.rating").value(5))
                .andReturn();

        String response = result.getResponse().getContentAsString();
        com.fasterxml.jackson.databind.JsonNode jsonNode = objectMapper.readTree(response);
        
        System.out.println("✅ TEST 8 PASÓ: Review final creada con ID: " + jsonNode.get("id").asText());
        System.out.println("📋 Verifica en tu gestor de BD:");
        System.out.println("   - Review del usuario: " + UNIQUE_EMAIL);
        System.out.println("   - Review con rating 5 y comentario final\n");
    }
}
