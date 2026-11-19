package com.patitas.report_service.webclient;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Cliente de auth-service (puerto 8081): usuarios registrados. */
@Component
public class AuthClient extends ClienteBase {

    public AuthClient(@Value("${auth.service.url}") String url, @Value("${webclient.timeout-segundos}") long timeoutSegundos) {
        super(url, timeoutSegundos);
    }

    @Override
    public String nombreServicio() {
        return "auth-service";
    }

    /** GET /usuarios (solo ADMIN). Cada usuario trae rol, region, comuna, avatar y fechaRegistro. */
    public Optional<List<Map<String, Object>>> listarUsuarios(String authHeader) {
        return getLista("/usuarios", authHeader);
    }
}
