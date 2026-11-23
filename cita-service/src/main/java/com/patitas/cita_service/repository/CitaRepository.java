
package com.patitas.cita_service.repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.patitas.cita_service.model.Cita;

/**
 * Repositorio encargado de gestionar las citas
 * veterinarias de Patitas Felices.
 *
 * Utiliza Spring Data JPA para comunicarse
 * con la tabla "citas" de MySQL.
 *
 * Al extender JpaRepository, dispone de las
 * siguientes operaciones automaticamente:
 *
 * - findAll(): consultar todas las citas.
 * - findById(): buscar una cita por ID.
 * - save(): registrar o actualizar una cita.
 * - deleteById(): eliminar una cita.
 * - existsById(): verificar si existe una cita.
 *
 * Tambien incluye consultas personalizadas
 * para gestionar los clientes y la disponibilidad.
 */
@Repository
public interface CitaRepository extends JpaRepository<Cita, Long> {

    /**
     * Obtiene las citas registradas por un usuario.
     *
     * La busqueda utiliza el correo de la cuenta
     * autenticada y no distingue mayusculas
     * de minusculas.
     *
     * Las citas se ordenan desde la fecha
     * y hora mas recientes.
     *
     * @param correoUsuario Correo del usuario autenticado.
     * @return Lista de citas pertenecientes al usuario.
     */
    List<Cita> findByCorreoUsuarioIgnoreCaseOrderByFechaDescHoraDesc(
            String correoUsuario
    );

    /**
     * Busca las citas de un veterinario
     * para una fecha determinada.
     *
     * Permite seleccionar los estados que
     * deben considerarse al revisar su agenda.
     *
     * Por ejemplo, las citas Pendientes y
     * Confirmadas ocupan horarios, mientras
     * que las Canceladas no deben bloquearlos.
     *
     * @param veterinarioId ID del veterinario.
     * @param fecha Fecha de la consulta.
     * @param estados Estados que se desean consultar.
     * @return Lista de citas ordenadas por hora.
     */
    List<Cita> findByVeterinarioIdAndFechaAndEstadoInOrderByHoraAsc(
            Long veterinarioId,
            LocalDate fecha,
            Collection<String> estados
    );

    /**
     * Obtiene todas las citas programadas
     * para una fecha especifica.
     *
     * Este metodo permite al administrador
     * consultar la agenda diaria de la clinica.
     *
     * @param fecha Fecha que se desea consultar.
     * @return Lista de citas ordenadas por hora.
     */
    List<Cita> findByFechaOrderByHoraAsc(
            LocalDate fecha
    );

    /**
     * Obtiene las citas que tienen un estado
     * determinado.
     *
     * Permite consultar citas Pendientes,
     * Confirmadas, Completadas o Canceladas.
     *
     * @param estado Estado que se desea consultar.
     * @return Lista de citas con el estado indicado.
     */
    List<Cita> findByEstadoOrderByFechaAscHoraAsc(
            String estado
    );
}
