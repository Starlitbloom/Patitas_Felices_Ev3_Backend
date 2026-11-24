// Carpeta (paquete) del test: la misma que la del service que se prueba
package com.patitas.servicio_clinico_service.service;

// Comprobaciones de JUnit
import static org.junit.jupiter.api.Assertions.assertEquals;   // ¿Son iguales?
import static org.junit.jupiter.api.Assertions.assertFalse;    // ¿Es falso?
import static org.junit.jupiter.api.Assertions.assertThrows;   // ¿Lanza este error?
import static org.junit.jupiter.api.Assertions.assertTrue;     // ¿Es verdadero?
// Herramientas de Mockito
import static org.mockito.ArgumentMatchers.any;                // "Cualquier objeto de este tipo"
import static org.mockito.Mockito.never;                       // "Nunca"
import static org.mockito.Mockito.verify;                      // ¿Se llamó este método?
import static org.mockito.Mockito.when;                        // "Cuando se llame X, responde Y"

// Tipo "lista"
import java.util.List;
// Caja que puede tener un valor o estar vacía
import java.util.Optional;

// @Test: marca un método como prueba
import org.junit.jupiter.api.Test;
// Conecta JUnit con Mockito
import org.junit.jupiter.api.extension.ExtendWith;
// @InjectMocks: crea el service real con los mocks dentro
import org.mockito.InjectMocks;
// @Mock: crea un objeto falso (no toca la base de datos)
import org.mockito.Mock;
// Extensión de Mockito para JUnit 5
import org.mockito.junit.jupiter.MockitoExtension;

// Clases del proyecto que se usan en las pruebas
import com.patitas.servicio_clinico_service.dto.ServicioRequest;
import com.patitas.servicio_clinico_service.exception.ConflictoException;
import com.patitas.servicio_clinico_service.exception.RecursoNoEncontradoException;
import com.patitas.servicio_clinico_service.model.ServicioClinico;
import com.patitas.servicio_clinico_service.repository.ServicioClinicoRepository;

// Pruebas unitarias del catálogo: el repositorio es falso (Mockito), no se necesita MySQL
@ExtendWith(MockitoExtension.class)
class ServicioClinicoServiceTest {

    @Mock ServicioClinicoRepository repo;      // Repositorio falso
    @InjectMocks ServicioClinicoService servicio; // Service real que usa el repositorio falso

    // Ayudante: crea un servicio de ejemplo en memoria
    private ServicioClinico nuevo(String grupo, String nombre, String... especies) {
        ServicioClinico s = new ServicioClinico();
        s.setGrupo(grupo);
        s.setNombre(nombre);
        s.getEspecies().addAll(List.of(especies));
        return s;
    }

    // Prueba 1: el filtro por especie funciona y "sin especies" significa "para todas"
    @Test
    void listarFiltraPorEspecieYTrataSinEspecieComoParaTodas() {
        ServicioClinico canina = nuevo("Cirugia", "Esterilizacion hembra canina", "Perro");
        ServicioClinico felina = nuevo("Cirugia", "Esterilizacion hembra felina", "Gato");
        ServicioClinico hemograma = nuevo("Examenes", "Hemograma completo");   // Sin especies = todas
        when(repo.findByActivoTrueOrderByGrupoAscNombreAsc()).thenReturn(List.of(canina, felina, hemograma));

        List<ServicioClinico> paraGato = servicio.listar("Gato", null);
        assertEquals(2, paraGato.size());                                    // Felina + hemograma
        assertTrue(paraGato.contains(felina) && paraGato.contains(hemograma));
        assertEquals(1, servicio.listar(null, "examenes").size());          // Filtro por grupo sin importar mayúsculas
    }

    // Prueba 2: no se puede crear un servicio con un nombre que ya existe
    @Test
    void crearServicioDuplicadoLanzaConflicto() {
        when(repo.existsByNombreIgnoreCase("Radiografia")).thenReturn(true);   // El nombre ya existe
        ServicioRequest req = new ServicioRequest("Examenes", "Radiografia", null, null);
        assertThrows(ConflictoException.class, () -> servicio.crear(req));    // Lanza 409
        verify(repo, never()).save(any(ServicioClinico.class));                // Y nunca se guardó
    }

    // Prueba 3: un servicio nuevo queda activo por defecto y con los textos limpios
    @Test
    void crearServicioNuevoQuedaActivoPorDefecto() {
        when(repo.existsByNombreIgnoreCase("Radiografia")).thenReturn(false);
        when(repo.save(any(ServicioClinico.class))).thenAnswer(inv -> inv.getArgument(0));
        ServicioClinico s = servicio.crear(new ServicioRequest(" Examenes ", " Radiografia ", List.of("Perro", "Gato"), null));
        assertEquals("Radiografia", s.getNombre());              // Sin espacios sobrantes
        assertTrue(s.isActivo());                                // Activo por defecto
        assertEquals(List.of("Perro", "Gato"), s.getEspecies()); // Especies copiadas
    }

    // Prueba 4: una especie que la clínica no atiende se rechaza
    @Test
    void especieInvalidaSeRechaza() {
        when(repo.existsByNombreIgnoreCase("Rareza")).thenReturn(false);
        ServicioRequest req = new ServicioRequest("Otros", "Rareza", List.of("Dragon"), null);
        assertThrows(ConflictoException.class, () -> servicio.crear(req));
    }

    // Prueba 5: pedir un servicio que no existe lanza 404
    @Test
    void obtenerInexistenteLanza404() {
        when(repo.findById(50L)).thenReturn(Optional.empty());
        assertThrows(RecursoNoEncontradoException.class, () -> servicio.obtener(50L));
    }

    // Prueba 6: actualizar sin cambiar el nombre no choca consigo mismo, y se puede desactivar
    @Test
    void actualizarSinCambiarNombreNoChocaConsigoMismo() {
        ServicioClinico existente = nuevo("Otros", "Corte de unas");
        when(repo.findById(1L)).thenReturn(Optional.of(existente));
        when(repo.save(any(ServicioClinico.class))).thenAnswer(inv -> inv.getArgument(0));
        ServicioClinico s = servicio.actualizar(1L, new ServicioRequest("Otros", "Corte de unas", null, false));
        assertFalse(s.isActivo());  // Quedó desactivado
    }

    // Prueba 7: eliminar un servicio existente llama a delete del repositorio
    @Test
    void eliminarServicioExistenteLoBorra() {
        ServicioClinico existente = nuevo("Otros", "Hospitalizacion");
        when(repo.findById(2L)).thenReturn(Optional.of(existente));
        servicio.eliminar(2L);
        verify(repo).delete(existente);  // Comprueba que sí se borró
    }
}
