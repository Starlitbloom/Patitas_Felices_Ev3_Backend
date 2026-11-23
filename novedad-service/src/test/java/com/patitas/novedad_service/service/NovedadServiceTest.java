
package com.patitas.novedad_service.service;

import com.patitas.novedad_service.model.Novedad;
import com.patitas.novedad_service.repository.NovedadRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NovedadServiceTest {

    @Mock
    private NovedadRepository repository;

    @InjectMocks
    private NovedadService service;

    private Novedad novedad;

    @BeforeEach
    void configurar() {
        novedad = new Novedad();
        novedad.setId(1L);
        novedad.setTitulo("Campaña de vacunación");
        novedad.setDescripcion("Vacunación para mascotas");
        novedad.setImagenUrl("imagen.jpg");
        novedad.setActiva(true);
    }

    // TEST 1: Listar novedades
    @Test
    void listarNovedadesCorrectamente() {
        when(repository.findAll())
                .thenReturn(List.of(novedad));

        List<Novedad> resultado = service.listar();

        assertEquals(1, resultado.size());
        assertEquals("Campaña de vacunación",
                resultado.get(0).getTitulo());

        verify(repository).findAll();
    }

    // TEST 2: Buscar novedad existente
    @Test
    void buscarNovedadPorIdCorrectamente() {
        when(repository.findById(1L))
                .thenReturn(Optional.of(novedad));

        Novedad resultado = service.buscarPorId(1L);

        assertNotNull(resultado);
        assertEquals(1L, resultado.getId());
        assertEquals("Campaña de vacunación",
                resultado.getTitulo());

        verify(repository).findById(1L);
    }

    // TEST 3: Buscar novedad inexistente
    @Test
    void buscarNovedadInexistenteRetorna404() {
        when(repository.findById(99L))
                .thenReturn(Optional.empty());

        ResponseStatusException error = assertThrows(
                ResponseStatusException.class,
                () -> service.buscarPorId(99L)
        );

        assertEquals(HttpStatus.NOT_FOUND,
                error.getStatusCode());
    }

    // TEST 4: Crear novedad
    @Test
    void crearNovedadCorrectamente() {
        when(repository.save(any(Novedad.class)))
                .thenAnswer(invocacion -> {
                    Novedad nueva = invocacion.getArgument(0);
                    nueva.setId(2L);
                    return nueva;
                });

        Novedad resultado = service.crear(novedad);

        assertNotNull(resultado);
        assertEquals(2L, resultado.getId());
        assertEquals("Campaña de vacunación",
                resultado.getTitulo());

        verify(repository).save(novedad);
    }

    // TEST 5: Actualizar novedad
    @Test
    void actualizarNovedadCorrectamente() {
        Novedad datos = new Novedad();
        datos.setTitulo("Nueva campaña");
        datos.setDescripcion("Nueva descripción");
        datos.setImagenUrl("nueva-imagen.jpg");
        datos.setActiva(true);

        when(repository.findById(1L))
                .thenReturn(Optional.of(novedad));

        when(repository.save(any(Novedad.class)))
                .thenAnswer(invocacion ->
                        invocacion.getArgument(0));

        Novedad resultado = service.actualizar(1L, datos);

        assertEquals("Nueva campaña",
                resultado.getTitulo());
        assertEquals("Nueva descripción",
                resultado.getDescripcion());
        assertEquals("nueva-imagen.jpg",
                resultado.getImagenUrl());
        assertEquals(1L, resultado.getId());

        verify(repository).save(novedad);
    }

    // TEST 6: Eliminar novedad
    @Test
    void eliminarNovedadCorrectamente() {
        when(repository.findById(1L))
                .thenReturn(Optional.of(novedad));

        service.eliminar(1L);

        verify(repository).findById(1L);
        verify(repository).delete(novedad);
    }

    // TEST 7: No eliminar novedad inexistente
    @Test
    void eliminarNovedadInexistenteRetorna404() {
        when(repository.findById(99L))
                .thenReturn(Optional.empty());

        ResponseStatusException error = assertThrows(
                ResponseStatusException.class,
                () -> service.eliminar(99L)
        );

        assertEquals(HttpStatus.NOT_FOUND,
                error.getStatusCode());

        verify(repository, never())
                .delete(any(Novedad.class));
    }
}
