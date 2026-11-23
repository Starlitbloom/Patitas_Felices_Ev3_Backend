
package com.patitas.novedad_service.controller;

import com.patitas.novedad_service.model.Novedad;
import com.patitas.novedad_service.service.NovedadService;

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

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class NovedadControllerTest {

    @Mock
    private NovedadService service;

    @InjectMocks
    private NovedadController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void configurar() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(controller)
                .build();
    }

    private Novedad crearNovedad() {
        Novedad novedad = new Novedad();
        novedad.setId(1L);
        novedad.setTitulo("Campaña de vacunación");
        novedad.setDescripcion("Vacunación de mascotas");
        novedad.setActiva(true);
        return novedad;
    }

    @Test
    void listarNovedadesRetorna200() throws Exception {
        when(service.listar())
                .thenReturn(List.of(crearNovedad()));

        mockMvc.perform(get("/api/novedades"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].titulo")
                        .value("Campaña de vacunación"));
    }

    @Test
    void buscarNovedadRetorna200() throws Exception {
        when(service.buscarPorId(1L))
                .thenReturn(crearNovedad());

        mockMvc.perform(get("/api/novedades/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void buscarNovedadInexistenteRetorna404()
            throws Exception {

        when(service.buscarPorId(99L))
                .thenThrow(new ResponseStatusException(
                        HttpStatus.NOT_FOUND));

        mockMvc.perform(get("/api/novedades/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    void crearNovedadRetorna201() throws Exception {
        when(service.crear(any(Novedad.class)))
                .thenAnswer(invocacion -> {
                    Novedad nueva = invocacion.getArgument(0);
                    nueva.setId(2L);
                    return nueva;
                });

        String json = """
            {
                "titulo": "Nueva noticia",
                "descripcion": "Nueva campaña veterinaria",
                "activa": true
            }
            """;

        mockMvc.perform(post("/api/novedades")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(2));
    }

    @Test
    void actualizarNovedadRetorna200()
            throws Exception {

        when(service.actualizar(eq(1L), any(Novedad.class)))
                .thenAnswer(invocacion -> {
                    Novedad datos = invocacion.getArgument(1);
                    datos.setId(1L);
                    return datos;
                });

        String json = """
            {
                "titulo": "Noticia actualizada",
                "descripcion": "Información actualizada",
                "activa": true
            }
            """;

        mockMvc.perform(put("/api/novedades/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.titulo")
                        .value("Noticia actualizada"));
    }

    @Test
    void eliminarNovedadRetorna204() throws Exception {
        mockMvc.perform(delete("/api/novedades/1"))
                .andExpect(status().isNoContent());

        verify(service).eliminar(1L);
    }
}
