package com.laptophub.backend.service;


import com.laptophub.backend.dto.CreatePaymentDTO;
import com.laptophub.backend.dto.DTOMapper;
import com.laptophub.backend.dto.PaymentResponseDTO;
import com.laptophub.backend.model.Order;
import com.laptophub.backend.model.Payment;
import com.laptophub.backend.model.PaymentStatus;
import com.laptophub.backend.repository.OrderRepository;
import com.laptophub.backend.repository.PaymentRepository;
import com.laptophub.backend.exception.ResourceNotFoundException;
import com.laptophub.backend.exception.ValidationException;
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
    private final OrderRepository orderRepository;
    private final StripeService stripeService;
    
    @Transactional
    @SuppressWarnings("null")
    public Payment createPayment(Order order, BigDecimal amount) throws StripeException {
        if (order.getPayment() != null) {
            throw new ValidationException("La orden ya tiene un pago asociado");
        }
        if (order.getTotal() == null) {
            throw new ValidationException("La orden no tiene total");
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidationException("El monto debe ser mayor a 0");
        }
        if (amount.compareTo(order.getTotal()) != 0) {
            throw new ValidationException("El monto no coincide con el total de la orden");
        }

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
                .orElseThrow(() -> new ResourceNotFoundException("Pago no encontrado con id: " + paymentId));
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
            throw new ValidationException("El pago no tiene asociado un stripePaymentId");
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
    
    // Métodos que retornan DTOs
    
    @Transactional
    @SuppressWarnings("null")
    public PaymentResponseDTO createPaymentDTO(CreatePaymentDTO dto) throws StripeException {
        Order order = orderRepository.findById(dto.getOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Orden no encontrada con id: " + dto.getOrderId()));
        BigDecimal orderTotal = order.getTotal();
        if (orderTotal == null) {
            throw new ValidationException("La orden no tiene total");
        }
        if (dto.getAmount() != null && orderTotal.compareTo(dto.getAmount()) != 0) {
            throw new ValidationException("El monto no coincide con el total de la orden");
        }
        Payment payment = createPayment(order, orderTotal);
        return DTOMapper.toPaymentResponse(payment);
    }
    
    @Transactional(readOnly = true)
    public PaymentResponseDTO findByIdDTO(Long paymentId) {
        return DTOMapper.toPaymentResponse(findById(paymentId));
    }
    
    @Transactional(readOnly = true)
    public PaymentResponseDTO findByStripeIdDTO(String stripePaymentId) {
        return DTOMapper.toPaymentResponse(findByStripePaymentId(stripePaymentId));
    }
    
    @Transactional
    public PaymentResponseDTO updatePaymentStatusDTO(Long paymentId, PaymentStatus newStatus) {
        Payment payment = updatePaymentStatus(paymentId, newStatus);
        return DTOMapper.toPaymentResponse(payment);
    }
    
    @Transactional
    public PaymentResponseDTO setStripePaymentIdDTO(Long paymentId, String stripePaymentId) {
        Payment payment = setStripePaymentId(paymentId, stripePaymentId);
        return DTOMapper.toPaymentResponse(payment);
    }
    
    @Transactional
    public PaymentResponseDTO processPaymentSimulatedDTO(Long paymentId, boolean success) {
        Payment payment = processPaymentSimulated(paymentId, success);
        return DTOMapper.toPaymentResponse(payment);
    }
    
    @Transactional
    public PaymentResponseDTO processStripePaymentDTO(Long paymentId) throws StripeException {
        Payment payment = processStripePayment(paymentId);
        return DTOMapper.toPaymentResponse(payment);
    }
    
    @Transactional
    public PaymentResponseDTO checkAndSyncPaymentStatusDTO(Long paymentId) throws StripeException {
        Payment payment = checkAndSyncPaymentStatus(paymentId);
        return DTOMapper.toPaymentResponse(payment);
    }
    
    @Transactional
    public PaymentResponseDTO cancelPaymentDTO(Long paymentId) throws StripeException {
        Payment payment = cancelPayment(paymentId);
        return DTOMapper.toPaymentResponse(payment);
    }
}
