
package com.patitas.cita_service.controller;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.patitas.cita_service.model.Cita;
import com.patitas.cita_service.service.CitaService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Pruebas unitarias del controlador REST de citas.
 *
 * Comprueban las respuestas HTTP y la comunicacion
 * del controlador con CitaService.
 *
 * Se utiliza MockMvc para simular solicitudes HTTP
 * y Mockito para simular la logica de negocio.
 *
 * Estas pruebas no modifican registros de MySQL
 * ni sustituyen las pruebas reales de seguridad JWT.
 */
@ExtendWith(MockitoExtension.class)
class CitaControllerTest {

    /**
     * Servicio simulado mediante Mockito.
     */
    @Mock
    private CitaService citaService;

    /**
     * Controlador que se desea probar.
     */
    @InjectMocks
    private CitaController citaController;

    /**
     * Herramienta para simular solicitudes REST.
     */
    private MockMvc mockMvc;

    /**
     * Convierte objetos Java a formato JSON.
     *
     * Registra los modulos necesarios para
     * serializar LocalDate y LocalTime.
     */
    private final ObjectMapper objectMapper =
            new ObjectMapper().findAndRegisterModules();

    /**
     * Prepara el controlador antes de cada test.
     */
    @BeforeEach
    void configurar() {

        mockMvc = MockMvcBuilders
                .standaloneSetup(citaController)
                .build();
    }

    /**
     * Crea una identidad de cliente para simular
     * la informacion que recibe el controlador
     * despues de una autenticacion correcta.
     */
    private Authentication cliente() {

        return new UsernamePasswordAuthenticationToken(
                "cliente@patitas.cl",
                null,
                List.of(
                    new SimpleGrantedAuthority("ROLE_CLIENTE")
                )
        );
    }

    /**
     * Crea una cita de ejemplo para las pruebas.
     *
     * No se registra realmente en MySQL.
     */
    private Cita crearCitaEjemplo() {

        Cita cita = new Cita();

        cita.setId(1L);

        cita.setCorreoUsuario("cliente@patitas.cl");
        cita.setNombreUsuario("Cliente de Prueba");

        cita.setDueno("Cliente de Prueba");
        cita.setPropietario("Cliente de Prueba");
        cita.setCorreo("contacto@patitas.cl");
        cita.setTelefono("912345678");

        cita.setMascota("Luna");
        cita.setTipoMascota("Perro");
        cita.setOtraEspecie(null);
        cita.setEdadMascota(3.0);

        cita.setVeterinarioId(2L);
        cita.setVeterinarioNombre("Camila González");

        cita.setServicio("Consulta general");
        cita.setFecha(LocalDate.of(2027, 1, 15));
        cita.setHora(LocalTime.of(10, 0));
        cita.setMotivo("Control preventivo");
        cita.setDuracionMinutos(30);

        cita.setProfesionalPorConfirmar(false);
        cita.setEstado("Pendiente");

        return cita;
    }

    /**
     * TEST 1:
     * Comprueba que GET /api/v1/citas devuelva
     * HTTP 200 con las citas proporcionadas
     * por el servicio.
     */
    @Test
    void listarTodasRetorna200() throws Exception {

        Cita cita = crearCitaEjemplo();

        when(citaService.listarTodas())
                .thenReturn(List.of(cita));

        mockMvc.perform(get("/api/v1/citas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].mascota")
                        .value("Luna"));

        verify(citaService).listarTodas();
    }

    /**
     * TEST 2:
     * Comprueba que el controlador obtenga
     * el correo de la identidad autenticada
     * al consultar las citas de un cliente.
     */
    @Test
    void listarMisCitasRetorna200() throws Exception {

        Cita cita = crearCitaEjemplo();

        when(citaService.listarMisCitas(
                "cliente@patitas.cl"))
                .thenReturn(List.of(cita));

        mockMvc.perform(
                get("/api/v1/citas/mis-citas")
                        .principal(cliente())
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].mascota")
                        .value("Luna"));

        verify(citaService)
                .listarMisCitas("cliente@patitas.cl");
    }

    /**
     * TEST 3:
     * Comprueba que GET por ID devuelva
     * HTTP 200 cuando la cita existe.
     */
    @Test
    void obtenerCitaRetorna200() throws Exception {

        Cita cita = crearCitaEjemplo();

        when(citaService.obtenerCita(
                1L,
                "cliente@patitas.cl",
                false))
                .thenReturn(cita);

        mockMvc.perform(
                get("/api/v1/citas/1")
                        .principal(cliente())
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.mascota")
                        .value("Luna"));
    }

    /**
     * TEST 4:
     * Comprueba que una cita inexistente
     * genere una respuesta HTTP 404.
     */
    @Test
    void obtenerCitaInexistenteRetorna404()
            throws Exception {

        when(citaService.obtenerCita(
                99L,
                "cliente@patitas.cl",
                false))
                .thenThrow(new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Cita no encontrada"
                ));

        mockMvc.perform(
                get("/api/v1/citas/99")
                        .principal(cliente())
        )
                .andExpect(status().isNotFound());
    }

    /**
     * TEST 5:
     * Comprueba que POST /api/v1/citas devuelva
     * HTTP 201 al registrar una reserva.
     *
     * El servicio es simulado con Mockito.
     */
    @Test
    void crearCitaRetorna201() throws Exception {

        Cita cita = crearCitaEjemplo();

        when(citaService.crearCita(
                any(Cita.class),
                eq("cliente@patitas.cl"),
                eq(null)))
                .thenReturn(cita);

        mockMvc.perform(
                post("/api/v1/citas")
                        .principal(cliente())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                            objectMapper.writeValueAsString(cita)
                        )
        )
                .andExpect(status().isCreated())
                .andExpect(
                    header().string(
                        "Location",
                        "/api/v1/citas/1"
                    )
                )
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.mascota")
                        .value("Luna"));

        verify(citaService).crearCita(
                any(Cita.class),
                eq("cliente@patitas.cl"),
                eq(null)
        );
    }

    /**
     * TEST 6:
     * Comprueba que PUT /api/v1/citas/{id}
     * devuelva HTTP 200 al actualizar
     * una reserva existente.
     */
    @Test
    void actualizarCitaRetorna200() throws Exception {

        Cita cita = crearCitaEjemplo();
        cita.setMotivo("Nueva consulta preventiva");

        when(citaService.actualizarCita(
                eq(1L),
                any(Cita.class),
                eq("cliente@patitas.cl"),
                eq(false)))
                .thenReturn(cita);

        mockMvc.perform(
                put("/api/v1/citas/1")
                        .principal(cliente())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                            objectMapper.writeValueAsString(cita)
                        )
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.motivo")
                        .value("Nueva consulta preventiva"));

        verify(citaService).actualizarCita(
                eq(1L),
                any(Cita.class),
                eq("cliente@patitas.cl"),
                eq(false)
        );
    }

    /**
     * TEST 7:
     * Comprueba que PATCH cancelar devuelva
     * HTTP 200 con la cita cancelada.
     */
    @Test
    void cancelarCitaRetorna200() throws Exception {

        Cita cita = crearCitaEjemplo();
        cita.setEstado("Cancelada");

        when(citaService.cancelarCita(
                1L,
                "cliente@patitas.cl",
                false))
                .thenReturn(cita);

        mockMvc.perform(
                patch("/api/v1/citas/1/cancelar")
                        .principal(cliente())
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado")
                        .value("Cancelada"));

        verify(citaService).cancelarCita(
                1L,
                "cliente@patitas.cl",
                false
        );
    }

    /**
     * TEST 8:
     * Comprueba que el endpoint administrativo
     * de cambio de estado devuelva HTTP 200.
     *
     * La comprobacion real del rol ADMIN
     * corresponde a las pruebas de seguridad.
     */
    @Test
    void cambiarEstadoRetorna200() throws Exception {

        Cita cita = crearCitaEjemplo();
        cita.setEstado("Confirmada");

        when(citaService.cambiarEstado(
                1L,
                "Confirmada"))
                .thenReturn(cita);

        mockMvc.perform(
                patch("/api/v1/citas/1/estado")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"estado\":\"Confirmada\"}")
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado")
                        .value("Confirmada"));

        verify(citaService)
                .cambiarEstado(1L, "Confirmada");
    }

    /**
     * TEST 9:
     * Comprueba que DELETE /api/v1/citas/{id}
     * devuelva HTTP 204 cuando el servicio
     * elimina correctamente la cita.
     */
    @Test
    void eliminarCitaRetorna204() throws Exception {

        mockMvc.perform(
                delete("/api/v1/citas/1")
        )
                .andExpect(status().isNoContent());

        verify(citaService).eliminarCita(1L);
    }
}

