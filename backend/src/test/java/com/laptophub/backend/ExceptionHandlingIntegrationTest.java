package com.laptophub.backend;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.laptophub.backend.exception.ApiResponse;
import com.laptophub.backend.model.Product;
import com.laptophub.backend.model.User;
import com.laptophub.backend.repository.ProductRepository;
import com.laptophub.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class ExceptionHandlingIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductRepository productRepository;

    private User testUser;
    private Product testProduct;

    @BeforeEach
    @SuppressWarnings("null")
    public void setup() {
        testUser = User.builder()
                .email("exception.test." + System.currentTimeMillis() + "@test.com")
                .password("test123")
                .nombre("Test")
                .apellido("User")
                .telefono("555-1234")
                .direccion("Test Address")
                .build();
        userRepository.save(testUser);

        testProduct = Product.builder()
                .nombre("Test Product")
                .descripcion("Test Description")
                .precio(BigDecimal.valueOf(999.99))
                .stock(10)
                .marca("TestBrand")
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

        MvcResult result = mockMvc.perform(post("/api/cart/user/" + userId + "/items")
                .param("productId", String.valueOf(productId))
                .param("cantidad", "0")) // Cantidad inválida
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
        MvcResult result = mockMvc.perform(post("/api/reviews")
                .param("productId", String.valueOf(testProduct.getId()))
                .param("userId", testUser.getId().toString())
                .param("rating", "6") // Rating inválido (debe estar entre 1 y 5)
                .param("comentario", "Invalid rating test"))
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
        MvcResult result = mockMvc.perform(delete("/api/products/99999"))
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
}
