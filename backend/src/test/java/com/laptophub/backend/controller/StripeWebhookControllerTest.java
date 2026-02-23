package com.laptophub.backend.controller;

import com.laptophub.backend.model.*;
import com.laptophub.backend.repository.OrderRepository;
import com.laptophub.backend.repository.PaymentRepository;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.PaymentIntent;
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

    @BeforeEach
    public void setUp() {
        order = new Order();
        order.setEstado(OrderStatus.PENDIENTE);
        payment = new Payment();
        payment.setStripePaymentId("pi_test");
        payment.setEstado(PaymentStatus.PENDIENTE);
        payment.setOrder(order);
        when(paymentRepository.findByStripePaymentId("pi_test")).thenReturn(Optional.of(payment));
    }

    @Test
    public void testWebhookPaymentIntentSucceeded() throws Exception {
        String payload = "{\"type\":\"payment_intent.succeeded\",\"data\":{\"object\":{\"id\":\"pi_test\"}}}";

        try (MockedStatic<Webhook> webhookMock = mockStatic(Webhook.class)) {
            PaymentIntent mockPaymentIntent = mock(PaymentIntent.class);
            when(mockPaymentIntent.getId()).thenReturn("pi_test");

            EventDataObjectDeserializer mockDeserializer = mock(EventDataObjectDeserializer.class);
            when(mockDeserializer.getObject()).thenReturn(Optional.of(mockPaymentIntent));

            Event mockEvent = mock(Event.class);
            when(mockEvent.getType()).thenReturn("payment_intent.succeeded");
            when(mockEvent.getDataObjectDeserializer()).thenReturn(mockDeserializer);

            webhookMock.when(() -> Webhook.constructEvent(anyString(), anyString(), anyString()))
                    .thenReturn(mockEvent);

            mockMvc.perform(post("/api/stripe/webhook")
                    .content(payload)
                    .contentType("application/json")
                    .header("Stripe-Signature", "test_signature"))
                    .andExpect(status().isOk());
        }
    }

    @Test
    public void testWebhookPaymentIntentFailed() throws Exception {
        String payload = "{\"type\":\"payment_intent.payment_failed\",\"data\":{\"object\":{\"id\":\"pi_test\"}}}";

        try (MockedStatic<Webhook> webhookMock = mockStatic(Webhook.class)) {
            PaymentIntent mockPaymentIntent = mock(PaymentIntent.class);
            when(mockPaymentIntent.getId()).thenReturn("pi_test");

            EventDataObjectDeserializer mockDeserializer = mock(EventDataObjectDeserializer.class);
            when(mockDeserializer.getObject()).thenReturn(Optional.of(mockPaymentIntent));

            Event mockEvent = mock(Event.class);
            when(mockEvent.getType()).thenReturn("payment_intent.payment_failed");
            when(mockEvent.getDataObjectDeserializer()).thenReturn(mockDeserializer);

            webhookMock.when(() -> Webhook.constructEvent(anyString(), anyString(), anyString()))
                    .thenReturn(mockEvent);

            mockMvc.perform(post("/api/stripe/webhook")
                    .content(payload)
                    .contentType("application/json")
                    .header("Stripe-Signature", "test_signature"))
                    .andExpect(status().isOk());
        }
    }
}
