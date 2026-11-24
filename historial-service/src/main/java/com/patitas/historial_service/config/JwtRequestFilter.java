// Carpeta (paquete) donde vive este archivo: config
package com.patitas.historial_service.config;

// Error de lectura/escritura que puede lanzar el filtro
import java.io.IOException;
// Tipo "lista" de Java
import java.util.List;

// Objeto que representa al usuario autenticado
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
// Un permiso o rol (ej: ROLE_ADMIN)
import org.springframework.security.core.authority.SimpleGrantedAuthority;
// Lugar donde Spring guarda quién está autenticado en esta petición
import org.springframework.security.core.context.SecurityContextHolder;
// Agrega datos de la petición (IP, etc.) al usuario autenticado
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
// @Component: Spring crea esta clase
import org.springframework.stereotype.Component;
// Filtro que se ejecuta una sola vez por petición
import org.springframework.web.filter.OncePerRequestFilter;

// Datos que van dentro del token
import io.jsonwebtoken.Claims;
// Clases de la petición/respuesta HTTP
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

// Filtro que se ejecuta en CADA petición (igual al de auth-service):
// 1. Lee el header "Authorization: Bearer <token>"
// 2. Si el token es válido y de tipo "access", registra al usuario con su rol (ROLE_ADMIN, ROLE_RECEPCIONISTA, ROLE_CLIENTE)
// 3. Si no hay token o es inválido, la petición sigue SIN autenticar y SecurityConfig responde 401
@Component
public class JwtRequestFilter extends OncePerRequestFilter {

    // Herramienta para validar el token
    private final JwtUtil jwtUtil;

    // Constructor: Spring entrega el JwtUtil
    public JwtRequestFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    // Método que se ejecuta en cada petición
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        // Lee el header Authorization (ej: "Bearer eyJhbGci...")
        String authHeader = request.getHeader("Authorization");

        // Solo sigue si hay header, empieza con "Bearer " y todavía nadie está autenticado
        if (authHeader != null && authHeader.startsWith("Bearer ")
                && SecurityContextHolder.getContext().getAuthentication() == null) {
            // Quita "Bearer " (7 caracteres) y deja solo el token
            String token = authHeader.substring(7);

            // Revisa firma, expiración y que sea de tipo "access"
            if (jwtUtil.esValido(token, JwtUtil.TIPO_ACCESS)) {
                // Lee los datos del token
                Claims claims = jwtUtil.leerClaims(token);
                // El "subject" del token es el correo del usuario
                String correo = claims.getSubject();
                // El rol viene en el claim "rol" (ADMIN, RECEPCIONISTA o CLIENTE)
                String rol = claims.get("rol", String.class);

                // Crea el usuario autenticado: nombre = correo, sin contraseña, con el permiso ROLE_<rol>
                UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                    correo, null, List.of(new SimpleGrantedAuthority("ROLE_" + rol)));
                // Le agrega los detalles de la petición
                auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                // Lo registra: desde aquí @PreAuthorize sabe quién es y qué rol tiene
                SecurityContextHolder.getContext().setAuthentication(auth);
            }
        }

        // Deja pasar la petición al siguiente filtro / al controller
        chain.doFilter(request, response);
    }
}
