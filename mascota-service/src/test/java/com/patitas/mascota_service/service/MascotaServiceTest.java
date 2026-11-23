// Carpeta (paquete) donde vive este archivo de pruebas
package com.patitas.mascota_service.service;
// ---- Imports de comprobaciones (assert): son las "preguntas" que hace cada prueba ----
// assertEquals(esperado, real): comprueba que dos valores sean iguales
import static org.junit.jupiter.api.Assertions.assertEquals;
// assertFalse(x): comprueba que x sea false
import static org.junit.jupiter.api.Assertions.assertFalse;
// assertThrows(Error.class, ...): comprueba que el código lance ese error
import static org.junit.jupiter.api.Assertions.assertThrows;
// assertTrue(x): comprueba que x sea true
import static org.junit.jupiter.api.Assertions.assertTrue;
// ---- Imports de Mockito: sirve para SIMULAR los repositorios (sin base de datos) ----
// any(): "cualquier valor de este tipo", para no tener que indicar uno exacto
import static org.mockito.ArgumentMatchers.any;
// never(): "esta llamada no debe ocurrir nunca"
import static org.mockito.Mockito.never;
// verify(): comprueba que se hizo (o no) cierta llamada al repositorio simulado
import static org.mockito.Mockito.verify;
// when(...).thenReturn(...): define "cuando te llamen así, responde esto"
import static org.mockito.Mockito.when;

// Herramientas de Java para trabajar con listas y valores opcionales
import java.util.List;
import java.util.Optional;

// @Test: marca un método como prueba
import org.junit.jupiter.api.Test;
// Permite conectar JUnit con Mockito
import org.junit.jupiter.api.extension.ExtendWith;
// @InjectMocks: crea el objeto real a probar e inyecta los simulados
import org.mockito.InjectMocks;
// @Mock: crea un objeto simulado (falso)
import org.mockito.Mock;
// Activa las anotaciones de Mockito en JUnit
import org.mockito.junit.jupiter.MockitoExtension;
// Error 403 de Spring Security (acceso denegado)
import org.springframework.security.access.AccessDeniedException;

// Clases de tu proyecto que se usan en las pruebas
import com.patitas.mascota_service.config.UsuarioActual;
import com.patitas.mascota_service.dto.MascotaRequest;
import com.patitas.mascota_service.dto.SolicitudRequest;
import com.patitas.mascota_service.model.Mascota;
import com.patitas.mascota_service.model.SolicitudCambio;
import com.patitas.mascota_service.exception.ConflictoException;
import com.patitas.mascota_service.exception.RecursoNoEncontradoException;
import com.patitas.mascota_service.repository.MascotaRepository;
import com.patitas.mascota_service.repository.SolicitudCambioRepository;
import com.patitas.mascota_service.service.MascotaService;

/** Pruebas unitarias de la logica de negocio (sin base de datos: los repositorios son simulados con Mockito). */
// Activa Mockito en esta clase de pruebas
@ExtendWith(MockitoExtension.class)
// Clase de pruebas: cada método con @Test es una prueba independiente
class MascotaServiceTest {

    // Repositorio de mascotas SIMULADO: no toca MySQL, responde lo que tú le indiques
    @Mock MascotaRepository mascotas;
    // Repositorio de solicitudes SIMULADO
    @Mock SolicitudCambioRepository solicitudes;
    // El MascotaService REAL que se prueba, con los dos repositorios simulados puestos adentro
    @InjectMocks MascotaService servicio;

    // Usuario de prueba "cliente" (María): staff = false, o sea, no es personal de la clínica
    private final UsuarioActual maria = new UsuarioActual("ma.calfileo@duoc.cl", false);
    // Usuario de prueba "personal": staff = true (admin o recepcionista)
    private final UsuarioActual admin = new UsuarioActual("admin@duoc.cl", true);

    // Método ayudante: fabrica una mascota de ejemplo (Rocky). Parámetro: si debe tener la ficha completa
    private Mascota rocky(boolean completo) {
        // Crea una mascota vacía
        Mascota m = new Mascota();
        // Le pone id 1
        m.setId(1L);
        // Nombre Rocky
        m.setNombre("Rocky");
        // Especie Perro
        m.setEspecie("Perro");
        // Nombre del dueño
        m.setNombreDueno("Maria Calfileo");
        // Correo del dueño (el mismo que el de "maria")
        m.setCorreoDueno("ma.calfileo@duoc.cl");
        // Estado de salud al día
        m.setEstado("al-dia");
        // Si se pidió completa, le agrega raza, sexo, peso e imagen
        if (completo) { m.setRaza("Golden"); m.setSexo("Macho"); m.setPeso("28 kg"); m.setImagen("/img/golden.jpg"); }
        // Calcula si queda completa o incompleta según esos datos
        m.recalcularCompleto();
        // Devuelve la mascota armada
        return m;
    }

    // Método ayudante: fabrica el JSON de entrada para crear una mascota. Recibe el correo del dueño
    private MascotaRequest request(String correo) {
        // Crea un MascotaRequest completo (Toby) con el correo recibido; el estado va null para probar el valor por defecto
        return new MascotaRequest("Toby", "Perro", "Poodle", "3 anos", "Macho", "6 kg", "Ana", correo, "/img/toby.jpg", null, "Simpatico");
    }

    // PRUEBA 1: un cliente solo ve sus mascotas
    @Test
    void clienteSoloVeSusMascotas() {
        // Simula: cuando pidan las mascotas de ese correo, el repositorio devuelve una lista con Rocky
        when(mascotas.findByCorreoDuenoIgnoreCase("ma.calfileo@duoc.cl")).thenReturn(List.of(rocky(true)));
        // Ejecuta: María pide su lista de mascotas, sin filtros
        List<Mascota> lista = servicio.listar(maria, null, null);
        // Comprueba: recibió exactamente 1 mascota
        assertEquals(1, lista.size());
        // Comprueba: nunca se llamó a findAll (que trae las de todos), o sea, no vio las ajenas
        verify(mascotas, never()).findAll();
    }

    // PRUEBA 2: el personal ve todas y puede filtrar por especie y por nombre
    @Test
    void personalVeTodasYFiltraPorEspecie() {
        // Crea una segunda mascota a partir de Rocky
        Mascota gato = rocky(true);
        // Le cambia el nombre a Luna
        gato.setNombre("Luna");
        // Y la especie a Gato
        gato.setEspecie("Gato");
        // Simula: el repositorio devuelve las dos mascotas (Rocky y Luna)
        when(mascotas.findAll()).thenReturn(List.of(rocky(true), gato));
        // Comprueba: filtrando por especie "Gato" queda solo 1
        assertEquals(1, servicio.listar(admin, "Gato", null).size());
        // Comprueba: buscando "lun" en el nombre aparece Luna
        assertEquals("Luna", servicio.listar(admin, null, "lun").get(0).getNombre());
    }

    // PRUEBA 3: un cliente no puede ver la mascota de otro dueño
    @Test
    void obtenerMascotaDeOtroDuenoLanzaAccesoDenegado() {
        // Crea una mascota de ejemplo
        Mascota ajena = rocky(true);
        // Le cambia el dueño a otra persona
        ajena.setCorreoDueno("otro@duoc.cl");
        // Simula: al buscar la mascota 1, el repositorio la devuelve
        when(mascotas.findById(1L)).thenReturn(Optional.of(ajena));
        // Comprueba: María pidiendo esa mascota recibe error 403 (AccessDeniedException)
        assertThrows(AccessDeniedException.class, () -> servicio.obtener(1L, maria));
    }

    // PRUEBA 4: buscar una mascota que no existe da error 404
    @Test
    void obtenerMascotaInexistenteLanza404() {
        // Simula: al buscar la mascota 99, el repositorio no encuentra nada (Optional vacío)
        when(mascotas.findById(99L)).thenReturn(Optional.empty());
        // Comprueba: se lanza RecursoNoEncontradoException (que el handler convierte en 404)
        assertThrows(RecursoNoEncontradoException.class, () -> servicio.obtener(99L, admin));
    }

    // PRUEBA 5: un cliente crea una mascota y el sistema fuerza su propio correo
    @Test
    void clienteCreaMascotaYSeFuerzaSuCorreo() {
        // Simula: al guardar, el repositorio devuelve la misma mascota que recibió
        when(mascotas.save(any(Mascota.class))).thenAnswer(inv -> inv.getArgument(0));
        // Ejecuta: María crea una mascota intentando poner como dueño a "otro@duoc.cl"
        Mascota creada = servicio.crear(request("otro@duoc.cl"), maria);
        // Comprueba: el dueño quedó con el correo de María, no el del JSON
        assertEquals("ma.calfileo@duoc.cl", creada.getCorreoDueno());   // no puede crear a nombre de otro
        // Comprueba: como no se envió estado, quedó "al-dia"
        assertEquals("al-dia", creada.getEstado());                      // estado por defecto
        // Comprueba: quedó marcada como completa
        assertTrue(creada.isCompleto());                                 // trae raza, sexo, peso e imagen
    }

    // PRUEBA 6: una mascota sin raza ni otros datos queda incompleta
    @Test
    void mascotaSinRazaQuedaIncompleta() {
        // Simula: al guardar, devuelve la misma mascota recibida
        when(mascotas.save(any(Mascota.class))).thenAnswer(inv -> inv.getArgument(0));
        // Arma un JSON con raza, edad, sexo, peso, imagen y descripción en null
        MascotaRequest sinRaza = new MascotaRequest("Toby", "Perro", null, null, null, null, "Ana", "ana@duoc.cl", null, null, null);
        // Comprueba: el personal la crea y queda con completo = false
        assertFalse(servicio.crear(sinRaza, admin).isCompleto());
    }

    // PRUEBA 7: si la mascota está incompleta, la solicitud se aplica directo (sin aprobación)
    @Test
    void solicitudEnMascotaIncompletaSeAplicaDirecto() {
        // Crea una mascota incompleta (sin raza, sexo, peso ni imagen)
        Mascota incompleta = rocky(false);
        // Simula: al buscar la mascota 1, devuelve la incompleta
        when(mascotas.findById(1L)).thenReturn(Optional.of(incompleta));
        // Simula: al guardar la solicitud, devuelve la misma
        when(solicitudes.save(any(SolicitudCambio.class))).thenAnswer(inv -> inv.getArgument(0));
        // Arma los cambios propuestos: raza, sexo, peso e imagen (descripción null)
        SolicitudRequest cambios = new SolicitudRequest("Golden", "Macho", "28 kg", "/img/golden.jpg", null);
        // Ejecuta: María envía la solicitud
        SolicitudCambio s = servicio.solicitarCambio(1L, cambios, maria);
        // Comprueba: la solicitud quedó "aprobada" automáticamente
        assertEquals("aprobada", s.getEstado());
        // Comprueba: la mascota ahora quedó completa
        assertTrue(incompleta.isCompleto());
    }

    // PRUEBA 8: si la mascota está completa, la solicitud queda pendiente y no se permiten dos a la vez
    @Test
    void solicitudEnMascotaCompletaQuedaPendienteYNoDuplica() {
        // Simula: la mascota 1 existe y está completa
        when(mascotas.findById(1L)).thenReturn(Optional.of(rocky(true)));
        // Simula: al guardar la solicitud, devuelve la misma
        when(solicitudes.save(any(SolicitudCambio.class))).thenAnswer(inv -> inv.getArgument(0));
        // Simula: todavía NO hay solicitud pendiente para esa mascota
        when(solicitudes.findFirstByMascotaIdAndEstado(1L, "pendiente")).thenReturn(Optional.empty());
        // Cambio propuesto: solo la raza
        SolicitudRequest cambios = new SolicitudRequest("Labrador", null, null, null, null);
        // Comprueba: la primera solicitud queda "pendiente"
        assertEquals("pendiente", servicio.solicitarCambio(1L, cambios, maria).getEstado());

        // Ahora simula: YA existe una solicitud pendiente para esa mascota
        when(solicitudes.findFirstByMascotaIdAndEstado(1L, "pendiente")).thenReturn(Optional.of(new SolicitudCambio()));
        // Comprueba: una segunda solicitud lanza ConflictoException (409)
        assertThrows(ConflictoException.class, () -> servicio.solicitarCambio(1L, cambios, maria));
    }

    // PRUEBA 9: al aprobar una solicitud, los cambios se aplican a la mascota
    @Test
    void aprobarSolicitudAplicaLosCambios() {
        // Crea una mascota completa de ejemplo
        Mascota m = rocky(true);
        // Crea una solicitud de cambio
        SolicitudCambio s = new SolicitudCambio();
        // Le pone id 5
        s.setId(5L);
        // La asocia a la mascota 1
        s.setMascotaId(1L);
        // Propone cambiar la raza a Labrador
        s.setRaza("Labrador");
        // Su estado es pendiente
        s.setEstado("pendiente");
        // Simula: al buscar la solicitud 5, la devuelve
        when(solicitudes.findById(5L)).thenReturn(Optional.of(s));
        // Simula: al buscar la mascota 1, la devuelve
        when(mascotas.findById(1L)).thenReturn(Optional.of(m));
        // Simula: al guardar la solicitud, devuelve la misma
        when(solicitudes.save(any(SolicitudCambio.class))).thenAnswer(inv -> inv.getArgument(0));

        // Ejecuta: el personal aprueba la solicitud (true = aprobar)
        SolicitudCambio resuelta = servicio.resolver(5L, true);
        // Comprueba: la solicitud quedó "aprobada"
        assertEquals("aprobada", resuelta.getEstado());
        // Comprueba: la raza de la mascota cambió de Golden a Labrador
        assertEquals("Labrador", m.getRaza());
    }

    // PRUEBA 10: rechazar no cambia la mascota, y una solicitud no se puede resolver dos veces
    @Test
    void rechazarNoCambiaLaMascotaYNoSePuedeResolverDosVeces() {
        // Crea una mascota completa (raza Golden)
        Mascota m = rocky(true);
        // Crea una solicitud de cambio
        SolicitudCambio s = new SolicitudCambio();
        // Id 5
        s.setId(5L);
        // De la mascota 1
        s.setMascotaId(1L);
        // Propone raza Labrador
        s.setRaza("Labrador");
        // Estado pendiente
        s.setEstado("pendiente");
        // Simula: al buscar la solicitud 5, la devuelve
        when(solicitudes.findById(5L)).thenReturn(Optional.of(s));
        // Simula: al guardar la solicitud, devuelve la misma
        when(solicitudes.save(any(SolicitudCambio.class))).thenAnswer(inv -> inv.getArgument(0));

        // Ejecuta: el personal rechaza (false = rechazar). Comprueba que quedó "rechazada"
        assertEquals("rechazada", servicio.resolver(5L, false).getEstado());
        // Comprueba: la mascota NO cambió, sigue con raza Golden
        assertEquals("Golden", m.getRaza());
        // Comprueba: intentar resolverla otra vez lanza ConflictoException (409)
        assertThrows(ConflictoException.class, () -> servicio.resolver(5L, true));
    }
}