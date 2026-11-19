package com.patitas.report_service.webclient;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Cliente de contacto-service (puerto 8088): mensajes del formulario de contacto. */
@Component
public class ContactoClient extends ClienteBase {

    public ContactoClient(@Value("${contacto.service.url}") String url, @Value("${webclient.timeout-segundos}") long timeoutSegundos) {
        super(url, timeoutSegundos);
    }

    @Override
    public String nombreServicio() {
        return "contacto-service";
    }

    /** GET /mensajes/estadisticas: { total, pendiente, leido, respondido, archivado }. */
    public Optional<Map<String, Object>> estadisticas(String authHeader) {
        return getMapa("/mensajes/estadisticas", authHeader);
    }
}
