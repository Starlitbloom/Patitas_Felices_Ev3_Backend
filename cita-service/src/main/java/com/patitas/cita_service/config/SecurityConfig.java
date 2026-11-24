
package com.patitas.cita_service.config;

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
 * Configuracion de seguridad del microservicio de citas
 * veterinarias de Patitas Felices.
 *
 * Utiliza tokens JWT generados por auth-service.
 *
 * Reglas generales:
 *
 * - Los usuarios deben iniciar sesion para gestionar citas.
 * - Los clientes pueden consultar y gestionar sus reservas.
 * - Los administradores pueden gestionar todas las citas.
 * - Swagger permite probar la API con un token JWT.
 *
 * Las restricciones especificas de cada endpoint
 * se definen mediante @PreAuthorize en CitaController.
 *
 * Las sesiones son STATELESS:
 * el servidor no almacena sesiones de usuario.
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
     * Filtro encargado de validar los tokens JWT
     * recibidos en las solicitudes HTTP.
     *
     * Se implementara en JwtRequestFilter.java.
     */
    private final JwtRequestFilter jwtRequestFilter;

    /**
     * Direcciones del frontend React autorizadas
     * para comunicarse con el microservicio.
     *
     * Se obtienen desde application.properties.
     */
    @Value("${cors.origenes}")
    private String origenesPermitidos;

    /**
     * Define las reglas de autenticacion y autorizacion.
     *
     * Los endpoints de citas requieren un JWT valido.
     *
     * Los permisos particulares se comprueban
     * mediante @PreAuthorize en el controlador.
     *
     * Por ejemplo:
     *
     * - GET /api/v1/citas: ADMIN y RECEPCIONISTA.
     * - GET /api/v1/citas/mis-citas: usuario autenticado.
     * - POST /api/v1/citas: usuario autenticado.
     * - PATCH /api/v1/citas/{id}/estado: ADMIN y RECEPCIONISTA.
     * - DELETE /api/v1/citas/{id}: solo ADMIN.
     *
     * @param http Configuracion HTTP de Spring Security.
     * @return Cadena de filtros de seguridad.
     * @throws Exception Si ocurre un error de configuracion.
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http)
            throws Exception {

        http

            /*
             * Deshabilitar CSRF para esta API REST.
             *
             * La autenticacion se realizara
             * mediante tokens JWT en el encabezado
             * Authorization.
             */
            .csrf(csrf -> csrf.disable())

            /*
             * Permitir la comunicacion con React.
             */
            .cors(cors ->
                cors.configurationSource(
                    corsConfigurationSource()
                )
            )

            /*
             * Configurar los permisos de acceso
             * a los endpoints del microservicio.
             */
            .authorizeHttpRequests(auth -> auth

                /*
                 * Permitir solicitudes OPTIONS
                 * utilizadas por los navegadores
                 * para verificar CORS.
                 */
                .requestMatchers(
                    HttpMethod.OPTIONS,
                    "/**"
                ).permitAll()

                /*
                 * Permitir consultar la documentacion
                 * de Swagger sin iniciar sesion.
                 *
                 * Para ejecutar operaciones de citas,
                 * si se necesita un token JWT valido.
                 */
                .requestMatchers(
                    "/swagger-ui/**",
                    "/swagger-ui.html",
                    "/v3/api-docs/**",
                    "/error"
                ).permitAll()

                /*
                 * Todas las operaciones de citas
                 * requieren autenticacion JWT.
                 *
                 * Los permisos mas especificos
                 * se validan mediante @PreAuthorize
                 * en CitaController.
                 */
                .requestMatchers(
                    "/api/v1/citas",
                    "/api/v1/citas/**"
                ).authenticated()

                /*
                 * Rechazar cualquier otra ruta
                 * que no este autorizada.
                 */
                .anyRequest().denyAll()
            )

            /*
             * No almacenar sesiones en el servidor.
             *
             * Cada solicitud debe enviar su JWT.
             */
            .sessionManagement(session ->
                session.sessionCreationPolicy(
                    SessionCreationPolicy.STATELESS
                )
            )

            /*
             * Manejar los errores relacionados
             * con autenticacion y permisos.
             */
            .exceptionHandling(ex -> ex

                /*
                 * HTTP 401:
                 * el usuario no esta autenticado
                 * o no presenta un token valido.
                 */
                .authenticationEntryPoint(
                    (request, response, error) -> {

                        response.setStatus(401);

                        response.setContentType(
                            "application/json;charset=UTF-8"
                        );

                        response.getWriter().write(
                            "{\"status\":401," +
                            "\"mensaje\":\"Debes iniciar sesion.\"}"
                        );
                    }
                )

                /*
                 * HTTP 403:
                 * el usuario esta autenticado,
                 * pero no tiene permisos suficientes.
                 */
                .accessDeniedHandler(
                    (request, response, error) -> {

                        response.setStatus(403);

                        response.setContentType(
                            "application/json;charset=UTF-8"
                        );

                        response.getWriter().write(
                            "{\"status\":403," +
                            "\"mensaje\":\"No tienes permisos para esta accion.\"}"
                        );
                    }
                )
            );

        /*
         * Ejecutar el filtro JWT antes del filtro
         * de autenticacion de Spring Security.
         *
         * Esto permite identificar al usuario
         * antes de evaluar sus permisos.
         */
        http.addFilterBefore(
            jwtRequestFilter,
            UsernamePasswordAuthenticationFilter.class
        );

        return http.build();
    }

    /**
     * Configura CORS para permitir que el frontend
     * React se comunique con cita-service.
     *
     * Autoriza:
     *
     * - Los origenes indicados en application.properties.
     * - Los metodos HTTP utilizados por el CRUD.
     * - Los encabezados Authorization y Content-Type.
     *
     * @return Configuracion CORS para las rutas de la API.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {

        CorsConfiguration config =
                new CorsConfiguration();

        /*
         * Leer y preparar los origenes permitidos.
         */
        config.setAllowedOrigins(
            Arrays.stream(origenesPermitidos.split(","))
                    .map(String::trim)
                    .toList()
        );

        /*
         * Permitir los metodos HTTP necesarios
         * para gestionar las reservas.
         */
        config.setAllowedMethods(
            List.of(
                "GET",
                "POST",
                "PUT",
                "PATCH",
                "DELETE",
                "OPTIONS"
            )
        );

        /*
         * Permitir los encabezados necesarios
         * para enviar JSON y tokens JWT.
         */
        config.setAllowedHeaders(
            List.of(
                "Authorization",
                "Content-Type"
            )
        );

        /*
         * Mantener temporalmente el resultado
         * de las solicitudes preflight.
         */
        config.setMaxAge(3600L);

        /*
         * Aplicar la configuracion CORS
         * a todas las rutas del microservicio.
         */
        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration(
            "/**",
            config
        );

        return source;
    }
}
