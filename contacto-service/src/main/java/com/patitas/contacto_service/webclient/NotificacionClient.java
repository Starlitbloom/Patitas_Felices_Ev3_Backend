package com.patitas.contacto_service.webclient;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Cliente para comunicarse con notificacion-service (puerto 8089).
 *
 * Se reenvía el mismo token del usuario que hizo la acción (header Authorization),
 * así notificacion-service sabe quién crea el aviso y valida sus permisos.
 *
 * Si notificacion-service está apagado, NO se cae la respuesta del mensaje:
 * el método devuelve false y el mensaje igual queda respondido.
 */
@Component
public class NotificacionClient {

    private final WebClient webClient;

    public NotificacionClient(@Value("${notificacion.service.url}") String notificacionServiceUrl) {
        this.webClient = WebClient.builder()
                .baseUrl(notificacionServiceUrl)
                .build();
    }

    public boolean crearNotificacion(String correoDestinatario, String titulo, String mensaje,
            String tipo, String enlace, String authHeader) {
        Map<String, String> cuerpo = new HashMap<>();
        cuerpo.put("correoDestinatario", correoDestinatario);
        cuerpo.put("titulo", titulo);
        cuerpo.put("mensaje", mensaje);
        cuerpo.put("tipo", tipo);
        cuerpo.put("enlace", enlace);
        try {
            webClient.post()
                .uri("/notificaciones")
                .header("Authorization", authHeader)
                .bodyValue(cuerpo)
                .retrieve()
                .bodyToMono(Void.class)
                .block();
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
