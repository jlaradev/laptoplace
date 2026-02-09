package com.laptophub.backend;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.laptophub.backend.dto.UserRegisterDTO;
import com.laptophub.backend.dto.UserResponseDTO;
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
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;

/**
 * Tests de endpoints CRUD para Usuario
 * Ejecuta las pruebas en orden específico para mantener consistencia
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@SuppressWarnings("null")
public class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    private static String userId; // Para compartir entre tests
    private static final String TEST_EMAIL = "test.user@laptophub.com";

    /**
     * Limpia la base de datos una sola vez antes de todos los tests
     */
    @BeforeAll
    public static void setUpDatabase() {
        // La limpieza ocurre una sola vez al inicio
    }

    /**
     * TEST 1: Crear usuario (POST /api/users/register)
     */
    @Test
    @Order(1)
    @SuppressWarnings("null")
    public void test1_CreateUser() throws Exception {
        // Limpiar BD solo antes del primer test
        userRepository.deleteAll();
        
        System.out.println("\n=== TEST 1: Crear nuevo usuario (POST /api/users/register) ===");
        
        UserRegisterDTO newUser = UserRegisterDTO.builder()
                .email(TEST_EMAIL)
                .password("password123")
                .nombre("Juan")
                .apellido("Pérez")
                .telefono("555-1234")
                .direccion("Calle Falsa 123")
                .build();

        MvcResult result = mockMvc.perform(post("/api/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newUser)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.email").value(TEST_EMAIL))
                .andExpect(jsonPath("$.nombre").value("Juan"))
                .andExpect(jsonPath("$.apellido").value("Pérez"))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andReturn();

        // Guardar el ID para los siguientes tests
        String response = result.getResponse().getContentAsString();
        UserResponseDTO createdUser = objectMapper.readValue(response, UserResponseDTO.class);
        userId = createdUser.getId().toString();
        
        System.out.println("✅ TEST 1 PASÓ: Usuario creado con ID: " + userId + "\n");
    }

    /**
     * TEST 2: Buscar usuario por ID (GET /api/users/{id})
     */
    @Test
    @Order(2)
    public void test2_FindUserById() throws Exception {
        System.out.println("\n=== TEST 2: Buscar usuario por ID (GET /api/users/{id}) ===");
        
        mockMvc.perform(get("/api/users/" + userId))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId))
                .andExpect(jsonPath("$.email").value(TEST_EMAIL))
                .andExpect(jsonPath("$.nombre").value("Juan"));
        
        System.out.println("✅ TEST 2 PASÓ: Usuario encontrado por ID\n");
    }

    /**
     * TEST 3: Listar todos los usuarios (GET /api/users) con paginación
     */
    @Test
    @Order(3)
    public void test3_FindAllUsers() throws Exception {
        System.out.println("\n=== TEST 3: Listar todos los usuarios (GET /api/users) ===");
        
        mockMvc.perform(get("/api/users")
                        .param("page", "0")
                        .param("size", "10"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].id").exists())
                .andExpect(jsonPath("$.totalElements").exists())
                .andExpect(jsonPath("$.totalPages").exists());
        
        System.out.println("✅ TEST 3 PASÓ: Lista paginada de usuarios obtenida\n");
    }

    /**
     * TEST 4: Buscar usuario por email (GET /api/users/email?email=...)
     */
    @Test
    @Order(4)
    public void test4_FindUserByEmail() throws Exception {
        System.out.println("\n=== TEST 4: Buscar usuario por email (GET /api/users/email) ===");
        
        mockMvc.perform(get("/api/users/email")
                        .param("email", TEST_EMAIL))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(TEST_EMAIL))
                .andExpect(jsonPath("$.nombre").value("Juan"))
                .andExpect(jsonPath("$.apellido").value("Pérez"));
        
        System.out.println("✅ TEST 4 PASÓ: Usuario encontrado por email\n");
    }

    /**
     * TEST 5: Actualizar usuario (PUT /api/users/{id})
     */
    @Test
    @Order(5)
    @SuppressWarnings("null")
    public void test5_UpdateUser() throws Exception {
        System.out.println("\n=== TEST 5: Actualizar usuario (PUT /api/users/{id}) ===");
        
        com.laptophub.backend.dto.UserUpdateDTO updateData = com.laptophub.backend.dto.UserUpdateDTO.builder()
                .nombre("Juan Carlos")
                .apellido("Pérez González")
                .telefono("555-9999")
                .direccion("Avenida Principal 456")
                .build();

        mockMvc.perform(put("/api/users/" + userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateData)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId))
                .andExpect(jsonPath("$.nombre").value("Juan Carlos"))
                .andExpect(jsonPath("$.apellido").value("Pérez González"))
                .andExpect(jsonPath("$.telefono").value("555-9999"));
        
        System.out.println("✅ TEST 5 PASÓ: Usuario actualizado correctamente\n");
    }

    /**
     * TEST 6: Crear usuario final para verificación manual en BD
     */
    @Test
    @Order(6)
    @SuppressWarnings("null")
    public void test6_CreateFinalUserForManualVerification() throws Exception {
        System.out.println("\n=== TEST 6: Crear usuario final para verificación manual ===");
        
        UserRegisterDTO finalUser = UserRegisterDTO.builder()
                .email("verificacion.manual@laptophub.com")
                .password("password456")
                .nombre("Usuario")
                .apellido("Verificación")
                .telefono("555-0000")
                .direccion("Dirección de Prueba 789")
                .build();

        MvcResult result = mockMvc.perform(post("/api/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(finalUser)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.email").value("verificacion.manual@laptophub.com"))
                .andReturn();

        String response = result.getResponse().getContentAsString();
        UserResponseDTO createdUser = objectMapper.readValue(response, UserResponseDTO.class);
        
        System.out.println("✅ TEST 6 PASÓ: Usuario final creado con ID: " + createdUser.getId());
        System.out.println("📋 Verifica en tu gestor de BD el usuario con email: verificacion.manual@laptophub.com\n");
    }
}
