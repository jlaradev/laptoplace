package com.laptophub.backend.scheduler;

import com.laptophub.backend.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OrderExpirationScheduler {
    private static final Logger logger = LoggerFactory.getLogger(OrderExpirationScheduler.class);
    private final OrderService orderService;

    // Ejecuta cada 5 minutos
    @Scheduled(cron = "0 */5 * * * *")
    public void expirePendingOrders() {
        logger.info("[OrderExpirationScheduler] Ejecutando expiración de órdenes pendientes...");
        try {
            int expired = orderService.expirePendingPaymentOrders();
            logger.info("[OrderExpirationScheduler] Órdenes expiradas: {}", expired);
        } catch (Exception e) {
            logger.error("[OrderExpirationScheduler] Error al expirar órdenes: ", e);
        }
    }
}
