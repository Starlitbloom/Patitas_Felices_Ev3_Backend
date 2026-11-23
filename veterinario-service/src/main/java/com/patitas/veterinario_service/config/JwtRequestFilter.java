
package com.patitas.veterinario_service.config;

import java.io.IOException;
import java.util.List;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import io.jsonwebtoken.Claims;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import lombok.RequiredArgsConstructor;

/**
 * Filtro de autenticacion JWT del microservicio de veterinarios.
 *
 * Se ejecuta una vez por cada solicitud HTTP.
 *
 * Su funcionamiento es el siguiente:
 *
 * 1. Lee el encabezado Authorization de la solicitud.
 * 2. Comprueba si contiene un token Bearer.
 * 3. Verifica que el token sea valido y de tipo "access".
 * 4. Obtiene el correo y el rol del usuario autenticado.
 * 5. Registra la autenticacion en Spring Security.
 *
 * Los tokens son generados por auth-service.
 * Este microservicio solo los valida.
 */
@Component
@RequiredArgsConstructor
public class JwtRequestFilter extends OncePerRequestFilter {

    /**
     * Utilidad encargada de validar la firma,
     * la expiracion y el tipo del token JWT.
     */
    private final JwtUtil jwtUtil;

    /**
     * Intercepta las solicitudes HTTP para comprobar
     * si contienen un token JWT valido.
     *
     * @param request Solicitud HTTP recibida.
     * @param response Respuesta HTTP.
     * @param chain Cadena de filtros de Spring Security.
     * @throws ServletException Si ocurre un error del servlet.
     * @throws IOException Si ocurre un error de entrada o salida.
     */
    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain chain)
            throws ServletException, IOException {

        // Obtener el encabezado de autorizacion.
        String authHeader = request.getHeader("Authorization");

        // Comprobar que exista un token Bearer y que
        // el usuario aun no este autenticado.
        if (authHeader != null
                && authHeader.startsWith("Bearer ")
                && SecurityContextHolder.getContext()
                        .getAuthentication() == null) {

            // Extraer el token, eliminando el prefijo "Bearer ".
            String token = authHeader.substring(7);

            // Solo se aceptan tokens validos de tipo "access".
            // Los tokens "refresh" no autorizan operaciones REST.
            if (jwtUtil.esValido(token, JwtUtil.TIPO_ACCESS)) {

                // Leer los datos contenidos en el token.
                Claims claims = jwtUtil.leerClaims(token);

                // Obtener el correo y rol del usuario.
                String correo = claims.getSubject();
                String rol = claims.get("rol", String.class);

                // Registrar la autenticacion solo cuando
                // el token contiene un usuario y rol validos.
                if (correo != null && !correo.isBlank()
                        && rol != null && !rol.isBlank()) {

                    // Crear la autoridad reconocida por Spring Security.
                    // Por ejemplo: ADMIN se convierte en ROLE_ADMIN.
                    SimpleGrantedAuthority autoridad =
                            new SimpleGrantedAuthority("ROLE_" + rol);

                    // Crear el objeto de autenticacion.
                    UsernamePasswordAuthenticationToken autenticacion =
                            new UsernamePasswordAuthenticationToken(
                                    correo,
                                    null,
                                    List.of(autoridad)
                            );

                    // Incorporar los detalles de la solicitud HTTP.
                    autenticacion.setDetails(
                            new WebAuthenticationDetailsSource()
                                    .buildDetails(request)
                    );

                    // Registrar al usuario como autenticado.
                    SecurityContextHolder.getContext()
                            .setAuthentication(autenticacion);
                }
            }
        }

        // Continuar con los siguientes filtros de seguridad.
        // SecurityConfig decidira si permite o rechaza la solicitud.
        chain.doFilter(request, response);
    }
}
