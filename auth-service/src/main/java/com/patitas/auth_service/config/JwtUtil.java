package com.patitas.auth_service.config;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.patitas.auth_service.model.Usuario;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

/**
 * Genera y valida los tokens JWT.
 *
 * Se manejan DOS tipos de token:
 * - "access": dura poco (30 min). Se envía en cada petición.
 * - "refresh": dura 7 días. Solo sirve para pedir un access nuevo
 *   en /auth/refresh, sin tener que volver a escribir la contraseña.
 *
 * Ambos van firmados con la clave secreta (HS256). Si alguien modifica
 * el token (por ejemplo, se cambia el rol a ADMIN), la firma deja de
 * calzar y el token se rechaza.
 */
@Component
public class JwtUtil {

    public static final String TIPO_ACCESS = "access";
    public static final String TIPO_REFRESH = "refresh";

    @Value("${jwt.secret}")
    private String secreto;

    @Value("${jwt.expiracion-ms}")
    private long expiracionMs;

    @Value("${jwt.refresh-expiracion-ms}")
    private long refreshExpiracionMs;

    private Key clave() {
        return Keys.hmacShaKeyFor(secreto.getBytes());
    }

    /* ---------------- Generar ---------------- */

    /** Token de acceso con el id, rol y nombre del usuario dentro (claims). */
    public String generarToken(Usuario usuario) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("id", usuario.getId());
        claims.put("rol", usuario.getRol().getNombre()); // ADMIN | RECEPCIONISTA | CLIENTE
        claims.put("nombre", usuario.getNombre());
        claims.put("tipo", TIPO_ACCESS);
        return crear(claims, usuario.getCorreo(), expiracionMs);
    }

    /** Token de renovación: solo lleva el correo y el tipo. */
    public String generarRefreshToken(Usuario usuario) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("tipo", TIPO_REFRESH);
        return crear(claims, usuario.getCorreo(), refreshExpiracionMs);
    }

    private String crear(Map<String, Object> claims, String correo, long duracionMs) {
        long ahora = System.currentTimeMillis();
        return Jwts.builder()
            .setClaims(claims)
            .setSubject(correo) // "sub" = correo del usuario
            .setIssuedAt(new Date(ahora))
            .setExpiration(new Date(ahora + duracionMs))
            .signWith(clave(), SignatureAlgorithm.HS256)
            .compact();
    }

    /* ---------------- Leer / validar ---------------- */

    /**
     * Lee el contenido del token. Lanza JwtException si la firma
     * no es válida o si el token ya expiró.
     */
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

    public String extraerCorreo(String token) {
        return leerClaims(token).getSubject();
    }

    public String extraerRol(String token) {
        return leerClaims(token).get("rol", String.class);
    }

    public long getExpiracionMs() {
        return expiracionMs;
    }
}
