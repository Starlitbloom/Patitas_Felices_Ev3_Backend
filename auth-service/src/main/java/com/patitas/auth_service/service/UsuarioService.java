package com.patitas.auth_service.service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.patitas.auth_service.model.Rol;
import com.patitas.auth_service.model.Usuario;
import com.patitas.auth_service.repository.UsuarioRepository;

import lombok.RequiredArgsConstructor;

/**
 * Lógica de negocio de usuarios (CRUD + reglas del caso).
 *
 * Reglas principales:
 * - Correo y RUN únicos
 * - La contraseña siempre se guarda encriptada (BCrypt)
 * - Las cuentas fijas (2 administradoras y la recepcionista) no se pueden
 *   eliminar, desactivar ni cambiar de rol
 * - Un usuario que NO es admin no puede cambiarse el rol ni el estado activo
 */
@Service
@Transactional
@RequiredArgsConstructor
public class UsuarioService {

    /** Cuentas fijas de la clínica (las mismas de CUENTAS_FIJAS en el frontend). */
    public static final List<String> CORREOS_FIJOS = List.of(
        "ma.calfileo@duoc.cl", "ro.cruces@duoc.cl", "jo.medina@duoc.cl");

    private final UsuarioRepository usuarioRepository;
    private final RolService rolService;
    private final BCryptPasswordEncoder passwordEncoder;

    /* ---------------- LEER (Read) ---------------- */

    @Transactional(readOnly = true)
    public List<Usuario> listarUsuarios() {
        return usuarioRepository.findAllByOrderByFechaRegistroDesc();
    }

    @Transactional(readOnly = true)
    public Usuario obtenerPorId(Long id) {
        return usuarioRepository.findById(id)
            .orElseThrow(() -> new NegocioException("id", "El usuario con id " + id + " no existe.", HttpStatus.NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public Usuario obtenerPorCorreo(String correo) {
        return usuarioRepository.findByCorreoIgnoreCase(correo.trim())
            .orElseThrow(() -> new NegocioException("correo", "No existe un usuario con el correo " + correo + ".", HttpStatus.NOT_FOUND));
    }

    /* ---------------- CREAR (Create) ---------------- */

    /**
     * Crea un usuario.
     * @param permitirRol true cuando lo crea un admin (puede elegir rol y estado);
     *                    false en el registro público (siempre CLIENTE y activo).
     */
    public Usuario crearUsuario(Usuario datos, boolean permitirRol) {
        Validaciones.validarRun(datos.getRun());
        Validaciones.validarRequerido(datos.getNombre(), "nombre", "El nombre", 50);
        Validaciones.validarRequerido(datos.getApellidos(), "apellidos", "Los apellidos", 100);
        Validaciones.validarCorreo(datos.getCorreo());
        Validaciones.validarPassword(datos.getPassword());
        Validaciones.validarTelefono(datos.getTelefono());

        String correo = datos.getCorreo().trim();
        String run = Validaciones.formatearRun(datos.getRun());

        if (usuarioRepository.existsByCorreoIgnoreCase(correo)) {
            throw new NegocioException("correo", "Ya existe una cuenta registrada con este correo.", HttpStatus.CONFLICT);
        }
        if (usuarioRepository.existsByRun(run)) {
            throw new NegocioException("run", "Ya existe una cuenta registrada con este RUN.", HttpStatus.CONFLICT);
        }

        Usuario nuevo = new Usuario();
        nuevo.setRun(run);
        nuevo.setNombre(datos.getNombre().trim());
        nuevo.setApellidos(datos.getApellidos().trim());
        nuevo.setCorreo(correo);
        nuevo.setPassword(passwordEncoder.encode(datos.getPassword()));
        nuevo.setTelefono(texto(datos.getTelefono()));
        nuevo.setRegion(texto(datos.getRegion()));
        nuevo.setComuna(texto(datos.getComuna()));
        nuevo.setDireccion(texto(datos.getDireccion()));
        nuevo.setAvatar(datos.getAvatar());

        if (permitirRol && datos.getRol() != null) {
            nuevo.setRol(rolService.obtenerPorNombre(datos.getRol().getNombre()));
            nuevo.setActivo(datos.getActivo() == null || datos.getActivo());
        } else {
            nuevo.setRol(rolService.obtenerPorNombre(Rol.CLIENTE));
            nuevo.setActivo(true);
        }
        return usuarioRepository.save(nuevo);
    }

    /* ---------------- ACTUALIZAR (Update) ---------------- */

    /**
     * Actualiza solo los campos que vienen con valor (los null se ignoran).
     * @param esAdmin si quien edita es admin (puede cambiar rol y estado)
     */
    public Usuario actualizarUsuario(Long id, Usuario cambios, boolean esAdmin) {
        Usuario usuario = obtenerPorId(id);
        boolean esFija = esCuentaFija(usuario);

        if (cambios.getRun() != null && !cambios.getRun().isBlank()) {
            Validaciones.validarRun(cambios.getRun());
            String run = Validaciones.formatearRun(cambios.getRun());
            if (!run.equals(usuario.getRun()) && usuarioRepository.existsByRun(run)) {
                throw new NegocioException("run", "Ese RUN ya está en uso por otra cuenta.", HttpStatus.CONFLICT);
            }
            usuario.setRun(run);
        }
        if (cambios.getNombre() != null) {
            Validaciones.validarRequerido(cambios.getNombre(), "nombre", "El nombre", 50);
            usuario.setNombre(cambios.getNombre().trim());
        }
        if (cambios.getApellidos() != null) {
            Validaciones.validarRequerido(cambios.getApellidos(), "apellidos", "Los apellidos", 100);
            usuario.setApellidos(cambios.getApellidos().trim());
        }
        if (cambios.getCorreo() != null && !cambios.getCorreo().trim().equalsIgnoreCase(usuario.getCorreo())) {
            if (esFija) throw new NegocioException("correo", "No se puede cambiar el correo de una cuenta fija del sistema.");
            Validaciones.validarCorreo(cambios.getCorreo());
            if (usuarioRepository.existsByCorreoIgnoreCase(cambios.getCorreo().trim())) {
                throw new NegocioException("correo", "Ese correo ya está en uso por otra cuenta.", HttpStatus.CONFLICT);
            }
            usuario.setCorreo(cambios.getCorreo().trim());
        }
        // Si la contraseña viene vacía se conserva la anterior
        if (cambios.getPassword() != null && !cambios.getPassword().isEmpty()) {
            Validaciones.validarPassword(cambios.getPassword());
            usuario.setPassword(passwordEncoder.encode(cambios.getPassword()));
        }
        if (cambios.getTelefono() != null) {
            Validaciones.validarTelefono(cambios.getTelefono());
            usuario.setTelefono(cambios.getTelefono().trim());
        }
        if (cambios.getRegion() != null) usuario.setRegion(cambios.getRegion().trim());
        if (cambios.getComuna() != null) usuario.setComuna(cambios.getComuna().trim());
        if (cambios.getDireccion() != null) usuario.setDireccion(cambios.getDireccion().trim());
        // Avatar: "" lo quita, null lo deja igual
        if (cambios.getAvatar() != null) usuario.setAvatar(cambios.getAvatar().isEmpty() ? null : cambios.getAvatar());

        // Rol y estado: solo un admin, y nunca en las cuentas fijas
        if (cambios.getRol() != null && cambios.getRol().getNombre() != null
                && !cambios.getRol().getNombre().equalsIgnoreCase(usuario.getRol().getNombre())) {
            if (!esAdmin) throw new NegocioException("rol", "Solo un administrador puede cambiar el rol.", HttpStatus.FORBIDDEN);
            if (esFija) throw new NegocioException("rol", "No se puede cambiar el rol de una cuenta fija del sistema.");
            usuario.setRol(rolService.obtenerPorNombre(cambios.getRol().getNombre()));
        }
        if (cambios.getActivo() != null && !cambios.getActivo().equals(usuario.getActivo())) {
            if (!esAdmin) throw new NegocioException("activo", "Solo un administrador puede activar o desactivar cuentas.", HttpStatus.FORBIDDEN);
            if (esFija) throw new NegocioException("activo", "No se puede desactivar una cuenta fija del sistema.");
            usuario.setActivo(cambios.getActivo());
        }
        return usuarioRepository.save(usuario);
    }

    /* ---------------- ELIMINAR (Delete) ---------------- */

    public void eliminarUsuario(Long id) {
        Usuario usuario = obtenerPorId(id);
        if (esCuentaFija(usuario)) {
            throw new NegocioException("id", "No se puede eliminar una cuenta fija del sistema.", HttpStatus.FORBIDDEN);
        }
        usuarioRepository.delete(usuario);
    }

    /* ---------------- ESTADÍSTICAS (dashboard) ---------------- */

    @Transactional(readOnly = true)
    public Map<String, Long> estadisticas() {
        long total = usuarioRepository.count();
        long activos = usuarioRepository.countByActivoTrue();
        long clientes = usuarioRepository.findByRol_Nombre(Rol.CLIENTE).size();

        Map<String, Long> kpis = new LinkedHashMap<>();
        kpis.put("total", total);
        kpis.put("activos", activos);
        kpis.put("inactivos", total - activos);
        kpis.put("nuevosEstaSemana", usuarioRepository.countByFechaRegistroAfter(LocalDateTime.now().minusDays(7)));
        kpis.put("clientes", clientes);
        kpis.put("personal", total - clientes); // admin + recepcionista
        return kpis;
    }

    /* ---------------- Auxiliares ---------------- */

    public boolean esCuentaFija(Usuario usuario) {
        return CORREOS_FIJOS.contains(usuario.getCorreo().toLowerCase());
    }

    private String texto(String valor) {
        return valor == null ? "" : valor.trim();
    }
}
