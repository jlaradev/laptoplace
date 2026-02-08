package com.laptophub.backend;

import com.laptophub.backend.model.Order;
import com.laptophub.backend.model.OrderStatus;
import com.laptophub.backend.model.Payment;
import com.laptophub.backend.model.PaymentStatus;
import com.laptophub.backend.model.User;
import com.laptophub.backend.repository.OrderRepository;
import com.laptophub.backend.repository.UserRepository;
import com.laptophub.backend.service.PaymentService;
import com.stripe.exception.StripeException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests para Stripe integration
 */
@SpringBootTest
public class StripeIntegrationTest {

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrderRepository orderRepository;

    private User testUser;
    private Order testOrder;

    @BeforeEach
    @SuppressWarnings("null")
    public void setUp() {
        // Crear usuario de prueba
        testUser = User.builder()
                .nombre("Test")
                .apellido("User")
                .email("stripe-test-" + UUID.randomUUID() + "@test.com")
                .password("password123")
                .build();
        testUser = userRepository.save(testUser);

        // Crear orden de prueba
        testOrder = Order.builder()
                .user(testUser)
                .estado(OrderStatus.PENDIENTE)
                .createdAt(LocalDateTime.now())
                .total(BigDecimal.valueOf(99.99))
                .direccionEnvio("Calle Test 123, Test City 12345")
                .build();
        testOrder = orderRepository.save(testOrder);
    }

    @Test
    public void testCreatePaymentIntentSuccessfully() throws StripeException {
        // Arrange
        BigDecimal amount = BigDecimal.valueOf(99.99);

        // Act
        Payment payment = paymentService.createPayment(testOrder, amount);

        // Assert
        assertNotNull(payment);
        assertNotNull(payment.getId());
        assertNotNull(payment.getStripePaymentId());
        assertEquals(PaymentStatus.PENDIENTE, payment.getEstado());
        assertEquals(amount, payment.getMonto());
        assertTrue(payment.getStripePaymentId().startsWith("pi_"));
    }

    @Test
    public void testPaymentPersistence() throws StripeException {
        // Arrange
        BigDecimal amount = BigDecimal.valueOf(49.99);

        // Act
        Payment createdPayment = paymentService.createPayment(testOrder, amount);
        Payment retrievedPayment = paymentService.findById(createdPayment.getId());

        // Assert
        assertNotNull(retrievedPayment);
        assertEquals(createdPayment.getId(), retrievedPayment.getId());
        assertEquals(amount, retrievedPayment.getMonto());
        assertNotNull(retrievedPayment.getStripePaymentId());
    }

    @Test
    public void testFindPaymentByStripeId() throws StripeException {
        // Arrange
        BigDecimal amount = BigDecimal.valueOf(29.99);
        Payment createdPayment = paymentService.createPayment(testOrder, amount);

        // Act
        Payment foundPayment = paymentService.findByStripePaymentId(createdPayment.getStripePaymentId());

        // Assert
        assertNotNull(foundPayment);
        assertEquals(createdPayment.getId(), foundPayment.getId());
    }

    @Test
    public void testUpdatePaymentStatus() throws StripeException {
        // Arrange
        BigDecimal amount = BigDecimal.valueOf(59.99);
        Payment payment = paymentService.createPayment(testOrder, amount);

        // Act
        Payment updated = paymentService.updatePaymentStatus(payment.getId(), PaymentStatus.COMPLETADO);

        // Assert
        assertNotNull(updated);
        assertEquals(PaymentStatus.COMPLETADO, updated.getEstado());
    }

    @Test
    public void testSimulatedPaymentProcessing() throws StripeException {
        // Arrange
        BigDecimal amount = BigDecimal.valueOf(79.99);
        Payment payment = paymentService.createPayment(testOrder, amount);

        // Act - Simulate payment success
        Payment successPayment = paymentService.processPaymentSimulated(payment.getId(), true);

        // Assert
        assertNotNull(successPayment);
        assertEquals(PaymentStatus.COMPLETADO, successPayment.getEstado());

        // Create another order to test failure (una orden puede tener solo un pago)
        @SuppressWarnings("null")
        Order testOrder2 = Order.builder()
                .user(testUser)
                .estado(OrderStatus.PENDIENTE)
                .createdAt(LocalDateTime.now())
                .total(BigDecimal.valueOf(79.99))
                .direccionEnvio("Calle Test 456, Test City 67890")
                .build();
        @SuppressWarnings("null")
        Order savedTestOrder2 = orderRepository.save(testOrder2);
        testOrder2 = savedTestOrder2;
        
        Payment payment2 = paymentService.createPayment(testOrder2, amount);
        Payment failurePayment = paymentService.processPaymentSimulated(payment2.getId(), false);

        // Assert failure
        assertEquals(PaymentStatus.FALLIDO, failurePayment.getEstado());
    }

    @Test
    public void testPaymentAmountConversion() throws StripeException {
        // Arrange - Test que el monto se convierte correctamente a centavos
        BigDecimal amount = BigDecimal.valueOf(100.50);

        // Act
        Payment payment = paymentService.createPayment(testOrder, amount);

        // Assert
        assertNotNull(payment);
        assertEquals(amount, payment.getMonto());
    }
}
