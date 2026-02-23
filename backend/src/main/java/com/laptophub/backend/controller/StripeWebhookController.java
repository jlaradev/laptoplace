package com.laptophub.backend.controller;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.laptophub.backend.model.Payment;
import com.laptophub.backend.model.PaymentStatus;
import com.laptophub.backend.model.Order;
import com.laptophub.backend.model.OrderStatus;
import com.laptophub.backend.repository.PaymentRepository;
import com.laptophub.backend.repository.OrderRepository;
import com.stripe.model.Event;
import com.stripe.net.Webhook;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@RestController
@RequestMapping("/api/stripe/webhook")
@RequiredArgsConstructor
public class StripeWebhookController {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;

    @Value("${stripe.webhook.secret}")
    private String stripeWebhookSecret;

    @PostMapping
    @Transactional
    public ResponseEntity<String> handleStripeWebhook(@RequestBody String payload,
                                                     @RequestHeader("Stripe-Signature") String sigHeader) {
        Event event;
        try {
            event = Webhook.constructEvent(payload, sigHeader, stripeWebhookSecret);
        } catch (Exception e) {
            return ResponseEntity.status(400).body("Webhook signature verification failed");
        }

        String eventType = event.getType();

        if ("payment_intent.succeeded".equals(eventType) || "payment_intent.payment_failed".equals(eventType)) {
            String stripePaymentId;
            try {
                JsonObject root = JsonParser.parseString(payload).getAsJsonObject();
                stripePaymentId = root.getAsJsonObject("data")
                        .getAsJsonObject("object")
                        .get("id").getAsString();
            } catch (Exception e) {
                return ResponseEntity.ok("No PaymentIntent id");
            }

            Optional<Payment> paymentOpt = paymentRepository.findByStripePaymentId(stripePaymentId);
            if (paymentOpt.isPresent()) {
                Payment payment = paymentOpt.get();
                Order order = payment.getOrder();

                if ("payment_intent.succeeded".equals(eventType)) {
                    payment.setEstado(PaymentStatus.COMPLETADO);
                    order.setEstado(OrderStatus.PROCESANDO);
                } else {
                    payment.setEstado(PaymentStatus.FALLIDO);
                    order.setEstado(OrderStatus.CANCELADO);
                }

                paymentRepository.save(payment);
                orderRepository.save(order);
            }
        }

        return ResponseEntity.ok("Webhook processed");
    }
}