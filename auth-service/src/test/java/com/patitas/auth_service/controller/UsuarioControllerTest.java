package com.patitas.auth_service.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.patitas.auth_service.config.JwtUtil;
import com.patitas.auth_service.model.Rol;
import com.patitas.auth_service.model.Usuario;
import com.patitas.auth_service.service.NegocioException;
import com.patitas.auth_service.service.UsuarioService;

@WebMvcTest(controllers = UsuarioController.class, excludeAutoConfiguration = {
    org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class
})
@AutoConfigureMockMvc(addFilters = false)
class UsuarioControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private UsuarioService usuarioService;
    @MockitoBean private JwtUtil jwtUtil;

    /** Simula la sesión que normalmente arma JwtRequestFilter a partir del token. */
    private UsernamePasswordAuthenticationToken sesion(String correo, String rol) {
        return new UsernamePasswordAuthenticationToken(correo, null, List.of(new SimpleGrantedAuthority("ROLE_" + rol)));
    }

    private Usuario usuario(Long id, String nombre, String correo, String rol) {
        Usuario u = new Usuario();
        u.setId(id);
        u.setNombre(nombre);
        u.setCorreo(correo);
        u.setActivo(true);
        u.setRol(new Rol(null, rol, null, null));
        return u;
    }

    @Test
    void listarUsuarios_returnOK() throws Exception {
        when(usuarioService.listarUsuarios()).thenReturn(List.of(
            usuario(1L, "Maria", "ma.calfileo@duoc.cl", Rol.ADMIN),
            usuario(4L, "Camila", "camila.rojas@gmail.com", Rol.CLIENTE)));

        mockMvc.perform(get("/api/v1/usuarios").principal(sesion("ma.calfileo@duoc.cl", Rol.ADMIN)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].nombre").value("Maria"))
            .andExpect(jsonPath("$[1].rol.nombre").value("CLIENTE"));
    }

    @Test
    void obtenerUsuario_clienteVeSuPropioPerfil_returnOK() throws Exception {
        when(usuarioService.obtenerPorId(4L)).thenReturn(usuario(4L, "Camila", "camila.rojas@gmail.com", Rol.CLIENTE));

        mockMvc.perform(get("/api/v1/usuarios/4").principal(sesion("camila.rojas@gmail.com", Rol.CLIENTE)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.correo").value("camila.rojas@gmail.com"));
    }

    @Test
    void obtenerUsuario_clienteVeOtroPerfil_return403() throws Exception {
        when(usuarioService.obtenerPorId(1L)).thenReturn(usuario(1L, "Maria", "ma.calfileo@duoc.cl", Rol.ADMIN));

        mockMvc.perform(get("/api/v1/usuarios/1").principal(sesion("camila.rojas@gmail.com", Rol.CLIENTE)))
            .andExpect(status().isForbidden());
    }

    @Test
    void obtenerUsuario_recepcionistaVeCualquiera_returnOK() throws Exception {
        when(usuarioService.obtenerPorId(4L)).thenReturn(usuario(4L, "Camila", "camila.rojas@gmail.com", Rol.CLIENTE));

        mockMvc.perform(get("/api/v1/usuarios/4").principal(sesion("jo.medina@duoc.cl", Rol.RECEPCIONISTA)))
            .andExpect(status().isOk());
    }

    @Test
    void obtenerUsuario_noExiste_return404() throws Exception {
        when(usuarioService.obtenerPorId(99L))
            .thenThrow(new NegocioException("id", "El usuario con id 99 no existe.", HttpStatus.NOT_FOUND));

        mockMvc.perform(get("/api/v1/usuarios/99").principal(sesion("ma.calfileo@duoc.cl", Rol.ADMIN)))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.mensaje").value("El usuario con id 99 no existe."));
    }

    @Test
    void actualizarUsuario_clienteEditaAOtro_return403() throws Exception {
        when(usuarioService.obtenerPorId(1L)).thenReturn(usuario(1L, "Maria", "ma.calfileo@duoc.cl", Rol.ADMIN));

        mockMvc.perform(put("/api/v1/usuarios/1").principal(sesion("camila.rojas@gmail.com", Rol.CLIENTE))
                .contentType(MediaType.APPLICATION_JSON).content("{\"nombre\":\"Hackeado\"}"))
            .andExpect(status().isForbidden());

        verify(usuarioService, never()).actualizarUsuario(anyLong(), any(), anyBoolean());
    }

    @Test
    void actualizarUsuario_clienteEditaSuPerfil_returnOK() throws Exception {
        Usuario camila = usuario(4L, "Camila", "camila.rojas@gmail.com", Rol.CLIENTE);
        Usuario actualizada = usuario(4L, "Camila", "camila.rojas@gmail.com", Rol.CLIENTE);
        actualizada.setTelefono("+56 9 1111 2222");
        when(usuarioService.obtenerPorId(4L)).thenReturn(camila);
        when(usuarioService.actualizarUsuario(eq(4L), any(Usuario.class), eq(false))).thenReturn(actualizada);

        mockMvc.perform(put("/api/v1/usuarios/4").principal(sesion("camila.rojas@gmail.com", Rol.CLIENTE))
                .contentType(MediaType.APPLICATION_JSON).content("{\"telefono\":\"+56 9 1111 2222\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.telefono").value("+56 9 1111 2222"));
    }

    @Test
    void eliminarUsuario_returnNoContent() throws Exception {
        mockMvc.perform(delete("/api/v1/usuarios/4").principal(sesion("ma.calfileo@duoc.cl", Rol.ADMIN)))
            .andExpect(status().isNoContent());
        verify(usuarioService).eliminarUsuario(4L);
    }

    @Test
    void eliminarCuentaFija_return403() throws Exception {
        doThrow(new NegocioException("id", "No se puede eliminar una cuenta fija del sistema.", HttpStatus.FORBIDDEN))
            .when(usuarioService).eliminarUsuario(1L);

        mockMvc.perform(delete("/api/v1/usuarios/1").principal(sesion("ro.cruces@duoc.cl", Rol.ADMIN)))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.mensaje").value("No se puede eliminar una cuenta fija del sistema."));
    }
}
