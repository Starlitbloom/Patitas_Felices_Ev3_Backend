
package com.patitas.veterinario_service.service;

import java.util.List;
import java.util.Locale;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.patitas.veterinario_service.model.Veterinario;
import com.patitas.veterinario_service.repository.VeterinarioRepository;

import lombok.RequiredArgsConstructor;

/**
 * Servicio encargado de gestionar la logica de negocio
 * de los veterinarios de Patitas Felices.
 *
 * Implementa las operaciones CRUD:
 * - Listar veterinarios.
 * - Buscar veterinarios por ID.
 * - Registrar veterinarios.
 * - Actualizar veterinarios.
 * - Eliminar veterinarios.
 *
 * Utiliza VeterinarioRepository para acceder a MySQL
 * mediante Spring Data JPA.
 */
@Service
@RequiredArgsConstructor
public class VeterinarioService {

    /**
     * Repositorio utilizado para realizar operaciones
     * sobre la tabla veterinarios.
     *
     * Lombok genera automaticamente el constructor
     * necesario para inyectar esta dependencia.
     */
    private final VeterinarioRepository veterinarioRepository;

    /**
     * Obtiene todos los veterinarios registrados.
     *
     * @return Lista de veterinarios almacenados en MySQL.
     */
    @Transactional(readOnly = true)
    public List<Veterinario> listarVeterinarios() {
        return veterinarioRepository.findAll();
    }

    /**
     * Busca un veterinario utilizando su ID.
     *
     * Si no existe, devuelve una respuesta HTTP 404.
     *
     * @param id Identificador del veterinario.
     * @return Veterinario encontrado.
     */
    @Transactional(readOnly = true)
    public Veterinario obtenerVeterinarioPorId(Long id) {
        return veterinarioRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Veterinario no encontrado con ID: " + id
                ));
    }

    /**
     * Registra un nuevo veterinario en MySQL.
     *
     * Antes de guardar:
     * - Normaliza el correo electronico.
     * - Comprueba que no exista otro veterinario
     *   con el mismo correo.
     *
     * Si el correo ya esta registrado,
     * devuelve una respuesta HTTP 409.
     *
     * @param veterinario Datos del nuevo veterinario.
     * @return Veterinario registrado con su ID generado.
     */
    @Transactional
    public Veterinario crearVeterinario(Veterinario veterinario) {

        String correo = normalizarCorreo(veterinario.getCorreo());

        if (veterinarioRepository.existsByCorreoIgnoreCase(correo)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Ya existe un veterinario con ese correo"
            );
        }

        // El ID lo genera automaticamente MySQL.
        veterinario.setId(null);
        veterinario.setCorreo(correo);

        return veterinarioRepository.save(veterinario);
    }

    /**
     * Actualiza la informacion de un veterinario.
     *
     * Primero comprueba que el veterinario exista.
     * Luego verifica que el nuevo correo no pertenezca
     * a otro registro.
     *
     * Finalmente actualiza sus datos en MySQL.
     *
     * @param id Identificador del veterinario.
     * @param datos Datos actualizados.
     * @return Veterinario actualizado.
     */
    @Transactional
    public Veterinario actualizarVeterinario(
            Long id,
            Veterinario datos) {

        // Buscar el veterinario existente.
        Veterinario veterinario = obtenerVeterinarioPorId(id);

        String correo = normalizarCorreo(datos.getCorreo());

        // Evitar que dos veterinarios compartan el mismo correo.
        veterinarioRepository.findByCorreoIgnoreCase(correo)
                .ifPresent(existente -> {
                    if (!existente.getId().equals(id)) {
                        throw new ResponseStatusException(
                                HttpStatus.CONFLICT,
                                "El correo ya pertenece a otro veterinario"
                        );
                    }
                });

        // Actualizar los datos profesionales.
        veterinario.setNombre(datos.getNombre());
        veterinario.setCargo(datos.getCargo());
        veterinario.setTitulo(datos.getTitulo());
        veterinario.setArea(datos.getArea());
        veterinario.setEspecies(datos.getEspecies());
        veterinario.setExperiencia(datos.getExperiencia());
        veterinario.setDescripcion(datos.getDescripcion());

        // Actualizar disponibilidad y contacto.
        veterinario.setDias(datos.getDias());
        veterinario.setHorario(datos.getHorario());
        veterinario.setImagen(datos.getImagen());
        veterinario.setCorreo(correo);

        // Guardar los cambios en MySQL.
        return veterinarioRepository.save(veterinario);
    }

    /**
     * Elimina un veterinario utilizando su ID.
     *
     * Comprueba primero que exista el registro.
     * Si no existe, devuelve HTTP 404.
     *
     * @param id Identificador del veterinario.
     */
    @Transactional
    public void eliminarVeterinario(Long id) {

        Veterinario veterinario = obtenerVeterinarioPorId(id);

        veterinarioRepository.delete(veterinario);
    }

    /**
     * Normaliza un correo electronico para evitar
     * diferencias por mayusculas o espacios.
     *
     * @param correo Correo recibido.
     * @return Correo en minusculas y sin espacios externos.
     */
    private String normalizarCorreo(String correo) {

        if (correo == null || correo.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "El correo es obligatorio"
            );
        }

        return correo.trim().toLowerCase(Locale.ROOT);
    }
}
