package com.patitas.notificacion_service.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.patitas.notificacion_service.config.JwtUtil;
import com.patitas.notificacion_service.model.Notificacion;
import com.patitas.notificacion_service.model.TipoNotificacion;
import com.patitas.notificacion_service.service.NegocioException;
import com.patitas.notificacion_service.service.NotificacionService;

@WebMvcTest(controllers = NotificacionController.class, excludeAutoConfiguration = {
    org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class
})
@AutoConfigureMockMvc(addFilters = false)
class NotificacionControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private NotificacionService notificacionService;
    @MockitoBean private JwtUtil jwtUtil;

    private UsernamePasswordAuthenticationToken sesion(String correo, String rol) {
        return new UsernamePasswordAuthenticationToken(correo, null, List.of(new SimpleGrantedAuthority("ROLE_" + rol)));
    }

    private Notificacion notificacion(Long id, boolean leida) {
        Notificacion n = new Notificacion();
        n.setId(id);
        n.setCorreoDestinatario("camila.rojas@gmail.com");
        n.setTitulo("Cita confirmada");
        n.setMensaje("Tu cita quedó confirmada.");
        n.setTipo(TipoNotificacion.CITA);
        n.setLeida(leida);
        return n;
    }

    @Test
    void crear_comoRecepcionista_esPersonal_returnCreated() throws Exception {
        when(notificacionService.crear(any(Notificacion.class), eq("jo.medina@duoc.cl"), eq(true)))
            .thenReturn(notificacion(1L, false));

        mockMvc.perform(post("/api/v1/notificaciones").principal(sesion("jo.medina@duoc.cl", "RECEPCIONISTA"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    { "correoDestinatario": "camila.rojas@gmail.com", "titulo": "Cita confirmada",
                      "mensaje": "Tu cita quedó confirmada.", "tipo": "CITA" }"""))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.leida").value(false))
            .andExpect(jsonPath("$.tipo").value("CITA"));
    }

    @Test
    void crear_clienteParaOtro_return403() throws Exception {
        when(notificacionService.crear(any(Notificacion.class), eq("camila.rojas@gmail.com"), eq(false)))
            .thenThrow(new NegocioException("correoDestinatario", "Solo el personal de la clínica puede enviar avisos a otros usuarios.", HttpStatus.FORBIDDEN));

        mockMvc.perform(post("/api/v1/notificaciones").principal(sesion("camila.rojas@gmail.com", "CLIENTE"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{ \"correoDestinatario\": \"otra@gmail.com\", \"titulo\": \"Hola\", \"mensaje\": \"Prueba\" }"))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.campo").value("correoDestinatario"));
    }

    @Test
    void misNotificaciones_soloNoLeidas_returnOK() throws Exception {
        when(notificacionService.misNotificaciones("camila.rojas@gmail.com", true))
            .thenReturn(List.of(notificacion(1L, false)));

        mockMvc.perform(get("/api/v1/notificaciones/mis-notificaciones").param("soloNoLeidas", "true")
                .principal(sesion("camila.rojas@gmail.com", "CLIENTE")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].titulo").value("Cita confirmada"));
    }

    @Test
    void contarNoLeidas_paraLaCampanita() throws Exception {
        when(notificacionService.contarNoLeidas("camila.rojas@gmail.com")).thenReturn(3L);

        mockMvc.perform(get("/api/v1/notificaciones/mis-notificaciones/no-leidas")
                .principal(sesion("camila.rojas@gmail.com", "CLIENTE")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.cantidad").value(3));
    }

    @Test
    void marcarLeida_returnOK() throws Exception {
        when(notificacionService.marcarLeida(1L, "camila.rojas@gmail.com")).thenReturn(notificacion(1L, true));

        mockMvc.perform(put("/api/v1/notificaciones/1/leer").principal(sesion("camila.rojas@gmail.com", "CLIENTE")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.leida").value(true));
    }

    @Test
    void marcarTodasLeidas_devuelveCantidad() throws Exception {
        when(notificacionService.marcarTodasLeidas("camila.rojas@gmail.com")).thenReturn(4);

        mockMvc.perform(put("/api/v1/notificaciones/mis-notificaciones/leer-todas")
                .principal(sesion("camila.rojas@gmail.com", "CLIENTE")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.marcadas").value(4));
    }

    @Test
    void eliminar_ajena_return403() throws Exception {
        doThrow(new NegocioException(null, "Esta notificación no es tuya.", HttpStatus.FORBIDDEN))
            .when(notificacionService).eliminar(1L, "be.soto@duoc.cl", false);

        mockMvc.perform(delete("/api/v1/notificaciones/1").principal(sesion("be.soto@duoc.cl", "CLIENTE")))
            .andExpect(status().isForbidden());
    }

    @Test
    void eliminar_comoAdmin_returnNoContent() throws Exception {
        mockMvc.perform(delete("/api/v1/notificaciones/1").principal(sesion("ma.calfileo@duoc.cl", "ADMIN")))
            .andExpect(status().isNoContent());
        verify(notificacionService).eliminar(1L, "ma.calfileo@duoc.cl", true);
    }
}
