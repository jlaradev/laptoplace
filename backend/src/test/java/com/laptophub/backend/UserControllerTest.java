package com.laptophub.backend;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.laptophub.backend.dto.UserRegisterDTO;
import com.laptophub.backend.dto.UserResponseDTO;
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
import org.springframework.security.crypto.password.PasswordEncoder;
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

        @Autowired
        private PasswordEncoder passwordEncoder;

    private static String userId; // Para compartir entre tests
    private static final String TEST_EMAIL = "test.user@laptophub.com";
        private static String userToken;
        private static String adminToken;

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
        
        AuthInfo authInfo = TestAuthHelper.registerAndLogin(
                mockMvc,
                objectMapper,
                TEST_EMAIL,
                "password123",
                "Juan",
                "Pérez"
        );
        userId = authInfo.getUserId();
        userToken = authInfo.getToken();
        adminToken = TestAuthHelper.createAdminAndLogin(
                userRepository,
                passwordEncoder,
                mockMvc,
                objectMapper,
                TestAuthHelper.uniqueEmail("user.admin"),
                "admin123"
        );
        
        System.out.println("✅ TEST 1 PASÓ: Usuario creado con ID: " + userId + "\n");
    }

    /**
     * TEST 2: Buscar usuario por ID (GET /api/users/{id})
     */
    @Test
    @Order(2)
    public void test2_FindUserById() throws Exception {
        System.out.println("\n=== TEST 2: Buscar usuario por ID (GET /api/users/{id}) ===");
        
        mockMvc.perform(get("/api/users/" + userId)
                        .header("Authorization", "Bearer " + userToken))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId))
                .andExpect(jsonPath("$.email").value(TEST_EMAIL))
                .andExpect(jsonPath("$.nombre").value("Juan"));
        
        System.out.println("✅ TEST 2 PASÓ: Usuario encontrado por ID\n");
    }

    /**
     * TEST 3: Listar usuarios activos (GET /api/users) — no debe incluir desactivados
     */
    @Test
    @Order(3)
    public void test3_FindAllUsers() throws Exception {
        System.out.println("\n=== TEST 3: Listar usuarios activos (GET /api/users) ===");
        
        // Solo debe aparecer el usuario activo (el admin no aparece aquí porque se filtra por deletedAt IS NULL)
        mockMvc.perform(get("/api/users")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("page", "0")
                        .param("size", "10"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].deletedAt").isEmpty())
                .andExpect(jsonPath("$.totalElements").exists())
                .andExpect(jsonPath("$.totalPages").exists());

        // Lista de inactivos debe estar vacía (ninguno desactivado aún)
        mockMvc.perform(get("/api/users/inactive")
                        .header("Authorization", "Bearer " + adminToken))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
        
        System.out.println("✅ TEST 3 PASÓ: Lista de usuarios activos verificada\n");
    }

    /**
     * TEST 4: Buscar usuario por email (GET /api/users/email?email=...)
     */
    @Test
    @Order(4)
    public void test4_FindUserByEmail() throws Exception {
        System.out.println("\n=== TEST 4: Buscar usuario por email (GET /api/users/email) ===");
        
        mockMvc.perform(get("/api/users/email")
                        .header("Authorization", "Bearer " + userToken)
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
                        .header("Authorization", "Bearer " + userToken)
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

    /**
     * TEST 7: Desactivar usuario (DELETE /api/users/{id}) y verificar que queda en lista inactiva
     */
    @Test
    @Order(7)
    public void test7_DeactivateUser() throws Exception {
        System.out.println("\n=== TEST 7: Desactivar usuario (DELETE /api/users/{id}) ===");

        mockMvc.perform(delete("/api/users/" + userId)
                        .header("Authorization", "Bearer " + adminToken))
                .andDo(print())
                .andExpect(status().isOk());

        // No aparece en lista activa
        mockMvc.perform(get("/api/users")
                        .header("Authorization", "Bearer " + adminToken))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.id == '" + userId + "')]").doesNotExist());

        // Sí aparece en lista inactiva
        mockMvc.perform(get("/api/users/inactive")
                        .header("Authorization", "Bearer " + adminToken))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.id == '" + userId + "')].deletedAt").isNotEmpty());

        System.out.println("✅ TEST 7 PASÓ: Usuario desactivado y aparece en inactivos\n");
    }

    /**
     * TEST 8: Usuario desactivado no puede autenticarse (login debe retornar 400)
     */
    @Test
    @Order(8)
    public void test8_DeactivatedUserCannotLogin() throws Exception {
        System.out.println("\n=== TEST 8: Usuario desactivado no puede autenticarse ===");

        String loginBody = "{\"email\":\"" + TEST_EMAIL + "\",\"password\":\"password123\"}";

        mockMvc.perform(post("/api/auth/login")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(loginBody))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Cuenta desactivada. Contacta con el administrador."));

        System.out.println("✅ TEST 8 PASÓ: Usuario desactivado es rechazado en login\n");
    }

    /**
     * TEST 9: Reactivar usuario (PUT /api/users/{id}/reactivate) y verificar que puede autenticarse
     */
    @Test
    @Order(9)
    public void test9_ReactivateUser() throws Exception {
        System.out.println("\n=== TEST 9: Reactivar usuario (PUT /api/users/{id}/reactivate) ===");

        mockMvc.perform(put("/api/users/" + userId + "/reactivate")
                        .header("Authorization", "Bearer " + adminToken))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId))
                .andExpect(jsonPath("$.deletedAt").isEmpty());

        // Ahora sí puede autenticarse
        String loginBody = "{\"email\":\"" + TEST_EMAIL + "\",\"password\":\"password123\"}";
        mockMvc.perform(post("/api/auth/login")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(loginBody))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty());

        System.out.println("✅ TEST 9 PASÓ: Usuario reactivado puede autenticarse\n");
    }

    /**
     * TEST 10: Cambiar contraseña (PATCH /api/users/{id}/password) y verificar login con la nueva
     */
    @Test
    @Order(10)
    public void test10_ChangePassword() throws Exception {
        System.out.println("\n=== TEST 10: Cambiar contraseña (PATCH /api/users/{id}/password) ===");

        String body = "{\"newPassword\":\"newpass456\"}";

        mockMvc.perform(patch("/api/users/" + userId + "/password")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andDo(print())
                .andExpect(status().isNoContent());

        // Verificar que la contraseña antigua ya no funciona
        String oldLoginBody = "{\"email\":\"" + TEST_EMAIL + "\",\"password\":\"password123\"}";
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(oldLoginBody))
                .andDo(print())
                .andExpect(status().isBadRequest());

        // Verificar que la nueva contraseña sí funciona
        String newLoginBody = "{\"email\":\"" + TEST_EMAIL + "\",\"password\":\"newpass456\"}";
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(newLoginBody))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty());

        System.out.println("✅ TEST 10 PASÓ: Contraseña cambiada y verificada correctamente\n");
    }
}
