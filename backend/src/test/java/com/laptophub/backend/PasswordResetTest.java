package com.laptophub.backend;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.laptophub.backend.model.PasswordResetToken;
import com.laptophub.backend.repository.PasswordResetTokenRepository;
import com.laptophub.backend.repository.UserRepository;
import com.laptophub.backend.support.TestAuthHelper;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests de integración para el flujo completo de restablecimiento de contraseña.
 *
 * IMPORTANTE: Este test envía un correo real a juanpablo3501@hotmail.com.
 * Asegúrate de tener MAIL_USERNAME y MAIL_PASSWORD configurados en .env antes de ejecutarlo.
 *
 * Ejecutar con:
 *   cd backend && ./mvnw test -Dtest="PasswordResetTest" -DfailIfNoTests=false
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@SuppressWarnings("null")
public class PasswordResetTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordResetTokenRepository tokenRepository;

    @SuppressWarnings("unused")
    @Autowired
    private PasswordEncoder passwordEncoder;

    private static final String TEST_EMAIL = "juanpablo3501@hotmail.com";
    private static final String ORIGINAL_PASSWORD = "password123";
    private static final String NEW_PASSWORD = "nuevaPass456";
    private static String tokenId;

    /**
     * TEST 1: Registrar usuario de prueba con el email destino del correo
     */
    @Test
    @Order(1)
    public void test1_RegisterUser() throws Exception {
        System.out.println("\n=== TEST 1: Registrar usuario de prueba ===");

        // Limpiar tokens y usuario previos si existen
        tokenRepository.deleteAll();
        userRepository.findByEmail(TEST_EMAIL).ifPresent(u -> userRepository.delete(u));

        TestAuthHelper.registerAndLogin(
                mockMvc, objectMapper,
                TEST_EMAIL, ORIGINAL_PASSWORD,
                "Juan", "Pablo"
        );

        assertTrue(userRepository.findByEmail(TEST_EMAIL).isPresent());
        System.out.println("✅ TEST 1 PASÓ: Usuario registrado con email " + TEST_EMAIL + "\n");
    }

    /**
     * TEST 2: Solicitar reset → endpoint siempre responde 200 y envía correo real
     */
    @Test
    @Order(2)
    public void test2_ForgotPassword_EnviaCorreo() throws Exception {
        System.out.println("\n=== TEST 2: Solicitar restablecimiento (POST /api/auth/forgot-password) ===");

        String body = "{\"email\":\"" + TEST_EMAIL + "\"}";

        mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andDo(print())
                .andExpect(status().isOk());

        // Verificar que se creó el token en BD
        List<PasswordResetToken> tokens = tokenRepository.findAll();
        assertFalse(tokens.isEmpty(), "Debe existir al menos un token tras la solicitud");

        tokenId = tokens.get(0).getId().toString();

        System.out.println("✅ TEST 2 PASÓ: Correo enviado y token generado: " + tokenId);
        System.out.println("   Revisa la bandeja de juanpablo3501@hotmail.com\n");
    }

    /**
     * TEST 3: Solicitar reset con email inexistente → sigue respondiendo 200 (no revela usuarios)
     */
    @Test
    @Order(3)
    public void test3_ForgotPassword_EmailInexistente_Responde200() throws Exception {
        System.out.println("\n=== TEST 3: forgot-password con email inexistente → siempre 200 ===");

        String body = "{\"email\":\"noexiste@ejemplo.com\"}";

        mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andDo(print())
                .andExpect(status().isOk());

        System.out.println("✅ TEST 3 PASÓ: No revela si el email existe o no\n");
    }

    /**
     * TEST 4: Token inválido → 400
     */
    @Test
    @Order(4)
    public void test4_ResetPassword_TokenInvalido() throws Exception {
        System.out.println("\n=== TEST 4: reset-password con token inválido → 400 ===");

        String body = "{\"token\":\"00000000-0000-0000-0000-000000000000\",\"newPassword\":\"" + NEW_PASSWORD + "\"}";

        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andDo(print())
                .andExpect(status().isBadRequest());

        System.out.println("✅ TEST 4 PASÓ: Token inexistente rechazado\n");
    }

    /**
     * TEST 5: Token válido → contraseña cambiada, token marcado como usado
     */
    @Test
    @Order(5)
    public void test5_ResetPassword_TokenValido() throws Exception {
        System.out.println("\n=== TEST 5: reset-password con token válido → 204 ===");

        assertNotNull(tokenId, "El tokenId debe haberse generado en test2");

        String body = "{\"token\":\"" + tokenId + "\",\"newPassword\":\"" + NEW_PASSWORD + "\"}";

        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andDo(print())
                .andExpect(status().isNoContent());

        // Verificar que el token queda marcado como usado
        PasswordResetToken token = tokenRepository.findById(java.util.UUID.fromString(tokenId)).orElseThrow();
        assertTrue(token.isUsed(), "El token debe marcarse como usado");

        System.out.println("✅ TEST 5 PASÓ: Contraseña restablecida y token invalidado\n");
    }

    /**
     * TEST 6: Token ya usado → 400
     */
    @Test
    @Order(6)
    public void test6_ResetPassword_TokenYaUsado() throws Exception {
        System.out.println("\n=== TEST 6: reset-password con token ya usado → 400 ===");

        String body = "{\"token\":\"" + tokenId + "\",\"newPassword\":\"otraPass789\"}";

        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andDo(print())
                .andExpect(status().isBadRequest());

        System.out.println("✅ TEST 6 PASÓ: Token ya utilizado es rechazado\n");
    }

    /**
     * TEST 7: Login con contraseña antigua falla, con la nueva funciona
     */
    @Test
    @Order(7)
    public void test7_VerificarNuevaContrasena() throws Exception {
        System.out.println("\n=== TEST 7: Verificar acceso con nueva contraseña ===");

        // Contraseña antigua → 400
        String oldLogin = "{\"email\":\"" + TEST_EMAIL + "\",\"password\":\"" + ORIGINAL_PASSWORD + "\"}";
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(oldLogin))
                .andDo(print())
                .andExpect(status().isBadRequest());

        // Nueva contraseña → 200 con token JWT
        String newLogin = "{\"email\":\"" + TEST_EMAIL + "\",\"password\":\"" + NEW_PASSWORD + "\"}";
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(newLogin))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty());

        System.out.println("✅ TEST 7 PASÓ: Contraseña antigua rechazada, nueva aceptada\n");
        System.out.println("=== FLUJO COMPLETO DE PASSWORD RESET VERIFICADO ===\n");
    }
}
