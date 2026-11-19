package com.patitas.report_service.webclient;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Base común de los clientes WebClient del report-service.
 *
 * - Reenvía el token del admin (header Authorization) para que el otro
 *   microservicio valide sus permisos.
 * - Si el servicio está apagado, tarda demasiado o responde error, devuelve
 *   Optional.empty() en vez de caerse: el reporte se arma igual con lo que haya.
 */
public abstract class ClienteBase {

    private static final ParameterizedTypeReference<List<Map<String, Object>>> TIPO_LISTA =
        new ParameterizedTypeReference<>() {};
    private static final ParameterizedTypeReference<Map<String, Object>> TIPO_MAPA =
        new ParameterizedTypeReference<>() {};

    protected final WebClient webClient;
    private final Duration timeout;

    protected ClienteBase(String baseUrl, long timeoutSegundos) {
        this.webClient = WebClient.builder()
            .baseUrl(baseUrl)
            .codecs(c -> c.defaultCodecs().maxInMemorySize(5 * 1024 * 1024)) // 5 MB (usuarios con foto)
            .build();
        this.timeout = Duration.ofSeconds(timeoutSegundos);
    }

    /** Nombre del microservicio (para informar cuál no respondió). */
    public abstract String nombreServicio();

    protected Optional<List<Map<String, Object>>> getLista(String uri, String authHeader) {
        try {
            return Optional.ofNullable(webClient.get()
                .uri(uri)
                .header("Authorization", authHeader)
                .retrieve()
                .bodyToMono(TIPO_LISTA)
                .block(timeout));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    protected Optional<Map<String, Object>> getMapa(String uri, String authHeader) {
        try {
            return Optional.ofNullable(webClient.get()
                .uri(uri)
                .header("Authorization", authHeader)
                .retrieve()
                .bodyToMono(TIPO_MAPA)
                .block(timeout));
        } catch (Exception e) {
            return Optional.empty();
        }
    }
}
