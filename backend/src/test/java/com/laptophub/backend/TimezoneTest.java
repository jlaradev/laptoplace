package com.laptophub.backend;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.TimeZone;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TimezoneTest {

    @Test
    void horaActualEnBogota() {
        TimeZone.setDefault(TimeZone.getTimeZone("America/Bogota"));

        ZonedDateTime ahoraEnBogota = ZonedDateTime.now(ZoneId.of("America/Bogota"));
        LocalDateTime horaLocal = LocalDateTime.now();

        System.out.println("Hora actual (Bogotá / UTC-5): " + ahoraEnBogota);
        System.out.println("LocalDateTime del sistema:    " + horaLocal);
        System.out.println("Timezone por defecto:         " + TimeZone.getDefault().getID());

        assertEquals("America/Bogota", TimeZone.getDefault().getID());
        assertEquals(ZoneId.of("America/Bogota"), ahoraEnBogota.getZone());
    }
}
