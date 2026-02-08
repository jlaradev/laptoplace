package com.laptophub.backend.service;


import com.laptophub.backend.model.Order;
import com.laptophub.backend.model.Payment;
import com.laptophub.backend.model.PaymentStatus;
import com.laptophub.backend.repository.PaymentRepository;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class PaymentService {
    
    private final PaymentRepository paymentRepository;
    private final StripeService stripeService;
    
    @Transactional
    @SuppressWarnings("null")
    public Payment createPayment(Order order, BigDecimal amount) throws StripeException {
        Payment payment = Payment.builder()
                .order(order)
                .monto(amount)
                .estado(PaymentStatus.PENDIENTE)
                .build();
        
        payment = paymentRepository.save(payment);

        // Crear PaymentIntent en Stripe
        PaymentIntent paymentIntent = stripeService.createPaymentIntent(
                order.getId(),
                amount,
                order.getUser().getEmail()
        );

        payment.setStripePaymentId(paymentIntent.getId());
        return paymentRepository.save(payment);
    }
    
    @Transactional(readOnly = true)
    @SuppressWarnings("null")
    public Payment findById(Long paymentId) {
        return paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Payment no encontrado con id: " + paymentId));
    }
    
    @Transactional(readOnly = true)
    public Payment findByStripePaymentId(String stripePaymentId) {
        return paymentRepository.findByStripePaymentId(stripePaymentId)
                .orElseThrow(() -> new RuntimeException(
                        "Payment no encontrado con stripePaymentId: " + stripePaymentId));
    }
    
    @Transactional
    public Payment updatePaymentStatus(Long paymentId, PaymentStatus newStatus) {
        Payment payment = findById(paymentId);
        payment.setEstado(newStatus);
        return paymentRepository.save(payment);
    }
    
    @Transactional
    public Payment setStripePaymentId(Long paymentId, String stripePaymentId) {
        Payment payment = findById(paymentId);
        payment.setStripePaymentId(stripePaymentId);
        return paymentRepository.save(payment);
    }
    
    @Transactional
    public Payment processPaymentSimulated(Long paymentId, boolean success) {
        Payment payment = findById(paymentId);
        
        if (success) {
            payment.setEstado(PaymentStatus.COMPLETADO);
            payment.setStripePaymentId("sim_" + System.currentTimeMillis());
        } else {
            payment.setEstado(PaymentStatus.FALLIDO);
        }
        
        return paymentRepository.save(payment);
    }


    @Transactional
    public Payment processStripePayment(Long paymentId) throws StripeException {
        Payment payment = findById(paymentId);
        String stripePaymentId = payment.getStripePaymentId();

        if (stripePaymentId == null) {
            throw new RuntimeException("El pago no tiene asociado un stripePaymentId");
        }

        // Confirmar el pago con Stripe
        PaymentIntent paymentIntent = stripeService.confirmPayment(stripePaymentId);

        // Actualizar el estado del pago basado en la respuesta de Stripe
        if ("succeeded".equals(paymentIntent.getStatus())) {
            payment.setEstado(PaymentStatus.COMPLETADO);
        } else if ("requires_action".equals(paymentIntent.getStatus())) {
            payment.setEstado(PaymentStatus.PENDIENTE);
        } else {
            payment.setEstado(PaymentStatus.FALLIDO);
        }

        return paymentRepository.save(payment);
    }

    /**
     * Verifica el estado de un pago en Stripe y actualiza el estado local
     */
    @Transactional
    public Payment checkAndSyncPaymentStatus(Long paymentId) throws StripeException {
        Payment payment = findById(paymentId);
        String stripePaymentId = payment.getStripePaymentId();

        if (stripePaymentId == null) {
            return payment;
        }

        boolean isSucceeded = stripeService.isPaymentSucceeded(stripePaymentId);

        if (isSucceeded && payment.getEstado() != PaymentStatus.COMPLETADO) {
            payment.setEstado(PaymentStatus.COMPLETADO);
            return paymentRepository.save(payment);
        }

        return payment;
    }


    @Transactional
    public Payment cancelPayment(Long paymentId) throws StripeException {
        Payment payment = findById(paymentId);
        String stripePaymentId = payment.getStripePaymentId();

        if (stripePaymentId != null) {
            stripeService.cancelPaymentIntent(stripePaymentId);
        }

        payment.setEstado(PaymentStatus.FALLIDO);
        return paymentRepository.save(payment);
    }
}
