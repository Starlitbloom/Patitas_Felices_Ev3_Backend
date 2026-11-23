
package com.patitas.novedad_service.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.security.Keys;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class JwtRequestFilter extends OncePerRequestFilter {

    @Value("${jwt.secret}")
    private String secreto;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain chain)
            throws ServletException, IOException {

        String header = request.getHeader("Authorization");

        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);

            try {
                SecretKey clave = Keys.hmacShaKeyFor(
                    secreto.getBytes(StandardCharsets.UTF_8)
                );

                Claims claims = Jwts.parserBuilder()
                    .setSigningKey(clave)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

                String tipo = claims.get("tipo", String.class);
                String rol = claims.get("rol", String.class);
                String correo = claims.getSubject();

                if ("access".equals(tipo)
                        && rol != null
                        && correo != null) {

                    var autenticacion =
                        new UsernamePasswordAuthenticationToken(
                            correo,
                            null,
                            List.of(
                                new SimpleGrantedAuthority(
                                    "ROLE_" + rol
                                )
                            )
                        );

                    SecurityContextHolder.getContext()
                        .setAuthentication(autenticacion);
                }

            } catch (JwtException | IllegalArgumentException e) {
                SecurityContextHolder.clearContext();
            }
        }

        chain.doFilter(request, response);
    }
}
