package com.patitas.auth_service.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
import com.patitas.auth_service.service.AuthService;
import com.patitas.auth_service.service.NegocioException;
import com.patitas.auth_service.service.UsuarioService;

/**
 * Pruebas del controlador de autenticación.
 * @WebMvcTest levanta solo la capa web (controlador + manejo de errores + JSON)
 * y los servicios se reemplazan por simulaciones (@MockitoBean, el nuevo nombre de @MockBean).
 */
@WebMvcTest(controllers = AuthController.class, excludeAutoConfiguration = {
    org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class
})
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private AuthService authService;
    @MockitoBean private UsuarioService usuarioService;
    @MockitoBean private JwtUtil jwtUtil;

    private Usuario maria() {
        Usuario u = new Usuario();
        u.setId(1L);
        u.setNombre("Maria");
        u.setCorreo("ma.calfileo@duoc.cl");
        u.setPassword("$2a$10$hashSecreto");
        u.setActivo(true);
        u.setRol(new Rol(1L, Rol.ADMIN, "Administrador", null));
        return u;
    }

    private Map<String, Object> respuestaTokens(Usuario u) {
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("token", "token-de-acceso");
        r.put("refreshToken", "token-de-renovacion");
        r.put("tipo", "Bearer");
        r.put("expiraEnMs", 1800000L);
        r.put("usuario", u);
        return r;
    }

    @Test
    void login_correcto_returnOK() throws Exception {
        when(authService.login("ma.calfileo@duoc.cl", "1234")).thenReturn(respuestaTokens(maria()));

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"correo\":\"ma.calfileo@duoc.cl\",\"password\":\"1234\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.token").value("token-de-acceso"))
            .andExpect(jsonPath("$.refreshToken").value("token-de-renovacion"))
            .andExpect(jsonPath("$.usuario.rol.nombre").value("ADMIN"))
            .andExpect(jsonPath("$.usuario.password").doesNotExist()); // La contraseña nunca sale
    }

    @Test
    void login_passwordIncorrecta_return401ConCampo() throws Exception {
        when(authService.login("ma.calfileo@duoc.cl", "mala"))
            .thenThrow(new NegocioException("password", "Contraseña incorrecta.", HttpStatus.UNAUTHORIZED));

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"correo\":\"ma.calfileo@duoc.cl\",\"password\":\"mala\"}"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.campo").value("password"))
            .andExpect(jsonPath("$.mensaje").value("Contraseña incorrecta."));
    }

    @Test
    void registro_correcto_returnCreated() throws Exception {
        Usuario nuevo = maria();
        nuevo.setRol(new Rol(3L, Rol.CLIENTE, "Dueño/a de mascota", null));
        when(authService.registrar(any(Usuario.class))).thenReturn(respuestaTokens(nuevo));

        mockMvc.perform(post("/api/v1/auth/registro")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"run":"12.345.678-5","nombre":"Pedro","apellidos":"Pérez",
                     "correo":"pedro@gmail.com","password":"pedro1"}
                    """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.usuario.rol.nombre").value("CLIENTE"));
    }

    @Test
    void registro_correoRepetido_return409() throws Exception {
        when(authService.registrar(any(Usuario.class)))
            .thenThrow(new NegocioException("correo", "Ya existe una cuenta registrada con este correo.", HttpStatus.CONFLICT));

        mockMvc.perform(post("/api/v1/auth/registro")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"correo\":\"ma.calfileo@duoc.cl\"}"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.campo").value("correo"));
    }

    @Test
    void refresh_tokenValido_returnOK() throws Exception {
        when(authService.renovar("token-de-renovacion")).thenReturn(respuestaTokens(maria()));

        mockMvc.perform(post("/api/v1/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"refreshToken\":\"token-de-renovacion\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.token").value("token-de-acceso"));
    }

    @Test
    void me_devuelveElUsuarioDelToken() throws Exception {
        when(usuarioService.obtenerPorCorreo("ma.calfileo@duoc.cl")).thenReturn(maria());
        var sesion = new UsernamePasswordAuthenticationToken(
            "ma.calfileo@duoc.cl", null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));

        mockMvc.perform(get("/api/v1/auth/me").principal(sesion))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.nombre").value("Maria"))
            .andExpect(jsonPath("$.password").doesNotExist());
    }
}
