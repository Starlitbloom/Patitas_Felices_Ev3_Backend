package com.patitas.report_service.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.patitas.report_service.model.ReporteGenerado;
import com.patitas.report_service.model.TipoReporte;
import com.patitas.report_service.repository.ReporteGeneradoRepository;
import com.patitas.report_service.webclient.AuthClient;
import com.patitas.report_service.webclient.CitaClient;
import com.patitas.report_service.webclient.ContactoClient;
import com.patitas.report_service.webclient.MascotaClient;
import com.patitas.report_service.webclient.NotificacionClient;

@ExtendWith(MockitoExtension.class)
class ReporteServiceTest {

    private static final String TOKEN = "Bearer token-admin";

    @Mock private AuthClient authClient;
    @Mock private MascotaClient mascotaClient;
    @Mock private CitaClient citaClient;
    @Mock private ContactoClient contactoClient;
    @Mock private NotificacionClient notificacionClient;
    @Mock private ReporteGeneradoRepository reporteRepository;

    @InjectMocks private ReporteService reporteService;

    @BeforeEach
    void nombres() {
        lenient().when(authClient.nombreServicio()).thenReturn("auth-service");
        lenient().when(mascotaClient.nombreServicio()).thenReturn("mascota-service");
        lenient().when(citaClient.nombreServicio()).thenReturn("cita-service");
        lenient().when(contactoClient.nombreServicio()).thenReturn("contacto-service");
        lenient().when(notificacionClient.nombreServicio()).thenReturn("notificacion-service");
    }

    /* ---------- Datos de prueba (con la misma forma que devuelve cada microservicio) ---------- */

    private Map<String, Object> usuario(String nombre, String rol, String region, String comuna, LocalDateTime registro) {
        Map<String, Object> u = new HashMap<>();
        u.put("nombre", nombre);
        u.put("apellidos", "Test");
        u.put("correo", nombre.toLowerCase() + "@gmail.com");
        u.put("rol", Map.of("nombre", rol)); // auth-service lo envía como objeto
        u.put("region", region);
        u.put("comuna", comuna);
        u.put("activo", true);
        u.put("fechaRegistro", registro.toString());
        return u;
    }

    private List<Map<String, Object>> usuarios() {
        LocalDateTime ahora = LocalDateTime.now();
        return List.of(
            usuario("Maria", "ADMIN", "O'Higgins", "Rancagua", ahora.minusDays(30)),
            usuario("Josefa", "RECEPCIONISTA", "O'Higgins", "Rancagua", ahora.minusDays(20)),
            usuario("Camila", "CLIENTE", "O'Higgins", "Machalí", ahora.minusDays(2)),
            usuario("Tomas", "CLIENTE", "Valparaíso", "Viña del Mar", ahora.minusDays(1)));
    }

    private List<Map<String, Object>> mascotas() {
        return List.of(
            Map.of("nombre", "Rocky", "especie", "Perro"),
            Map.of("nombre", "Luna", "especie", "Perro"),
            Map.of("nombre", "Michi", "especie", "Gato"));
    }

    private List<Map<String, Object>> citas() {
        return List.of(
            Map.of("estado", "PENDIENTE", "servicio", "Vacunación"),
            Map.of("estado", "PENDIENTE", "servicio", "Consulta general"),
            Map.of("estado", "CONFIRMADA", "servicio", "Vacunación"),
            Map.of("estado", "CANCELADA", "servicio", "Peluquería"));
    }

    /* ---------- Dashboard ---------- */

    @Test
    @SuppressWarnings("unchecked")
    void dashboardAdmin_todosLosServiciosResponden() {
        when(authClient.listarUsuarios(TOKEN)).thenReturn(Optional.of(usuarios()));
        when(mascotaClient.listarMascotas(TOKEN)).thenReturn(Optional.of(mascotas()));
        when(mascotaClient.listarSolicitudes(TOKEN)).thenReturn(Optional.of(List.of(Map.of("estado", "PENDIENTE"))));
        when(citaClient.listarCitas(TOKEN)).thenReturn(Optional.of(citas()));

        Map<String, Object> d = reporteService.dashboard(TOKEN, true);

        Map<String, Object> u = (Map<String, Object>) d.get("usuarios");
        Map<String, Object> c = (Map<String, Object>) d.get("clinica");
        assertEquals(4, u.get("total"));
        assertEquals(2L, u.get("nuevosEstaSemana"));
        assertEquals(3, c.get("mascotasRegistradas"));
        assertEquals(2L, c.get("citasPendientes"));
        assertEquals(1L, c.get("solicitudesPendientes"));
        assertTrue(((List<String>) d.get("serviciosNoDisponibles")).isEmpty());
    }

    @Test
    @SuppressWarnings("unchecked")
    void dashboard_servicioApagado_quedaNullYSeInforma() {
        when(authClient.listarUsuarios(TOKEN)).thenReturn(Optional.of(usuarios()));
        when(mascotaClient.listarMascotas(TOKEN)).thenReturn(Optional.empty()); // mascota-service apagado
        when(mascotaClient.listarSolicitudes(TOKEN)).thenReturn(Optional.empty());
        when(citaClient.listarCitas(TOKEN)).thenReturn(Optional.empty());       // cita-service apagado

        Map<String, Object> d = reporteService.dashboard(TOKEN, true);

        Map<String, Object> c = (Map<String, Object>) d.get("clinica");
        assertNull(c.get("mascotasRegistradas"));
        assertNull(c.get("citasPendientes"));
        assertEquals(List.of("mascota-service", "cita-service"), d.get("serviciosNoDisponibles"));
    }

    @Test
    void dashboardRecepcionista_noConsultaUsuarios() {
        when(mascotaClient.listarMascotas(TOKEN)).thenReturn(Optional.of(mascotas()));
        when(mascotaClient.listarSolicitudes(TOKEN)).thenReturn(Optional.of(List.of()));
        when(citaClient.listarCitas(TOKEN)).thenReturn(Optional.of(citas()));

        Map<String, Object> d = reporteService.dashboard(TOKEN, false);

        assertTrue(!d.containsKey("usuarios"));
        verify(authClient, never()).listarUsuarios(anyString());
    }

    /* ---------- Reporte completo ---------- */

    @Test
    @SuppressWarnings("unchecked")
    void reporteCompleto_calculaUsuariosPorRolRegionYComuna() {
        when(authClient.listarUsuarios(TOKEN)).thenReturn(Optional.of(usuarios()));
        when(mascotaClient.listarMascotas(TOKEN)).thenReturn(Optional.of(mascotas()));
        when(citaClient.listarCitas(TOKEN)).thenReturn(Optional.of(citas()));
        when(contactoClient.estadisticas(TOKEN)).thenReturn(Optional.of(Map.of("total", 3)));
        when(notificacionClient.estadisticas(TOKEN)).thenReturn(Optional.of(Map.of("total", 5)));

        Map<String, Object> r = reporteService.reporteCompleto(TOKEN, "ma.calfileo@duoc.cl");

        Map<String, Object> u = (Map<String, Object>) r.get("usuarios");
        assertEquals(1L, u.get("administradores"));
        assertEquals(1L, u.get("recepcionistas"));
        assertEquals(2L, u.get("clientes"));
        assertEquals(2L, u.get("regionesConUsuarios"));

        List<Map<String, Object>> porRegion = (List<Map<String, Object>>) u.get("porRegion");
        assertEquals("O'Higgins", porRegion.get(0).get("nombre")); // La región con más usuarios va primero
        assertEquals(3L, porRegion.get(0).get("cantidad"));

        List<Map<String, Object>> topComunas = (List<Map<String, Object>>) u.get("topComunas");
        assertEquals("Rancagua", topComunas.get(0).get("comuna"));
        assertEquals("O'Higgins", topComunas.get(0).get("region"));

        Map<String, Object> m = (Map<String, Object>) r.get("mascotas");
        assertEquals("Perro", ((List<Map<String, Object>>) m.get("porEspecie")).get(0).get("nombre"));
        assertEquals("ma.calfileo@duoc.cl", r.get("generadoPor"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void reporteCompleto_cuentaCitasPorEstadoYServicio() {
        when(authClient.listarUsuarios(TOKEN)).thenReturn(Optional.of(List.of()));
        when(mascotaClient.listarMascotas(TOKEN)).thenReturn(Optional.of(List.of()));
        when(citaClient.listarCitas(TOKEN)).thenReturn(Optional.of(citas()));

        Map<String, Object> c = (Map<String, Object>) reporteService.reporteCompleto(TOKEN, "ma.calfileo@duoc.cl").get("citas");

        assertEquals(2L, c.get("pendientes"));
        assertEquals(1L, c.get("confirmadas"));
        assertEquals(1L, c.get("canceladas"));
        Map<String, Object> top = ((List<Map<String, Object>>) c.get("porServicio")).get(0);
        assertEquals("Vacunación", top.get("nombre"));
        assertEquals(2L, top.get("cantidad"));
    }

    @Test
    void reporteCompleto_quedaEnElHistorialConServiciosCaidos() {
        when(authClient.listarUsuarios(TOKEN)).thenReturn(Optional.of(usuarios()));
        // Los demás clientes devuelven Optional.empty() (servicios apagados)

        reporteService.reporteCompleto(TOKEN, "ma.calfileo@duoc.cl");

        ArgumentCaptor<ReporteGenerado> captor = ArgumentCaptor.forClass(ReporteGenerado.class);
        verify(reporteRepository).save(captor.capture());
        ReporteGenerado guardado = captor.getValue();
        assertEquals(TipoReporte.COMPLETO, guardado.getTipo());
        assertEquals("ma.calfileo@duoc.cl", guardado.getGeneradoPor());
        assertTrue(guardado.getResumen().startsWith("4 usuarios"));
        assertTrue(guardado.getServiciosNoDisponibles().contains("mascota-service"));
    }

    /* ---------- Exportar ---------- */

    @Test
    @SuppressWarnings("unchecked")
    void exportar_armaLasTresHojas() {
        when(authClient.listarUsuarios(TOKEN)).thenReturn(Optional.of(usuarios()));
        when(mascotaClient.listarMascotas(TOKEN)).thenReturn(Optional.of(mascotas()));
        when(citaClient.listarCitas(TOKEN)).thenReturn(Optional.of(citas()));

        Map<String, Object> e = reporteService.exportar(TOKEN, "ma.calfileo@duoc.cl");
        Map<String, Object> hojas = (Map<String, Object>) e.get("hojas");

        assertEquals(4, ((List<?>) hojas.get("Usuarios")).size());
        assertEquals(3, ((List<?>) hojas.get("Mascotas")).size());
        assertEquals(4, ((List<?>) hojas.get("Citas")).size());
        Map<String, Object> primeraFila = ((List<Map<String, Object>>) hojas.get("Usuarios")).get(0);
        assertEquals("ADMIN", primeraFila.get("Rol"));
        verify(reporteRepository).save(any(ReporteGenerado.class));
    }
}
