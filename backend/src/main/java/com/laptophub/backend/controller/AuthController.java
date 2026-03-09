package com.laptophub.backend.controller;

import com.laptophub.backend.dto.AuthRequestDTO;
import com.laptophub.backend.dto.AuthResponseDTO;
import com.laptophub.backend.exception.TooManyRequestsException;
import com.laptophub.backend.exception.ValidationException;
import com.laptophub.backend.model.User;
import com.laptophub.backend.repository.UserRepository;
import com.laptophub.backend.security.JwtService;
import com.laptophub.backend.security.LoginRateLimiterService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;
    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final LoginRateLimiterService rateLimiterService;

    public AuthController(
            AuthenticationManager authenticationManager,
            UserDetailsService userDetailsService,
            JwtService jwtService,
            UserRepository userRepository,
            LoginRateLimiterService rateLimiterService
    ) {
        this.authenticationManager = authenticationManager;
        this.userDetailsService = userDetailsService;
        this.jwtService = jwtService;
        this.userRepository = userRepository;
        this.rateLimiterService = rateLimiterService;
    }

    @PostMapping("/login")
    public AuthResponseDTO login(@Valid @RequestBody AuthRequestDTO request, HttpServletRequest httpRequest) {
        String clientIp = getClientIp(httpRequest);

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );
        } catch (org.springframework.security.authentication.DisabledException ex) {
            throw new ValidationException("Cuenta desactivada. Contacta con el administrador.");
        } catch (BadCredentialsException ex) {
            // Solo consumir ficha del rate limiter cuando las credenciales son incorrectas
            if (!rateLimiterService.tryConsume(clientIp)) {
                throw new TooManyRequestsException(
                    "Demasiados intentos de login fallidos. Por favor, espera 15 minutos antes de volver a intentar."
                );
            }
            throw new ValidationException("Credenciales invalidas");
        }

        UserDetails userDetails = userDetailsService.loadUserByUsername(request.getEmail());
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ValidationException("Usuario no encontrado"));

        String token = jwtService.generateToken(userDetails, Map.of("role", user.getRole().name()));

        return AuthResponseDTO.builder()
                .token(token)
                .userId(user.getId())
                .email(user.getEmail())
                .role(user.getRole())
                .build();
    }

    /**
     * Obtiene la IP del cliente, manejando proxies y load balancers
     */
    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }
}
