package com.laptophub.backend.controller;

import com.laptophub.backend.model.Order;
import com.laptophub.backend.model.Payment;
import com.laptophub.backend.model.PaymentStatus;
import com.laptophub.backend.service.OrderService;
import com.laptophub.backend.service.PaymentService;
import com.stripe.exception.StripeException;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;
    private final OrderService orderService;

    @PostMapping("/create")
    public Payment createPayment(
            @RequestParam Long orderId,
            @RequestParam BigDecimal amount
    ) throws StripeException {
        Order order = orderService.findById(orderId);
        return paymentService.createPayment(order, amount);
    }

    @GetMapping("/{paymentId}")
    public Payment findById(@PathVariable Long paymentId) {
        return paymentService.findById(paymentId);
    }

    @GetMapping("/stripe/{stripePaymentId}")
    public Payment findByStripeId(@PathVariable String stripePaymentId) {
        return paymentService.findByStripePaymentId(stripePaymentId);
    }

    @PutMapping("/{paymentId}/status/{estado}")
    public Payment updateStatus(
            @PathVariable Long paymentId,
            @PathVariable PaymentStatus estado
    ) {
        return paymentService.updatePaymentStatus(paymentId, estado);
    }

    @PutMapping("/{paymentId}/stripe-id")
    public Payment setStripeId(
            @PathVariable Long paymentId,
            @RequestParam String value
    ) {
        return paymentService.setStripePaymentId(paymentId, value);
    }


    @GetMapping("/{paymentId}/sync")
    public Payment syncPaymentStatus(@PathVariable Long paymentId) throws StripeException {
        return paymentService.checkAndSyncPaymentStatus(paymentId);
    }


    @PostMapping("/{paymentId}/process")
    public Payment processPayment(@PathVariable Long paymentId) throws StripeException {
        return paymentService.processStripePayment(paymentId);
    }


    @PostMapping("/{paymentId}/cancel")
    public Payment cancelPayment(@PathVariable Long paymentId) throws StripeException {
        return paymentService.cancelPayment(paymentId);
    }

    @PostMapping("/{paymentId}/simulate")
    public Payment simulate(
            @PathVariable Long paymentId,
            @RequestParam boolean success
    ) {
        return paymentService.processPaymentSimulated(paymentId, success);
    }
}