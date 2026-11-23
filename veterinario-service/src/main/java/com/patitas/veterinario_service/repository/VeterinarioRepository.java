
package com.patitas.veterinario_service.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.patitas.veterinario_service.model.Veterinario;

/**
 * Repositorio encargado de gestionar el acceso a los datos
 * de los veterinarios de Patitas Felices.
 *
 * Utiliza Spring Data JPA para realizar operaciones
 * sobre la tabla "veterinarios" de MySQL.
 *
 * Al extender JpaRepository, dispone automaticamente
 * de operaciones como:
 *
 * - findAll(): obtener todos los veterinarios.
 * - findById(): buscar un veterinario por su ID.
 * - save(): registrar o actualizar un veterinario.
 * - deleteById(): eliminar un veterinario por su ID.
 * - existsById(): verificar si existe un veterinario.
 */
@Repository
public interface VeterinarioRepository
        extends JpaRepository<Veterinario, Long> {

    /**
     * Busca un veterinario mediante su correo electronico,
     * sin distinguir entre mayusculas y minusculas.
     *
     * Spring Data JPA genera automaticamente
     * la consulta correspondiente a partir del nombre
     * del metodo.
     *
     * @param correo Correo electronico del veterinario.
     * @return Optional con el veterinario encontrado
     *         o vacio si no existe.
     */
    Optional<Veterinario> findByCorreoIgnoreCase(String correo);

    /**
     * Comprueba si ya existe un veterinario registrado
     * con el correo electronico indicado.
     *
     * Se utiliza para evitar registros duplicados.
     *
     * @param correo Correo electronico que se desea validar.
     * @return true si el correo ya esta registrado;
     *         false en caso contrario.
     */
    boolean existsByCorreoIgnoreCase(String correo);
}
