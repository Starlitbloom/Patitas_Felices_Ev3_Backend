// Carpeta (paquete) donde vive este archivo: config
package com.patitas.servicio_clinico_service.config;

// Para separar el texto de cors.origenes por comas
import java.util.Arrays;
// Tipo "lista" de Java
import java.util.List;

// @Value: lee un valor de application.properties
import org.springframework.beans.factory.annotation.Value;
// @Bean: el objeto que devuelve el método lo administra Spring
import org.springframework.context.annotation.Bean;
// @Configuration: clase de configuración
import org.springframework.context.annotation.Configuration;
// Verbos HTTP (GET, POST, OPTIONS...)
import org.springframework.http.HttpMethod;
// Activa @PreAuthorize en los controllers
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
// Objeto para configurar la seguridad web
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
// Política de sesiones (usamos STATELESS)
import org.springframework.security.config.http.SessionCreationPolicy;
// Cadena de filtros de seguridad que arma Spring
import org.springframework.security.web.SecurityFilterChain;
// Filtro de login de Spring (nuestro filtro JWT va antes que él)
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
// Clases para configurar CORS
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

// Reglas de seguridad del microservicio (misma estructura que auth-service):
// - Rutas públicas: Swagger y el catálogo (solo GET: cualquiera puede ver los servicios)
// - Todo lo demás requiere un token JWT válido
// - Los permisos por rol se revisan en el controller con @PreAuthorize
// - STATELESS: el servidor no guarda sesiones; toda la info va en el token
@Configuration
@EnableMethodSecurity // Habilita @PreAuthorize en los controllers
public class SecurityConfig {

    // Nuestro filtro que lee el token
    private final JwtRequestFilter jwtRequestFilter;

    // Direcciones permitidas para CORS (de application.properties)
    @Value("${cors.origenes}")
    private String origenesPermitidos;

    // Constructor: Spring entrega el filtro JWT
    public SecurityConfig(JwtRequestFilter jwtRequestFilter) {
        this.jwtRequestFilter = jwtRequestFilter;
    }

    // Define las reglas de seguridad
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // API REST con JWT: no usa cookies, así que CSRF no aplica
            .csrf(csrf -> csrf.disable())
            // Usa la configuración de CORS definida más abajo
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            // Reglas por ruta
            .authorizeHttpRequests(auth -> auth
                // Peticiones OPTIONS (preflight del navegador) siempre permitidas
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                // Swagger es público
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html").permitAll()
                // El catálogo se puede LEER sin iniciar sesión (crear/editar/borrar sí piden token)
                .requestMatchers(HttpMethod.GET, "/api/v1/servicios/**").permitAll()
                // Todo lo demás necesita token
                .anyRequest().authenticated()
            )
            // No se guardan sesiones en el servidor
            .sessionManagement(sess -> sess.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            // Respuestas JSON en vez de la página por defecto de Spring
            .exceptionHandling(ex -> ex
                // Sin token o token inválido -> 401
                .authenticationEntryPoint((req, res, e) -> {
                    res.setStatus(401);
                    res.setContentType("application/json;charset=UTF-8");
                    res.getWriter().write("{\"status\":401,\"campo\":null,\"mensaje\":\"Debes iniciar sesión o tu token expiró.\"}");
                })
                // Token válido pero rol insuficiente -> 403
                .accessDeniedHandler((req, res, e) -> {
                    res.setStatus(403);
                    res.setContentType("application/json;charset=UTF-8");
                    res.getWriter().write("{\"status\":403,\"campo\":null,\"mensaje\":\"No tienes permisos para realizar esta acción.\"}");
                })
            );

        // Nuestro filtro JWT se ejecuta antes del filtro de login de Spring
        http.addFilterBefore(jwtRequestFilter, UsernamePasswordAuthenticationFilter.class);
        // Construye y devuelve la configuración
        return http.build();
    }

    // CORS: permite que el frontend (Vite, puerto 5173) llame a esta API desde el navegador
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        // Objeto de configuración
        CorsConfiguration config = new CorsConfiguration();
        // Orígenes permitidos (separa el texto por comas)
        config.setAllowedOrigins(Arrays.asList(origenesPermitidos.split(",")));
        // Verbos HTTP permitidos
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        // Headers que el frontend puede enviar
        config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        // El navegador recuerda esta respuesta 1 hora
        config.setMaxAge(3600L);

        // Aplica la configuración a todas las rutas
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        // Devuelve la configuración
        return source;
    }
}
