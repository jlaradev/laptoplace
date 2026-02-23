package com.laptophub.backend.controller;

import com.laptophub.backend.model.Payment;
import com.laptophub.backend.model.PaymentStatus;
import com.laptophub.backend.model.Order;
import com.laptophub.backend.model.OrderStatus;
import com.laptophub.backend.repository.PaymentRepository;
import com.laptophub.backend.repository.OrderRepository;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
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
            event = Webhook.constructEvent(
                payload, sigHeader, stripeWebhookSecret
            );
        } catch (Exception e) {
            return ResponseEntity.status(400).body("Webhook signature verification failed");
        }

        if ("payment_intent.succeeded".equals(event.getType())) {
            PaymentIntent paymentIntent = (PaymentIntent) event.getDataObjectDeserializer().getObject().orElse(null);
            if (paymentIntent == null) return ResponseEntity.ok("No PaymentIntent");
            String stripePaymentId = paymentIntent.getId();
            Optional<Payment> paymentOpt = paymentRepository.findByStripePaymentId(stripePaymentId);
            if (paymentOpt.isPresent()) {
                Payment payment = paymentOpt.get();
                payment.setEstado(PaymentStatus.COMPLETADO);
                paymentRepository.save(payment);
                Order order = payment.getOrder();
                order.setEstado(OrderStatus.PROCESANDO);
                orderRepository.save(order);
            }
        } else if ("payment_intent.payment_failed".equals(event.getType())) {
            PaymentIntent paymentIntent = (PaymentIntent) event.getDataObjectDeserializer().getObject().orElse(null);
            if (paymentIntent == null) return ResponseEntity.ok("No PaymentIntent");
            String stripePaymentId = paymentIntent.getId();
            Optional<Payment> paymentOpt = paymentRepository.findByStripePaymentId(stripePaymentId);
            if (paymentOpt.isPresent()) {
                Payment payment = paymentOpt.get();
                payment.setEstado(PaymentStatus.FALLIDO);
                paymentRepository.save(payment);
                Order order = payment.getOrder();
                order.setEstado(OrderStatus.PENDIENTE_PAGO);
                orderRepository.save(order);
            }
        }
        // Puedes agregar más eventos según lo requiera tu lógica
        return ResponseEntity.ok("Webhook processed");
    }
}
