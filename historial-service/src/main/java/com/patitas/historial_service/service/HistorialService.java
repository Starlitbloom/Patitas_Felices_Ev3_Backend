// Carpeta (paquete) donde vive este archivo: service
package com.patitas.historial_service.service;

// Fecha sin hora
import java.time.LocalDate;
// Para contar días entre dos fechas
import java.time.temporal.ChronoUnit;
// Tipo "lista" de Java
import java.util.List;

// Marca esta clase como servicio (Spring la crea y la entrega a quien la necesite)
import org.springframework.stereotype.Service;
// Agrupa las operaciones con la BD: si algo falla, se deshace todo
import org.springframework.transaction.annotation.Transactional;

// Clases del proyecto que usa el service
import com.patitas.historial_service.dto.AtencionRequest;
import com.patitas.historial_service.dto.VacunaRequest;
import com.patitas.historial_service.dto.VacunaResponse;
import com.patitas.historial_service.exception.ConflictoException;
import com.patitas.historial_service.exception.RecursoNoEncontradoException;
import com.patitas.historial_service.model.RegistroClinico;
import com.patitas.historial_service.model.RegistroVacuna;
import com.patitas.historial_service.repository.RegistroClinicoRepository;
import com.patitas.historial_service.repository.RegistroVacunaRepository;

/**
 * Lógica de negocio del historial clínico:
 *  - cada atención y cada vacuna pertenece a una mascota (mascotaId);
 *  - el próximo refuerzo de una vacuna debe ser posterior a la fecha de aplicación;
 *  - el estado de una vacuna se calcula con la fecha del próximo refuerzo:
 *      refuerzo ya vencido               -> atrasada
 *      vence en 30 días o menos          -> proxima
 *      más lejos                         -> al-dia
 */
@Service
public class HistorialService {

    // Días antes del refuerzo en que la vacuna pasa a "proxima" (public para usarlo en los tests)
    public static final long DIAS_PROXIMA = 30;

    // Repositorio de atenciones (acceso a la tabla registro_clinico)
    private final RegistroClinicoRepository atenciones;
    // Repositorio de vacunas (acceso a la tabla registro_vacuna)
    private final RegistroVacunaRepository vacunas;

    // Constructor: Spring entrega los dos repositorios automáticamente
    public HistorialService(RegistroClinicoRepository atenciones, RegistroVacunaRepository vacunas) {
        // Guarda el repositorio de atenciones
        this.atenciones = atenciones;
        // Guarda el repositorio de vacunas
        this.vacunas = vacunas;
    }

    // ======================== ATENCIONES ========================

    // READ: lista las atenciones de una mascota, más recientes primero
    @Transactional(readOnly = true)  // Solo lectura: más rápido y seguro
    public List<RegistroClinico> listarAtenciones(Long mascotaId) {
        // Consulta derivada del nombre del método (ver repository)
        return atenciones.findByMascotaIdOrderByFechaDesc(mascotaId);
    }

    // READ: busca una atención por id
    @Transactional(readOnly = true)
    public RegistroClinico obtenerAtencion(Long id) {
        // findById devuelve un Optional: si está vacío, lanza 404
        return atenciones.findById(id)
            .orElseThrow(() -> new RecursoNoEncontradoException("Atencion " + id + " no encontrada"));
    }

    // CREATE: registra una atención nueva para la mascota indicada
    @Transactional
    public RegistroClinico crearAtencion(Long mascotaId, AtencionRequest req) {
        // Objeto vacío (constructor de Lombok @NoArgsConstructor)
        RegistroClinico r = new RegistroClinico();
        // La asocia a la mascota de la URL
        r.setMascotaId(mascotaId);
        // Copia los datos del JSON
        copiar(req, r);
        // Guarda en MySQL y devuelve el registro ya con su id
        return atenciones.save(r);
    }

    // UPDATE: cambia los datos de una atención existente
    @Transactional
    public RegistroClinico actualizarAtencion(Long id, AtencionRequest req) {
        // Busca la atención (404 si no existe)
        RegistroClinico r = obtenerAtencion(id);
        // Reemplaza sus datos por los nuevos
        copiar(req, r);
        // Guarda los cambios
        return atenciones.save(r);
    }

    // DELETE: elimina una atención
    @Transactional
    public void eliminarAtencion(Long id) {
        // Busca (404 si no existe) y luego la borra
        atenciones.delete(obtenerAtencion(id));
    }

    // Ayudante: copia los datos del DTO a la entidad, limpiando espacios sobrantes
    private void copiar(AtencionRequest req, RegistroClinico r) {
        r.setFecha(req.fecha());                         // Fecha tal cual
        r.setMotivo(req.motivo().trim());                // trim() quita espacios al inicio y al final
        r.setDiagnostico(req.diagnostico().trim());      // Diagnóstico sin espacios sobrantes
        r.setTratamiento(req.tratamiento().trim());      // Tratamiento sin espacios sobrantes
        r.setVeterinario(req.veterinario().trim());      // Veterinario sin espacios sobrantes
        r.getServicios().clear();                        // Vacía la lista anterior de servicios
        if (req.servicios() != null) r.getServicios().addAll(req.servicios());  // Agrega los nuevos, si vienen
    }

    // ========================== VACUNAS ==========================

    // READ: lista las vacunas de una mascota, más recientes primero
    @Transactional(readOnly = true)
    public List<RegistroVacuna> listarVacunas(Long mascotaId) {
        return vacunas.findByMascotaIdOrderByFechaDesc(mascotaId);
    }

    // READ: busca una vacuna por id (404 si no existe)
    @Transactional(readOnly = true)
    public RegistroVacuna obtenerVacuna(Long id) {
        return vacunas.findById(id)
            .orElseThrow(() -> new RecursoNoEncontradoException("Vacuna " + id + " no encontrada"));
    }

    // CREATE: registra una vacuna nueva para la mascota indicada
    @Transactional
    public RegistroVacuna crearVacuna(Long mascotaId, VacunaRequest req) {
        // Regla de negocio: el refuerzo debe ser después de la aplicación (si no, 409)
        validarFechas(req);
        // Objeto vacío
        RegistroVacuna v = new RegistroVacuna();
        // Asocia la vacuna a la mascota
        v.setMascotaId(mascotaId);
        // Copia los datos del JSON
        copiar(req, v);
        // Guarda en MySQL
        return vacunas.save(v);
    }

    // UPDATE: corrige los datos de una vacuna ya registrada
    @Transactional
    public RegistroVacuna actualizarVacuna(Long id, VacunaRequest req) {
        // Misma regla de fechas que al crear
        validarFechas(req);
        // Busca la vacuna (404 si no existe)
        RegistroVacuna v = obtenerVacuna(id);
        // Reemplaza sus datos (la mascota no cambia)
        copiar(req, v);
        // Guarda los cambios
        return vacunas.save(v);
    }

    // DELETE: elimina una vacuna
    @Transactional
    public void eliminarVacuna(Long id) {
        // Busca (404 si no existe) y luego la borra
        vacunas.delete(obtenerVacuna(id));
    }

    // Ayudante: copia los datos del DTO a la entidad
    private void copiar(VacunaRequest req, RegistroVacuna v) {
        v.setVacuna(req.vacuna().trim());             // Nombre sin espacios sobrantes
        v.setFecha(req.fecha());                      // Fecha de aplicación
        v.setProximoRefuerzo(req.proximoRefuerzo());  // Fecha del próximo refuerzo
    }

    // Regla de negocio: el próximo refuerzo debe ser posterior a la fecha de aplicación
    private void validarFechas(VacunaRequest req) {
        // isAfter = "es después de"; si NO lo es, se rechaza con 409
        if (!req.proximoRefuerzo().isAfter(req.fecha())) {
            throw new ConflictoException("El proximo refuerzo debe ser posterior a la fecha de aplicacion");
        }
    }

    // Regla de negocio: calcula el estado de la vacuna respecto de "hoy" (static = se puede probar sin crear el service)
    public static String calcularEstado(LocalDate proximoRefuerzo, LocalDate hoy) {
        // Si el refuerzo ya pasó, está atrasada
        if (proximoRefuerzo.isBefore(hoy)) return "atrasada";
        // Si faltan 30 días o menos, está próxima
        if (ChronoUnit.DAYS.between(hoy, proximoRefuerzo) <= DIAS_PROXIMA) return "proxima";
        // Si falta más, está al día
        return "al-dia";
    }

    // Convierte una vacuna de la BD en la respuesta JSON, agregando el estado calculado con la fecha de hoy
    public VacunaResponse aRespuesta(RegistroVacuna v) {
        return new VacunaResponse(v.getId(), v.getMascotaId(), v.getVacuna(), v.getFecha(), v.getProximoRefuerzo(),
            calcularEstado(v.getProximoRefuerzo(), LocalDate.now()));
    }
}
