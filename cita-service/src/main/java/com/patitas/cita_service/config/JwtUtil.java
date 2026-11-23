
package com.patitas.cita_service.config;

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
 * utilizados en el microservicio de citas
 * de Patitas Felices.
 *
 * Los tokens son generados por auth-service.
 * cita-service solamente verifica su autenticidad.
 *
 * Esta clase permite:
 *
 * - Verificar la firma del token.
 * - Comprobar que no haya vencido.
 * - Validar que sea un token de acceso.
 * - Obtener los datos del usuario autenticado.
 *
 * Debe utilizar la misma clave JWT de auth-service.
 */
@Component
public class JwtUtil {

    /**
     * Tipo de token autorizado para acceder
     * a los endpoints protegidos.
     *
     * Los tokens refresh no pueden utilizarse
     * para realizar operaciones sobre citas.
     */
    public static final String TIPO_ACCESS = "access";

    /**
     * Clave criptografica utilizada para
     * verificar la firma de los tokens JWT.
     */
    private final SecretKey clave;

    /**
     * Constructor que obtiene la clave JWT
     * desde la configuracion del microservicio.
     *
     * La propiedad jwt.secret se configura
     * mediante la variable de entorno JWT_SECRET.
     *
     * @param secreto Clave compartida con auth-service.
     */
    public JwtUtil(@Value("${jwt.secret}") String secreto) {

        // Convertir la clave a bytes UTF-8.
        byte[] claveBytes =
                secreto.getBytes(StandardCharsets.UTF_8);

        // Crear la clave para verificar tokens HMAC.
        // HS256 requiere una clave de al menos 32 bytes.
        this.clave = Keys.hmacShaKeyFor(claveBytes);
    }

    /**
     * Obtiene la informacion contenida en un JWT.
     *
     * Antes de devolver los datos, la libreria
     * verifica la firma y la fecha de vencimiento.
     *
     * Entre los datos utilizados por cita-service estan:
     *
     * - sub: correo electronico del usuario.
     * - rol: ADMIN o CLIENTE.
     * - tipo: access o refresh.
     *
     * @param token Token JWT recibido.
     * @return Datos del token validado.
     * @throws JwtException Si el token es invalido.
     */
    public Claims leerClaims(String token) {

        return Jwts.parserBuilder()
                .setSigningKey(clave)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * Comprueba que un token JWT sea valido
     * y corresponda al tipo esperado.
     *
     * Verifica:
     *
     * 1. Que el token no este vacio.
     * 2. Que tenga una firma correcta.
     * 3. Que no haya vencido.
     * 4. Que contenga un usuario.
     * 5. Que su tipo sea el esperado.
     *
     * @param token Token JWT recibido.
     * @param tipoEsperado Tipo requerido, por ejemplo access.
     * @return true si el token es valido;
     *         false si no cumple las condiciones.
     */
    public boolean esValido(
            String token,
            String tipoEsperado) {

        // Rechazar tokens nulos o vacios.
        if (token == null || token.isBlank()
                || tipoEsperado == null) {
            return false;
        }

        try {

            // Verificar la firma y el vencimiento del JWT.
            Claims claims = leerClaims(token);

            // Obtener el correo del usuario.
            String usuario = claims.getSubject();

            // Obtener el tipo de token.
            String tipo = claims.get("tipo", String.class);

            // Comprobar que exista un usuario
            // y que se trate de un token de acceso.
            return usuario != null
                    && !usuario.isBlank()
                    && tipoEsperado.equals(tipo);

        } catch (JwtException | IllegalArgumentException e) {

            // Rechazar tokens vencidos, alterados,
            // mal formados o con firma incorrecta.
            return false;
        }
    }
}
