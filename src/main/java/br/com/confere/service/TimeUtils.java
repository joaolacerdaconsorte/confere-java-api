package br.com.confere.service;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;

public final class TimeUtils {

    public static final ZoneId SAO_PAULO_ZONE = ZoneId.of("America/Sao_Paulo");

    private TimeUtils() {
    }

    public static OffsetDateTime toOffset(LocalDateTime dateTime) {
        if (dateTime == null) {
            return null;
        }
        return dateTime.atZone(SAO_PAULO_ZONE).toOffsetDateTime();
    }
}
