package com.laptophub.backend.service;

import com.laptophub.backend.dto.ForgotPasswordDTO;
import com.laptophub.backend.dto.ResetPasswordDTO;
import com.laptophub.backend.exception.ValidationException;
import com.laptophub.backend.model.PasswordResetToken;
import com.laptophub.backend.model.User;
import com.laptophub.backend.repository.PasswordResetTokenRepository;
import com.laptophub.backend.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@SuppressWarnings("null")
@Service
public class PasswordResetService {

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;

    public PasswordResetService(
            UserRepository userRepository,
            PasswordResetTokenRepository tokenRepository,
            EmailService emailService,
            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
        this.emailService = emailService;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public void requestReset(ForgotPasswordDTO dto) {
        // Siempre responde 200 aunque el email no exista (evita user enumeration)
        userRepository.findByEmail(dto.getEmail()).ifPresent(user -> {
            PasswordResetToken token = PasswordResetToken.builder()
                    .user(user)
                    .expiresAt(LocalDateTime.now().plusMinutes(30))
                    .build();
            tokenRepository.save(token);

            emailService.sendPasswordResetEmail(user.getEmail(), token.getId().toString());
        });
    }

    @Transactional
    public void resetPassword(ResetPasswordDTO dto) {
        UUID tokenId;
        try {
            tokenId = UUID.fromString(dto.getToken());
        } catch (IllegalArgumentException e) {
            throw new ValidationException("Token inválido");
        }

        PasswordResetToken token = tokenRepository.findByIdAndUsedFalse(tokenId)
                .orElseThrow(() -> new ValidationException("Token inválido o ya utilizado"));

        if (token.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new ValidationException("El token ha expirado. Solicita un nuevo enlace.");
        }

        User user = token.getUser();
        user.setPassword(passwordEncoder.encode(dto.getNewPassword()));
        userRepository.save(user);

        token.setUsed(true);
        tokenRepository.save(token);
    }
}
