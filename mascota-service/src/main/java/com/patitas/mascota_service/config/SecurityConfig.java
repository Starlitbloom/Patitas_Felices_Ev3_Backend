// Carpeta (paquete) donde vive este archivo: config
package com.patitas.mascota_service.config;

// Herramientas de Java para trabajar con listas y arreglos
import java.util.Arrays;
import java.util.List;

// @Value: lee un valor del application.properties
import org.springframework.beans.factory.annotation.Value;
// @Bean: marca un método cuyo resultado Spring guarda y usa
import org.springframework.context.annotation.Bean;
// @Configuration: marca la clase como de configuración
import org.springframework.context.annotation.Configuration;
// Nombres de métodos HTTP (GET, POST, OPTIONS...)
import org.springframework.http.HttpMethod;
// Activa @PreAuthorize en los controllers
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
// Objeto para armar las reglas de seguridad
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
// Para indicar cómo se manejan las sesiones (aquí: no se guardan)
import org.springframework.security.config.http.SessionCreationPolicy;
// El resultado final: la cadena de reglas de seguridad
import org.springframework.security.web.SecurityFilterChain;
// Filtro de usuario/contraseña de Spring: se usa como punto de referencia para ubicar nuestro filtro JWT
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
// Clases para configurar CORS (permisos para que el frontend llame a la API)
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Reglas de seguridad del microservicio (misma estructura que auth-service):
 * - Rutas publicas: Swagger
 * - Todo lo demas requiere un token JWT valido
 * - Los permisos por rol se revisan en el controlador con @PreAuthorize
 * - STATELESS: el servidor no guarda sesiones, toda la info va en el token
 */
// Spring lee esta clase al iniciar para aplicar sus reglas
@Configuration
// Activa @PreAuthorize("hasRole('ADMIN')") en los controllers
@EnableMethodSecurity // Habilita @PreAuthorize en los controladores
// Clase que define las reglas de seguridad: quién entra, a qué, y con qué rol
public class SecurityConfig {

    // El filtro JWT (el "guardia" que lee el token)
    private final JwtRequestFilter jwtRequestFilter;

    // Lee del application.properties los orígenes permitidos (cors.origenes), o sea, las direcciones del frontend
    @Value("${cors.origenes}")
    private String origenesPermitidos;

    // Constructor: Spring entrega el filtro JWT automáticamente
    public SecurityConfig(JwtRequestFilter jwtRequestFilter) {
        // Guarda el filtro para usarlo más abajo
        this.jwtRequestFilter = jwtRequestFilter;
    }

    // Spring ejecuta este método al iniciar; el resultado son las reglas de seguridad de toda la API
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        // Empieza a configurar las reglas
        http
            // Desactiva CSRF: es una protección para páginas con cookies; esta API usa tokens, no cookies
            .csrf(csrf -> csrf.disable()) // API REST con JWT: no usa cookies, CSRF no aplica
            // Activa CORS usando la configuración de más abajo
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            // Empieza a definir qué rutas son públicas y cuáles no
            .authorizeHttpRequests(auth -> auth
                // Deja pasar las peticiones OPTIONS: el navegador las envía antes de la petición real (preflight de CORS)
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll() // Preflight del navegador
                // Deja pública la documentación Swagger (se puede abrir sin token)
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html").permitAll()
                // TODO lo demás exige estar autenticado (con token válido)
                .anyRequest().authenticated()
            )
            // No se guardan sesiones en el servidor: cada petición debe traer su token
            .sessionManagement(sess -> sess.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            // Respuestas JSON en vez de la pagina por defecto de Spring
            // Define qué responder cuando hay un error de seguridad
            .exceptionHandling(ex -> ex
                // Caso 401: no hay token, es falso o está vencido
                .authenticationEntryPoint((req, res, e) -> {
                    // Pone el código HTTP 401 (no autenticado)
                    res.setStatus(401);
                    // Indica que la respuesta es JSON, con tildes bien codificadas
                    res.setContentType("application/json;charset=UTF-8");
                    // Escribe el JSON de error con el mismo formato que usa auth-service
                    res.getWriter().write("{\"status\":401,\"campo\":null,\"mensaje\":\"Debes iniciar sesión o tu token expiró.\"}");
                })
                // Caso 403: hay usuario válido, pero su rol no tiene permiso
                .accessDeniedHandler((req, res, e) -> {
                    // Pone el código HTTP 403 (prohibido)
                    res.setStatus(403);
                    // Indica que la respuesta es JSON
                    res.setContentType("application/json;charset=UTF-8");
                    // Escribe el JSON de error
                    res.getWriter().write("{\"status\":403,\"campo\":null,\"mensaje\":\"No tienes permisos para realizar esta acción.\"}");
                })
            );

        // Coloca el filtro JWT ANTES del filtro de usuario/contraseña, para que lea el token primero
        http.addFilterBefore(jwtRequestFilter, UsernamePasswordAuthenticationFilter.class);
        // Termina de construir y devuelve las reglas
        return http.build();
    }

    /** CORS: permite que el frontend (Vite, puerto 5173) llame a esta API. */
    // Spring guarda este resultado y lo usa en la línea .cors(...) de arriba
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        // Crea la configuración de CORS vacía
        CorsConfiguration config = new CorsConfiguration();
        // Orígenes permitidos: parte el texto del properties por comas y los convierte en lista (localhost:5173 y 127.0.0.1:5173)
        config.setAllowedOrigins(Arrays.asList(origenesPermitidos.split(",")));
        // Métodos HTTP que el frontend puede usar
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        // Cabeceras permitidas: Authorization (para enviar el token) y Content-Type (para enviar JSON)
        config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        // El navegador recuerda estos permisos por 3600 segundos (1 hora) y no vuelve a preguntar
        config.setMaxAge(3600L);

        // Crea el objeto que asocia la configuración a las rutas
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        // Aplica esta configuración a TODAS las rutas ("/**")
        source.registerCorsConfiguration("/**", config);
        // Devuelve la configuración lista
        return source;
    }
}