package com.patitas.contacto_service.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.patitas.contacto_service.config.JwtUtil;
import com.patitas.contacto_service.model.EstadoMensaje;
import com.patitas.contacto_service.model.MensajeContacto;
import com.patitas.contacto_service.service.MensajeService;
import com.patitas.contacto_service.service.NegocioException;

@WebMvcTest(controllers = MensajeController.class, excludeAutoConfiguration = {
    org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class
})
@AutoConfigureMockMvc(addFilters = false)
class MensajeControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private MensajeService mensajeService;
    @MockitoBean private JwtUtil jwtUtil;

    private static final String JSON_MENSAJE = """
        { "nombre": "Camila Rojas", "correo": "camila.rojas@gmail.com", "telefono": "+56 9 1234 5678",
          "asunto": "Información sobre citas", "mensaje": "¿Atienden conejos los sábados?" }""";

    private UsernamePasswordAuthenticationToken sesion(String correo, String rol) {
        return new UsernamePasswordAuthenticationToken(correo, null, List.of(new SimpleGrantedAuthority("ROLE_" + rol)));
    }

    private MensajeContacto mensaje(Long id, EstadoMensaje estado, String correoUsuario) {
        MensajeContacto m = new MensajeContacto();
        m.setId(id);
        m.setNombre("Camila Rojas");
        m.setCorreo("camila.rojas@gmail.com");
        m.setAsunto("Información sobre citas");
        m.setEstado(estado);
        m.setCorreoUsuario(correoUsuario);
        return m;
    }

    @Test
    void enviarMensaje_sinSesion_returnCreated() throws Exception {
        when(mensajeService.enviarMensaje(any(MensajeContacto.class), isNull()))
            .thenReturn(mensaje(1L, EstadoMensaje.PENDIENTE, null));

        mockMvc.perform(post("/api/v1/mensajes").contentType(MediaType.APPLICATION_JSON).content(JSON_MENSAJE))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.estado").value("PENDIENTE"));
    }

    @Test
    void enviarMensaje_conSesion_guardaCorreoDeLaCuenta() throws Exception {
        when(mensajeService.enviarMensaje(any(MensajeContacto.class), eq("camila.rojas@gmail.com")))
            .thenReturn(mensaje(1L, EstadoMensaje.PENDIENTE, "camila.rojas@gmail.com"));

        mockMvc.perform(post("/api/v1/mensajes").principal(sesion("camila.rojas@gmail.com", "CLIENTE"))
                .contentType(MediaType.APPLICATION_JSON).content(JSON_MENSAJE))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.correoUsuario").value("camila.rojas@gmail.com"));
    }

    @Test
    void enviarMensaje_datoInvalido_return400ConCampo() throws Exception {
        when(mensajeService.enviarMensaje(any(MensajeContacto.class), isNull()))
            .thenThrow(new NegocioException("telefono", "Ingresa un celular chileno válido. Ejemplo: +56 9 1234 5678."));

        mockMvc.perform(post("/api/v1/mensajes").contentType(MediaType.APPLICATION_JSON).content(JSON_MENSAJE))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.campo").value("telefono"));
    }

    @Test
    void listarBandeja_filtrandoPorEstado_returnOK() throws Exception {
        when(mensajeService.listarMensajes(EstadoMensaje.PENDIENTE))
            .thenReturn(List.of(mensaje(1L, EstadoMensaje.PENDIENTE, null)));

        mockMvc.perform(get("/api/v1/mensajes").param("estado", "PENDIENTE")
                .principal(sesion("jo.medina@duoc.cl", "RECEPCIONISTA")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].estado").value("PENDIENTE"));
    }

    @Test
    void obtenerMensaje_clienteVeElSuyo_returnOK() throws Exception {
        when(mensajeService.obtenerPorId(1L)).thenReturn(mensaje(1L, EstadoMensaje.RESPONDIDO, "camila.rojas@gmail.com"));

        mockMvc.perform(get("/api/v1/mensajes/1").principal(sesion("camila.rojas@gmail.com", "CLIENTE")))
            .andExpect(status().isOk());
    }

    @Test
    void obtenerMensaje_clienteVeUnoAjeno_return403() throws Exception {
        when(mensajeService.obtenerPorId(1L)).thenReturn(mensaje(1L, EstadoMensaje.PENDIENTE, "otra.persona@gmail.com"));

        mockMvc.perform(get("/api/v1/mensajes/1").principal(sesion("camila.rojas@gmail.com", "CLIENTE")))
            .andExpect(status().isForbidden());
    }

    @Test
    void cambiarEstado_aLeido_returnOK() throws Exception {
        when(mensajeService.cambiarEstado(1L, EstadoMensaje.LEIDO)).thenReturn(mensaje(1L, EstadoMensaje.LEIDO, null));

        mockMvc.perform(put("/api/v1/mensajes/1/estado").principal(sesion("ma.calfileo@duoc.cl", "ADMIN"))
                .contentType(MediaType.APPLICATION_JSON).content("{\"estado\":\"leido\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.estado").value("LEIDO"));
    }

    @Test
    void cambiarEstado_estadoInexistente_return400() throws Exception {
        mockMvc.perform(put("/api/v1/mensajes/1/estado").principal(sesion("ma.calfileo@duoc.cl", "ADMIN"))
                .contentType(MediaType.APPLICATION_JSON).content("{\"estado\":\"BORRADO\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.campo").value("estado"));
    }

    @Test
    void responder_guardaQuienResponde() throws Exception {
        MensajeContacto respondido = mensaje(1L, EstadoMensaje.RESPONDIDO, null);
        respondido.setRespondidoPor("jo.medina@duoc.cl");
        when(mensajeService.responder(1L, "Sí, atendemos conejos.", "jo.medina@duoc.cl", "Bearer token-de-prueba"))
            .thenReturn(respondido);

        mockMvc.perform(put("/api/v1/mensajes/1/responder").principal(sesion("jo.medina@duoc.cl", "RECEPCIONISTA"))
                .header("Authorization", "Bearer token-de-prueba")
                .contentType(MediaType.APPLICATION_JSON).content("{\"respuesta\":\"Sí, atendemos conejos.\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.estado").value("RESPONDIDO"))
            .andExpect(jsonPath("$.respondidoPor").value("jo.medina@duoc.cl"));
    }

    @Test
    void eliminarMensaje_returnNoContent() throws Exception {
        mockMvc.perform(delete("/api/v1/mensajes/1").principal(sesion("ma.calfileo@duoc.cl", "ADMIN")))
            .andExpect(status().isNoContent());
        verify(mensajeService).eliminarMensaje(1L);
    }
}
