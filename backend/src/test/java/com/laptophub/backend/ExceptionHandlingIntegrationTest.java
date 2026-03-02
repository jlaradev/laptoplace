package com.laptophub.backend;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.laptophub.backend.dto.AddToCartDTO;
import com.laptophub.backend.dto.CreateReviewDTO;
import com.laptophub.backend.exception.ApiResponse;
import com.laptophub.backend.exception.GlobalExceptionHandler;
import com.laptophub.backend.model.Brand;
import com.laptophub.backend.model.Product;
import com.laptophub.backend.model.User;
import com.laptophub.backend.repository.BrandRepository;
import com.laptophub.backend.repository.ProductRepository;
import com.laptophub.backend.repository.UserRepository;
import com.laptophub.backend.support.TestAuthHelper;
import com.laptophub.backend.support.TestAuthHelper.AuthInfo;
import com.stripe.exception.StripeException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.mockito.Mockito;

import jakarta.servlet.http.HttpServletRequest;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@SuppressWarnings("null")
public class ExceptionHandlingIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private BrandRepository brandRepository;

        @Autowired
        private PasswordEncoder passwordEncoder;

    private User testUser;
    private Product testProduct;
        private String userToken;
        private String adminToken;

    @BeforeEach
    @SuppressWarnings("null")
        public void setup() throws Exception {
        // Limpiar datos previos para evitar conflictos de constrainta única
        productRepository.deleteAll();
        brandRepository.deleteAll();
        
        AuthInfo authInfo = TestAuthHelper.registerAndLogin(
                mockMvc,
                objectMapper,
                TestAuthHelper.uniqueEmail("exception.test"),
                "test1234",
                "Test",
                "User"
        );
        testUser = userRepository.findById(java.util.UUID.fromString(authInfo.getUserId()))
                .orElseThrow();
        userToken = authInfo.getToken();
        adminToken = TestAuthHelper.createAdminAndLogin(
                userRepository,
                passwordEncoder,
                mockMvc,
                objectMapper,
                TestAuthHelper.uniqueEmail("exception.admin"),
                "admin123"
        );

        // Crear marca primero
        Brand testBrand = Brand.builder()
                .nombre("TestBrand")
                .descripcion("Test Brand")
                .build();
        Brand savedBrand = brandRepository.save(testBrand);

        testProduct = Product.builder()
                .nombre("Test Product")
                .descripcion("Test Description")
                .precio(BigDecimal.valueOf(999.99))
                .stock(10)
                .brand(savedBrand)
                .build();
        productRepository.save(testProduct);
    }

    @Test
    public void testResourceNotFoundExceptionWhenProductDoesNotExist() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/products/99999"))
                .andExpect(status().isNotFound())
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        System.out.println("\n=== TEST 1: Producto no encontrado ===");
        System.out.println("Response: " + responseBody);
        
        ApiResponse<?> response = objectMapper.readValue(responseBody, ApiResponse.class);

        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getMessage()).contains("Producto no encontrado");
        assertThat(response.getTimestamp()).isNotNull();
        assertThat(response.getPath()).isEqualTo("/api/products/99999");
    }

    @Test
    @SuppressWarnings("null")
    public void testConflictExceptionWhenRegisteringDuplicateEmail() throws Exception {
        User duplicateUser = User.builder()
                .email(testUser.getEmail())
                .password("password123")
                .nombre("Another")
                .apellido("User")
                .telefono("555-5678")
                .direccion("Another Address")
                .build();

        MvcResult result = mockMvc.perform(post("/api/users/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(duplicateUser)))
                .andExpect(status().isConflict())
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        System.out.println("\n=== TEST 2: Email duplicado (ConflictException) ===");
        System.out.println("Response: " + responseBody);
        
        ApiResponse<?> response = objectMapper.readValue(responseBody, ApiResponse.class);

        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getMessage()).contains("ya está registrado");
        assertThat(response.getTimestamp()).isNotNull();
    }

    @Test
    @SuppressWarnings("null")
    public void testValidationExceptionWhenAddingInvalidQuantityToCart() throws Exception {
        UUID userId = testUser.getId();
        Long productId = testProduct.getId();
        
        AddToCartDTO invalidDto = AddToCartDTO.builder()
                .productId(productId)
                .cantidad(0) // Inválido
                .build();

        MvcResult result = mockMvc.perform(post("/api/cart/user/" + userId + "/items")
                .header("Authorization", "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidDto)))
                .andExpect(status().isBadRequest())
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        System.out.println("\n=== TEST 3: Cantidad inválida en carrito (ValidationException) ===");
        System.out.println("Response: " + responseBody);
        
        ApiResponse<?> response = objectMapper.readValue(responseBody, ApiResponse.class);

        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getMessage()).contains("cantidad debe ser mayor a 0");
        assertThat(response.getTimestamp()).isNotNull();
    }

    @Test
    @SuppressWarnings("null")
    public void testValidationExceptionWhenAddingInvalidRatingToReview() throws Exception {
        CreateReviewDTO invalidDto = CreateReviewDTO.builder()
                .productId(testProduct.getId())
                .rating(6) // Inválido (debe estar entre 1 y 5)
                .comentario("Invalid rating test")
                .build();
        
        MvcResult result = mockMvc.perform(post("/api/reviews")
                .header("Authorization", "Bearer " + userToken)
                .param("userId", testUser.getId().toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidDto)))
                .andExpect(status().isBadRequest())
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        System.out.println("\n=== TEST 4: Rating inválido en review (ValidationException) ===");
        System.out.println("Response: " + responseBody);
        
        ApiResponse<?> response = objectMapper.readValue(responseBody, ApiResponse.class);

        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getMessage()).contains("rating debe estar entre 1 y 5");
        assertThat(response.getTimestamp()).isNotNull();
    }

    @Test
    public void testResourceNotFoundExceptionWhenProductToDeleteDoesNotExist() throws Exception {
        MvcResult result = mockMvc.perform(delete("/api/products/99999")
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound())
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        System.out.println("\n=== TEST 5: Producto no encontrado para eliminar (ResourceNotFoundException) ===");
        System.out.println("Response: " + responseBody);
        
        ApiResponse<?> response = objectMapper.readValue(responseBody, ApiResponse.class);

        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getMessage()).contains("Producto no encontrado");
        assertThat(response.getTimestamp()).isNotNull();
        assertThat(response.getPath()).isEqualTo("/api/products/99999");
    }

    @Test
    public void testApiResponseStructureIncludesAllRequiredFields() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/products/99999"))
                .andExpect(status().isNotFound())
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        System.out.println("\n=== TEST 6: Validar estructura completa de ApiResponse ===");
        System.out.println("Response: " + responseBody);
        
        ApiResponse<?> response = objectMapper.readValue(responseBody, ApiResponse.class);

        assertThat(response).isNotNull();
        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getMessage()).isNotNull();
        assertThat(response.getTimestamp()).isNotNull();
        assertThat(response.getPath()).isNotNull();
        System.out.println("\n✅ Todos los tests de excepciones completados exitosamente");
    }

        @Test
        public void testStripeExceptionHandlerStructure() {
                GlobalExceptionHandler handler = new GlobalExceptionHandler();
                StripeException stripeException = Mockito.mock(StripeException.class);
                HttpServletRequest request = Mockito.mock(HttpServletRequest.class);

                Mockito.when(stripeException.getMessage()).thenReturn("Stripe error");
                Mockito.when(request.getRequestURI()).thenReturn("/api/payments/create");

                ResponseEntity<ApiResponse<?>> responseEntity = handler.handleStripeException(stripeException, request);
                ApiResponse<?> response = responseEntity.getBody();

                System.out.println("\n=== TEST 7: StripeException (handler) ===");
                System.out.println("Response: " + objectMapper.valueToTree(response));

                assertThat(responseEntity.getStatusCode().value()).isEqualTo(502);
                assertThat(response).isNotNull();
                assertThat(response.isSuccess()).isFalse();
                assertThat(response.getMessage()).contains("Stripe error");
                assertThat(response.getTimestamp()).isNotNull();
                assertThat(response.getPath()).isEqualTo("/api/payments/create");
        }
}
