
package com.patitas.cita_service.service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.time.format.TextStyle;

import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;

import com.patitas.cita_service.model.Cita;
import com.patitas.cita_service.repository.CitaRepository;

/**
 * Servicio encargado de gestionar las citas de Patitas Felices.
 *
 * Contiene la logica de negocio para:
 *
 * - Registrar citas veterinarias.
 * - Consultar las citas de los clientes.
 * - Consultar todas las citas como administrador.
 * - Actualizar una reserva.
 * - Cancelar una cita.
 * - Cambiar el estado de una cita.
 * - Eliminar citas como administrador.
 * - Comprobar disponibilidad de veterinarios.
 *
 * Utiliza MySQL mediante CitaRepository.
 *
 * Tambien consulta veterinario-service para verificar
 * que el profesional exista y tenga disponibilidad.
 */
@Service
public class CitaService {

    /**
     * Repositorio de acceso a la tabla citas de MySQL.
     */
    private final CitaRepository citaRepository;

    /**
     * Cliente HTTP utilizado para comunicarse
     * con el microservicio de veterinarios.
     */
    private final RestClient veterinarioClient;

    /**
     * Zona horaria de la clinica.
     */
    private static final ZoneId ZONA_CLINICA =
            ZoneId.of("America/Santiago");

    /**
     * Duracion inicial de referencia para una cita.
     *
     * Se puede ajustar posteriormente segun
     * las duraciones reales de cada servicio.
     */
    private static final int DURACION_PREDETERMINADA = 30;

    /**
     * Estados que ocupan un espacio en la agenda.
     */
    private static final List<String> ESTADOS_OCUPADOS =
            List.of("Pendiente", "Confirmada");

    /**
     * Estados validos para las citas veterinarias.
     */
    private static final Set<String> ESTADOS_VALIDOS =
            Set.of(
                    "Pendiente",
                    "Confirmada",
                    "Completada",
                    "Cancelada"
            );

    /**
     * Constructor del servicio.
     *
     * Recibe el repositorio mediante inyeccion
     * de dependencias y configura la comunicacion
     * con veterinario-service.
     *
     * @param citaRepository Repositorio de citas.
     * @param veterinarioUrl URL del servicio de veterinarios.
     */
    public CitaService(
            CitaRepository citaRepository,
            @Value("${servicios.veterinarios.url:http://localhost:8085}")
            String veterinarioUrl) {

        this.citaRepository = citaRepository;

        this.veterinarioClient = RestClient.builder()
                .baseUrl(veterinarioUrl)
                .build();
    }

    /**
     * Obtiene todas las citas registradas.
     *
     * Esta operacion se reservara para ADMIN
     * desde el controlador y SecurityConfig.
     *
     * @return Lista de todas las citas.
     */
    @Transactional(readOnly = true)
    public List<Cita> listarTodas() {

        return citaRepository.findAll();
    }

    /**
     * Obtiene las citas pertenecientes
     * al usuario que inicio sesion.
     *
     * El correo se obtendra del JWT,
     * no del JSON enviado por el cliente.
     *
     * @param correoUsuario Correo del usuario autenticado.
     * @return Historial de citas del usuario.
     */
    @Transactional(readOnly = true)
    public List<Cita> listarMisCitas(String correoUsuario) {

        return citaRepository
                .findByCorreoUsuarioIgnoreCaseOrderByFechaDescHoraDesc(
                        normalizarCorreo(correoUsuario)
                );
    }

    /**
     * Busca una cita mediante su identificador.
     *
     * Si el usuario no es administrador, solo puede
     * consultar una reserva que le pertenezca.
     *
     * @param id Identificador de la cita.
     * @param correoUsuario Correo obtenido del JWT.
     * @param esAdmin Indica si tiene rol ADMIN.
     * @return Cita encontrada.
     */
    @Transactional(readOnly = true)
    public Cita obtenerCita(
            Long id,
            String correoUsuario,
            boolean esAdmin) {

        Cita cita = buscarPorId(id);

        verificarPermiso(cita, correoUsuario, esAdmin);

        return cita;
    }

    /**
     * Registra una nueva cita veterinaria.
     *
     * Antes de guardar:
     *
     * - Comprueba la fecha y la hora.
     * - Consulta la disponibilidad del veterinario.
     * - Evita conflictos con otras citas.
     * - Asigna el usuario autenticado.
     * - Establece el estado Pendiente.
     *
     * @param datos Informacion de la nueva reserva.
     * @param correoUsuario Correo obtenido del JWT.
     * @param nombreUsuario Nombre del usuario autenticado.
     * @return Cita registrada en MySQL.
     */
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public Cita crearCita(
            Cita datos,
            String correoUsuario,
            String nombreUsuario) {

        // La base de datos genera el identificador.
        datos.setId(null);

        // El propietario de la cuenta se obtiene del JWT.
        datos.setCorreoUsuario(
                normalizarCorreo(correoUsuario)
        );

        datos.setNombreUsuario(nombreUsuario);

        // Una cita nueva siempre comienza pendiente.
        datos.setEstado("Pendiente");

        // Preparar duracion y verificar disponibilidad.
        prepararReserva(datos, null);

        // Registrar la cita en MySQL.
        return citaRepository.save(datos);
    }

    /**
     * Actualiza una reserva existente.
     *
     * El cliente solo puede modificar sus propias citas.
     * No se permite editar citas completadas o canceladas.
     *
     * El estado y la cuenta propietaria se conservan.
     *
     * @param id Identificador de la cita.
     * @param datos Datos nuevos.
     * @param correoUsuario Correo obtenido del JWT.
     * @param esAdmin Indica si tiene rol ADMIN.
     * @return Cita actualizada.
     */
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public Cita actualizarCita(
            Long id,
            Cita datos,
            String correoUsuario,
            boolean esAdmin) {

        Cita cita = buscarPorId(id);

        verificarPermiso(cita, correoUsuario, esAdmin);

        if ("Cancelada".equals(cita.getEstado())
                || "Completada".equals(cita.getEstado())) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "No se puede modificar una cita finalizada"
            );
        }

        // Actualizar datos del propietario.
        cita.setDueno(datos.getDueno());
        cita.setPropietario(datos.getPropietario());
        cita.setCorreo(datos.getCorreo());
        cita.setTelefono(datos.getTelefono());

        // Actualizar datos de la mascota.
        cita.setMascota(datos.getMascota());
        cita.setTipoMascota(datos.getTipoMascota());
        cita.setOtraEspecie(datos.getOtraEspecie());
        cita.setEdadMascota(datos.getEdadMascota());

        // Actualizar informacion de la reserva.
        cita.setVeterinarioId(datos.getVeterinarioId());
        cita.setServicio(datos.getServicio());
        cita.setFecha(datos.getFecha());
        cita.setHora(datos.getHora());
        cita.setMotivo(datos.getMotivo());

        // Validar el nuevo horario y profesional.
        // Se ignora la misma cita al buscar conflictos.
        prepararReserva(cita, id);

        return citaRepository.save(cita);
    }

    /**
     * Cancela una cita veterinaria.
     *
     * Un cliente podra cancelar sus propias citas.
     * Los administradores tambien podran cancelarlas.
     *
     * No se permite cancelar una cita completada.
     *
     * @param id Identificador de la cita.
     * @param correoUsuario Correo obtenido del JWT.
     * @param esAdmin Indica si tiene rol ADMIN.
     * @return Cita con estado Cancelada.
     */
    @Transactional
    public Cita cancelarCita(
            Long id,
            String correoUsuario,
            boolean esAdmin) {

        Cita cita = buscarPorId(id);

        verificarPermiso(cita, correoUsuario, esAdmin);

        if ("Completada".equals(cita.getEstado())) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "No se puede cancelar una cita completada"
            );
        }

        cita.setEstado("Cancelada");

        return citaRepository.save(cita);
    }

    /**
     * Permite al administrador cambiar
     * el estado de una cita.
     *
     * Las transiciones admitidas son:
     *
     * Pendiente  -> Confirmada o Cancelada.
     * Confirmada -> Completada o Cancelada.
     *
     * Las citas Completadas y Canceladas
     * no pueden volver a abrirse.
     *
     * @param id Identificador de la cita.
     * @param nuevoEstado Estado solicitado.
     * @return Cita con su estado actualizado.
     */
    @Transactional
    public Cita cambiarEstado(
            Long id,
            String nuevoEstado) {

        Cita cita = buscarPorId(id);

        if (nuevoEstado == null
                || !ESTADOS_VALIDOS.contains(nuevoEstado)) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "El estado solicitado no es valido"
            );
        }

        String actual = cita.getEstado();

        // No modificar una cita ya finalizada.
        if ("Cancelada".equals(actual)
                || "Completada".equals(actual)) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "La cita ya esta finalizada"
            );
        }

        boolean transicionValida =
                ("Pendiente".equals(actual)
                        && ("Confirmada".equals(nuevoEstado)
                            || "Cancelada".equals(nuevoEstado)))
                ||
                ("Confirmada".equals(actual)
                        && ("Completada".equals(nuevoEstado)
                            || "Cancelada".equals(nuevoEstado)));

        if (!transicionValida) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "No se permite ese cambio de estado"
            );
        }

        cita.setEstado(nuevoEstado);

        return citaRepository.save(cita);
    }

    /**
     * Elimina definitivamente una cita de MySQL.
     *
     * Por seguridad, esta operacion se reservara
     * para administradores desde SecurityConfig.
     *
     * Los clientes utilizaran cancelarCita()
     * para conservar su historial.
     *
     * @param id Identificador de la cita.
     */
    @Transactional
    public void eliminarCita(Long id) {

        Cita cita = buscarPorId(id);

        citaRepository.delete(cita);
    }

    /**
     * Busca una cita por ID.
     *
     * @param id Identificador de la reserva.
     * @return Cita encontrada.
     * @throws ResponseStatusException HTTP 404 si no existe.
     */
    private Cita buscarPorId(Long id) {

        return citaRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Cita no encontrada con ID: " + id
                ));
    }

    /**
     * Comprueba que el usuario tenga permiso
     * para consultar o modificar una cita.
     *
     * Un administrador puede gestionar todas.
     * Un cliente solo puede gestionar las propias.
     */
    private void verificarPermiso(
            Cita cita,
            String correoUsuario,
            boolean esAdmin) {

        if (esAdmin) {
            return;
        }

        String correoAutenticado =
                normalizarCorreo(correoUsuario);

        if (cita.getCorreoUsuario() == null
                || !cita.getCorreoUsuario()
                        .equalsIgnoreCase(correoAutenticado)) {

            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "No tienes permisos para gestionar esta cita"
            );
        }
    }

    /**
     * Verifica los datos y la disponibilidad
     * antes de registrar o actualizar una reserva.
     */
    private void prepararReserva(
            Cita cita,
            Long idCitaActual) {

        validarFechaYHora(cita);

        // Duracion inicial de referencia.
        // Se ajustara al catalogo real de atenciones.
        if (cita.getDuracionMinutos() == null
                || cita.getDuracionMinutos() < 1) {

            cita.setDuracionMinutos(
                    DURACION_PREDETERMINADA
            );
        }

        if (cita.getDuracionMinutos() > 180) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "La duracion no puede superar 180 minutos"
            );
        }

        /**
         * El formulario puede permitir solicitar
         * un profesional por confirmar.
         */
        if (cita.getVeterinarioId() == null) {

            cita.setProfesionalPorConfirmar(true);
            cita.setVeterinarioNombre("Por confirmar");

            return;
        }

        // Consultar al microservicio de veterinarios.
        VeterinarioDatos veterinario =
                buscarVeterinario(cita.getVeterinarioId());

        // Comprobar que atienda ese dia y horario.
        validarHorarioVeterinario(cita, veterinario);

        // Comprobar si existen reservas superpuestas.
        validarConflictos(cita, idCitaActual);

        // Guardar el nombre confirmado del profesional.
        cita.setVeterinarioNombre(veterinario.nombre());
        cita.setProfesionalPorConfirmar(false);
    }

    /**
     * Comprueba que la reserva no corresponda
     * a una fecha u hora pasada.
     */
    private void validarFechaYHora(Cita cita) {

        if (cita.getFecha() == null
                || cita.getHora() == null) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "La fecha y la hora son obligatorias"
            );
        }

        LocalDate hoy = LocalDate.now(ZONA_CLINICA);

        if (cita.getFecha().isBefore(hoy)) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "No se pueden reservar fechas pasadas"
            );
        }

        if (cita.getFecha().equals(hoy)
                && !cita.getHora().isAfter(
                        LocalTime.now(ZONA_CLINICA))) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "La hora de la cita debe ser futura"
            );
        }
    }

    /**
     * Consulta los datos del veterinario a traves
     * de su endpoint REST publico.
     *
     * GET http://localhost:8085/api/veterinarios/{id}
     *
     * @param id Identificador del veterinario.
     * @return Informacion del profesional.
     */
    private VeterinarioDatos buscarVeterinario(Long id) {

        try {

            VeterinarioDatos veterinario = veterinarioClient
                    .get()
                    .uri("/api/veterinarios/{id}", id)
                    .retrieve()
                    .body(VeterinarioDatos.class);

            if (veterinario == null) {

                throw new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY,
                        "No se recibieron datos del veterinario"
                );
            }

            return veterinario;

        } catch (RestClientResponseException error) {

            if (error.getStatusCode().value() == 404) {

                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "El veterinario seleccionado no existe"
                );
            }

            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Error al consultar veterinario-service"
            );

        } catch (RestClientException error) {

            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "No se pudo conectar con veterinario-service"
            );
        }
    }

    /**
     * Comprueba que el veterinario atienda
     * en el dia y horario solicitados.
     */
    private void validarHorarioVeterinario(
            Cita cita,
            VeterinarioDatos veterinario) {

        // Obtener el dia de la semana en español.
        String dia = cita.getFecha()
                .getDayOfWeek()
                .getDisplayName(
                        TextStyle.FULL,
                        Locale.forLanguageTag("es-CL")
                );

        boolean atiendeEseDia =
                veterinario.dias() != null
                && veterinario.dias().stream()
                        .anyMatch(d -> d.equalsIgnoreCase(dia));

        if (!atiendeEseDia) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "El veterinario no atiende ese dia"
            );
        }

        try {

            // Ejemplo de horario: 09:00 - 18:00.
            String[] partes =
                    veterinario.horario().split("\\s*-\\s*");

            LocalTime apertura =
                    LocalTime.parse(partes[0].trim());

            LocalTime cierre =
                    LocalTime.parse(partes[1].trim());

            LocalTime inicio = cita.getHora();

            LocalTime fin = inicio.plusMinutes(
                    cita.getDuracionMinutos()
            );

            if (inicio.isBefore(apertura)
                    || fin.isAfter(cierre)
                    || !fin.isAfter(inicio)) {

                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "La cita esta fuera del horario de atencion"
                );
            }

        } catch (NullPointerException
                 | ArrayIndexOutOfBoundsException
                 | DateTimeParseException error) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "El horario del veterinario no tiene un formato valido"
            );
        }
    }

    /**
     * Comprueba que el horario no se superponga
     * con otra reserva Pendiente o Confirmada.
     *
     * Las citas Canceladas y Completadas
     * no bloquean nuevos horarios.
     */
    private void validarConflictos(
            Cita cita,
            Long idCitaActual) {

        List<Cita> citasExistentes =
                citaRepository
                    .findByVeterinarioIdAndFechaAndEstadoInOrderByHoraAsc(
                            cita.getVeterinarioId(),
                            cita.getFecha(),
                            ESTADOS_OCUPADOS
                    );

        LocalTime inicioNuevo = cita.getHora();

        LocalTime finNuevo = inicioNuevo.plusMinutes(
                cita.getDuracionMinutos()
        );

        for (Cita existente : citasExistentes) {

            // Ignorar la misma cita durante una actualizacion.
            if (idCitaActual != null
                    && idCitaActual.equals(existente.getId())) {
                continue;
            }

            LocalTime inicioExistente = existente.getHora();

            int duracionExistente =
                    existente.getDuracionMinutos() != null
                    ? existente.getDuracionMinutos()
                    : DURACION_PREDETERMINADA;

            LocalTime finExistente =
                    inicioExistente.plusMinutes(duracionExistente);

            // Dos citas se superponen cuando sus
            // intervalos de tiempo coinciden.
            boolean hayConflicto =
                    inicioNuevo.isBefore(finExistente)
                    && inicioExistente.isBefore(finNuevo);

            if (hayConflicto) {

                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "El veterinario ya tiene una cita en ese horario"
                );
            }
        }
    }

    /**
     * Normaliza el correo de la cuenta autenticada.
     */
    private String normalizarCorreo(String correo) {

        if (correo == null || correo.isBlank()) {

            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Debes iniciar sesion para gestionar tus citas"
            );
        }

        return correo.trim().toLowerCase(Locale.ROOT);
    }

    /**
     * Estructura de datos utilizada para recibir
     * la informacion de veterinario-service.
     *
     * No representa una tabla de MySQL.
     * Solamente permite interpretar la respuesta JSON.
     */
    public static record VeterinarioDatos(
            Long id,
            String nombre,
            List<String> dias,
            String horario
    ) {
    }
}
