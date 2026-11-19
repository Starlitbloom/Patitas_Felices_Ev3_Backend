package com.patitas.report_service.webclient;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Cliente de notificacion-service (puerto 8089): avisos enviados a los usuarios. */
@Component
public class NotificacionClient extends ClienteBase {

    public NotificacionClient(@Value("${notificacion.service.url}") String url, @Value("${webclient.timeout-segundos}") long timeoutSegundos) {
        super(url, timeoutSegundos);
    }

    @Override
    public String nombreServicio() {
        return "notificacion-service";
    }

    /** GET /notificaciones/estadisticas: { total, noLeidas, cita, mascota, ... }. */
    public Optional<Map<String, Object>> estadisticas(String authHeader) {
        return getMapa("/notificaciones/estadisticas", authHeader);
    }
}
