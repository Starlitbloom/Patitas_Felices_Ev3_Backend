# notificacion-service — Endpoints (puerto 8089)

Base: `http://localhost:8089/api/v1`
Swagger: http://localhost:8089/swagger-ui/index.html

| Método | Ruta | Quién puede | Descripción |
|---|---|---|---|
| POST | /notificaciones | Con sesión (personal → a cualquiera; cliente → solo a sí mismo) | Crear aviso |
| GET | /notificaciones/mis-notificaciones?soloNoLeidas= | Con sesión | Mis avisos |
| GET | /notificaciones/mis-notificaciones/no-leidas | Con sesión | `{ "cantidad": 3 }` para la campanita |
| PUT | /notificaciones/{id}/leer | Destinatario | Marcar como leída |
| PUT | /notificaciones/mis-notificaciones/leer-todas | Con sesión | Marcar todas → `{ "marcadas": 4 }` |
| DELETE | /notificaciones/{id} | Destinatario o ADMIN | Eliminar |
| GET | /notificaciones | ADMIN | Todas |
| GET | /notificaciones/estadisticas | ADMIN | Total, no leídas y por tipo |

Tipos: `CITA`, `MASCOTA`, `SOLICITUD`, `CONTACTO`, `SISTEMA`.

## Quién lo llama por WebClient
| Servicio | Cuándo | Aviso |
|---|---|---|
| contacto-service | El personal responde un mensaje de un usuario con cuenta | "Respondimos tu mensaje" (CONTACTO) |
| mascota-service | Se aprueba/rechaza una solicitud de cambio | (pendiente) |
| cita-service | Se confirma/cancela una cita | (pendiente) |
