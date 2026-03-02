package com.laptophub.backend;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.laptophub.backend.dto.BrandCreateDTO;
import com.laptophub.backend.dto.BrandResponseDTO;
import com.laptophub.backend.repository.BrandRepository;
import com.laptophub.backend.repository.ProductRepository;
import com.laptophub.backend.repository.UserRepository;
import com.laptophub.backend.support.TestAuthHelper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;

/**
 * Tests para Brand CRUD
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@SuppressWarnings("null")
public class BrandControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private BrandRepository brandRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private static String brandId;
    private static final String TEST_BRAND_NAME = "Dell";
    private static String adminToken;

    /**
     * TEST 1: Crear marca (POST /api/brands)
     */
    @Test
    @Order(1)
    public void test1_CreateBrand() throws Exception {
        // Limpiar BD
        productRepository.deleteAll();
        brandRepository.deleteAll();

        System.out.println("\n=== TEST 1: Crear nueva marca (POST /api/brands) ===");

        adminToken = TestAuthHelper.createAdminAndLogin(
                userRepository,
                passwordEncoder,
                mockMvc,
                objectMapper,
                TestAuthHelper.uniqueEmail("brand.admin"),
                "admin123"
        );

        BrandCreateDTO newBrand = BrandCreateDTO.builder()
                .nombre(TEST_BRAND_NAME)
                .descripcion("Marca Dell - Computadoras de alto rendimiento")
                .build();

        MvcResult result = mockMvc.perform(post("/api/brands")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newBrand)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.nombre").value(TEST_BRAND_NAME))
                .andExpect(jsonPath("$.descripcion").value("Marca Dell - Computadoras de alto rendimiento"))
                .andReturn();

        String response = result.getResponse().getContentAsString();
        BrandResponseDTO createdBrand = objectMapper.readValue(response, BrandResponseDTO.class);
        brandId = createdBrand.getId().toString();

        System.out.println("✅ TEST 1 PASÓ: Marca creada con ID: " + brandId + "\n");
    }

    /**
     * TEST 2: Buscar marca por ID (GET /api/brands/{id})
     */
    @Test
    @Order(2)
    public void test2_FindBrandById() throws Exception {
        System.out.println("\n=== TEST 2: Buscar marca por ID (GET /api/brands/{id}) ===");

        mockMvc.perform(get("/api/brands/" + brandId)
                        .header("Authorization", "Bearer " + adminToken))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(brandId))
                .andExpect(jsonPath("$.nombre").value(TEST_BRAND_NAME));

        System.out.println("✅ TEST 2 PASÓ: Marca encontrada por ID\n");
    }

    /**
     * TEST 3: Listar todas las marcas (GET /api/brands)
     */
    @Test
    @Order(3)
    public void test3_FindAllBrands() throws Exception {
        System.out.println("\n=== TEST 3: Listar todas las marcas (GET /api/brands) ===");

        mockMvc.perform(get("/api/brands")
                        .param("page", "0")
                        .param("size", "10")
                        .header("Authorization", "Bearer " + adminToken))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").exists());

        System.out.println("✅ TEST 3 PASÓ: Lista de marcas obtenida\n");
    }

    /**
     * TEST 4: Actualizar marca (PUT /api/brands/{id})
     */
    @Test
    @Order(4)
    public void test4_UpdateBrand() throws Exception {
        System.out.println("\n=== TEST 4: Actualizar marca (PUT /api/brands/{id}) ===");

        BrandCreateDTO updatedBrand = BrandCreateDTO.builder()
                .nombre("Dell Inc.")
                .descripcion("Marca Dell Inc. - Actualizado")
                .build();

        mockMvc.perform(put("/api/brands/" + brandId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updatedBrand)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Dell Inc."))
                .andExpect(jsonPath("$.descripcion").value("Marca Dell Inc. - Actualizado"));

        System.out.println("✅ TEST 4 PASÓ: Marca actualizada\n");
    }

    /**
     * TEST 5: Validar marca duplicada
     */
    @Test
    @Order(5)
    public void test5_CreateDuplicateBrand() throws Exception {
        System.out.println("\n=== TEST 5: Validar marca duplicada (error esperado) ===");

        BrandCreateDTO duplicateBrand = BrandCreateDTO.builder()
                .nombre("Dell Inc.")
                .descripcion("Intento duplicado")
                .build();

        mockMvc.perform(post("/api/brands")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(duplicateBrand)))
                .andDo(print())
                .andExpect(status().is4xxClientError());

        System.out.println("✅ TEST 5 PASÓ: Marca duplicada rechazada correctamente\n");
    }

    /**
     * TEST 6: Eliminar marca (DELETE /api/brands/{id})
     */
    @Test
    @Order(6)
    public void test6_DeleteBrand() throws Exception {
        System.out.println("\n=== TEST 6: Eliminar marca (DELETE /api/brands/{id}) ===");

        mockMvc.perform(delete("/api/brands/" + brandId)
                        .header("Authorization", "Bearer " + adminToken))
                .andDo(print())
                .andExpect(status().isOk());

        // Verificar que fue eliminada
        mockMvc.perform(get("/api/brands/" + brandId)
                        .header("Authorization", "Bearer " + adminToken))
                .andDo(print())
                .andExpect(status().isNotFound());

        System.out.println("✅ TEST 6 PASÓ: Marca eliminada correctamente\n");
    }
}
