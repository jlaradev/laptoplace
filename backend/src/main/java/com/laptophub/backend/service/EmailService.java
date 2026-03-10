package com.laptophub.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@SuppressWarnings("null")
@Service
public class EmailService {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${brevo.api-key}")
    private String brevoApiKey;

    @Value("${brevo.sender-email}")
    private String senderEmail;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    public void sendPasswordResetEmail(String toEmail, String token) {
        String resetLink = frontendUrl + "/reset-password?token=" + token;
        String htmlBody = buildEmailBody(resetLink);

        try {
            Map<String, Object> payload = Map.of(
                "sender", Map.of("name", "LaptoPlace", "email", senderEmail),
                "to", List.of(Map.of("email", toEmail)),
                "subject", "Restablece tu contraseña - LaptoPlace",
                "htmlContent", htmlBody
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("api-key", brevoApiKey);

            HttpEntity<String> request = new HttpEntity<>(objectMapper.writeValueAsString(payload), headers);
            new RestTemplate().postForObject("https://api.brevo.com/v3/smtp/email", request, Object.class);
        } catch (Exception e) {
            throw new RuntimeException("Error al enviar correo por Brevo: " + e.getMessage(), e);
        }
    }

    private String buildEmailBody(String resetLink) {
        return """
                <div style="font-family: Arial, sans-serif; max-width: 500px; margin: 0 auto;">
                  <h2 style="color: #333;">Restablecer contraseña</h2>
                  <p>Recibimos una solicitud para restablecer la contraseña de tu cuenta en <strong>LaptoPlace</strong>.</p>
                  <p>Haz clic en el siguiente botón para crear una nueva contraseña. El enlace expira en <strong>30 minutos</strong>.</p>
                  <a href="%s"
                     style="display:inline-block;padding:12px 24px;background:#1a73e8;color:#fff;text-decoration:none;border-radius:4px;margin:16px 0;">
                    Restablecer contraseña
                  </a>
                  <p style="color:#888;font-size:12px;">Si no solicitaste esto, ignora este correo.</p>
                </div>
                """.formatted(resetLink);
    }
}


