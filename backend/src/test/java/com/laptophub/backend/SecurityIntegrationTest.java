package com.laptophub.backend;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.laptophub.backend.dto.ProductCreateDTO;
import com.laptophub.backend.model.Brand;
import com.laptophub.backend.support.TestAuthHelper;
import com.laptophub.backend.support.TestAuthHelper.AuthInfo;
import com.laptophub.backend.repository.BrandRepository;
import com.laptophub.backend.repository.OrderItemRepository;
import com.laptophub.backend.repository.ProductRepository;
import com.laptophub.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@SuppressWarnings("null")
public class SecurityIntegrationTest {

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
    private OrderItemRepository orderItemRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String userToken;
    private String adminToken;
    private String userId;

    @BeforeEach
    public void setup() throws Exception {
        AuthInfo authInfo = TestAuthHelper.registerAndLogin(
                mockMvc,
                objectMapper,
                TestAuthHelper.uniqueEmail("security.user"),
                "password123",
                "Security",
                "User"
        );
        userToken = authInfo.getToken();
        userId = authInfo.getUserId();

        adminToken = TestAuthHelper.createAdminAndLogin(
                userRepository,
                passwordEncoder,
                mockMvc,
                objectMapper,
                TestAuthHelper.uniqueEmail("security.admin"),
                "admin123"
        );
    }

    @Test
    public void testPublicEndpointsAccessibleWithoutAuth() throws Exception {
        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk());
    }

    @Test
    public void testProtectedEndpointRequiresAuth() throws Exception {
        mockMvc.perform(get("/api/cart/user/" + userId))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/cart/user/" + userId)
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk());
    }

    @Test
    public void testAdminOnlyProductCreation() throws Exception {
        orderItemRepository.deleteAll();
        productRepository.deleteAll();
        brandRepository.deleteAll();

        // Crear una marca primero
        Brand testBrand = Brand.builder()
                .nombre("TestBrand")
                .descripcion("Test Brand")
                .build();
        Brand savedBrand = brandRepository.save(testBrand);

        ProductCreateDTO newProduct = ProductCreateDTO.builder()
                .nombre("Laptop Test Security")
                .descripcion("Producto de prueba seguridad")
                .precio(new BigDecimal("999.99"))
                .stock(10)
                .brandId(savedBrand.getId())
                .build();

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newProduct)))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/products")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newProduct)))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/products")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newProduct)))
                .andExpect(status().isOk());
    }
}
