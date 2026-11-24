package com.patitas.report_service.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.patitas.report_service.model.ReporteGenerado;
import com.patitas.report_service.model.TipoReporte;
import com.patitas.report_service.repository.ReporteGeneradoRepository;
import com.patitas.report_service.webclient.AuthClient;
import com.patitas.report_service.webclient.CitaClient;
import com.patitas.report_service.webclient.ContactoClient;
import com.patitas.report_service.webclient.MascotaClient;
import com.patitas.report_service.webclient.NotificacionClient;

import lombok.RequiredArgsConstructor;

/**
 * Arma el dashboard y los reportes consultando a los demás microservicios.
 *
 * Este servicio NO es dueño de los usuarios, mascotas ni citas: los pide
 * por WebClient y calcula los indicadores (los mismos que antes calculaban
 * DashboardPage.tsx y ReportesPage.tsx con localStorage).
 *
 * Si un servicio no responde, su sección queda en null y su nombre se agrega
 * a "serviciosNoDisponibles": el reporte nunca se cae por culpa de otro servicio.
 */
@Service
@RequiredArgsConstructor
public class ReporteService {

    private final AuthClient authClient;
    private final MascotaClient mascotaClient;
    private final CitaClient citaClient;
    private final ContactoClient contactoClient;
    private final NotificacionClient notificacionClient;
    private final ReporteGeneradoRepository reporteRepository;

    /* =========================================================
       DASHBOARD (admin y recepcionista)
    ========================================================= */

    public Map<String, Object> dashboard(String authHeader, boolean esAdmin) {
        List<String> caidos = new ArrayList<>();
        Map<String, Object> respuesta = new LinkedHashMap<>();

        // Indicadores de usuarios: solo el ADMIN (auth-service no le entrega usuarios a la recepcionista)
        if (esAdmin) {
            Optional<List<Map<String, Object>>> usuarios = authClient.listarUsuarios(authHeader);
            if (usuarios.isPresent()) {
                List<Map<String, Object>> lista = usuarios.get();
                Map<String, Object> seccion = new LinkedHashMap<>();
                seccion.put("total", lista.size());
                seccion.put("nuevosEstaSemana", lista.stream().filter(this::registradoEstaSemana).count());
                seccion.put("ultimosRegistrados", ultimosRegistrados(lista, 5));
                respuesta.put("usuarios", seccion);
            } else {
                respuesta.put("usuarios", null);
                caidos.add(authClient.nombreServicio());
            }
        }

        Map<String, Object> clinica = new LinkedHashMap<>();
        clinica.put("mascotasRegistradas", mascotaClient.listarMascotas(authHeader)
            .map(l -> (Object) l.size()).orElseGet(() -> marcarCaido(caidos, mascotaClient.nombreServicio())));
        clinica.put("solicitudesPendientes", mascotaClient.listarSolicitudes(authHeader)
            .map(l -> (Object) contarEstado(l, "pendiente")).orElseGet(() -> marcarCaido(caidos, mascotaClient.nombreServicio())));
        clinica.put("citasPendientes", citaClient.listarCitas(authHeader)
            .map(l -> (Object) contarEstado(l, "pendiente")).orElseGet(() -> marcarCaido(caidos, citaClient.nombreServicio())));
        respuesta.put("clinica", clinica);

        respuesta.put("serviciosNoDisponibles", caidos.stream().distinct().toList());
        return respuesta;
    }

    /* =========================================================
       REPORTE COMPLETO (página Reportes, solo admin)
    ========================================================= */

    public Map<String, Object> reporteCompleto(String authHeader, String correoAdmin) {
        List<String> caidos = new ArrayList<>();
        Map<String, Object> reporte = new LinkedHashMap<>();

        Optional<List<Map<String, Object>>> usuarios = authClient.listarUsuarios(authHeader);
        Optional<List<Map<String, Object>>> mascotas = mascotaClient.listarMascotas(authHeader);
        Optional<List<Map<String, Object>>> solicitudes = mascotaClient.listarSolicitudes(authHeader);
        Optional<List<Map<String, Object>>> citas = citaClient.listarCitas(authHeader);
        Optional<Map<String, Object>> contacto = contactoClient.estadisticas(authHeader);
        Optional<Map<String, Object>> notificaciones = notificacionClient.estadisticas(authHeader);

        reporte.put("usuarios", usuarios.map(this::seccionUsuarios).orElse(null));
        reporte.put("mascotas", mascotas.map(m -> seccionMascotas(m, solicitudes.orElse(null))).orElse(null));
        reporte.put("citas", citas.map(this::seccionCitas).orElse(null));
        reporte.put("contacto", contacto.orElse(null));
        reporte.put("notificaciones", notificaciones.orElse(null));

        if (usuarios.isEmpty()) caidos.add(authClient.nombreServicio());
        if (mascotas.isEmpty()) caidos.add(mascotaClient.nombreServicio());
        if (citas.isEmpty()) caidos.add(citaClient.nombreServicio());
        if (contacto.isEmpty()) caidos.add(contactoClient.nombreServicio());
        if (notificaciones.isEmpty()) caidos.add(notificacionClient.nombreServicio());

        reporte.put("serviciosNoDisponibles", caidos);
        reporte.put("generadoPor", correoAdmin);
        reporte.put("generadoEn", LocalDateTime.now().toString());

        registrar(TipoReporte.COMPLETO, correoAdmin,
            resumen(usuarios.map(List::size), mascotas.map(List::size), citas.map(List::size)), caidos);
        return reporte;
    }

    /* =========================================================
       EXPORTAR (botón "Exportar reporte completo" → Excel de 3 hojas)
       El frontend arma el .xlsx con SheetJS a partir de estas filas.
    ========================================================= */

    public Map<String, Object> exportar(String authHeader, String correoAdmin) {
        List<String> caidos = new ArrayList<>();
        Optional<List<Map<String, Object>>> usuarios = authClient.listarUsuarios(authHeader);
        Optional<List<Map<String, Object>>> mascotas = mascotaClient.listarMascotas(authHeader);
        Optional<List<Map<String, Object>>> citas = citaClient.listarCitas(authHeader);

        if (usuarios.isEmpty()) caidos.add(authClient.nombreServicio());
        if (mascotas.isEmpty()) caidos.add(mascotaClient.nombreServicio());
        if (citas.isEmpty()) caidos.add(citaClient.nombreServicio());

        Map<String, Object> hojas = new LinkedHashMap<>();
        hojas.put("Usuarios", usuarios.orElse(List.of()).stream().map(u -> fila(
            "Nombre", texto(u.get("nombre")) + " " + texto(u.get("apellidos")),
            "RUN", texto(u.get("run")),
            "Correo", texto(u.get("correo")),
            "Rol", rolDe(u),
            "Región", texto(u.get("region")),
            "Comuna", texto(u.get("comuna")),
            "Activo", Boolean.TRUE.equals(u.get("activo")) ? "Sí" : "No",
            "Registro", texto(u.get("fechaRegistro")))).toList());
        hojas.put("Mascotas", mascotas.orElse(List.of()).stream().map(m -> fila(
            "Nombre", texto(m.get("nombre")),
            "Especie", texto(m.get("especie")),
            "Raza", texto(m.get("raza")),
            "Estado", texto(m.get("estado")),
            "Dueño", texto(m.get("correoDueno")))).toList());
        hojas.put("Citas", citas.orElse(List.of()).stream().map(c -> fila(
            "Fecha", texto(c.get("fecha")),
            "Servicio", texto(c.get("servicio")),
            "Motivo", texto(c.get("motivo")),
            "Estado", texto(c.get("estado")),
            "Usuario", texto(c.get("correoUsuario")))).toList());

        Map<String, Object> respuesta = new LinkedHashMap<>();
        respuesta.put("nombreArchivo", "reporte-completo-patitas-felices");
        respuesta.put("hojas", hojas);
        respuesta.put("serviciosNoDisponibles", caidos);

        registrar(TipoReporte.EXPORTACION, correoAdmin,
            resumen(usuarios.map(List::size), mascotas.map(List::size), citas.map(List::size)), caidos);
        return respuesta;
    }

    /* =========================================================
       HISTORIAL DE REPORTES (CRUD propio del servicio)
    ========================================================= */

    public List<ReporteGenerado> historial() {
        return reporteRepository.findAllByOrderByFechaGeneracionDesc();
    }

    public ReporteGenerado obtenerDelHistorial(Long id) {
        return reporteRepository.findById(id)
            .orElseThrow(() -> new NegocioException("id", "El reporte con id " + id + " no existe.", HttpStatus.NOT_FOUND));
    }

    public void eliminarDelHistorial(Long id) {
        reporteRepository.delete(obtenerDelHistorial(id));
    }

    /* =========================================================
       CÁLCULO DE CADA SECCIÓN
    ========================================================= */

    Map<String, Object> seccionUsuarios(List<Map<String, Object>> usuarios) {
        Map<String, Object> s = new LinkedHashMap<>();
        s.put("total", usuarios.size());
        s.put("administradores", usuarios.stream().filter(u -> "ADMIN".equals(rolDe(u))).count());
        s.put("recepcionistas", usuarios.stream().filter(u -> "RECEPCIONISTA".equals(rolDe(u))).count());
        s.put("clientes", usuarios.stream().filter(u -> "CLIENTE".equals(rolDe(u))).count());
        s.put("regionesConUsuarios", usuarios.stream().map(u -> texto(u.get("region"))).filter(r -> !r.isEmpty()).distinct().count());
        s.put("conFotoPerfil", usuarios.stream().filter(u -> !texto(u.get("avatar")).isEmpty()).count());
        s.put("nuevosEstaSemana", usuarios.stream().filter(this::registradoEstaSemana).count());
        s.put("porRegion", contarPor(usuarios, u -> vacioComo(u.get("region"), "Sin región")));

        // Top 10 comunas, con su región
        List<Map<String, Object>> conComuna = usuarios.stream().filter(u -> !texto(u.get("comuna")).isEmpty()).toList();
        List<Map<String, Object>> topComunas = contarPor(conComuna,
            u -> texto(u.get("comuna")) + "||" + vacioComo(u.get("region"), "Sin región"))
            .stream().limit(10).map(c -> {
                String[] partes = ((String) c.get("nombre")).split("\\|\\|", 2);
                return fila("comuna", partes[0], "region", partes.length > 1 ? partes[1] : "", "cantidad", c.get("cantidad"));
            }).toList();
        s.put("topComunas", topComunas);
        return s;
    }

    /**
     * @param solicitudes solicitudes de cambio (null si mascota-service no las entregó)
     */
    Map<String, Object> seccionMascotas(List<Map<String, Object>> mascotas, List<Map<String, Object>> solicitudes) {
        Map<String, Object> s = new LinkedHashMap<>();
        s.put("total", mascotas.size());
        s.put("conDatosIncompletos", mascotas.stream().filter(m -> Boolean.FALSE.equals(m.get("completo"))).count());
        s.put("solicitudesPendientes", solicitudes == null ? null : contarEstado(solicitudes, "pendiente"));
        // Estado clínico: al-dia | tratamiento | pendiente (vacío cuenta como pendiente, igual que el frontend)
        Map<String, Long> porEstado = new LinkedHashMap<>();
        porEstado.put("alDia", mascotas.stream().filter(m -> "al-dia".equalsIgnoreCase(texto(m.get("estado")))).count());
        porEstado.put("tratamiento", mascotas.stream().filter(m -> "tratamiento".equalsIgnoreCase(texto(m.get("estado")))).count());
        porEstado.put("pendiente", mascotas.stream().filter(m -> "pendiente".equalsIgnoreCase(vacioComo(m.get("estado"), "pendiente"))).count());
        s.put("porEstado", porEstado);
        s.put("porEspecie", contarPor(mascotas, m -> vacioComo(m.get("especie"), "Sin especificar")));
        return s;
    }

    Map<String, Object> seccionCitas(List<Map<String, Object>> citas) {
        Map<String, Object> s = new LinkedHashMap<>();
        s.put("total", citas.size());
        s.put("pendientes", contarEstado(citas, "pendiente"));
        s.put("confirmadas", contarEstado(citas, "confirmad"));
        s.put("canceladas", contarEstado(citas, "cancelad"));
        s.put("porServicio", contarPor(citas, c -> vacioComo(c.get("servicio"), "Sin especificar")).stream().limit(6).toList());
        return s;
    }

    /* =========================================================
       AUXILIARES
    ========================================================= */

    /** Agrupa y cuenta: [{ nombre: "Perro", cantidad: 5 }, ...] ordenado de mayor a menor. */
    List<Map<String, Object>> contarPor(List<Map<String, Object>> datos, Function<Map<String, Object>, String> clave) {
        Map<String, Long> conteo = datos.stream().collect(Collectors.groupingBy(clave, Collectors.counting()));
        return conteo.entrySet().stream()
            .sorted(Map.Entry.<String, Long>comparingByValue().reversed().thenComparing(Map.Entry.<String, Long>comparingByKey()))
            .map(e -> fila("nombre", e.getKey(), "cantidad", e.getValue()))
            .toList();
    }

    /** Cuenta los que tienen un estado que empieza con el prefijo (sin importar mayúsculas). */
    long contarEstado(List<Map<String, Object>> datos, String prefijo) {
        return datos.stream().filter(d -> texto(d.get("estado")).toLowerCase().startsWith(prefijo)).count();
    }

    /** El rol viene de auth-service como objeto { nombre: "ADMIN" } (o como texto en otros casos). */
    String rolDe(Map<String, Object> usuario) {
        Object rol = usuario.get("rol");
        if (rol instanceof Map<?, ?> mapa) return texto(mapa.get("nombre")).toUpperCase();
        return texto(rol).toUpperCase();
    }

    private boolean registradoEstaSemana(Map<String, Object> usuario) {
        try {
            LocalDateTime fecha = LocalDateTime.parse(texto(usuario.get("fechaRegistro")));
            return fecha.isAfter(LocalDateTime.now().minusDays(7));
        } catch (Exception e) {
            return false;
        }
    }

    private List<Map<String, Object>> ultimosRegistrados(List<Map<String, Object>> usuarios, int cantidad) {
        return usuarios.stream()
            .sorted(Comparator.comparing((Map<String, Object> u) -> texto(u.get("fechaRegistro"))).reversed())
            .limit(cantidad)
            .map(u -> fila(
                "id", u.get("id"),
                "nombre", texto(u.get("nombre")) + " " + texto(u.get("apellidos")),
                "correo", texto(u.get("correo")),
                "comuna", texto(u.get("comuna")),
                "rol", rolDe(u),
                "fechaRegistro", texto(u.get("fechaRegistro"))))
            .toList();
    }

    private Object marcarCaido(List<String> caidos, String servicio) {
        caidos.add(servicio);
        return null;
    }

    private void registrar(TipoReporte tipo, String correoAdmin, String resumen, List<String> caidos) {
        ReporteGenerado r = new ReporteGenerado();
        r.setTipo(tipo);
        r.setGeneradoPor(correoAdmin);
        r.setResumen(resumen);
        r.setServiciosNoDisponibles(String.join(", ", caidos));
        reporteRepository.save(r);
    }

    private String resumen(Optional<Integer> usuarios, Optional<Integer> mascotas, Optional<Integer> citas) {
        return usuarios.map(n -> n + " usuarios").orElse("usuarios no disponibles") + " · "
            + mascotas.map(n -> n + " mascotas").orElse("mascotas no disponibles") + " · "
            + citas.map(n -> n + " citas").orElse("citas no disponibles");
    }

    /** Crea un mapa ordenado a partir de pares clave, valor, clave, valor... */
    private static Map<String, Object> fila(Object... pares) {
        Map<String, Object> m = new LinkedHashMap<>();
        for (int i = 0; i < pares.length; i += 2) m.put((String) pares[i], pares[i + 1]);
        return m;
    }

    private static String texto(Object valor) {
        return valor == null ? "" : Objects.toString(valor).trim();
    }

    private static String vacioComo(Object valor, String porDefecto) {
        String t = texto(valor);
        return t.isEmpty() ? porDefecto : t;
    }
}