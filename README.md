# calendario_citas_SDD_TDD

Calendario empresarial de reuniones (citas con clientes, reuniones de equipo y bloqueos de sala) con **flujo de aprobación** y **control de concurrencia** para garantizar que dos reuniones confirmadas nunca se solapan sobre la misma sala, empleado o cliente, ni siquiera bajo peticiones concurrentes.

Proyecto desarrollado con metodología **Spec-Driven Development**.

## Estado

Especificación cerrada ([`docs/PRD.md`](docs/PRD.md)) y esqueleto en marcha: backend y frontend arrancan, CI en verde en cada PR. Sin lógica de negocio todavía.

## Stack

| Capa | Tecnología |
|------|------------|
| Backend | Spring Boot 4 · Java 21 · Maven |
| Base de datos | MySQL 8.4 |
| Frontend | Vue 3 · Vite · pnpm (web responsive) |
| Correo (dev) | MailHog |
| Empaquetado | Docker / `docker compose` |

## Entorno de desarrollo local

Requisitos: Docker, JDK 21, Node 24 y pnpm (`corepack enable`).

```sh
# 1. Infraestructura (MySQL + MailHog)
docker compose up -d          # MySQL en :3306, MailHog UI en http://localhost:8025

# 2. Backend  ->  http://localhost:8080
cd backend && ./mvnw spring-boot:run

# 3. Frontend ->  http://localhost:5173
cd frontend && pnpm install && pnpm dev
```

El backend usa por defecto la BD `calendario` / usuario `calendario` que crea el compose. El perfil de Spring por defecto es `local` (`docker` para el contenedor, `test` para los tests); se cambia con `SPRING_PROFILES_ACTIVE`. Para puertos o credenciales, copia `.env.example` a `.env`.

El esquema lo gestiona **Flyway**. Para añadir un cambio, crea un fichero nuevo en `backend/src/main/resources/db/migration/` con el patrón `V<n>__descripcion.sql` (las migraciones ya mergeadas son inmutables). Se aplican solas al arrancar el backend y en los tests de integración.

**Login OAuth2 (GitHub / Google).** Copia `.env.example` a `.env` en la raíz y rellena `GITHUB_CLIENT_ID` / `GITHUB_CLIENT_SECRET` (y los de Google). Ejecutando el backend desde `backend/` con el perfil `local`, esas variables se cargan automáticamente desde `../.env`. Sin credenciales el backend arranca igual, pero ese proveedor de login no funcionará. Inicio de sesión: el navegador va a `/oauth2/authorization/github` (o `/google`); cierre de sesión: `POST /logout`.

> Los Dockerfiles de backend y frontend (y sus servicios en el compose) se añadirán más adelante; de momento ambos se ejecutan en local contra la infraestructura del compose.

## Tests

```sh
cd backend  && ./mvnw verify         # unit (surefire) + integración y concurrencia (failsafe, MySQL real vía Testcontainers; requiere Docker)
cd frontend && pnpm test:unit
```

Estrategia completa (niveles, convenciones, andamiaje): [`docs/TESTING.md`](docs/TESTING.md).

## Núcleo técnico

- **No solape** de reuniones `CONFIRMADA` por sala / empleado / cliente (intervalos semiabiertos).
- **Dos estrategias de control de concurrencia intercambiables por configuración**: bloqueo pesimista (`SELECT ... FOR UPDATE`) y bloqueo optimista (`@Version` + reintentos).
- **Flujo de aprobación**: toda reunión nace `PENDIENTE` y solo un administrador la confirma o la rechaza.
- Escenarios de verificación con hilos concurrentes reales (ver §5.5 del PRD).

## Documentación

- [`docs/PRD.md`](docs/PRD.md) — Product Requirements Document (objetivo, modelo de datos, reglas de negocio, requisitos funcionales, requisito no funcional de concurrencia, fuera de alcance).
- [`docs/TESTING.md`](docs/TESTING.md) — Estrategia de testing (niveles unit / integración / concurrencia, convenciones y andamiaje).
