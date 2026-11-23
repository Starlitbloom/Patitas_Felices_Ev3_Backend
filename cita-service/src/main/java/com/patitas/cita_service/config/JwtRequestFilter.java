
package com.patitas.cita_service.config;

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
 * Filtro de autenticacion JWT del microservicio de citas.
 *
 * Su objetivo es identificar al usuario que realiza
 * una solicitud HTTP utilizando el token generado
 * por auth-service.
 *
 * El filtro realiza las siguientes operaciones:
 *
 * 1. Obtiene el encabezado Authorization.
 * 2. Extrae el token JWT de tipo Bearer.
 * 3. Comprueba su firma, vencimiento y tipo.
 * 4. Obtiene el correo y rol del usuario.
 * 5. Registra la autenticacion en Spring Security.
 *
 * Este microservicio no genera tokens JWT:
 * solamente valida los emitidos por auth-service.
 */
@Component
@RequiredArgsConstructor
public class JwtRequestFilter extends OncePerRequestFilter {

    /**
     * Clase encargada de validar los tokens JWT
     * y obtener sus datos.
     *
     * Se implementara en JwtUtil.java.
     */
    private final JwtUtil jwtUtil;

    /**
     * Intercepta cada solicitud HTTP para comprobar
     * si el usuario presenta un token JWT valido.
     *
     * @param request Solicitud HTTP recibida.
     * @param response Respuesta HTTP.
     * @param chain Cadena de filtros de seguridad.
     * @throws ServletException Error en el procesamiento.
     * @throws IOException Error de entrada o salida.
     */
    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain chain)
            throws ServletException, IOException {

        // Obtener el encabezado Authorization.
        String authHeader =
                request.getHeader("Authorization");

        /*
         * Verificar que:
         *
         * - Exista el encabezado Authorization.
         * - El token utilice el prefijo Bearer.
         * - No exista ya una autenticacion registrada.
         */
        if (authHeader != null
                && authHeader.startsWith("Bearer ")
                && SecurityContextHolder.getContext()
                        .getAuthentication() == null) {

            // Extraer el token sin el prefijo Bearer.
            String token = authHeader.substring(7).trim();

            /*
             * Comprobar que el token:
             *
             * - Tenga una firma valida.
             * - No este vencido.
             * - Sea de tipo access.
             *
             * Los tokens refresh no sirven
             * para acceder a los endpoints de citas.
             */
            if (jwtUtil.esValido(
                    token,
                    JwtUtil.TIPO_ACCESS)) {

                // Obtener los datos del token validado.
                Claims claims = jwtUtil.leerClaims(token);

                // Correo de la cuenta autenticada.
                String correoUsuario = claims.getSubject();

                // Rol asignado por auth-service.
                String rol = claims.get("rol", String.class);

                /*
                 * Registrar solamente usuarios
                 * que tengan correo y rol validos.
                 */
                if (correoUsuario != null
                        && !correoUsuario.isBlank()
                        && rol != null
                        && ("ADMIN".equals(rol)
                            || "CLIENTE".equals(rol))) {

                    /*
                     * Spring Security identifica
                     * los roles mediante ROLE_.
                     *
                     * Ejemplos:
                     *
                     * ADMIN   -> ROLE_ADMIN
                     * CLIENTE -> ROLE_CLIENTE
                     */
                    SimpleGrantedAuthority autoridad =
                            new SimpleGrantedAuthority(
                                    "ROLE_" + rol
                            );

                    /*
                     * Crear la autenticacion del usuario.
                     *
                     * El correo se utiliza como identidad
                     * principal del usuario autenticado.
                     */
                    UsernamePasswordAuthenticationToken autenticacion =
                            new UsernamePasswordAuthenticationToken(
                                    correoUsuario,
                                    null,
                                    List.of(autoridad)
                            );

                    /*
                     * Asociar los detalles de la solicitud
                     * a la autenticacion.
                     */
                    autenticacion.setDetails(
                            new WebAuthenticationDetailsSource()
                                    .buildDetails(request)
                    );

                    /*
                     * Registrar al usuario autenticado
                     * en el contexto de Spring Security.
                     *
                     * De esta forma, CitaController
                     * podra obtener el correo usando:
                     *
                     * authentication.getName()
                     */
                    SecurityContextHolder.getContext()
                            .setAuthentication(autenticacion);
                }
            }
        }

        /*
         * Continuar con la cadena de filtros.
         *
         * SecurityConfig y @PreAuthorize determinaran
         * si el usuario tiene permisos para acceder.
         *
         * Las solicitudes sin autenticacion valida
         * seran rechazadas en los endpoints protegidos.
         */
        chain.doFilter(request, response);
    }
}
