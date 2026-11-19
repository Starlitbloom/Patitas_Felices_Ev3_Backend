package com.patitas.auth_service.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import com.patitas.auth_service.model.Rol;
import com.patitas.auth_service.model.Usuario;
import com.patitas.auth_service.repository.UsuarioRepository;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    @Mock private UsuarioRepository usuarioRepository;
    @Mock private RolService rolService;
    @Mock private BCryptPasswordEncoder passwordEncoder;

    @InjectMocks private UsuarioService usuarioService;

    private Usuario datosValidos() {
        Usuario r = new Usuario();
        r.setRun("19.011.022-2");
        r.setNombre("Camila");
        r.setApellidos("Rojas");
        r.setCorreo("camila@gmail.com");
        r.setPassword("gato123");
        r.setRol(new Rol("ADMIN", null)); // En el registro público debe ignorarse
        return r;
    }

    @Test
    void registroPublicoQuedaComoClienteYConPasswordEncriptada() {
        Rol cliente = new Rol(3L, Rol.CLIENTE, "Dueño/a", null);
        when(rolService.obtenerPorNombre(Rol.CLIENTE)).thenReturn(cliente);
        when(passwordEncoder.encode("gato123")).thenReturn("$2a$hash");
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        Usuario creado = usuarioService.crearUsuario(datosValidos(), false);

        assertEquals(Rol.CLIENTE, creado.getRol().getNombre());
        assertEquals("19011022-2", creado.getRun());
        assertNotEquals("gato123", creado.getPassword());
    }

    @Test
    void noPermiteCorreoRepetido() {
        when(usuarioRepository.existsByCorreoIgnoreCase("camila@gmail.com")).thenReturn(true);

        NegocioException e = assertThrows(NegocioException.class,
            () -> usuarioService.crearUsuario(datosValidos(), false));

        assertEquals("correo", e.getCampo());
        assertEquals(HttpStatus.CONFLICT, e.getStatus());
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void noPermiteEliminarCuentaFija() {
        Usuario admin = new Usuario();
        admin.setId(1L);
        admin.setCorreo("ma.calfileo@duoc.cl");
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(admin));

        assertThrows(NegocioException.class, () -> usuarioService.eliminarUsuario(1L));
        verify(usuarioRepository, never()).delete(any());
    }

    @Test
    void clienteNoPuedeCambiarseElRol() {
        Usuario u = new Usuario();
        u.setId(9L);
        u.setCorreo("camila@gmail.com");
        u.setRol(new Rol(3L, Rol.CLIENTE, "Dueño/a", null));
        when(usuarioRepository.findById(9L)).thenReturn(Optional.of(u));

        Usuario cambios = new Usuario();
        cambios.setRol(new Rol("ADMIN", null));

        NegocioException e = assertThrows(NegocioException.class,
            () -> usuarioService.actualizarUsuario(9L, cambios, false));
        assertEquals(HttpStatus.FORBIDDEN, e.getStatus());
    }
}
