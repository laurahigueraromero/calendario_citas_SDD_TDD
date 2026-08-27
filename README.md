# calendario_citas_SDD_TDD

Calendario empresarial de reuniones (citas con clientes, reuniones de equipo y bloqueos de sala) con **flujo de aprobación** y **control de concurrencia** para garantizar que dos reuniones confirmadas nunca se solapan sobre la misma sala, empleado o cliente, ni siquiera bajo peticiones concurrentes.

Proyecto desarrollado con metodología **Spec-Driven Development**.

## Estado

En especificación. El documento de requisitos vive en [`docs/PRD.md`](docs/PRD.md).

## Stack previsto

| Capa | Tecnología |
|------|------------|
| Backend | Spring Boot |
| Base de datos | MySQL |
| Frontend | Vue (web responsive) |
| Empaquetado | Docker / `docker compose` |

## Núcleo técnico

- **No solape** de reuniones `CONFIRMADA` por sala / empleado / cliente (intervalos semiabiertos).
- **Dos estrategias de control de concurrencia intercambiables por configuración**: bloqueo pesimista (`SELECT ... FOR UPDATE`) y bloqueo optimista (`@Version` + reintentos).
- **Flujo de aprobación**: toda reunión nace `PENDIENTE` y solo un administrador la confirma o la rechaza.
- Escenarios de verificación con hilos concurrentes reales (ver §5.5 del PRD).

## Documentación

- [`docs/PRD.md`](docs/PRD.md) — Product Requirements Document (objetivo, modelo de datos, reglas de negocio, requisitos funcionales, requisito no funcional de concurrencia, fuera de alcance).
