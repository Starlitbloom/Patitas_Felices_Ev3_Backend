
package com.patitas.veterinario_service.config;

import java.nio.charset.StandardCharsets;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

/**
 * Utilidad encargada de validar los tokens JWT
 * utilizados en Patitas Felices.
 *
 * Los tokens son generados por auth-service.
 * El microservicio veterinario-service los recibe
 * y comprueba que sean autenticos y esten vigentes.
 *
 * Para que funcione correctamente, ambos servicios
 * deben utilizar la misma clave secreta JWT.
 *
 * Esta clase permite:
 *
 * - Verificar la firma digital del token.
 * - Comprobar su fecha de vencimiento.
 * - Validar que sea del tipo esperado.
 * - Obtener los datos del usuario autenticado.
 */
@Component
public class JwtUtil {

    /**
     * Tipo de token permitido para acceder
     * a los endpoints protegidos.
     *
     * Los tokens de renovacion "refresh"
     * no deben utilizarse para autorizar operaciones.
     */
    public static final String TIPO_ACCESS = "access";

    /**
     * Clave utilizada para verificar la firma JWT.
     *
     * Debe coincidir con la clave de auth-service.
     */
    private final SecretKey clave;

    /**
     * Constructor que obtiene la clave JWT
     * desde application.properties.
     *
     * La propiedad jwt.secret utiliza la variable
     * de entorno JWT_SECRET.
     *
     * @param secreto Clave compartida con auth-service.
     */
    public JwtUtil(@Value("${jwt.secret}") String secreto) {

        // Convertir la clave a bytes UTF-8.
        byte[] claveBytes =
                secreto.getBytes(StandardCharsets.UTF_8);

        // Construir la clave criptografica.
        // Para HS256 se requieren al menos 32 bytes.
        this.clave = Keys.hmacShaKeyFor(claveBytes);
    }

    /**
     * Lee los datos contenidos en un JWT.
     *
     * Antes de entregar los datos, JJWT comprueba:
     *
     * - Que la firma sea valida.
     * - Que el token no haya vencido.
     * - Que su estructura sea correcta.
     *
     * @param token Token JWT recibido.
     * @return Datos contenidos en el token.
     * @throws JwtException Si el token no es valido.
     */
    public Claims leerClaims(String token) {

        return Jwts.parserBuilder()
                .setSigningKey(clave)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * Comprueba si un JWT es valido y corresponde
     * al tipo de token esperado.
     *
     * En este microservicio solo se aceptan
     * tokens de tipo "access".
     *
     * Si el token fue alterado, esta vencido
     * o tiene un tipo incorrecto, devuelve false.
     *
     * @param token Token JWT que se desea verificar.
     * @param tipoEsperado Tipo requerido, por ejemplo "access".
     * @return true si el token es valido;
     *         false en caso contrario.
     */
    public boolean esValido(
            String token,
            String tipoEsperado) {

        // Rechazar tokens vacios o nulos.
        if (token == null || token.isBlank()) {
            return false;
        }

        try {

            // Verificar firma y vencimiento.
            Claims claims = leerClaims(token);

            // Obtener el tipo de token.
            String tipo = claims.get("tipo", String.class);

            // Obtener el usuario autenticado.
            String usuario = claims.getSubject();

            // El token debe contener un usuario valido
            // y coincidir con el tipo esperado.
            return usuario != null
                    && !usuario.isBlank()
                    && tipoEsperado.equals(tipo);

        } catch (JwtException | IllegalArgumentException e) {

            // Rechazar tokens vencidos, modificados,
            // mal formados o con firma incorrecta.
            return false;
        }
    }
}
