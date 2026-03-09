package com.laptophub.backend.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@SuppressWarnings("null")
@Service
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String from;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendPasswordResetEmail(String toEmail, String token) throws MessagingException {
        String link = frontendUrl + "/reset-password?token=" + token;

        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setFrom(from);
        helper.setTo(toEmail);
        helper.setSubject("Restablece tu contraseña - LaptoPlace");
        helper.setText(buildEmailBody(link), true);

        mailSender.send(message);
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
