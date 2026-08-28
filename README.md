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

El backend usa por defecto la BD `calendario` / usuario `calendario` que crea el compose. Para cambiar puertos o credenciales, copia `.env.example` a `.env`.

> Los Dockerfiles de backend y frontend (y sus servicios en el compose) se añadirán más adelante; de momento ambos se ejecutan en local contra la infraestructura del compose.

## Tests

```sh
cd backend  && ./mvnw verify         # incluye tests con MySQL real vía Testcontainers (requiere Docker)
cd frontend && pnpm test:unit
```

## Núcleo técnico

- **No solape** de reuniones `CONFIRMADA` por sala / empleado / cliente (intervalos semiabiertos).
- **Dos estrategias de control de concurrencia intercambiables por configuración**: bloqueo pesimista (`SELECT ... FOR UPDATE`) y bloqueo optimista (`@Version` + reintentos).
- **Flujo de aprobación**: toda reunión nace `PENDIENTE` y solo un administrador la confirma o la rechaza.
- Escenarios de verificación con hilos concurrentes reales (ver §5.5 del PRD).

## Documentación

- [`docs/PRD.md`](docs/PRD.md) — Product Requirements Document (objetivo, modelo de datos, reglas de negocio, requisitos funcionales, requisito no funcional de concurrencia, fuera de alcance).
