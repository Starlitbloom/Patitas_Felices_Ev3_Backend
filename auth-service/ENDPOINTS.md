# auth-service — Endpoints (puerto 8081)

Base: `http://localhost:8081/api/v1`

| Método | Ruta | Quién puede | Descripción |
|---|---|---|---|
| POST | /auth/login | Público | Login → token + refreshToken + usuario |
| POST | /auth/registro | Público | Crea cuenta CLIENTE y deja la sesión iniciada |
| POST | /auth/refresh | Público (con refreshToken) | Renueva el token de acceso |
| GET | /auth/me | Con sesión | Usuario dueño del token |
| GET | /roles | Público | Lista de roles |
| GET | /usuarios | ADMIN | Lista de usuarios |
| GET | /usuarios/estadisticas | ADMIN | KPIs para el dashboard |
| GET | /usuarios/{id} | ADMIN, RECEPCIONISTA o el mismo usuario | Usuario por id |
| GET | /usuarios/correo/{correo} | ADMIN, RECEPCIONISTA o el mismo usuario | Usuario por correo |
| POST | /usuarios | ADMIN | Crear usuario con cualquier rol |
| PUT | /usuarios/{id} | ADMIN o el mismo usuario | Actualizar (solo ADMIN cambia rol/estado) |
| DELETE | /usuarios/{id} | ADMIN | Eliminar (no las cuentas fijas) |

## Errores
Todos los errores responden el mismo formato:
```json
{ "status": 409, "campo": "correo", "mensaje": "Ya existe una cuenta registrada con este correo." }
```
`campo` indica qué input del formulario marcar en rojo.

## Ejemplo de login
```json
POST /api/v1/auth/login
{ "correo": "ma.calfileo@duoc.cl", "password": "1234" }
```
Respuesta:
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiJ9...",
  "tipo": "Bearer",
  "expiraEnMs": 1800000,
  "usuario": { "id": 1, "nombre": "Maria", "correo": "ma.calfileo@duoc.cl",
               "rol": { "id": 1, "nombre": "ADMIN", "descripcion": "Administrador" }, "activo": true, ... }
}
```

## Crear usuario con rol (admin)
```json
POST /api/v1/usuarios
{ "run": "12.345.678-5", "nombre": "Pedro", "apellidos": "Pérez", "correo": "pedro@duoc.cl",
  "password": "1234", "rol": { "nombre": "RECEPCIONISTA" }, "activo": true }
```
La contraseña nunca aparece en las respuestas.
