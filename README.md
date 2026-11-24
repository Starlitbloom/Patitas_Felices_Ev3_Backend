# 🐾 Patitas Felices — Backend (EP3)

Backend de microservicios de la **Veterinaria Patitas Felices**, desarrollado para la Evaluación Parcial 3 de **Desarrollo Fullstack II** (Duoc UC).

El sistema está dividido en **10 microservicios Spring Boot**, cada uno con su propia base de datos MySQL. Todos comparten el mismo mecanismo de seguridad: **auth-service** emite tokens **JWT** y el resto de los servicios los valida con una clave compartida, aplicando permisos según el **rol** del usuario (`ADMIN`, `RECEPCIONISTA`, `CLIENTE`).

---

## 📑 Contenido

1. [Equipo](#-equipo)
2. [Tecnologías](#-tecnologías)
3. [Arquitectura](#-arquitectura)
4. [Microservicios](#-microservicios)
5. [Requisitos previos](#-requisitos-previos)
6. [Instalación y ejecución](#-instalación-y-ejecución)
7. [Autenticación y roles (JWT)](#-autenticación-y-roles-jwt)
8. [Usuarios de prueba](#-usuarios-de-prueba)
9. [Documentación Swagger](#-documentación-swagger)
10. [Endpoints por servicio](#-endpoints-por-servicio)
11. [Formato de errores](#-formato-de-errores)
12. [Pruebas unitarias](#-pruebas-unitarias)
13. [Estructura de cada microservicio](#-estructura-de-cada-microservicio)
14. [Solución de problemas](#-solución-de-problemas)

---

## 👥 Equipo

| Integrante | Usuario GitHub | Microservicios |
|---|---|---|
| María Calfileo | Starlitbloom | auth-service, contacto-service, notificacion-service, report-service |
| Rocío Cruces | ChioBlossom | novedad-service, veterinario-service, cita-service |
| Fanny Chamorro| fanny698 | mascota-service, historial-service, servicio-clinico-service |

---

## 🛠 Tecnologías

| Tecnología | Versión | Uso |
|---|---|---|
| Java | 21 | Lenguaje |
| Spring Boot | 3.4.6 | Framework base de todos los servicios |
| Spring Web | — | API REST |
| Spring Data JPA + Hibernate | 6.6 | Acceso a datos; las tablas se crean con `ddl-auto=update` |
| Spring Security | — | Protección de endpoints y permisos por rol (`@PreAuthorize`) |
| jjwt | 0.11.5 | Creación y validación de tokens JWT (HS256) |
| Bean Validation | — | Validación de datos de entrada (`@NotBlank`, `@Size`, `@PastOrPresent`…) |
| springdoc-openapi | 2.8.8 | Documentación Swagger UI |
| Lombok | — | Getters, setters y constructores generados |
| MySQL | 8 | Base de datos (una por microservicio) |
| JUnit 5 + Mockito | — | Pruebas unitarias |
| Maven Wrapper | — | Compilación y ejecución (`mvnw`) |

---

## 🏗 Arquitectura

```
                     ┌──────────────────────────┐
                     │  Frontend React (Vite)   │
                     │  http://localhost:5173   │
                     └────────────┬─────────────┘
                                  │  HTTP + JSON · Authorization: Bearer <token>
                     ┌────────────▼─────────────┐        ┌──────────────────────┐
                     │       api-gateway        │───────►│    eureka-server     │
                     │  http://localhost:8080   │consulta│ http://localhost:8761│
                     │ /auth/** → auth-service  │        │ registro de servicios│
                     │ /cita/** → cita-service… │        └──────────▲───────────┘
                     └────────────┬─────────────┘                   │ cada servicio
        ┌─────────────────────────┼────────────────────────────┐    │ se registra
┌───────▼────────┐   ┌────────────▼──────────┐   ┌─────────────▼────┴────────┐
│  auth-service  │   │   mascota-service     │   │  historial-service ...    │
│     :8081      │   │        :8082          │   │  (resto de servicios)     │
│ emite el JWT   │   │  valida el JWT        │   │  validan el JWT           │
└───────┬────────┘   └───────────┬───────────┘   └─────────────┬─────────────┘
   ┌────▼────┐              ┌────▼─────┐                  ┌────▼─────┐
   │ auth_db │              │mascota_db│                  │ ..._db   │
   └─────────┘              └──────────┘                  └──────────┘
                     MySQL 8 (contenedor Docker, puerto 3306)
```

- **eureka-server (8761):** cada microservicio se registra al iniciar; en http://localhost:8761 se ve cuáles están arriba.
- **api-gateway (8080):** punto de entrada único. Recibe `/<servicio>/**`, quita el primer segmento y lo reenvía al servicio registrado en Eureka. Ejemplo: `http://localhost:8080/auth/api/v1/auth/login` → `auth-service /api/v1/auth/login`.
- Todas las rutas de los microservicios usan el prefijo `/api/v1`. Los servicios también responden directo en su puerto (útil para Swagger y pruebas).
- Llamadas entre servicios: `report-service` consulta a auth, mascota, cita, contacto y notificación; `contacto-service` avisa a `notificacion-service`; `cita-service` valida el veterinario en `veterinario-service`.

**Flujo de una petición protegida**

1. El usuario inicia sesión en `auth-service` (`POST /api/v1/auth/login`) y recibe un **token de acceso** (30 min) y un **refresh token** (7 días).
2. El frontend envía el token en cada petición: `Authorization: Bearer <token>`.
3. Cada microservicio tiene un filtro (`JwtRequestFilter`) que verifica la **firma**, la **expiración** y que el token sea de tipo `access`, y luego registra al usuario con su rol (`ROLE_ADMIN`, `ROLE_RECEPCIONISTA` o `ROLE_CLIENTE`).
4. `@PreAuthorize` en cada endpoint decide si ese rol puede realizar la acción.
5. Si no hay token o es inválido → `401`. Si el rol no alcanza → `403`.

Los servicios son **stateless**: no guardan sesión; toda la información del usuario viaja dentro del token.

---

## 📦 Microservicios

| Servicio | Puerto | Base de datos | Ruta base | Responsable | Descripción |
|---|---|---|---|---|---|
| auth-service | 8081 | `auth_db` | `/api/v1/auth`, `/api/v1/usuarios`, `/api/v1/roles` | María | Login, registro, renovación de token y gestión de usuarios y roles |
| mascota-service | 8082 | `mascota_db` | `/api/v1/mascotas`, `/api/v1/solicitudes` | fanny698 | Fichas de mascotas y solicitudes de cambio de datos |
| historial-service | 8083 | `historial_db` | `/api/v1/historial` | fanny698 | Atenciones clínicas y vacunas de cada mascota |
| servicio-clinico-service | 8084 | `servicio_clinico_db` | `/api/v1/servicios` | fanny698 | Catálogo de servicios clínicos (cirugías, exámenes, etc.) |
| veterinario-service | 8085 | `veterinario_db` | `/api/veterinarios` | Rocío | Equipo de veterinarios |
| cita-service | 8086 | `cita_db` | `/api/citas` | Rocío | Agenda y gestión de citas |
| novedad-service | 8087 | `novedad_db` | `/api/novedades` | Rocío | Noticias y novedades de la clínica |
| contacto-service | 8088 | `contacto_db` | `/api/v1/mensajes` | María | Mensajes del formulario de contacto |
| notificacion-service | 8089 | `notificacion_db` | `/api/v1/notificaciones` | María | Notificaciones para los usuarios |
| report-service | 8090 | `report_db` | `/api/v1/reportes` | María | Reportes y estadísticas para el panel de administración |
| eureka-server | 8761 | — | — | Equipo | Registro de microservicios (Netflix Eureka) |
| api-gateway | 8080 | — | `/auth`, `/mascota`, `/historial`, `/servicio-clinico`, `/veterinario`, `/cita`, `/novedad`, `/contacto`, `/notificacion`, `/reporte` | Equipo | Punto de entrada único (Spring Cloud Gateway) |

---

## ✅ Requisitos previos

- **JDK 21** o superior (`java -version`)
- **Docker Desktop** (para MySQL)
- **Git**
- Opcional: **VS Code** con el *Extension Pack for Java* y **Postman**

No es necesario instalar Maven: cada servicio trae su propio **Maven Wrapper** (`mvnw` / `mvnw.cmd`).

---

## 🚀 Instalación y ejecución

### 1. Clonar el repositorio

```bash
git clone https://github.com/Starlitbloom/Patitas_Felices_Ev3_Backend.git
cd Patitas_Felices_Ev3_Backend
```

### 2. Levantar MySQL en Docker

Solo la primera vez (crea el contenedor `mysql-patitas`, usuario `root` sin contraseña):

```bash
docker run -d --name mysql-patitas -p 3306:3306 -e MYSQL_ALLOW_EMPTY_PASSWORD=yes mysql:8.0
```

Las veces siguientes basta con encenderlo (con Docker Desktop abierto):

```bash
docker start mysql-patitas
```

### 3. Crear las bases de datos

Las tablas las crea Hibernate solo, pero las bases deben existir. Se crean todas con un comando:

```bash
docker exec mysql-patitas mysql -uroot -e "CREATE DATABASE IF NOT EXISTS auth_db; CREATE DATABASE IF NOT EXISTS mascota_db; CREATE DATABASE IF NOT EXISTS historial_db; CREATE DATABASE IF NOT EXISTS servicio_clinico_db; CREATE DATABASE IF NOT EXISTS veterinario_db; CREATE DATABASE IF NOT EXISTS cita_db; CREATE DATABASE IF NOT EXISTS novedad_db; CREATE DATABASE IF NOT EXISTS contacto_db; CREATE DATABASE IF NOT EXISTS notificacion_db; CREATE DATABASE IF NOT EXISTS report_db;"
```

### 4. Ejecutar los servicios

Cada servicio se ejecuta en **su propia terminal**, desde **su propia carpeta**. Orden recomendado:

1. **eureka-server** (para que los demás se registren).
2. **auth-service**, porque es el que entrega los tokens.
3. El resto de los microservicios.
4. **api-gateway** (al final; en http://localhost:8761 se revisa que todos estén registrados).

```bash
cd eureka-server
./mvnw spring-boot:run        # Windows: .\mvnw.cmd spring-boot:run
```

Luego auth-service:

```bash
cd auth-service
./mvnw spring-boot:run        # Windows: .\mvnw.cmd spring-boot:run
```

Luego, en otras terminales, los que se necesiten:

```bash
cd mascota-service && ./mvnw spring-boot:run
cd historial-service && ./mvnw spring-boot:run
cd servicio-clinico-service && ./mvnw spring-boot:run
# ...y así con el resto
```

Al arrancar, cada servicio muestra `Started ...Application` y carga **datos de ejemplo** si sus tablas están vacías (no se duplican al reiniciar).

---

## 🔐 Autenticación y roles (JWT)

### Obtener un token

```http
POST http://localhost:8081/api/v1/auth/login
Content-Type: application/json

{ "correo": "ma.calfileo@duoc.cl", "password": "1234" }
```

Respuesta (resumida):

```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiJ9...",
  "tipo": "Bearer",
  "expiraEnMs": 1800000,
  "usuario": { "id": 1, "nombre": "María", "correo": "ma.calfileo@duoc.cl", "rol": { "nombre": "ADMIN" } }
}
```

### Usar el token

Enviar el header en cada petición a cualquier microservicio:

```
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
```

### Contenido del token

| Claim | Ejemplo | Uso |
|---|---|---|
| `sub` | `ma.calfileo@duoc.cl` | Correo del usuario (identifica al dueño de los datos) |
| `id` | `1` | Id del usuario en auth-service |
| `nombre` | `María` | Nombre para mostrar |
| `rol` | `ADMIN` | Rol con el que se evalúan los permisos |
| `tipo` | `access` / `refresh` | Solo los `access` sirven para llamar a la API |
| `exp` | — | Expiración: 30 min (access) y 7 días (refresh) |

### Renovar y verificar la sesión

| Endpoint | Uso |
|---|---|
| `POST /api/v1/auth/refresh` | Entrega un token de acceso nuevo a partir del refresh token |
| `GET /api/v1/auth/me` | Devuelve el usuario dueño del token (el frontend lo usa al recargar la página para mantener la sesión) |

### Roles

| Rol | Descripción | Permisos generales |
|---|---|---|
| `ADMIN` | Administrador de la clínica | Acceso total, incluido **eliminar** |
| `RECEPCIONISTA` | Personal de recepción | Crear y editar fichas, atenciones, vacunas; aprobar solicitudes |
| `CLIENTE` | Dueño/a de mascota | Ver **solo sus** mascotas, solicitar cambios, consultar información |

---

## 🧪 Usuarios de prueba

Los crea auth-service automáticamente al iniciar:

| Correo | Contraseña | Rol |
|---|---|---|
| ma.calfileo@duoc.cl | 1234 | ADMIN |
| ro.cruces@duoc.cl | 1234 | ADMIN |
| jo.medina@duoc.cl | 1234 | RECEPCIONISTA |
| be.soto@duoc.cl | perro12 | CLIENTE |
| to.herrera@duoc.cl | loro99 | CLIENTE |

---

## 📘 Documentación Swagger

Con cada servicio corriendo, su documentación interactiva está en:

| Servicio | Swagger UI |
|---|---|
| auth-service | http://localhost:8081/swagger-ui.html |
| mascota-service | http://localhost:8082/swagger-ui.html |
| historial-service | http://localhost:8083/swagger-ui.html |
| servicio-clinico-service | http://localhost:8084/swagger-ui.html |
| veterinario-service | http://localhost:8085/swagger-ui.html |
| cita-service | http://localhost:8086/swagger-ui.html |
| novedad-service | http://localhost:8087/swagger-ui.html |
| contacto-service | http://localhost:8088/swagger-ui.html |
| notificacion-service | http://localhost:8089/swagger-ui.html |
| report-service | http://localhost:8090/swagger-ui.html |

**Cómo probar un endpoint protegido en Swagger**

1. Hacer login en el Swagger de auth-service y copiar el `token`.
2. En el Swagger del otro servicio, pulsar **Authorize**, pegar el token (sin escribir `Bearer`) y confirmar.
3. Usar **Try it out → Execute** en cualquier endpoint.

---

## 🔗 Endpoints por servicio

Leyenda de permisos: **Público** = sin token · **Sesión** = cualquier usuario con token · **A** = ADMIN · **R** = RECEPCIONISTA · **C** = CLIENTE

### auth-service (8081)

| Método | Ruta | Permiso | Descripción |
|---|---|---|---|
| POST | `/api/v1/auth/login` | Público | Iniciar sesión |
| POST | `/api/v1/auth/registro` | Público | Registrar una cuenta (rol CLIENTE) |
| POST | `/api/v1/auth/refresh` | Público | Renovar el token de acceso |
| GET | `/api/v1/auth/me` | Sesión | Usuario de la sesión actual |
| GET | `/api/v1/roles` | Público | Listar roles |
| GET | `/api/v1/usuarios` | A | Listar usuarios |
| GET | `/api/v1/usuarios/estadisticas` | A | Estadísticas de usuarios |
| GET | `/api/v1/usuarios/{id}` | Sesión | Obtener usuario |
| GET | `/api/v1/usuarios/correo/{correo}` | Sesión | Buscar usuario por correo |
| POST | `/api/v1/usuarios` | A | Crear usuario |
| PUT | `/api/v1/usuarios/{id}` | Sesión | Actualizar usuario |
| DELETE | `/api/v1/usuarios/{id}` | A | Eliminar usuario |

### mascota-service (8082)

| Método | Ruta | Permiso | Descripción |
|---|---|---|---|
| GET | `/api/v1/mascotas?especie=&texto=` | Sesión | Listar mascotas. **A/R ven todas; C ve solo las suyas** |
| GET | `/api/v1/mascotas/{id}` | Sesión | Detalle (un C solo puede ver las suyas → `403`) |
| POST | `/api/v1/mascotas` | Sesión | Crear mascota (si es C, queda a su nombre) |
| PUT | `/api/v1/mascotas/{id}` | A, R | Actualizar mascota |
| DELETE | `/api/v1/mascotas/{id}` | A | Eliminar mascota y sus solicitudes |
| POST | `/api/v1/mascotas/{id}/solicitudes` | Sesión | Solicitar cambio de datos |
| GET | `/api/v1/solicitudes?estado=` | A, R | Listar solicitudes de cambio |
| PUT | `/api/v1/solicitudes/{id}/aprobar` | A, R | Aprobar: aplica los cambios a la mascota |
| PUT | `/api/v1/solicitudes/{id}/rechazar` | A, R | Rechazar la solicitud |

**Reglas de negocio**

- Un cliente solo ve y modifica **sus** mascotas (se compara el correo del token con `correoDueno`).
- Una mascota está **completa** cuando tiene raza, sexo, peso e imagen.
- Si la mascota está **incompleta**, los cambios del dueño se aplican directo (solicitud `aprobada`).
- Si está **completa**, el cambio queda `pendiente` hasta que el personal lo apruebe o rechace.
- Solo puede haber **una solicitud pendiente** por mascota (si no → `409`).

### historial-service (8083)

| Método | Ruta | Permiso | Descripción |
|---|---|---|---|
| GET | `/api/v1/historial/mascotas/{mascotaId}/atenciones` | Sesión | Atenciones de una mascota (más recientes primero) |
| POST | `/api/v1/historial/mascotas/{mascotaId}/atenciones` | A, R | Registrar atención |
| GET | `/api/v1/historial/atenciones/{id}` | Sesión | Detalle de una atención |
| PUT | `/api/v1/historial/atenciones/{id}` | A, R | Actualizar atención |
| DELETE | `/api/v1/historial/atenciones/{id}` | A | Eliminar atención |
| GET | `/api/v1/historial/mascotas/{mascotaId}/vacunas` | Sesión | Vacunas de una mascota con su **estado calculado** |
| POST | `/api/v1/historial/mascotas/{mascotaId}/vacunas` | A, R | Registrar vacuna |
| GET | `/api/v1/historial/vacunas/{id}` | Sesión | Detalle de una vacuna |
| PUT | `/api/v1/historial/vacunas/{id}` | A, R | Actualizar vacuna |
| DELETE | `/api/v1/historial/vacunas/{id}` | A | Eliminar vacuna |

**Reglas de negocio**

- El **estado de la vacuna** no se guarda: se calcula cada vez con la fecha actual.
  - Refuerzo ya vencido → `atrasada`
  - Vence en 30 días o menos → `proxima`
  - Más lejos → `al-dia`
- El **próximo refuerzo** debe ser posterior a la fecha de aplicación (si no → `409`).
- La fecha de una atención o vacuna **no puede ser futura** (→ `400`).
- Cada atención puede registrar los **servicios realizados** (ej.: "Hemograma completo").

### servicio-clinico-service (8084)

| Método | Ruta | Permiso | Descripción |
|---|---|---|---|
| GET | `/api/v1/servicios?especie=&grupo=` | **Público** | Catálogo de servicios activos, filtrable |
| GET | `/api/v1/servicios/{id}` | **Público** | Detalle de un servicio |
| POST | `/api/v1/servicios` | A | Crear servicio |
| PUT | `/api/v1/servicios/{id}` | A | Actualizar servicio (permite desactivarlo) |
| DELETE | `/api/v1/servicios/{id}` | A | Eliminar servicio |

**Reglas de negocio**

- El **nombre** del servicio no se puede repetir (→ `409`).
- Las especies válidas son **Perro, Gato, Conejo y Ave** (otra → `409`).
- Un servicio **sin especies** sirve para todas.
- El listado público muestra solo los servicios **activos**, ordenados por grupo y nombre.

### veterinario-service (8085)

| Método | Ruta | Permiso | Descripción |
|---|---|---|---|
| GET | `/api/veterinarios` | Público | Listar veterinarios |
| GET | `/api/veterinarios/{id}` | Público | Obtener veterinario |
| POST | `/api/veterinarios` | A | Crear veterinario |
| PUT | `/api/veterinarios/{id}` | A | Actualizar veterinario |
| DELETE | `/api/veterinarios/{id}` | A | Eliminar veterinario |

### cita-service (8086)

| Método | Ruta | Permiso | Descripción |
|---|---|---|---|
| GET | `/api/citas` | A | Listar todas las citas |
| GET | `/api/citas/mis-citas` | Sesión | Citas del usuario de la sesión |
| GET | `/api/citas/{id}` | Sesión | Detalle de una cita |
| POST | `/api/citas` | Sesión | Solicitar una cita |
| PUT | `/api/citas/{id}` | Sesión | Modificar una cita |
| PATCH | `/api/citas/{id}/cancelar` | Sesión | Cancelar una cita |
| PATCH | `/api/citas/{id}/estado` | A | Cambiar el estado de una cita |
| DELETE | `/api/citas/{id}` | A | Eliminar una cita |

### novedad-service (8087)

| Método | Ruta | Permiso | Descripción |
|---|---|---|---|
| GET | `/api/novedades` | Público | Listar novedades |
| GET | `/api/novedades/{id}` | Público | Obtener novedad |
| POST | `/api/novedades` | A | Crear novedad |
| PUT | `/api/novedades/{id}` | A | Actualizar novedad |
| DELETE | `/api/novedades/{id}` | A | Eliminar novedad |

### contacto-service (8088)

| Método | Ruta | Permiso | Descripción |
|---|---|---|---|
| POST | `/api/v1/mensajes` | Público | Enviar un mensaje de contacto |
| GET | `/api/v1/mensajes` | A, R | Listar mensajes |
| GET | `/api/v1/mensajes/mis-mensajes` | Sesión | Mensajes del usuario de la sesión |
| GET | `/api/v1/mensajes/estadisticas` | A | Estadísticas de mensajes |
| GET | `/api/v1/mensajes/{id}` | Sesión | Detalle de un mensaje |
| PUT | `/api/v1/mensajes/{id}/estado` | A, R | Cambiar estado |
| PUT | `/api/v1/mensajes/{id}/responder` | A, R | Responder un mensaje |
| DELETE | `/api/v1/mensajes/{id}` | A | Eliminar mensaje |

### notificacion-service (8089)

| Método | Ruta | Permiso | Descripción |
|---|---|---|---|
| POST | `/api/v1/notificaciones` | Sesión | Crear notificación |
| GET | `/api/v1/notificaciones/mis-notificaciones` | Sesión | Notificaciones del usuario |
| GET | `/api/v1/notificaciones/mis-notificaciones/no-leidas` | Sesión | Notificaciones no leídas |
| PUT | `/api/v1/notificaciones/{id}/leer` | Sesión | Marcar como leída |
| PUT | `/api/v1/notificaciones/mis-notificaciones/leer-todas` | Sesión | Marcar todas como leídas |
| DELETE | `/api/v1/notificaciones/{id}` | Sesión | Eliminar notificación |
| GET | `/api/v1/notificaciones` | A | Listar todas |
| GET | `/api/v1/notificaciones/estadisticas` | A | Estadísticas |

### report-service (8090)

| Método | Ruta | Permiso | Descripción |
|---|---|---|---|
| GET | `/api/v1/reportes/dashboard` | A, R | Datos del panel principal |
| GET | `/api/v1/reportes/completo` | A | Reporte completo |
| GET | `/api/v1/reportes/exportar` | A | Exportar reporte |
| GET | `/api/v1/reportes/historial` | A | Historial de reportes generados |
| GET | `/api/v1/reportes/historial/{id}` | A | Detalle de un reporte |
| DELETE | `/api/v1/reportes/historial/{id}` | A | Eliminar un reporte del historial |

> El detalle completo de cada servicio (cuerpos de petición, respuestas y validaciones) está en su Swagger.

---

## ⚠️ Formato de errores

Todos los servicios responden los errores con el mismo formato JSON, para que el frontend pueda mostrarlos de forma uniforme:

```json
{ "status": 409, "campo": null, "mensaje": "El proximo refuerzo debe ser posterior a la fecha de aplicacion" }
```

En errores de validación, `campo` indica qué input marcar en rojo y se agrega la lista completa:

```json
{ "status": 400, "campo": "motivo", "mensaje": "El motivo de consulta es obligatorio", "errores": { "motivo": "El motivo de consulta es obligatorio" } }
```

| Código | Significado |
|---|---|
| `200` / `201` / `204` | OK / Creado / Eliminado (sin contenido) |
| `400` | Datos inválidos o JSON mal formado |
| `401` | Sin token, token inválido o expirado |
| `403` | El rol no tiene permiso para esa acción |
| `404` | El recurso no existe |
| `409` | Se rompe una regla de negocio (duplicado, fechas incoherentes, etc.) |

---

## 🧪 Pruebas unitarias

Las pruebas usan **JUnit 5 + Mockito**: los repositorios se simulan, por lo que **no necesitan MySQL** encendido. Se ejecutan desde la carpeta de cada servicio:

```bash
./mvnw clean test          # Windows: .\mvnw.cmd clean test
```

| Servicio | Pruebas | Qué validan |
|---|---|---|
| mascota-service | 10 | Visibilidad por dueño, solicitudes pendientes/aprobadas, una pendiente a la vez, permisos |
| historial-service | 9 | CRUD de atenciones y vacunas, cálculo de estado, regla de fechas, 404 |
| servicio-clinico-service | 7 | Filtros por especie/grupo, nombre único, especies válidas, activo por defecto |

Los servicios de María y Rocío también incluyen sus pruebas (de service y de controller) en `src/test`.

---

## 📁 Estructura de cada microservicio

Todos siguen la misma organización por capas:

```
<servicio>/
├── pom.xml                      Dependencias (Spring Boot 3.4.6, JPA, Security, JWT, Swagger, Lombok)
├── mvnw / mvnw.cmd              Maven Wrapper
└── src/
    ├── main/
    │   ├── java/com/patitas/<servicio>/
    │   │   ├── config/          SecurityConfig, JwtRequestFilter, JwtUtil, OpenApiConfig, DataLoader
    │   │   ├── controller/      Endpoints REST (@RestController) con @PreAuthorize
    │   │   ├── dto/             Datos de entrada (Request) y salida (Response) con validaciones
    │   │   ├── exception/       Excepciones propias y ApiExceptionHandler (formato común de errores)
    │   │   ├── model/           Entidades JPA (@Entity) = tablas de MySQL
    │   │   ├── repository/      Interfaces JpaRepository (consultas derivadas del nombre)
    │   │   └── service/         Lógica de negocio (@Service, @Transactional)
    │   └── resources/
    │       ├── application.properties   Puerto, conexión a MySQL, clave JWT, CORS
    │       └── banner.txt               Banner que se muestra al iniciar
    └── test/java/...            Pruebas unitarias
```

**Responsabilidad de cada capa**

- **controller**: recibe la petición, valida el DTO (`@Valid`), revisa el rol (`@PreAuthorize`) y llama al service.
- **service**: aplica las reglas de negocio y decide qué se guarda.
- **repository**: lee y escribe en MySQL mediante Spring Data JPA.
- **model**: define las tablas; Hibernate las crea o actualiza al arrancar (`ddl-auto=update`).
- **dto**: separa lo que entra y sale por la API de la entidad interna.
- **config**: seguridad, JWT, Swagger y carga de datos de ejemplo.

**CORS**: los servicios aceptan peticiones del frontend en `http://localhost:5173` (Vite), configurado en `cors.origenes`.

---

## 🩺 Solución de problemas

| Problema | Causa | Solución |
|---|---|---|
| `".\mvnw.cmd" no se reconoce...` | La terminal no está dentro de la carpeta del servicio | `cd <servicio>` antes de correr Maven |
| `Communications link failure` / `Connection refused` | MySQL no está encendido | Abrir Docker Desktop y `docker start mysql-patitas` |
| `failed to connect to the docker API` | Docker Desktop está cerrado o iniciando | Abrir Docker Desktop y esperar *Engine running* |
| `Unknown database '..._db'` | La base no existe | Ejecutar el comando del paso 3 |
| `Port 80xx was already in use` | Ese servicio ya está corriendo en otra terminal, u otro programa usa el puerto (por ejemplo Jenkins en el 8080) | Cerrar la otra instancia (`Ctrl + C`); para ver quién usa el puerto: `netstat -ano \| findstr :8080` |
| Un servicio no aparece en http://localhost:8761 | Se inició antes que eureka-server o con una versión anterior | Reiniciarlo con eureka-server ya encendido |
| `503` al llamar por el gateway | El servicio destino no está registrado en Eureka | Encenderlo y esperar unos segundos a que aparezca en http://localhost:8761 |
| `401` con un token recién copiado | Token incompleto al copiarlo, o expiró (30 min) | Volver a hacer login y copiar el token completo |
| `403` | El rol del token no tiene permiso | Usar un usuario con el rol adecuado |
| Getters de Lombok marcados en rojo en VS Code | El editor no procesó Lombok | `Ctrl + Shift + P` → *Java: Clean Java Language Server Workspace* (Maven compila igual) |

---

## 🔗 Repositorios del proyecto

- **Backend**: https://github.com/Starlitbloom/Patitas_Felices_Ev3_Backend
- **Frontend**: https://github.com/Starlitbloom/Patitas_Felices_Ev3

---

Proyecto académico — Desarrollo Fullstack II · Duoc UC · 2026