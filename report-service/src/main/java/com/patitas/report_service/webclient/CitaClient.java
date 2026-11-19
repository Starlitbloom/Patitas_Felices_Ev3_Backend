package com.patitas.report_service.webclient;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Cliente de cita-service (puerto 8086): citas de la clínica. */
@Component
public class CitaClient extends ClienteBase {

    public CitaClient(@Value("${cita.service.url}") String url, @Value("${webclient.timeout-segundos}") long timeoutSegundos) {
        super(url, timeoutSegundos);
    }

    @Override
    public String nombreServicio() {
        return "cita-service";
    }

    /** GET /citas: cada cita trae fecha, servicio, motivo, estado y correoUsuario. */
    public Optional<List<Map<String, Object>>> listarCitas(String authHeader) {
        return getLista("/citas", authHeader);
    }
}
