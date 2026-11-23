// Carpeta (paquete) donde vive este archivo: config
package com.patitas.mascota_service.config;

// Error de entrada/salida que puede ocurrir al procesar una petición
import java.io.IOException;
// Para crear una lista (aquí, la lista de roles del usuario)
import java.util.List;

// Objeto que representa "este usuario ya está autenticado" para Spring Security
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
// Representa un permiso/rol (ej: ROLE_ADMIN)
import org.springframework.security.core.authority.SimpleGrantedAuthority;
// Es la "memoria" de Spring Security: aquí se guarda quién es el usuario de la petición actual
import org.springframework.security.core.context.SecurityContextHolder;
// Agrega datos extra de la petición (IP, etc.) al usuario autenticado
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
// @Component: Spring crea esta clase solo y la registra
import org.springframework.stereotype.Component;
// Clase base de filtros: garantiza que el filtro se ejecute UNA sola vez por petición
import org.springframework.web.filter.OncePerRequestFilter;

// Claims = el contenido del token (correo, rol, tipo, etc.)
import io.jsonwebtoken.Claims;
// Permite pasar la petición al siguiente filtro de la cadena
import jakarta.servlet.FilterChain;
// Error que puede ocurrir dentro de un filtro
import jakarta.servlet.ServletException;
// La petición HTTP que llega (con sus headers)
import jakarta.servlet.http.HttpServletRequest;
// La respuesta HTTP que se devuelve
import jakarta.servlet.http.HttpServletResponse;

/**
 * Filtro que se ejecuta en CADA peticion (igual al de auth-service):
 * 1. Lee el header "Authorization: Bearer <token>"
 * 2. Si el token es valido y es de tipo "access", registra al usuario autenticado con su rol
 *    (ROLE_ADMIN, ROLE_RECEPCIONISTA, ROLE_CLIENTE)
 * 3. Si no hay token o es invalido, la peticion sigue SIN autenticar y SecurityConfig responde 401.
 */
// Spring crea este filtro automáticamente
@Component
// Un filtro es un "guardia" por el que pasa cada petición ANTES de llegar al controller
public class JwtRequestFilter extends OncePerRequestFilter {

    // Herramienta que lee y valida tokens (la clase JwtUtil)
    private final JwtUtil jwtUtil;

    // Constructor: Spring entrega el JwtUtil automáticamente
    public JwtRequestFilter(JwtUtil jwtUtil) {
        // Guarda el JwtUtil para usarlo más abajo
        this.jwtUtil = jwtUtil;
    }

    // Método que se ejecuta en cada petición. Recibe la petición, la respuesta y la cadena de filtros
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        // Lee el header "Authorization" de la petición; ahí viene el token, con la forma "Bearer eyJ..."
        String authHeader = request.getHeader("Authorization");

        // Solo sigue si: (1) existe el header, (2) empieza con "Bearer " y (3) todavía no hay un usuario autenticado
        if (authHeader != null && authHeader.startsWith("Bearer ")
                && SecurityContextHolder.getContext().getAuthentication() == null) {
            // Quita las 7 letras de "Bearer " y se queda solo con el token
            String token = authHeader.substring(7);

            // Revisa que el token tenga firma válida, no esté vencido y sea de tipo "access" (el refresh no sirve aquí)
            if (jwtUtil.esValido(token, JwtUtil.TIPO_ACCESS)) {
                // Lee el contenido del token
                Claims claims = jwtUtil.leerClaims(token);
                // Saca el correo del usuario (en el token va como "subject")
                String correo = claims.getSubject();
                // Saca el rol del usuario: ADMIN, RECEPCIONISTA o CLIENTE
                String rol = claims.get("rol", String.class);

                // Crea el objeto "usuario autenticado": su correo, sin contraseña (null) y su rol con el prefijo ROLE_ que exige Spring
                UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                    correo, null, List.of(new SimpleGrantedAuthority("ROLE_" + rol)));
                // Le agrega detalles de la petición (IP, sesión)
                auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                // Lo guarda en la "memoria" de Spring Security: desde aquí el resto del sistema sabe quién es y qué rol tiene
                SecurityContextHolder.getContext().setAuthentication(auth);
            }
        }

        // Deja pasar la petición al siguiente paso (otro filtro o el controller). Si no hubo token válido, pasa SIN autenticar y SecurityConfig responde 401
        chain.doFilter(request, response);
    }
}