package com.laptophub.backend.scheduler;

import com.laptophub.backend.model.Order;
import com.laptophub.backend.model.OrderStatus;
import com.laptophub.backend.model.User;
import com.laptophub.backend.repository.OrderRepository;
import com.laptophub.backend.repository.UserRepository;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
public class OrderExpirationSchedulerIntegrationTest {
    @Autowired
    private OrderRepository orderRepository;
    @Autowired
    private UserRepository userRepository;
    private User testUser;
    private Long testOrderId;

    @BeforeEach
    void setUp() {
        // Crear usuario de prueba
        testUser = User.builder()
                .email("test-scheduler@laptophub.com")
                .password("1234")
                .nombre("Test")
                .apellido("Scheduler")
                .build();
        userRepository.save(testUser);
        // Crear una orden pendiente vencida con total válido y usuario
        Order order = Order.builder()
                .estado(OrderStatus.PENDIENTE_PAGO)
                .expiresAt(LocalDateTime.now().minusMinutes(10))
                .total(BigDecimal.ONE)
                .user(testUser)
                .direccionEnvio("Calle Falsa 123")
                .build();
        orderRepository.save(order);
        testOrderId = order.getId();
        printOrderDetails("[TEST] Orden creada", order);
    }

    @AfterEach
    void tearDown() {
        orderRepository.deleteAll();
        userRepository.deleteAll();
    }

    private void printOrderDetails(String label, Order order) {
        if (order == null) {
            System.out.println(label + ": null");
            return;
        }
        System.out.println(label + ": {"
                + "id=" + order.getId()
                + ", estado=" + order.getEstado()
                + ", expiresAt=" + order.getExpiresAt()
                + ", total=" + order.getTotal()
                + ", direccionEnvio=" + order.getDireccionEnvio()
                + ", userId=" + (order.getUser() != null ? order.getUser().getId() : null)
                + "}");
    }

    @Test
    void schedulerShouldExpireOrders() {
        Order before = orderRepository.findById(testOrderId).orElse(null);
        printOrderDetails("[TEST] Estado inicial de la orden", before);
        Awaitility.await()
                .atMost(6, TimeUnit.MINUTES)
                .pollInterval(30, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    Order expired = orderRepository.findById(testOrderId).orElse(null);
                    printOrderDetails("[TEST] Estado tras scheduler", expired);
                    assertThat(expired).isNotNull();
                    assertThat(expired.getEstado()).isEqualTo(OrderStatus.EXPIRADO);
                });
        System.out.println("[TEST] El scheduler expiró la orden correctamente.\n");
    }
}
