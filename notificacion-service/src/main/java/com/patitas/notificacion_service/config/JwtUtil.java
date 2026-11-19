package com.patitas.notificacion_service.config;

import java.security.Key;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

/**
 * Este microservicio NO genera tokens: solo VALIDA los que entrega auth-service.
 * Funciona porque todos los microservicios comparten la misma clave secreta
 * (jwt.secret en application.properties). Si la firma no calza o el token
 * expiró, se rechaza.
 */
@Component
public class JwtUtil {

    public static final String TIPO_ACCESS = "access";

    @Value("${jwt.secret}")
    private String secreto;

    private Key clave() {
        return Keys.hmacShaKeyFor(secreto.getBytes());
    }

    /** Lee el contenido del token. Lanza JwtException si es inválido o expiró. */
    public Claims leerClaims(String token) {
        return Jwts.parserBuilder()
            .setSigningKey(clave())
            .build()
            .parseClaimsJws(token)
            .getBody();
    }

    /** true si el token es válido, no ha expirado y es del tipo esperado. */
    public boolean esValido(String token, String tipoEsperado) {
        try {
            Claims claims = leerClaims(token);
            return tipoEsperado.equals(claims.get("tipo", String.class));
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }
}
