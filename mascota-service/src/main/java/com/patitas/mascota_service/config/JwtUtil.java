// Carpeta (paquete) donde vive este archivo: config
package com.patitas.mascota_service.config;

// @Value: permite leer un valor del archivo application.properties
import org.springframework.beans.factory.annotation.Value;
// @Component: Spring crea esta clase solo y la deja disponible para otras (como el filtro)
import org.springframework.stereotype.Component;

// Claims = el contenido del token (correo, rol, tipo, fecha de expiración)
import io.jsonwebtoken.Claims;
// Error general de jjwt: token mal formado, firma falsa o vencido
import io.jsonwebtoken.JwtException;
// Herramienta principal de la librería jjwt para leer y verificar tokens
import io.jsonwebtoken.Jwts;
// Herramienta para convertir el secreto (texto) en una llave criptográfica
import io.jsonwebtoken.security.Keys;

// Tipo "Key": una llave criptográfica
import java.security.Key;

/**
 * Valida los tokens JWT que emite auth-service.
 * Este microservicio NO genera tokens: solo comprueba la firma (HS256) con la clave
 * compartida, que no haya expirado y que sea de tipo "access".
 */
// Spring crea esta clase automáticamente
@Component
// Clase con las herramientas para revisar tokens. Solo VALIDA, nunca crea tokens (eso lo hace auth-service)
public class JwtUtil {

    // Constante con el tipo de token que se acepta: "access". El token "refresh" no sirve para llamar a la API
    public static final String TIPO_ACCESS = "access";

    // Lee la propiedad jwt.secret del application.properties y la guarda aquí. Es la clave compartida con auth-service
    @Value("${jwt.secret}")
    private String secreto;

    // Método ayudante (privado): convierte el texto del secreto en una llave criptográfica
    private Key clave() {
        // Pasa el texto a bytes y crea la llave HMAC (la usada para firmar y verificar con HS256)
        return Keys.hmacShaKeyFor(secreto.getBytes());
    }

    /** Lee el contenido del token. Lanza JwtException si la firma es invalida o ya expiro. */
    // Abre el token y devuelve su contenido. Si algo está mal, lanza un error
    public Claims leerClaims(String token) {
        // Empieza a construir el "lector" de tokens
        return Jwts.parserBuilder()
            // Le dice con qué llave comprobar la firma. Si el token fue firmado con otra clave o alterado, falla
            .setSigningKey(clave())
            // Termina de construir el lector
            .build()
            // Lee el token y verifica la firma y la fecha de expiración; si todo está bien, sigue
            .parseClaimsJws(token)
            // Devuelve el contenido (los claims) del token
            .getBody();
    }

    /** true si el token es valido, no ha expirado y es del tipo esperado. */
    // Dice si el token sirve: devuelve true o false. No lanza errores hacia afuera
    public boolean esValido(String token, String tipoEsperado) {
        // Intenta leer el token (aquí es donde puede fallar)
        try {
            // Lee el contenido; si la firma es falsa o está vencido, salta al catch
            Claims claims = leerClaims(token);
            // Compara el claim "tipo" del token con el esperado ("access"). true solo si coinciden
            return tipoEsperado.equals(claims.get("tipo", String.class));
        // Si hubo cualquier problema con el token (firma falsa, vencido, mal formado, vacío)...
        } catch (JwtException | IllegalArgumentException e) {
            // ...devuelve false: el token no sirve
            return false;
        }
    }
}