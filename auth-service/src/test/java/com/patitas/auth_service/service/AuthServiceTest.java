package com.patitas.auth_service.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import com.patitas.auth_service.config.JwtUtil;
import com.patitas.auth_service.model.Rol;
import com.patitas.auth_service.model.Usuario;
import com.patitas.auth_service.repository.UsuarioRepository;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UsuarioRepository usuarioRepository;
    @Mock private UsuarioService usuarioService;
    @Mock private BCryptPasswordEncoder passwordEncoder;
    @Mock private JwtUtil jwtUtil;

    @InjectMocks private AuthService authService;

    private Usuario camila(boolean activo) {
        Usuario u = new Usuario();
        u.setId(4L);
        u.setCorreo("camila.rojas@gmail.com");
        u.setPassword("$2a$10$hash");
        u.setActivo(activo);
        u.setRol(new Rol(3L, Rol.CLIENTE, "Dueño/a de mascota", null));
        return u;
    }

    @Test
    void login_correcto_entregaTokens() {
        Usuario u = camila(true);
        when(usuarioRepository.findByCorreoIgnoreCase("camila.rojas@gmail.com")).thenReturn(Optional.of(u));
        when(passwordEncoder.matches("gato123", "$2a$10$hash")).thenReturn(true);
        when(jwtUtil.generarToken(u)).thenReturn("access");
        when(jwtUtil.generarRefreshToken(u)).thenReturn("refresh");

        Map<String, Object> respuesta = authService.login("camila.rojas@gmail.com", "gato123");

        assertEquals("access", respuesta.get("token"));
        assertEquals("refresh", respuesta.get("refreshToken"));
        assertEquals(u, respuesta.get("usuario"));
    }

    @Test
    void login_correoNoRegistrado_lanza401() {
        when(usuarioRepository.findByCorreoIgnoreCase("nadie@gmail.com")).thenReturn(Optional.empty());

        NegocioException e = assertThrows(NegocioException.class, () -> authService.login("nadie@gmail.com", "1234"));
        assertEquals("correo", e.getCampo());
        assertEquals(HttpStatus.UNAUTHORIZED, e.getStatus());
    }

    @Test
    void login_passwordIncorrecta_lanza401() {
        when(usuarioRepository.findByCorreoIgnoreCase("camila.rojas@gmail.com")).thenReturn(Optional.of(camila(true)));
        when(passwordEncoder.matches("mala", "$2a$10$hash")).thenReturn(false);

        NegocioException e = assertThrows(NegocioException.class, () -> authService.login("camila.rojas@gmail.com", "mala"));
        assertEquals("password", e.getCampo());
    }

    @Test
    void login_cuentaDesactivada_lanza403() {
        when(usuarioRepository.findByCorreoIgnoreCase("camila.rojas@gmail.com")).thenReturn(Optional.of(camila(false)));
        when(passwordEncoder.matches("gato123", "$2a$10$hash")).thenReturn(true);

        NegocioException e = assertThrows(NegocioException.class, () -> authService.login("camila.rojas@gmail.com", "gato123"));
        assertEquals(HttpStatus.FORBIDDEN, e.getStatus());
    }

    @Test
    void renovar_refreshInvalido_lanza401() {
        when(jwtUtil.esValido("token-falso", JwtUtil.TIPO_REFRESH)).thenReturn(false);

        NegocioException e = assertThrows(NegocioException.class, () -> authService.renovar("token-falso"));
        assertEquals(HttpStatus.UNAUTHORIZED, e.getStatus());
    }

    @Test
    void renovar_refreshValido_entregaTokensNuevos() {
        Usuario u = camila(true);
        when(jwtUtil.esValido("refresh", JwtUtil.TIPO_REFRESH)).thenReturn(true);
        when(jwtUtil.extraerCorreo("refresh")).thenReturn("camila.rojas@gmail.com");
        when(usuarioRepository.findByCorreoIgnoreCase("camila.rojas@gmail.com")).thenReturn(Optional.of(u));
        when(jwtUtil.generarToken(u)).thenReturn("access-nuevo");
        when(jwtUtil.generarRefreshToken(u)).thenReturn("refresh-nuevo");

        assertEquals("access-nuevo", authService.renovar("refresh").get("token"));
    }
}
