package com.patitas.report_service.webclient;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Cliente de mascota-service (puerto 8082): mascotas y solicitudes de cambio. */
@Component
public class MascotaClient extends ClienteBase {

    public MascotaClient(@Value("${mascota.service.url}") String url, @Value("${webclient.timeout-segundos}") long timeoutSegundos) {
        super(url, timeoutSegundos);
    }

    @Override
    public String nombreServicio() {
        return "mascota-service";
    }

    /** GET /mascotas: cada mascota trae nombre, especie, raza, estado y correoDueno. */
    public Optional<List<Map<String, Object>>> listarMascotas(String authHeader) {
        return getLista("/mascotas", authHeader);
    }

    /** GET /solicitudes: cada solicitud trae su estado (PENDIENTE, APROBADA, RECHAZADA). */
    public Optional<List<Map<String, Object>>> listarSolicitudes(String authHeader) {
        return getLista("/solicitudes", authHeader);
    }
}
