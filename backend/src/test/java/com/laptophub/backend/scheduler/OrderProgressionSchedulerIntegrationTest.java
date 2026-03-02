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
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
public class OrderProgressionSchedulerIntegrationTest {
    @Autowired
    private OrderRepository orderRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private OrderProgressionScheduler orderProgressionScheduler;
    
    private User testUser;
    private Long orderId;

    @BeforeEach
    @SuppressWarnings("null")
    void setUp() {
        // Crear usuario de prueba
        testUser = User.builder()
                .email("test-scheduler@laptophub.com")
                .password("1234")
                .nombre("Test")
                .apellido("Scheduler")
                .build();
        userRepository.save(testUser);

        // Crear una orden en estado PROCESANDO
        Order order = Order.builder()
                .estado(OrderStatus.PROCESANDO)
                .total(BigDecimal.valueOf(99.99))
                .user(testUser)
                .direccionEnvio("Calle Test 123")
                .build();
        orderRepository.save(order);
        orderId = order.getId();
        System.out.println("[TEST] Orden creada en PROCESANDO: ID=" + orderId);
    }

    @AfterEach
    void tearDown() {
        orderRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @SuppressWarnings("null")
    void schedulerShouldProgressOrderThroughStates() throws InterruptedException {
        // Estado inicial: PROCESANDO
        Order initial = orderRepository.findById(orderId).orElse(null);
        assertThat(initial).isNotNull();
        assertThat(initial.getEstado()).isEqualTo(OrderStatus.PROCESANDO);
        System.out.println("[TEST] ✓ Orden inicial en PROCESANDO");
        
        // Thread para ejecutar scheduler cada 1 minuto
        Thread schedulerThread = new Thread(() -> {
            try {
                for (int i = 0; i < 3; i++) {
                    Thread.sleep(60000); // 1 minuto
                    orderProgressionScheduler.progressOrders();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        schedulerThread.start();
        
        // Esperar a que pase a ENVIADO
        Awaitility.await()
                .atMost(2, TimeUnit.MINUTES)
                .pollInterval(15, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    Order progressed = orderRepository.findById(orderId).orElse(null);
                    assertThat(progressed).isNotNull();
                    assertThat(progressed.getEstado()).isEqualTo(OrderStatus.ENVIADO);
                });
        System.out.println("[TEST] ✓ Scheduler: PROCESANDO → ENVIADO");
        
        // Esperar a que pase a ENTREGADO
        Awaitility.await()
                .atMost(2, TimeUnit.MINUTES)
                .pollInterval(15, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    Order delivered = orderRepository.findById(orderId).orElse(null);
                    assertThat(delivered).isNotNull();
                    assertThat(delivered.getEstado()).isEqualTo(OrderStatus.ENTREGADO);
                });
        System.out.println("[TEST] ✓ Scheduler: ENVIADO → ENTREGADO");
        
        schedulerThread.join();
    }
}
