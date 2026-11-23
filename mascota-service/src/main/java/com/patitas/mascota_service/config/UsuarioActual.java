// Carpeta (paquete) donde vive este archivo: config
package com.patitas.mascota_service.config;

// Authentication: el objeto de Spring Security que representa "el usuario autenticado de esta petición"
import org.springframework.security.core.Authentication;
// GrantedAuthority: representa un permiso o rol (ej: ROLE_ADMIN)
import org.springframework.security.core.GrantedAuthority;

/** Datos del usuario que hace la peticion, sacados del JWT (JwtRequestFilter guarda el correo como nombre). */
// Record = clase corta de solo datos. Guarda 2 cosas del usuario: su correo y si es personal de la clínica (staff)
public record UsuarioActual(String correo, boolean staff) {

    /** staff = admin o recepcionista (personal de la clinica). */
    // Método convertidor: recibe el usuario autenticado de Spring y devuelve un UsuarioActual. "static" = se usa sin crear objeto: UsuarioActual.de(auth)
    public static UsuarioActual de(Authentication auth) {
        // Calcula si es personal: true si tiene el rol ADMIN o RECEPCIONISTA; false si es CLIENTE
        boolean staff = auth.getAuthorities().stream()
            // Convierte cada permiso en su texto (ej: "ROLE_ADMIN")
            .map(GrantedAuthority::getAuthority)
            // Pregunta: ¿alguno de los roles es ROLE_ADMIN o ROLE_RECEPCIONISTA?
            .anyMatch(a -> a.equals("ROLE_ADMIN") || a.equals("ROLE_RECEPCIONISTA"));
        // Crea el UsuarioActual: auth.getName() es el correo (lo guardó JwtRequestFilter) y staff es lo calculado arriba
        return new UsuarioActual(auth.getName(), staff);
    }
}