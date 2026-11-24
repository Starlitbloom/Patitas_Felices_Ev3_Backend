// Carpeta (paquete) donde vive este archivo: service
package com.patitas.servicio_clinico_service.service;

// Tipo "lista" de Java
import java.util.List;
// Conjunto de valores sin repetir (para las especies válidas)
import java.util.Set;

// Marca esta clase como servicio
import org.springframework.stereotype.Service;
// Agrupa las operaciones con la BD: si algo falla, se deshace todo
import org.springframework.transaction.annotation.Transactional;

// Clases del proyecto que usa el service
import com.patitas.servicio_clinico_service.dto.ServicioRequest;
import com.patitas.servicio_clinico_service.exception.ConflictoException;
import com.patitas.servicio_clinico_service.exception.RecursoNoEncontradoException;
import com.patitas.servicio_clinico_service.model.ServicioClinico;
import com.patitas.servicio_clinico_service.repository.ServicioClinicoRepository;

/**
 * Lógica de negocio del catálogo:
 *  - no puede haber dos servicios con el mismo nombre;
 *  - las especies válidas son Perro, Gato, Conejo y Ave;
 *  - el listado solo muestra servicios activos y se puede filtrar por especie o grupo.
 */
@Service
public class ServicioClinicoService {

    // Especies que atiende la clínica (las mismas del frontend)
    private static final Set<String> ESPECIES_VALIDAS = Set.of("Perro", "Gato", "Conejo", "Ave");

    // Repositorio del catálogo (acceso a la tabla servicio_clinico)
    private final ServicioClinicoRepository repo;

    // Constructor: Spring entrega el repositorio
    public ServicioClinicoService(ServicioClinicoRepository repo) {
        this.repo = repo;
    }

    // READ: lista los servicios activos, con filtros opcionales
    @Transactional(readOnly = true)
    public List<ServicioClinico> listar(String especie, String grupo) {
        // Trae todos los activos ordenados
        return repo.findByActivoTrueOrderByGrupoAscNombreAsc().stream()
            // Si viene especie, deja solo los que sirven para esa especie
            .filter(s -> especie == null || especie.isBlank() || s.sirveParaEspecie(especie))
            // Si viene grupo, deja solo los de ese grupo (sin importar mayúsculas)
            .filter(s -> grupo == null || grupo.isBlank() || s.getGrupo().equalsIgnoreCase(grupo))
            // Convierte el resultado en lista
            .toList();
    }

    // READ: busca un servicio por id (404 si no existe)
    @Transactional(readOnly = true)
    public ServicioClinico obtener(Long id) {
        return repo.findById(id)
            .orElseThrow(() -> new RecursoNoEncontradoException("Servicio " + id + " no encontrado"));
    }

    // CREATE: agrega un servicio al catálogo
    @Transactional
    public ServicioClinico crear(ServicioRequest req) {
        // Regla: el nombre no se puede repetir (409 si ya existe)
        if (repo.existsByNombreIgnoreCase(req.nombre().trim())) {
            throw new ConflictoException("Ya existe un servicio con el nombre '" + req.nombre().trim() + "'");
        }
        // Objeto vacío (constructor de Lombok)
        ServicioClinico s = new ServicioClinico();
        // Copia los datos del JSON (y valida las especies)
        copiar(req, s);
        // Guarda en MySQL
        return repo.save(s);
    }

    // UPDATE: cambia los datos de un servicio existente
    @Transactional
    public ServicioClinico actualizar(Long id, ServicioRequest req) {
        // Busca el servicio (404 si no existe)
        ServicioClinico s = obtener(id);
        // ¿Se le está cambiando el nombre?
        boolean cambiaNombre = !s.getNombre().equalsIgnoreCase(req.nombre().trim());
        // Si cambia y el nombre nuevo ya lo usa otro servicio, se rechaza (409)
        if (cambiaNombre && repo.existsByNombreIgnoreCase(req.nombre().trim())) {
            throw new ConflictoException("Ya existe un servicio con el nombre '" + req.nombre().trim() + "'");
        }
        // Copia los datos nuevos
        copiar(req, s);
        // Guarda los cambios
        return repo.save(s);
    }

    // DELETE: elimina un servicio del catálogo
    @Transactional
    public void eliminar(Long id) {
        // Busca (404 si no existe) y luego lo borra
        repo.delete(obtener(id));
    }

    // Ayudante: copia los datos del DTO a la entidad y valida las especies
    private void copiar(ServicioRequest req, ServicioClinico s) {
        s.setGrupo(req.grupo().trim());                        // Grupo sin espacios sobrantes
        s.setNombre(req.nombre().trim());                      // Nombre sin espacios sobrantes
        s.setActivo(req.activo() == null || req.activo());     // Si no viene "activo", queda en true
        s.getEspecies().clear();                               // Vacía las especies anteriores
        if (req.especies() != null) {                          // Si vienen especies...
            for (String e : req.especies()) {                  // ...revisa una por una
                if (!ESPECIES_VALIDAS.contains(e)) {           // Si no es válida, se rechaza (409)
                    throw new ConflictoException("Especie no valida: " + e + " (usa Perro, Gato, Conejo o Ave)");
                }
                s.getEspecies().add(e);                        // Si es válida, se agrega
            }
        }
    }
}
