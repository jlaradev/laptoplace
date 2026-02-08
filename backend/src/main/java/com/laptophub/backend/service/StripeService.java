package com.laptophub.backend.service;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class StripeService {

    @Value("${stripe.api.key}")
    private String stripeApiKey;

    public void initializeStripe() {
        Stripe.apiKey = stripeApiKey;
    }

    /**
     * Crea un PaymentIntent en Stripe para iniciar un pago
     */
    public PaymentIntent createPaymentIntent(Long orderId, BigDecimal amount, String email) throws StripeException {
        initializeStripe();

        // Convertir a centavos (Stripe usa centavos para USD)
        long amountInCents = amount.multiply(new BigDecimal("100")).longValue();

        Map<String, String> metadata = new HashMap<>();
        metadata.put("orderId", orderId.toString());

        PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                .setAmount(amountInCents)
                .setCurrency("usd")
                .setDescription("Pago de orden LaptopHub #" + orderId)
                .setReceiptEmail(email)
                .putAllMetadata(metadata)
                .build();

        return PaymentIntent.create(params);
    }

    /**
     * Confirma un pago usando el PaymentIntent ID
     */
    public PaymentIntent confirmPayment(String paymentIntentId) throws StripeException {
        initializeStripe();

        PaymentIntent paymentIntent = PaymentIntent.retrieve(paymentIntentId);
        
        if ("succeeded".equals(paymentIntent.getStatus())) {
            return paymentIntent;
        }

        return paymentIntent;
    }

    /**
     * Recupera un PaymentIntent por su ID
     */
    public PaymentIntent retrievePaymentIntent(String paymentIntentId) throws StripeException {
        initializeStripe();
        return PaymentIntent.retrieve(paymentIntentId);
    }

    /**
     * Cancela un PaymentIntent
     */
    public PaymentIntent cancelPaymentIntent(String paymentIntentId) throws StripeException {
        initializeStripe();

        PaymentIntent paymentIntent = PaymentIntent.retrieve(paymentIntentId);
        paymentIntent.cancel();

        return paymentIntent;
    }

    /**
     * Verifica si un pago fue exitoso
     */
    public boolean isPaymentSucceeded(String paymentIntentId) throws StripeException {
        initializeStripe();

        PaymentIntent paymentIntent = PaymentIntent.retrieve(paymentIntentId);
        return "succeeded".equals(paymentIntent.getStatus());
    }
}
