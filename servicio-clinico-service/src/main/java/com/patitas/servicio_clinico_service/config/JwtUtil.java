// Carpeta (paquete) donde vive este archivo: config
package com.patitas.servicio_clinico_service.config;

// @Value: lee un valor de application.properties
import org.springframework.beans.factory.annotation.Value;
// @Component: Spring crea esta clase y la entrega a quien la necesite
import org.springframework.stereotype.Component;

// Claims: los datos que van dentro del token (correo, rol, id, tipo...)
import io.jsonwebtoken.Claims;
// Error que lanza la librería si el token es inválido o expiró
import io.jsonwebtoken.JwtException;
// Herramienta principal para leer tokens
import io.jsonwebtoken.Jwts;
// Para crear la llave de firma a partir del texto secreto
import io.jsonwebtoken.security.Keys;

// Tipo de dato "llave criptográfica"
import java.security.Key;

// Valida los tokens JWT que emite auth-service.
// Este microservicio NO crea tokens: solo revisa la firma (HS256) con la clave compartida,
// que no haya expirado y que sea de tipo "access".
@Component
public class JwtUtil {

    // Tipo de token que se acepta para usar la API (el "refresh" solo sirve para renovar)
    public static final String TIPO_ACCESS = "access";

    // Clave secreta compartida, leída de application.properties (jwt.secret)
    @Value("${jwt.secret}")
    private String secreto;

    // Convierte el texto secreto en una llave para verificar la firma
    private Key clave() {
        return Keys.hmacShaKeyFor(secreto.getBytes());
    }

    // Lee el contenido del token. Lanza JwtException si la firma es inválida o ya expiró
    public Claims leerClaims(String token) {
        return Jwts.parserBuilder()   // Crea un lector de tokens
            .setSigningKey(clave())   // Con qué llave verificar la firma
            .build()                  // Arma el lector
            .parseClaimsJws(token)    // Verifica firma y expiración
            .getBody();               // Devuelve los datos del token
    }

    // true si el token es válido, no expiró y es del tipo esperado
    public boolean esValido(String token, String tipoEsperado) {
        try {
            // Si el token está mal o expiró, esta línea lanza una excepción
            Claims claims = leerClaims(token);
            // Compara el claim "tipo" con el esperado ("access")
            return tipoEsperado.equals(claims.get("tipo", String.class));
        } catch (JwtException | IllegalArgumentException e) {
            // Cualquier problema con el token = no es válido
            return false;
        }
    }
}
