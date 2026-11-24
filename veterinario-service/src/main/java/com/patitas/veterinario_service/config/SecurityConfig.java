
package com.patitas.veterinario_service.config;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;

import lombok.RequiredArgsConstructor;

/**
 * Configuracion de seguridad del microservicio de veterinarios.
 *
 * Utiliza JWT para autenticar las solicitudes provenientes
 * del frontend React y de otros clientes de la API.
 *
 * Reglas de acceso:
 * - GET: publico, permite consultar veterinarios.
 * - POST: solo ADMIN puede registrar veterinarios.
 * - PUT: solo ADMIN puede actualizar veterinarios.
 * - DELETE: solo ADMIN puede eliminar veterinarios.
 *
 * Las sesiones son STATELESS: el servidor no almacena
 * sesiones, sino que valida el token JWT recibido.
 */
@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
@SecurityScheme(
    name = "bearerAuth",
    type = SecuritySchemeType.HTTP,
    scheme = "bearer",
    bearerFormat = "JWT"
)
public class SecurityConfig {

    /**
     * Filtro encargado de validar los tokens JWT.
     *
     * Se implementara en JwtRequestFilter.java,
     * siguiendo el sistema utilizado por auth-service.
     */
    private final JwtRequestFilter jwtRequestFilter;

    /**
     * Origenes del frontend autorizados para realizar
     * solicitudes al microservicio.
     *
     * Se obtienen desde application.properties.
     */
    @Value("${cors.origenes}")
    private String origenesPermitidos;

    /**
     * Define los permisos de acceso a los endpoints REST.
     *
     * Las consultas son publicas para mostrar los
     * veterinarios en la pagina web.
     *
     * Las operaciones de administracion requieren
     * un token JWT valido con rol ADMIN.
     *
     * @param http Configuracion de seguridad HTTP.
     * @return Cadena de filtros de seguridad.
     * @throws Exception Si ocurre un error de configuracion.
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http)
            throws Exception {

        http
            // API REST con tokens JWT; no utiliza sesiones web.
            .csrf(csrf -> csrf.disable())

            // Permite las solicitudes desde React.
            .cors(cors ->
                cors.configurationSource(corsConfigurationSource())
            )

            // Define los permisos de cada endpoint.
            .authorizeHttpRequests(auth -> auth

                // Solicitudes de comprobacion del navegador.
                .requestMatchers(HttpMethod.OPTIONS, "/**")
                .permitAll()

                // Documentacion publica de Swagger.
                .requestMatchers(
                    "/swagger-ui/**",
                    "/swagger-ui.html",
                    "/v3/api-docs/**",
                    "/error"
                ).permitAll()

                // Cualquier usuario puede consultar veterinarios.
                .requestMatchers(
                    HttpMethod.GET,
                    "/api/v1/veterinarios",
                    "/api/v1/veterinarios/**"
                ).permitAll()

                // Solo ADMIN puede registrar veterinarios.
                .requestMatchers(
                    HttpMethod.POST,
                    "/api/v1/veterinarios"
                ).hasRole("ADMIN")

                // Solo ADMIN puede actualizar veterinarios.
                .requestMatchers(
                    HttpMethod.PUT,
                    "/api/v1/veterinarios/*"
                ).hasRole("ADMIN")

                // Solo ADMIN puede eliminar veterinarios.
                .requestMatchers(
                    HttpMethod.DELETE,
                    "/api/v1/veterinarios/*"
                ).hasRole("ADMIN")

                // Bloquear cualquier otra ruta no definida.
                .anyRequest().denyAll()
            )

            // No se almacenan sesiones en el servidor.
            .sessionManagement(session ->
                session.sessionCreationPolicy(
                    SessionCreationPolicy.STATELESS
                )
            )

            // Manejo de errores de autenticacion y permisos.
            .exceptionHandling(ex -> ex

                // HTTP 401: usuario sin autenticacion.
                .authenticationEntryPoint((req, res, error) -> {
                    res.setStatus(401);
                    res.setContentType(
                        "application/json;charset=UTF-8"
                    );
                    res.getWriter().write(
                        "{\"status\":401," +
                        "\"mensaje\":\"Debes iniciar sesion.\"}"
                    );
                })

                // HTTP 403: usuario sin permisos suficientes.
                .accessDeniedHandler((req, res, error) -> {
                    res.setStatus(403);
                    res.setContentType(
                        "application/json;charset=UTF-8"
                    );
                    res.getWriter().write(
                        "{\"status\":403," +
                        "\"mensaje\":\"No tienes permisos para esta accion.\"}"
                    );
                })
            );

        /**
         * Ejecuta el filtro JWT antes del filtro de
         * autenticacion de Spring Security.
         */
        http.addFilterBefore(
            jwtRequestFilter,
            UsernamePasswordAuthenticationFilter.class
        );

        return http.build();
    }

    /**
     * Configura CORS para permitir la comunicacion
     * entre el frontend React y el backend Spring Boot.
     *
     * @return Configuracion de origenes y metodos permitidos.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {

        CorsConfiguration config = new CorsConfiguration();

        // Origenes permitidos desde application.properties.
        config.setAllowedOrigins(
            Arrays.asList(origenesPermitidos.split(","))
        );

        // Metodos HTTP permitidos.
        config.setAllowedMethods(
            List.of("GET", "POST", "PUT", "DELETE", "OPTIONS")
        );

        // Encabezados necesarios para enviar JWT y JSON.
        config.setAllowedHeaders(
            List.of("Authorization", "Content-Type")
        );

        // Tiempo de almacenamiento del preflight.
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source =
            new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration("/**", config);

        return source;
    }
}
