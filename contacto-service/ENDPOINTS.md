# contacto-service — Endpoints (puerto 8088)

Base: `http://localhost:8088/api/v1`
Swagger: http://localhost:8088/swagger-ui/index.html

| Método | Ruta | Quién puede | Descripción |
|---|---|---|---|
| POST | /mensajes | Público | Enviar mensaje del formulario de contacto |
| GET | /mensajes?estado= | ADMIN, RECEPCIONISTA | Bandeja (filtro opcional por estado) |
| GET | /mensajes/mis-mensajes | Con sesión | Mensajes que envió el usuario y sus respuestas |
| GET | /mensajes/estadisticas | ADMIN | Total y cantidad por estado |
| GET | /mensajes/{id} | Personal o dueño del mensaje | Ver un mensaje |
| PUT | /mensajes/{id}/estado | ADMIN, RECEPCIONISTA | `{ "estado": "LEIDO" }` o ARCHIVADO |
| PUT | /mensajes/{id}/responder | ADMIN, RECEPCIONISTA | `{ "respuesta": "..." }` → queda RESPONDIDO |
| DELETE | /mensajes/{id} | ADMIN | Eliminar |

Estados: `PENDIENTE` → `LEIDO` → `RESPONDIDO` / `ARCHIVADO`.

El token se obtiene en **auth-service** (`POST http://localhost:8081/api/v1/auth/login`).
Este servicio solo lo valida con la misma `jwt.secret`.

## Comunicación con otros microservicios (WebClient)
Carpeta `webclient/NotificacionClient.java`.
Cuando el personal **responde** un mensaje que se envió con sesión iniciada,
contacto-service llama a `POST http://localhost:8089/api/v1/notificaciones`
(notificacion-service) reenviando el token, y al usuario le aparece el aviso
"Respondimos tu mensaje" en la campanita.
Si notificacion-service está apagado, el mensaje igual queda respondido.
