package com.patitas.mascota_service.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.patitas.mascota_service.config.UsuarioActual;
import com.patitas.mascota_service.dto.MascotaRequest;
import com.patitas.mascota_service.dto.SolicitudRequest;
import com.patitas.mascota_service.model.Mascota;
import com.patitas.mascota_service.model.SolicitudCambio;
import com.patitas.mascota_service.exception.ConflictoException;
import com.patitas.mascota_service.exception.RecursoNoEncontradoException;
import com.patitas.mascota_service.repository.MascotaRepository;
import com.patitas.mascota_service.repository.SolicitudCambioRepository;

/**
 * Logica de negocio de mascotas:
 *  - el personal (admin/recepcionista) ve y administra todas las mascotas;
 *  - un cliente solo ve y crea las suyas (se compara el correo del token con el del dueno);
 *  - si la mascota esta completa, el dueno no la edita directo: envia una solicitud de cambio
 *    que el personal aprueba o rechaza.
 */
// @Service: le dice a Spring "esta clase contiene la lógica de negocio". Spring crea un objeto de ella y lo entrega al controller.
@Service
// Clase que guarda las REGLAS del sistema (quién ve qué, cuándo se aprueba un cambio...). El controller solo recibe peticiones y llama a esta clase.
public class MascotaService {

    // Repositorio de mascotas: es el que habla con la tabla "mascota" en MySQL (guardar, buscar, borrar). "final" = no se reasigna después.
    private final MascotaRepository mascotas;
    // Repositorio de solicitudes: habla con la tabla "solicitud_cambio".
    private final SolicitudCambioRepository solicitudes;

    // Constructor: Spring entrega aquí los dos repositorios automáticamente (inyección de dependencias). No necesitas hacer "new".
    public MascotaService(MascotaRepository mascotas, SolicitudCambioRepository solicitudes) {
        // Guarda el repositorio de mascotas en el campo de la clase para usarlo en los demás métodos.
        this.mascotas = mascotas;
        // Guarda el repositorio de solicitudes en el campo de la clase.
        this.solicitudes = solicitudes;
    }

    // ---------------- CRUD de mascotas ----------------

    /** Lista las mascotas visibles para el usuario, con filtros opcionales por especie y nombre. */
    // @Transactional(readOnly = true): abre una transacción SOLO de lectura. Es más rápida y evita que se modifique algo por error.
    @Transactional(readOnly = true)
    // READ (listar): recibe quién pregunta (usuario), y dos filtros opcionales: especie y texto del nombre. Devuelve una lista de mascotas.
    public List<Mascota> listar(UsuarioActual usuario, String especie, String texto) {
        // Regla de seguridad: si es personal (admin/recepcionista) trae TODAS las mascotas; si es cliente, solo las de su correo (sin importar mayúsculas).
        List<Mascota> base = usuario.staff() ? mascotas.findAll() : mascotas.findByCorreoDuenoIgnoreCase(usuario.correo());
        // Convierte la lista en un "stream" para poder filtrarla paso a paso.
        return base.stream()
            // Filtro 1 (especie): deja pasar todo si no se envió especie; si se envió, solo las mascotas de esa especie (ej: ?especie=Gato).
            .filter(m -> especie == null || especie.isBlank() || m.getEspecie().equalsIgnoreCase(especie))
            // Filtro 2 (nombre): deja pasar todo si no hay texto; si hay, solo las mascotas cuyo nombre lo contiene (ej: "roc" encuentra "Rocky").
            .filter(m -> texto == null || texto.isBlank() || m.getNombre().toLowerCase().contains(texto.toLowerCase()))
            // Convierte el stream de vuelta en una lista y la devuelve.
            .toList();
    }

    // Transacción de solo lectura, porque este método solo consulta.
    @Transactional(readOnly = true)
    // READ (uno): busca una mascota por su id, comprobando antes que el usuario tenga derecho a verla.
    public Mascota obtener(Long id, UsuarioActual usuario) {
        // Busca la mascota en la base de datos; si no existe, "buscar" lanza error 404.
        Mascota m = buscar(id);
        // Revisa que el usuario pueda ver esa mascota: si es cliente y la mascota es de otro dueño, lanza error 403.
        verificarAcceso(m, usuario);
        // Devuelve la mascota encontrada.
        return m;
    }

    // @Transactional: todo el método es una sola operación; si algo falla, no se guarda nada a medias.
    @Transactional
    // CREATE: crea una mascota nueva con los datos del JSON (req) y el usuario que la registra.
    public Mascota crear(MascotaRequest req, UsuarioActual usuario) {
        // Crea una mascota vacía (todavía no se guarda en la base de datos).
        Mascota m = new Mascota();
        // Copia los datos del JSON recibido a la mascota (nombre, especie, raza...). Método auxiliar de más abajo.
        copiar(req, m);
        // Un cliente solo puede registrar mascotas a su nombre, aunque en el JSON venga otro correo.
        // Regla de seguridad: si NO es personal, fuerza que el dueño sea el correo de su token (así no puede crear mascotas a nombre de otro).
        if (!usuario.staff()) m.setCorreoDueno(usuario.correo());
        // Si no se indicó estado de salud, le pone "al-dia" por defecto.
        if (m.getEstado() == null || m.getEstado().isBlank()) m.setEstado("al-dia");
        // Regla de negocio: calcula si la ficha está completa (tiene raza, sexo, peso e imagen).
        m.recalcularCompleto();
        // Guarda la mascota en MySQL (INSERT) y devuelve la mascota ya con su id asignado.
        return mascotas.save(m);
    }

    // Una sola transacción para todo el método.
    @Transactional
    // UPDATE: modifica una mascota existente con los datos nuevos. (El controller solo deja usarlo a admin y recepcionista.)
    public Mascota actualizar(Long id, MascotaRequest req) {
        // Busca la mascota por id; si no existe, error 404.
        Mascota m = buscar(id);
        // Reemplaza los datos de la mascota por los del JSON.
        copiar(req, m);
        // Si quedó sin estado, le pone "al-dia".
        if (m.getEstado() == null || m.getEstado().isBlank()) m.setEstado("al-dia");
        // Vuelve a calcular si la ficha está completa con los datos nuevos.
        m.recalcularCompleto();
        // Guarda los cambios en MySQL (UPDATE, porque la mascota ya tiene id) y devuelve la mascota actualizada.
        return mascotas.save(m);
    }

    // Una sola transacción: si falla el borrado de la mascota, tampoco se borran sus solicitudes.
    @Transactional
    // DELETE: elimina una mascota por su id. (El controller solo deja usarlo al admin.)
    public void eliminar(Long id) {
        // Busca la mascota; si no existe, error 404 (así no se borra algo que no hay).
        Mascota m = buscar(id);
        // Primero borra las solicitudes de cambio de esa mascota; si no, quedarían "huérfanas" en la tabla.
        solicitudes.deleteByMascotaId(id);   // primero sus solicitudes de cambio
        // Ahora sí borra la mascota de la base de datos.
        mascotas.delete(m);
    }

    // ---------------- Solicitudes de cambio ----------------

    // Una sola transacción para todo el proceso.
    @Transactional
    // El dueño pide cambiar datos de su mascota. Recibe el id de la mascota, los cambios propuestos y quién lo pide.
    public SolicitudCambio solicitarCambio(Long mascotaId, SolicitudRequest req, UsuarioActual usuario) {
        // Busca la mascota; si no existe, error 404.
        Mascota m = buscar(mascotaId);
        // Seguridad: un cliente solo puede pedir cambios sobre SUS mascotas; si no, error 403.
        verificarAcceso(m, usuario);

        // Crea una solicitud nueva (todavía vacía).
        SolicitudCambio s = new SolicitudCambio();
        // Anota a qué mascota pertenece la solicitud.
        s.setMascotaId(m.getId());
        // Anota quién la pide, tomando el correo del token (no del JSON).
        s.setCorreoSolicitante(usuario.correo());
        // Guarda la raza propuesta (puede venir null si no quiere cambiarla).
        s.setRaza(req.raza());
        // Guarda el sexo propuesto.
        s.setSexo(req.sexo());
        // Guarda el peso propuesto.
        s.setPeso(req.peso());
        // Guarda la imagen propuesta.
        s.setImagen(req.imagen());
        // Guarda la descripción propuesta.
        s.setDescripcion(req.descripcion());
        // Anota la fecha y hora exactas en que se hizo la solicitud.
        s.setFechaSolicitud(LocalDateTime.now());

        // Regla de negocio: ¿la ficha de la mascota está incompleta?
        if (!m.isCompleto()) {
            // Mascota incompleta: el dueno puede completar la ficha directamente, sin aprobacion.
            // Aplica los cambios directo a la mascota (no hace falta aprobación porque aún está completando su ficha).
            aplicarCambios(m, s);
            // Guarda la mascota ya modificada en la base de datos.
            mascotas.save(m);
            // Marca la solicitud como aprobada automáticamente.
            s.setEstado(SolicitudCambio.APROBADA);
            // Anota la fecha en que se resolvió (ahora mismo).
            s.setFechaResolucion(LocalDateTime.now());
        // Si la ficha YA estaba completa, el cambio necesita aprobación del personal.
        } else {
            // Revisa si esa mascota ya tiene una solicitud pendiente (solo se permite una a la vez).
            if (solicitudes.findFirstByMascotaIdAndEstado(mascotaId, SolicitudCambio.PENDIENTE).isPresent()) {
                // Si ya hay una pendiente, lanza error 409 (conflicto) con este mensaje.
                throw new ConflictoException("La mascota ya tiene una solicitud de cambio pendiente");
            }
            // Deja la solicitud en estado "pendiente", esperando que el personal la apruebe o rechace.
            s.setEstado(SolicitudCambio.PENDIENTE);
        }
        // Guarda la solicitud en MySQL y la devuelve (con su id y estado).
        return solicitudes.save(s);
    }
    @Transactional(readOnly = true)
    public List<SolicitudCambio> listarSolicitudesDeMascota(Long mascotaId, UsuarioActual usuario) {
        Mascota m = buscar(mascotaId);
        verificarAcceso(m, usuario);
        return solicitudes.findByMascotaIdOrderByFechaSolicitudDesc(mascotaId);
    }
    // Transacción de solo lectura, porque solo consulta.
    @Transactional(readOnly = true)
    // Lista las solicitudes de cambio, con filtro opcional por estado (ej: solo las pendientes).
    public List<SolicitudCambio> listarSolicitudes(String estado) {
        // Si no se envió estado, devuelve todas las solicitudes; si se envió, solo las de ese estado (pendiente, aprobada o rechazada).
        return (estado == null || estado.isBlank()) ? solicitudes.findAll() : solicitudes.findByEstado(estado);
    }

    /** El personal aprueba (aplica los cambios a la mascota) o rechaza la solicitud. */
    // Una sola transacción: si algo falla, ni la mascota ni la solicitud quedan a medias.
    @Transactional
    // Resuelve una solicitud: aprobar = true la aprueba, false la rechaza. (El controller solo deja usarlo a admin y recepcionista.)
    public SolicitudCambio resolver(Long solicitudId, boolean aprobar) {
        // Busca la solicitud por id.
        SolicitudCambio s = solicitudes.findById(solicitudId)
            // Si no existe, lanza error 404 con este mensaje.
            .orElseThrow(() -> new RecursoNoEncontradoException("Solicitud " + solicitudId + " no encontrada"));
        // Revisa que la solicitud siga pendiente; no se puede resolver dos veces.
        if (!SolicitudCambio.PENDIENTE.equals(s.getEstado())) {
            // Si ya estaba aprobada o rechazada, lanza error 409 (conflicto).
            throw new ConflictoException("La solicitud ya fue resuelta");
        }
        // Si el personal decidió aprobar...
        if (aprobar) {
            // ...busca la mascota a la que pertenece la solicitud.
            Mascota m = buscar(s.getMascotaId());
            // ...le aplica los cambios propuestos.
            aplicarCambios(m, s);
            // ...y guarda la mascota modificada en la base de datos.
            mascotas.save(m);
        }
        // Cambia el estado de la solicitud: "aprobada" si aprobar es true, "rechazada" si es false.
        s.setEstado(aprobar ? SolicitudCambio.APROBADA : SolicitudCambio.RECHAZADA);
        // Anota la fecha y hora en que se resolvió.
        s.setFechaResolucion(LocalDateTime.now());
        // Guarda la solicitud actualizada y la devuelve.
        return solicitudes.save(s);
    }

    // ---------------- Auxiliares ----------------

    // Método ayudante (privado: solo se usa dentro de esta clase). Busca una mascota o falla.
    private Mascota buscar(Long id) {
        // Pide la mascota a la base de datos por su id.
        return mascotas.findById(id)
            // Si no existe, lanza error 404 con el id en el mensaje. Así se evita repetir este código en cada método.
            .orElseThrow(() -> new RecursoNoEncontradoException("Mascota " + id + " no encontrada"));
    }

    /** Un cliente solo accede a sus mascotas; el personal accede a todas. */
    // Método ayudante: comprueba que el usuario tenga derecho a ver/usar esa mascota.
    private void verificarAcceso(Mascota m, UsuarioActual usuario) {
        // Si NO es personal Y el correo del dueño de la mascota es distinto al correo del usuario (sin importar mayúsculas)...
        if (!usuario.staff() && !m.getCorreoDueno().equalsIgnoreCase(usuario.correo())) {
            // ...lanza error 403: "no tienes permiso". Evita que un cliente vea mascotas ajenas.
            throw new AccessDeniedException("No puedes acceder a una mascota de otro dueno");
        }
    }

    // Método ayudante: pasa los datos del DTO de entrada (MascotaRequest) a la entidad Mascota.
    private void copiar(MascotaRequest r, Mascota m) {
        // Copia el nombre.
        m.setNombre(r.nombre());
        // Copia la especie.
        m.setEspecie(r.especie());
        // Copia la raza.
        m.setRaza(r.raza());
        // Copia la edad.
        m.setEdad(r.edad());
        // Copia el sexo.
        m.setSexo(r.sexo());
        // Copia el peso.
        m.setPeso(r.peso());
        // Copia el nombre del dueño.
        m.setNombreDueno(r.nombreDueno());
        // Copia el correo del dueño.
        m.setCorreoDueno(r.correoDueno());
        // Copia la imagen.
        m.setImagen(r.imagen());
        // Copia el estado de salud.
        m.setEstado(r.estado());
        // Copia la descripción.
        m.setDescripcion(r.descripcion());
    }

    /** Solo pisa los campos que vienen con valor. */
    // Método ayudante: pasa los cambios de una solicitud a la mascota, pero solo los campos que traen algo.
    private void aplicarCambios(Mascota m, SolicitudCambio s) {
        // Si la solicitud trae raza, la cambia en la mascota; si viene vacía, deja la raza actual.
        if (hay(s.getRaza())) m.setRaza(s.getRaza());
        // Igual con el sexo.
        if (hay(s.getSexo())) m.setSexo(s.getSexo());
        // Igual con el peso.
        if (hay(s.getPeso())) m.setPeso(s.getPeso());
        // Igual con la imagen.
        if (hay(s.getImagen())) m.setImagen(s.getImagen());
        // Igual con la descripción.
        if (s.getDescripcion() != null) m.setDescripcion(hay(s.getDescripcion()) ? s.getDescripcion() : null);
        // Después de los cambios, recalcula si la ficha quedó completa.
        m.recalcularCompleto();
    }

    // Método ayudante: dice si un texto trae algo escrito.
    private boolean hay(String s) {
        // true si no es null y no está vacío ni son solo espacios.
        return s != null && !s.isBlank();
    }
}
