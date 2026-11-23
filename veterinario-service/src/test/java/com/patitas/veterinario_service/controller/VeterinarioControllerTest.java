
package com.patitas.veterinario_service.controller;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.databind.ObjectMapper;

import com.patitas.veterinario_service.model.Veterinario;
import com.patitas.veterinario_service.service.VeterinarioService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Pruebas unitarias del controlador REST de veterinarios.
 *
 * Verifican las respuestas HTTP de los endpoints:
 *
 * GET    /api/veterinarios
 * GET    /api/veterinarios/{id}
 * POST   /api/veterinarios
 * PUT    /api/veterinarios/{id}
 * DELETE /api/veterinarios/{id}
 *
 * MockMvc simula solicitudes HTTP sin levantar
 * el servidor web.
 *
 * Mockito simula VeterinarioService para evitar
 * modificar los registros reales de MySQL.
 *
 * Estas pruebas verifican el controlador,
 * no la autenticacion JWT.
 */
@ExtendWith(MockitoExtension.class)
class VeterinarioControllerTest {

    /**
     * Servicio simulado mediante Mockito.
     */
    @Mock
    private VeterinarioService veterinarioService;

    /**
     * Controlador al que se inyecta el servicio simulado.
     */
    @InjectMocks
    private VeterinarioController veterinarioController;

    /**
     * Permite simular solicitudes HTTP.
     */
    private MockMvc mockMvc;

    /**
     * Convierte objetos Java a JSON.
     */
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Prepara MockMvc antes de cada prueba.
     */
    @BeforeEach
    void configurar() {

        mockMvc = MockMvcBuilders
                .standaloneSetup(veterinarioController)
                .build();
    }

    /**
     * Crea un veterinario de ejemplo
     * con datos validos.
     *
     * @return Veterinario utilizado en las pruebas.
     */
    private Veterinario crearVeterinarioEjemplo() {

        Veterinario veterinario = new Veterinario();

        veterinario.setId(1L);
        veterinario.setNombre("Camila González");
        veterinario.setCargo("Médico Veterinario");
        veterinario.setTitulo("Medicina Veterinaria");
        veterinario.setArea("Medicina general");

        veterinario.setEspecies(
                List.of("Perros", "Gatos")
        );

        veterinario.setExperiencia(5);

        veterinario.setDescripcion(
                "Especialista en medicina preventiva"
        );

        veterinario.setDias(
                List.of("Lunes", "Martes", "Miércoles")
        );

        veterinario.setHorario("09:00 - 18:00");
        veterinario.setImagen("/images/veterinarios/camila.jpg");

        veterinario.setCorreo(
                "camila.gonzalez@patitasfelices.cl"
        );

        return veterinario;
    }

    /**
     * TEST 1:
     * Verifica que GET retorne HTTP 200
     * y la lista de veterinarios.
     */
    @Test
    void listarVeterinariosRetorna200() throws Exception {

        Veterinario veterinario = crearVeterinarioEjemplo();

        when(veterinarioService.listarVeterinarios())
                .thenReturn(List.of(veterinario));

        mockMvc.perform(get("/api/veterinarios"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].nombre")
                        .value("Camila González"));

        verify(veterinarioService).listarVeterinarios();
    }

    /**
     * TEST 2:
     * Verifica que GET por ID retorne HTTP 200
     * cuando el veterinario existe.
     */
    @Test
    void obtenerVeterinarioPorIdRetorna200() throws Exception {

        Veterinario veterinario = crearVeterinarioEjemplo();

        when(veterinarioService.obtenerVeterinarioPorId(1L))
                .thenReturn(veterinario);

        mockMvc.perform(get("/api/veterinarios/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.nombre")
                        .value("Camila González"));

        verify(veterinarioService)
                .obtenerVeterinarioPorId(1L);
    }

    /**
     * TEST 3:
     * Verifica que GET por ID retorne HTTP 404
     * cuando el veterinario no existe.
     */
    @Test
    void obtenerVeterinarioInexistenteRetorna404()
            throws Exception {

        when(veterinarioService.obtenerVeterinarioPorId(99L))
                .thenThrow(new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Veterinario no encontrado"
                ));

        mockMvc.perform(get("/api/veterinarios/99"))
                .andExpect(status().isNotFound());
    }

    /**
     * TEST 4:
     * Verifica que POST retorne HTTP 201
     * al registrar un veterinario.
     */
    @Test
    void crearVeterinarioRetorna201() throws Exception {

        Veterinario veterinario = crearVeterinarioEjemplo();

        when(veterinarioService.crearVeterinario(
                any(Veterinario.class)))
                .thenReturn(veterinario);

        mockMvc.perform(post("/api/veterinarios")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper
                        .writeValueAsString(veterinario)))

                .andExpect(status().isCreated())
                .andExpect(header().string(
                        "Location",
                        "/api/veterinarios/1"
                ))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.nombre")
                        .value("Camila González"));

        verify(veterinarioService)
                .crearVeterinario(any(Veterinario.class));
    }

    /**
     * TEST 5:
     * Verifica que PUT retorne HTTP 200
     * al actualizar los datos de un veterinario.
     */
    @Test
    void actualizarVeterinarioRetorna200() throws Exception {

        Veterinario veterinario = crearVeterinarioEjemplo();

        veterinario.setArea("Dermatología veterinaria");
        veterinario.setExperiencia(6);

        when(veterinarioService.actualizarVeterinario(
                eq(1L),
                any(Veterinario.class)))
                .thenReturn(veterinario);

        mockMvc.perform(put("/api/veterinarios/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper
                        .writeValueAsString(veterinario)))

                .andExpect(status().isOk())
                .andExpect(jsonPath("$.area")
                        .value("Dermatología veterinaria"))
                .andExpect(jsonPath("$.experiencia")
                        .value(6));

        verify(veterinarioService)
                .actualizarVeterinario(
                        eq(1L),
                        any(Veterinario.class)
                );
    }

    /**
     * TEST 6:
     * Verifica que DELETE retorne HTTP 204
     * cuando se elimina un veterinario.
     */
    @Test
    void eliminarVeterinarioRetorna204() throws Exception {

        doNothing().when(veterinarioService)
                .eliminarVeterinario(1L);

        mockMvc.perform(delete("/api/veterinarios/1"))
                .andExpect(status().isNoContent());

        verify(veterinarioService)
                .eliminarVeterinario(1L);
    }
}
