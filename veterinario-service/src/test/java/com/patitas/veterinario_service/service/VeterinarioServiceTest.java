
package com.patitas.veterinario_service.service;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.patitas.veterinario_service.model.Veterinario;
import com.patitas.veterinario_service.repository.VeterinarioRepository;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias del servicio de veterinarios.
 *
 * Verifican la logica de negocio del CRUD:
 *
 * - Listar veterinarios.
 * - Buscar veterinarios por ID.
 * - Registrar veterinarios.
 * - Validar correos duplicados.
 * - Actualizar veterinarios.
 * - Eliminar veterinarios.
 *
 * Mockito simula el repositorio para comprobar
 * cada operacion sin modificar los datos reales.
 *
 * La conexion con MySQL se verifica mediante
 * VeterinarioServiceApplicationTests.
 */
@ExtendWith(MockitoExtension.class)
class VeterinarioServiceTest {

    /**
     * Repositorio simulado mediante Mockito.
     */
    @Mock
    private VeterinarioRepository veterinarioRepository;

    /**
     * Servicio que contiene la logica a probar.
     *
     * Mockito inyecta automaticamente
     * el repositorio simulado.
     */
    @InjectMocks
    private VeterinarioService veterinarioService;

    /**
     * Genera un veterinario de ejemplo
     * para reutilizarlo en las pruebas.
     *
     * @return Objeto Veterinario con datos validos.
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
     * TEST 1: Comprueba que se puedan
     * listar los veterinarios registrados.
     */
    @Test
    void listarVeterinariosRetornaLista() {

        Veterinario veterinario = crearVeterinarioEjemplo();

        when(veterinarioRepository.findAll())
                .thenReturn(List.of(veterinario));

        List<Veterinario> resultado =
                veterinarioService.listarVeterinarios();

        assertEquals(1, resultado.size());
        assertEquals("Camila González",
                resultado.get(0).getNombre());

        verify(veterinarioRepository).findAll();
    }

    /**
     * TEST 2: Comprueba que se pueda
     * buscar un veterinario por su ID.
     */
    @Test
    void obtenerVeterinarioPorIdExistente() {

        Veterinario veterinario = crearVeterinarioEjemplo();

        when(veterinarioRepository.findById(1L))
                .thenReturn(Optional.of(veterinario));

        Veterinario resultado =
                veterinarioService.obtenerVeterinarioPorId(1L);

        assertEquals(1L, resultado.getId());
        assertEquals("Camila González", resultado.getNombre());

        verify(veterinarioRepository).findById(1L);
    }

    /**
     * TEST 3: Verifica que un ID inexistente
     * genere una respuesta HTTP 404.
     */
    @Test
    void obtenerVeterinarioInexistenteRetorna404() {

        when(veterinarioRepository.findById(99L))
                .thenReturn(Optional.empty());

        ResponseStatusException error = assertThrows(
                ResponseStatusException.class,
                () -> veterinarioService
                        .obtenerVeterinarioPorId(99L)
        );

        assertEquals(HttpStatus.NOT_FOUND,
                error.getStatusCode());
    }

    /**
     * TEST 4: Comprueba que se pueda
     * registrar un veterinario correctamente.
     */
    @Test
    void crearVeterinarioCorrectamente() {

        Veterinario veterinario = crearVeterinarioEjemplo();

        when(veterinarioRepository.existsByCorreoIgnoreCase(
                veterinario.getCorreo()))
                .thenReturn(false);

        when(veterinarioRepository.save(any(Veterinario.class)))
                .thenAnswer(invocacion -> {

                    Veterinario guardado =
                            invocacion.getArgument(0);

                    // Simular el ID generado por MySQL.
                    guardado.setId(1L);

                    return guardado;
                });

        Veterinario resultado =
                veterinarioService.crearVeterinario(veterinario);

        assertNotNull(resultado);
        assertEquals(1L, resultado.getId());
        assertEquals("Camila González", resultado.getNombre());

        verify(veterinarioRepository).save(veterinario);
    }

    /**
     * TEST 5: Verifica que no se pueda registrar
     * otro veterinario con un correo existente.
     */
    @Test
    void crearVeterinarioConCorreoDuplicadoRetorna409() {

        Veterinario veterinario = crearVeterinarioEjemplo();

        when(veterinarioRepository.existsByCorreoIgnoreCase(
                veterinario.getCorreo()))
                .thenReturn(true);

        ResponseStatusException error = assertThrows(
                ResponseStatusException.class,
                () -> veterinarioService
                        .crearVeterinario(veterinario)
        );

        assertEquals(HttpStatus.CONFLICT,
                error.getStatusCode());

        verify(veterinarioRepository, never())
                .save(any(Veterinario.class));
    }

    /**
     * TEST 6: Comprueba que se puedan
     * actualizar los datos de un veterinario.
     */
    @Test
    void actualizarVeterinarioCorrectamente() {

        Veterinario existente = crearVeterinarioEjemplo();
        Veterinario datos = crearVeterinarioEjemplo();

        // Cambiar los datos profesionales.
        datos.setArea("Dermatología veterinaria");
        datos.setExperiencia(6);

        when(veterinarioRepository.findById(1L))
                .thenReturn(Optional.of(existente));

        when(veterinarioRepository.findByCorreoIgnoreCase(
                datos.getCorreo()))
                .thenReturn(Optional.of(existente));

        when(veterinarioRepository.save(any(Veterinario.class)))
                .thenAnswer(invocacion ->
                        invocacion.getArgument(0)
                );

        Veterinario resultado =
                veterinarioService.actualizarVeterinario(1L, datos);

        assertEquals("Dermatología veterinaria",
                resultado.getArea());

        assertEquals(6, resultado.getExperiencia());

        verify(veterinarioRepository)
                .save(existente);
    }

    /**
     * TEST 7: Verifica que no se pueda actualizar
     * un veterinario utilizando el correo
     * registrado por otro profesional.
     */
    @Test
    void actualizarConCorreoDeOtroVeterinarioRetorna409() {

        Veterinario existente = crearVeterinarioEjemplo();
        Veterinario datos = crearVeterinarioEjemplo();

        datos.setCorreo("otro@patitasfelices.cl");

        Veterinario otroVeterinario =
                crearVeterinarioEjemplo();

        otroVeterinario.setId(2L);
        otroVeterinario.setCorreo("otro@patitasfelices.cl");

        when(veterinarioRepository.findById(1L))
                .thenReturn(Optional.of(existente));

        when(veterinarioRepository.findByCorreoIgnoreCase(
                "otro@patitasfelices.cl"))
                .thenReturn(Optional.of(otroVeterinario));

        ResponseStatusException error = assertThrows(
                ResponseStatusException.class,
                () -> veterinarioService
                        .actualizarVeterinario(1L, datos)
        );

        assertEquals(HttpStatus.CONFLICT,
                error.getStatusCode());

        verify(veterinarioRepository, never())
                .save(any(Veterinario.class));
    }

    /**
     * TEST 8: Comprueba que se pueda
     * eliminar un veterinario existente.
     */
    @Test
    void eliminarVeterinarioCorrectamente() {

        Veterinario veterinario = crearVeterinarioEjemplo();

        when(veterinarioRepository.findById(1L))
                .thenReturn(Optional.of(veterinario));

        veterinarioService.eliminarVeterinario(1L);

        verify(veterinarioRepository)
                .delete(veterinario);
    }

    /**
     * TEST 9: Verifica que eliminar un veterinario
     * inexistente genere una respuesta HTTP 404.
     */
    @Test
    void eliminarVeterinarioInexistenteRetorna404() {

        when(veterinarioRepository.findById(99L))
                .thenReturn(Optional.empty());

        ResponseStatusException error = assertThrows(
                ResponseStatusException.class,
                () -> veterinarioService
                        .eliminarVeterinario(99L)
        );

        assertEquals(HttpStatus.NOT_FOUND,
                error.getStatusCode());

        verify(veterinarioRepository, never())
                .delete(any(Veterinario.class));
    }
}
