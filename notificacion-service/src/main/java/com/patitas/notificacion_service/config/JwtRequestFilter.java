package com.patitas.notificacion_service.config;

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
 * Filtro que se ejecuta en CADA petición:
 * 1. Lee el header "Authorization: Bearer <token>"
 * 2. Si el token es válido y es de tipo "access", registra al usuario
 *    como autenticado con su rol (ROLE_ADMIN, ROLE_CLIENTE...)
 * 3. Si no hay token o es inválido, la petición sigue SIN autenticar
 *    y SecurityConfig decide si la deja pasar o responde 401.
 */
@Component
@RequiredArgsConstructor
public class JwtRequestFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")
                && SecurityContextHolder.getContext().getAuthentication() == null) {
            String token = authHeader.substring(7);

            if (jwtUtil.esValido(token, JwtUtil.TIPO_ACCESS)) {
                Claims claims = jwtUtil.leerClaims(token);
                String correo = claims.getSubject();
                String rol = claims.get("rol", String.class);

                UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                    correo, null, List.of(new SimpleGrantedAuthority("ROLE_" + rol)));
                auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(auth);
            }
        }

        chain.doFilter(request, response);
    }
}
