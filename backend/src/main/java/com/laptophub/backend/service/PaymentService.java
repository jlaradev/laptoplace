package com.laptophub.backend.service;


import com.laptophub.backend.model.Order;
import com.laptophub.backend.model.Payment;
import com.laptophub.backend.model.PaymentStatus;
import com.laptophub.backend.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class PaymentService {
    
    private final PaymentRepository paymentRepository;
    
    @Transactional
    @SuppressWarnings("null")
    public Payment createPayment(Order order, BigDecimal amount) {
        Payment payment = Payment.builder()
                .order(order)
                .monto(amount)
                .estado(PaymentStatus.PENDIENTE)
                .stripePaymentId(null)
                .build();
        
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
}
