package com.patitas.auth_service.service;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import com.patitas.auth_service.config.JwtUtil;
import com.patitas.auth_service.model.Usuario;
import com.patitas.auth_service.repository.UsuarioRepository;

import lombok.RequiredArgsConstructor;

/**
 * Inicio de sesión, registro y renovación de tokens.
 * Todas devuelven el mismo JSON:
 * { token, refreshToken, tipo: "Bearer", expiraEnMs, usuario }
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final UsuarioService usuarioService;
    private final BCryptPasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    /** Verifica correo y contraseña. Si son correctos entrega los tokens. */
    public Map<String, Object> login(String correo, String password) {
        if (correo == null || correo.isBlank()) throw new NegocioException("correo", "El correo es obligatorio.");
        if (password == null || password.isEmpty()) throw new NegocioException("password", "La contraseña es obligatoria.");

        Usuario usuario = usuarioRepository.findByCorreoIgnoreCase(correo.trim())
            .orElseThrow(() -> new NegocioException("correo",
                "No existe una cuenta registrada con este correo.", HttpStatus.UNAUTHORIZED));

        // BCrypt compara la contraseña escrita con el hash guardado
        if (!passwordEncoder.matches(password, usuario.getPassword())) {
            throw new NegocioException("password", "Contraseña incorrecta.", HttpStatus.UNAUTHORIZED);
        }
        if (!Boolean.TRUE.equals(usuario.getActivo())) {
            throw new NegocioException("correo", "Tu cuenta está desactivada. Contacta a la clínica.", HttpStatus.FORBIDDEN);
        }
        return emitirTokens(usuario);
    }

    /** Registro público desde la página /registro: siempre queda como CLIENTE. */
    public Map<String, Object> registrar(Usuario datos) {
        Usuario nuevo = usuarioService.crearUsuario(datos, false);
        return emitirTokens(nuevo); // Queda con la sesión iniciada al registrarse
    }

    /**
     * Renueva el token de acceso usando el refresh token.
     * Se vuelve a leer el usuario de la base de datos: si fue desactivado
     * o le cambiaron el rol, el token nuevo ya refleja ese cambio.
     */
    public Map<String, Object> renovar(String refreshToken) {
        if (refreshToken == null || !jwtUtil.esValido(refreshToken, JwtUtil.TIPO_REFRESH)) {
            throw new NegocioException("refreshToken", "La sesión expiró. Inicia sesión nuevamente.", HttpStatus.UNAUTHORIZED);
        }
        Usuario usuario = usuarioRepository.findByCorreoIgnoreCase(jwtUtil.extraerCorreo(refreshToken))
            .orElseThrow(() -> new NegocioException("refreshToken", "El usuario ya no existe.", HttpStatus.UNAUTHORIZED));
        if (!Boolean.TRUE.equals(usuario.getActivo())) {
            throw new NegocioException("refreshToken", "Tu cuenta está desactivada.", HttpStatus.FORBIDDEN);
        }
        return emitirTokens(usuario);
    }

    private Map<String, Object> emitirTokens(Usuario usuario) {
        Map<String, Object> respuesta = new LinkedHashMap<>();
        respuesta.put("token", jwtUtil.generarToken(usuario));
        respuesta.put("refreshToken", jwtUtil.generarRefreshToken(usuario));
        respuesta.put("tipo", "Bearer");
        respuesta.put("expiraEnMs", jwtUtil.getExpiracionMs());
        respuesta.put("usuario", usuario); // La contraseña NO se envía (WRITE_ONLY)
        return respuesta;
    }
}

