package com.patitas.report_service.controller;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.patitas.report_service.config.JwtUtil;
import com.patitas.report_service.model.ReporteGenerado;
import com.patitas.report_service.model.TipoReporte;
import com.patitas.report_service.service.NegocioException;
import com.patitas.report_service.service.ReporteService;

@WebMvcTest(controllers = ReporteController.class, excludeAutoConfiguration = {
    org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class
})
@AutoConfigureMockMvc(addFilters = false)
class ReporteControllerTest {

    private static final String TOKEN = "Bearer token-de-prueba";

    @Autowired private MockMvc mockMvc;

    @MockitoBean private ReporteService reporteService;
    @MockitoBean private JwtUtil jwtUtil;

    private UsernamePasswordAuthenticationToken sesion(String correo, String rol) {
        return new UsernamePasswordAuthenticationToken(correo, null, List.of(new SimpleGrantedAuthority("ROLE_" + rol)));
    }

    @Test
    void dashboard_admin_reenviaTokenYEsAdmin() throws Exception {
        Map<String, Object> clinica = new LinkedHashMap<>();
        clinica.put("mascotasRegistradas", 3);
        clinica.put("citasPendientes", null); // cita-service no respondió
        Map<String, Object> respuesta = new LinkedHashMap<>();
        respuesta.put("clinica", clinica);
        respuesta.put("serviciosNoDisponibles", List.of("cita-service"));
        when(reporteService.dashboard(TOKEN, true)).thenReturn(respuesta);

        mockMvc.perform(get("/api/v1/reportes/dashboard").principal(sesion("ma.calfileo@duoc.cl", "ADMIN"))
                .header("Authorization", TOKEN))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.clinica.mascotasRegistradas").value(3))
            .andExpect(jsonPath("$.serviciosNoDisponibles[0]").value("cita-service"));
    }

    @Test
    void dashboard_recepcionista_noEsAdmin() throws Exception {
        when(reporteService.dashboard(TOKEN, false)).thenReturn(Map.of("clinica", Map.of()));

        mockMvc.perform(get("/api/v1/reportes/dashboard").principal(sesion("jo.medina@duoc.cl", "RECEPCIONISTA"))
                .header("Authorization", TOKEN))
            .andExpect(status().isOk());
        verify(reporteService).dashboard(TOKEN, false);
    }

    @Test
    void reporteCompleto_registraQuienLoGenera() throws Exception {
        when(reporteService.reporteCompleto(TOKEN, "ma.calfileo@duoc.cl"))
            .thenReturn(Map.of("generadoPor", "ma.calfileo@duoc.cl"));

        mockMvc.perform(get("/api/v1/reportes/completo").principal(sesion("ma.calfileo@duoc.cl", "ADMIN"))
                .header("Authorization", TOKEN))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.generadoPor").value("ma.calfileo@duoc.cl"));
    }

    @Test
    void exportar_devuelveHojas() throws Exception {
        when(reporteService.exportar(TOKEN, "ma.calfileo@duoc.cl")).thenReturn(Map.of(
            "nombreArchivo", "reporte-completo-patitas-felices",
            "hojas", Map.of("Usuarios", List.of(Map.of("Nombre", "Maria Calfileo")))));

        mockMvc.perform(get("/api/v1/reportes/exportar").principal(sesion("ma.calfileo@duoc.cl", "ADMIN"))
                .header("Authorization", TOKEN))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.hojas.Usuarios[0].Nombre").value("Maria Calfileo"));
    }

    @Test
    void historial_returnOK() throws Exception {
        ReporteGenerado r = new ReporteGenerado();
        r.setId(1L);
        r.setTipo(TipoReporte.EXPORTACION);
        r.setGeneradoPor("ma.calfileo@duoc.cl");
        when(reporteService.historial()).thenReturn(List.of(r));

        mockMvc.perform(get("/api/v1/reportes/historial").principal(sesion("ma.calfileo@duoc.cl", "ADMIN")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].tipo").value("EXPORTACION"));
    }

    @Test
    void historial_noExiste_return404() throws Exception {
        when(reporteService.obtenerDelHistorial(99L))
            .thenThrow(new NegocioException("id", "El reporte con id 99 no existe.", HttpStatus.NOT_FOUND));

        mockMvc.perform(get("/api/v1/reportes/historial/99").principal(sesion("ma.calfileo@duoc.cl", "ADMIN")))
            .andExpect(status().isNotFound());
    }

    @Test
    void eliminarHistorial_returnNoContent() throws Exception {
        mockMvc.perform(delete("/api/v1/reportes/historial/1").principal(sesion("ma.calfileo@duoc.cl", "ADMIN")))
            .andExpect(status().isNoContent());
        verify(reporteService).eliminarDelHistorial(1L);
    }

    @Test
    void eliminarHistorial_noExiste_return404() throws Exception {
        doThrow(new NegocioException("id", "El reporte con id 5 no existe.", HttpStatus.NOT_FOUND))
            .when(reporteService).eliminarDelHistorial(5L);

        mockMvc.perform(delete("/api/v1/reportes/historial/5").principal(sesion("ma.calfileo@duoc.cl", "ADMIN")))
            .andExpect(status().isNotFound());
    }
}
