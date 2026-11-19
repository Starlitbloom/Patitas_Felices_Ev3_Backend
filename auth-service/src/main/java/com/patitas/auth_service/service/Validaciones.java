package com.patitas.auth_service.service;

import java.util.List;


/**
 * Mismas reglas de validación que src/utils/validaciones.ts del frontend.
 * Se repiten en el backend porque el frontend se puede saltar
 * (por ejemplo, llamando la API directo desde Swagger o Postman).
 */
public final class Validaciones {

    private static final List<String> DOMINIOS_PERMITIDOS = List.of("duoc.cl", "profesor.duoc.cl", "gmail.com");

    private Validaciones() {}

    /** Quita puntos, guion y espacios: "11.111.111-1" → "111111111". */
    public static String limpiarRun(String run) {
        return run == null ? "" : run.replace(".", "").replace("-", "").trim().toUpperCase();
    }

    /** Deja el RUN en formato estándar: "111111111" → "11111111-1". */
    public static String formatearRun(String run) {
        String limpio = limpiarRun(run);
        return limpio.substring(0, limpio.length() - 1) + "-" + limpio.charAt(limpio.length() - 1);
    }

    /** Algoritmo módulo 11 del dígito verificador chileno. */
    public static boolean digitoVerificadorValido(String runLimpio) {
        String cuerpo = runLimpio.substring(0, runLimpio.length() - 1);
        char dv = runLimpio.charAt(runLimpio.length() - 1);
        if (!cuerpo.matches("\\d+")) return false;

        int suma = 0;
        int multiplo = 2;
        for (int i = cuerpo.length() - 1; i >= 0; i--) {
            suma += Character.getNumericValue(cuerpo.charAt(i)) * multiplo;
            multiplo = multiplo == 7 ? 2 : multiplo + 1;
        }
        int resto = 11 - (suma % 11);
        char esperado = resto == 11 ? '0' : resto == 10 ? 'K' : (char) ('0' + resto);
        return dv == esperado;
    }

    public static void validarRun(String run) {
        if (run == null || run.isBlank()) throw new NegocioException("run", "El RUN es obligatorio.");
        String limpio = limpiarRun(run);
        if (limpio.length() < 7 || limpio.length() > 9) {
            throw new NegocioException("run", "El RUN debe tener entre 7 y 9 caracteres, sin puntos ni guion.");
        }
        if (!digitoVerificadorValido(limpio)) throw new NegocioException("run", "El RUN ingresado no es válido.");
    }

    public static void validarCorreo(String correo) {
        if (correo == null || correo.isBlank()) throw new NegocioException("correo", "El correo es obligatorio.");
        String valor = correo.trim();
        if (valor.length() > 100) throw new NegocioException("correo", "El correo no puede superar los 100 caracteres.");
        String[] partes = valor.split("@");
        if (partes.length != 2 || partes[0].isEmpty()) throw new NegocioException("correo", "Ingresa un correo válido.");
        if (!DOMINIOS_PERMITIDOS.contains(partes[1].toLowerCase())) {
            throw new NegocioException("correo", "Solo se aceptan correos @duoc.cl, @profesor.duoc.cl o @gmail.com.");
        }
    }

    public static void validarPassword(String password) {
        if (password == null || password.isEmpty()) throw new NegocioException("password", "La contraseña es obligatoria.");
        if (password.length() < 4 || password.length() > 10) {
            throw new NegocioException("password", "La contraseña debe tener entre 4 y 10 caracteres.");
        }
    }

    public static void validarRequerido(String valor, String campo, String texto, int max) {
        if (valor == null || valor.isBlank()) throw new NegocioException(campo, texto + " es obligatorio.");
        if (valor.length() > max) throw new NegocioException(campo, texto + " no puede superar los " + max + " caracteres.");
    }

    public static void validarTelefono(String telefono) {
        if (telefono == null || telefono.isBlank()) return; // Opcional
        if (!telefono.trim().matches("[0-9+ ]{8,15}")) {
            throw new NegocioException("telefono", "El teléfono solo puede tener números, espacios o '+' (8 a 15 caracteres).");
        }
    }
}
