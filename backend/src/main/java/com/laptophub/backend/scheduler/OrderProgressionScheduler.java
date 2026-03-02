package com.laptophub.backend.scheduler;

import com.laptophub.backend.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OrderProgressionScheduler {
    private static final Logger logger = LoggerFactory.getLogger(OrderProgressionScheduler.class);
    private final OrderService orderService;

    // Orden de ejecución: PRIMERO ENTREGADO, LUEGO ENVIADO
    // Esto previene que una orden haga dos transiciones en el mismo ciclo
    @Scheduled(cron = "0 */5 * * * *")
    public void progressOrders() {
        logger.info("[OrderProgressionScheduler] Iniciando progresión automática de órdenes...");
        try {
            // PRIMERO: Procesa ENVIADO → ENTREGADO
            // Si una orden acaba de pasar a ENVIADO en este ciclo, no habrá sido creada/actualizada aún
            int enviadoToEntregado = orderService.progressShippedToDelivered();
            logger.info("[OrderProgressionScheduler] Órdenes movidas de ENVIADO a ENTREGADO: {}", enviadoToEntregado);
            
            // LUEGO: Procesa PROCESANDO → ENVIADO
            // De esta forma, una orden que acaba de ser procesada a ENVIADO aquí, no será procesada a ENTREGADO hasta el próximo ciclo (2 min después)
            int procesandoToEnviado = orderService.progressProcessingToShipped();
            logger.info("[OrderProgressionScheduler] Órdenes movidas de PROCESANDO a ENVIADO: {}", procesandoToEnviado);
            
            int total = enviadoToEntregado + procesandoToEnviado;
            logger.info("[OrderProgressionScheduler] Total de órdenes procesadas en este ciclo: {}", total);
        } catch (Exception e) {
            logger.error("[OrderProgressionScheduler] Error al procesar progresión de órdenes: ", e);
        }
    }
}
