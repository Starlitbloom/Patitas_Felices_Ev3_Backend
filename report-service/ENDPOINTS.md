# report-service — Endpoints (puerto 8090)

Base: http://localhost:8090/api/v1
Swagger: http://localhost:8090/swagger-ui/index.html

| Método | Ruta | Quién puede | Descripción |
|---|---|---|---|
| GET | /reportes/dashboard | ADMIN, RECEPCIONISTA | KPIs del inicio del panel (usuarios solo para ADMIN) |
| GET | /reportes/completo | ADMIN | Datos de la página Reportes (gráficos y tablas) |
| GET | /reportes/exportar | ADMIN | Filas para el Excel de 3 hojas (Usuarios, Mascotas, Citas) |
| GET | /reportes/historial | ADMIN | Quién generó cada reporte y cuándo |
| GET | /reportes/historial/{id} | ADMIN | Un registro del historial |
| DELETE | /reportes/historial/{id} | ADMIN | Eliminar un registro del historial |

## Microservicios que consulta (carpeta webclient/)
| Cliente | Servicio | Endpoint que llama | Para qué |
|---|---|---|---|
| AuthClient | auth-service :8081 | GET /usuarios | Usuarios por rol, región, comuna, nuevos, últimos registrados |
| MascotaClient | mascota-service :8082 | GET /mascotas, GET /solicitudes | Mascotas por especie, solicitudes pendientes |
| CitaClient | cita-service :8086 | GET /citas | Citas por estado y por servicio |
| ContactoClient | contacto-service :8088 | GET /mensajes/estadisticas | Mensajes por estado |
| NotificacionClient | notificacion-service :8089 | GET /notificaciones/estadisticas | Avisos enviados / no leídos |

- Todos reenvían el token del usuario (Authorization: Bearer ...).
- Si un servicio está apagado o tarda más de 3 segundos, su sección queda en null
  y aparece en serviciosNoDisponibles. El reporte *nunca se cae* por otro servicio.

## Contrato que deben cumplir mascota-service y cita-service
- GET /api/v1/mascotas → lista con nombre, especie, raza, estado (al-dia, tratamiento, pendiente), completo (true/false), correoDueno
- GET /api/v1/solicitudes → lista con estado (PENDIENTE, APROBADA, RECHAZADA)
- GET /api/v1/citas → lista con fecha, servicio, motivo, estado (PENDIENTE, CONFIRMADA, CANCELADA...), correoUsuario
- Los tres deben permitir el acceso a ADMIN y RECEPCIONISTA.