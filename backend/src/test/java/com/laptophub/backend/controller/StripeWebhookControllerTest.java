package com.laptophub.backend.controller;

import com.laptophub.backend.model.*;
import com.laptophub.backend.repository.OrderRepository;
import com.laptophub.backend.repository.PaymentRepository;
import com.stripe.net.Webhook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import com.stripe.model.Event;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(StripeWebhookController.class)
@Import(StripeWebhookControllerTest.TestSecurityConfig.class)
public class StripeWebhookControllerTest {

    @Configuration
    static class TestSecurityConfig {
        @Bean
        public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
            http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
            return http.build();
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PaymentRepository paymentRepository;
    @MockitoBean
    private OrderRepository orderRepository;
    @MockitoBean
    private com.laptophub.backend.security.JwtAuthenticationFilter jwtAuthenticationFilter;

    private Payment payment;
    private Order order;

    private static final String PAYLOAD_SUCCEEDED = "{"
        + "\"id\":\"evt_3T3qMTKui0hnNmHH1E52Kpwz\","
        + "\"object\":\"event\","
        + "\"api_version\":\"2026-01-28.clover\","
        + "\"created\":1771819397,"
        + "\"data\":{"
        +   "\"object\":{"
        +     "\"id\":\"pi_test\","
        +     "\"object\":\"payment_intent\","
        +     "\"amount\":159999,"
        +     "\"status\":\"succeeded\","
        +     "\"metadata\":{\"orderId\":\"1\"}"
        +   "}"
        + "},"
        + "\"livemode\":false,"
        + "\"type\":\"payment_intent.succeeded\""
        + "}";

    private static final String PAYLOAD_FAILED = "{"
        + "\"id\":\"evt_failed123\","
        + "\"object\":\"event\","
        + "\"api_version\":\"2026-01-28.clover\","
        + "\"created\":1771819397,"
        + "\"data\":{"
        +   "\"object\":{"
        +     "\"id\":\"pi_test\","
        +     "\"object\":\"payment_intent\","
        +     "\"amount\":159999,"
        +     "\"status\":\"requires_payment_method\","
        +     "\"metadata\":{\"orderId\":\"1\"}"
        +   "}"
        + "},"
        + "\"livemode\":false,"
        + "\"type\":\"payment_intent.payment_failed\""
        + "}";

    @BeforeEach
    public void setUp() {
        order = new Order();
        order.setEstado(OrderStatus.PENDIENTE_PAGO);
        payment = new Payment();
        payment.setStripePaymentId("pi_test");
        payment.setEstado(PaymentStatus.PENDIENTE);
        payment.setOrder(order);
        when(paymentRepository.findByStripePaymentId("pi_test")).thenReturn(Optional.of(payment));
    }

    @Test
    public void testWebhookPaymentIntentSucceeded() throws Exception {
        try (MockedStatic<Webhook> webhookMock = mockStatic(Webhook.class)) {
            Event mockEvent = mock(Event.class);
            when(mockEvent.getType()).thenReturn("payment_intent.succeeded");

            webhookMock.when(() -> Webhook.constructEvent(anyString(), anyString(), anyString()))
                    .thenReturn(mockEvent);

            mockMvc.perform(post("/api/stripe/webhook")
                    .content(PAYLOAD_SUCCEEDED)
                    .contentType("application/json")
                    .header("Stripe-Signature", "test_signature"))
                    .andExpect(status().isOk());
        }
    }

    @Test
    public void testWebhookPaymentIntentFailed() throws Exception {
        try (MockedStatic<Webhook> webhookMock = mockStatic(Webhook.class)) {
            Event mockEvent = mock(Event.class);
            when(mockEvent.getType()).thenReturn("payment_intent.payment_failed");

            webhookMock.when(() -> Webhook.constructEvent(anyString(), anyString(), anyString()))
                    .thenReturn(mockEvent);

            mockMvc.perform(post("/api/stripe/webhook")
                    .content(PAYLOAD_FAILED)
                    .contentType("application/json")
                    .header("Stripe-Signature", "test_signature"))
                    .andExpect(status().isOk());
        }
    }
}