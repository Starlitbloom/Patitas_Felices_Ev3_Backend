package com.patitas.auth_service.config;

import java.time.LocalDateTime;

import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import com.patitas.auth_service.model.Rol;
import com.patitas.auth_service.model.Usuario;
import com.patitas.auth_service.repository.RolRepository;
import com.patitas.auth_service.repository.UsuarioRepository;

import lombok.RequiredArgsConstructor;

/**
 * Se ejecuta al iniciar el microservicio (reemplaza asegurarDatosIniciales()
 * del frontend):
 * 1. Crea los 3 roles si no existen.
 * 2. Garantiza que las cuentas fijas existan con su rol y contraseña "1234".
 * 3. Si la tabla de usuarios estaba vacía, carga los clientes de ejemplo.
 */
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private static final String REGION_OHIGGINS = "Región del Libertador General Bernardo O'Higgins";

    private final RolRepository rolRepository;
    private final UsuarioRepository usuarioRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        Rol admin = asegurarRol(Rol.ADMIN, "Administrador");
        Rol recepcionista = asegurarRol(Rol.RECEPCIONISTA, "Recepcionista");
        Rol cliente = asegurarRol(Rol.CLIENTE, "Dueño/a de mascota");

        boolean primeraVez = usuarioRepository.count() == 0;

        // Cuentas fijas de la clínica
        asegurarCuentaFija("11111111-1", "Maria", "Calfileo Ceballos", "ma.calfileo@duoc.cl", "Rancagua", "Av. Brasil 1020", admin);
        asegurarCuentaFija("22222222-2", "Rocio", "Cruces", "ro.cruces@duoc.cl", "Machalí", "San Juan 455", admin);
        asegurarCuentaFija("19876543-0", "Josefa", "Medina", "jo.medina@duoc.cl", "Rancagua", "Av. Brasil 1020", recepcionista);

        // Clientes de ejemplo (los mismos de CLIENTES_EJEMPLO del frontend)
        if (primeraVez) {
            crear("19011022-2", "Camila", "Rojas Muñoz", "camila.rojas@gmail.com", "gato123", "+56 9 1234 5678", REGION_OHIGGINS, "Rancagua", "Los Aromos 230", true, cliente);
            crear("20345678-6", "Benjamín", "Soto Pérez", "be.soto@duoc.cl", "perro12", "+56 9 8765 4321", "Región Metropolitana de Santiago", "Maipú", "Pajaritos 3300", true, cliente);
            crear("18765432-7", "Valentina", "González Díaz", "vale.gonzalez@gmail.com", "conejo1", "", REGION_OHIGGINS, "Machalí", "El Polo 88", false, cliente);
            crear("21456789-K", "Tomás", "Herrera Vidal", "to.herrera@duoc.cl", "loro99", "+56 9 5555 1111", "Valparaíso", "Viña del Mar", "5 Norte 120", true, cliente);
            crear("17234567-0", "Antonia", "Fuentes Lagos", "antonia.fuentes@gmail.com", "miau2026", "+56 9 2222 3333", REGION_OHIGGINS, "Rengo", "Urriola 77", true, cliente);
        }
    }

    private Rol asegurarRol(String nombre, String descripcion) {
        return rolRepository.findByNombre(nombre).orElseGet(() -> rolRepository.save(new Rol(nombre, descripcion)));
    }

    private void asegurarCuentaFija(String run, String nombre, String apellidos, String correo,
            String comuna, String direccion, Rol rol) {
        Usuario cuenta = usuarioRepository.findByCorreoIgnoreCase(correo).orElse(null);
        if (cuenta == null) {
            crear(run, nombre, apellidos, correo, "1234", "", REGION_OHIGGINS, comuna, direccion, true, rol);
        } else {
            // Si alguien la modifio, se restaura el rol, estado y contraseña
            cuenta.setRol(rol);
            cuenta.setActivo(true);
            cuenta.setPassword(passwordEncoder.encode("1234"));
            usuarioRepository.save(cuenta);
        }
    }

    private void crear(String run, String nombre, String apellidos, String correo, String password, String telefono,
            String region, String comuna, String direccion, boolean activo, Rol rol) {
        Usuario u = new Usuario();
        u.setRun(run);
        u.setNombre(nombre);
        u.setApellidos(apellidos);
        u.setCorreo(correo);
        u.setPassword(passwordEncoder.encode(password));
        u.setTelefono(telefono);
        u.setRegion(region);
        u.setComuna(comuna);
        u.setDireccion(direccion);
        u.setActivo(activo);
        u.setRol(rol);
        u.setFechaRegistro(LocalDateTime.now());
        usuarioRepository.save(u);
    }
}
