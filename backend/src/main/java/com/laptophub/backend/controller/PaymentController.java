package com.laptophub.backend.controller;

import com.laptophub.backend.dto.CreatePaymentDTO;
import com.laptophub.backend.dto.PaymentResponseDTO;
import com.laptophub.backend.model.PaymentStatus;
import com.laptophub.backend.service.PaymentService;
import com.stripe.exception.StripeException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/create")
    public PaymentResponseDTO createPayment(@Valid @RequestBody CreatePaymentDTO dto) throws StripeException {
        return paymentService.createPaymentDTO(dto);
    }

    @GetMapping("/{paymentId}")
    public PaymentResponseDTO findById(@PathVariable Long paymentId) {
        return paymentService.findByIdDTO(paymentId);
    }

    @GetMapping("/stripe/{stripePaymentId}")
    public PaymentResponseDTO findByStripeId(@PathVariable String stripePaymentId) {
        return paymentService.findByStripeIdDTO(stripePaymentId);
    }

    @PutMapping("/{paymentId}/status/{estado}")
    public PaymentResponseDTO updateStatus(
            @PathVariable Long paymentId,
            @PathVariable PaymentStatus estado
    ) {
        return paymentService.updatePaymentStatusDTO(paymentId, estado);
    }

    @PutMapping("/{paymentId}/stripe-id")
    public PaymentResponseDTO setStripeId(
            @PathVariable Long paymentId,
            @RequestParam String value
    ) {
        return paymentService.setStripePaymentIdDTO(paymentId, value);
    }

    @GetMapping("/{paymentId}/sync")
    public PaymentResponseDTO syncPaymentStatus(@PathVariable Long paymentId) throws StripeException {
        return paymentService.checkAndSyncPaymentStatusDTO(paymentId);
    }

    @PostMapping("/{paymentId}/process")
    public PaymentResponseDTO processPayment(@PathVariable Long paymentId) throws StripeException {
        return paymentService.processStripePaymentDTO(paymentId);
    }

    @PostMapping("/{paymentId}/cancel")
    public PaymentResponseDTO cancelPayment(@PathVariable Long paymentId) throws StripeException {
        return paymentService.cancelPaymentDTO(paymentId);
    }

    @PostMapping("/{paymentId}/simulate")
    public PaymentResponseDTO simulate(
            @PathVariable Long paymentId,
            @RequestParam boolean success
    ) {
        return paymentService.processPaymentSimulatedDTO(paymentId, success);
    }
}