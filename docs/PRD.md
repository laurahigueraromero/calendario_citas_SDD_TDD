# PRD — Calendario empresarial de reuniones

**Proyecto:** `calendario_citas_SDD_TDD`
**Versión del documento:** 1.0
**Fecha:** 2026-08-27
**Metodología:** Spec-Driven Development
**Stack:** Spring Boot (backend) · MySQL · Vue (frontend) · Docker

> Este documento define **qué** se construye y **por qué**. La estrategia de pruebas, el desglose en issues y el plan de implementación se definen aparte, en el backlog.

---

## 1. Objetivo

Construir un calendario de reuniones para **una única empresa** que permita solicitar, aprobar y visualizar reuniones (citas con cliente, reuniones internas de equipo y bloqueos de sala), con dos propiedades no negociables:

1. **Ninguna sala, empleado ni cliente puede estar en dos reuniones confirmadas que se solapen en el tiempo**, ni siquiera bajo peticiones concurrentes.
2. **Ninguna reunión queda confirmada sin pasar por un flujo de aprobación** de un administrador.

El éxito del proyecto se mide por la **solidez y verificabilidad del control de concurrencia y del flujo de aprobación**, no por la amplitud funcional. La interfaz y las funcionalidades periféricas se mantienen deliberadamente mínimas. El sistema debe implementar **dos estrategias de control de concurrencia intercambiables por configuración** (bloqueo pesimista y bloqueo optimista) y demostrar, con escenarios de ejecución concurrente reproducibles, que la propiedad de no solape se mantiene con cualquiera de las dos.

### 1.1 Usuarios y roles

| Rol | Descripción | Capacidades |
|-----|-------------|-------------|
| **ADMIN** | Administrador de la empresa | Aprobar/rechazar solicitudes, gestionar salas, gestionar usuarios y clientes, cancelar y reprogramar cualquier reunión, todas las capacidades de EMPLEADO |
| **EMPLEADO** | Comercial / trabajador de la empresa | Crear solicitudes de reunión, editar y retirar sus solicitudes pendientes, cancelar y solicitar reprogramación de sus reuniones confirmadas, consultar calendarios |

- El **cliente** es un dato dentro de la reunión. **No inicia sesión** ni tiene acceso a la aplicación.
- No hay multi-empresa: la instancia entera representa a una sola organización.

### 1.2 Autenticación

- **OAuth2 "Iniciar sesión con GitHub" y "Iniciar sesión con Google"** mediante `spring-boot-starter-oauth2-client`. El usuario elige proveedor en la pantalla de login. No se gestionan contraseñas propias.
- La sesión del SPA se sostiene con el mecanismo estándar de Spring Security para clientes OAuth2 (cookie de sesión del backend; el frontend nunca maneja el token del proveedor).
- **Identidad:** cada usuario se identifica de forma interna por `(proveedor, proveedor_id)`. Si un usuario inicia sesión con un segundo proveedor usando **el mismo correo verificado** que ya existe en el sistema, la nueva identidad se **vincula** a la cuenta existente en lugar de crear un usuario duplicado. Para ello, ambos proveedores deben devolver un correo verificado (en GitHub se solicita el *scope* `user:email`).
- **Asignación de rol:** una *allowlist* de **correos electrónicos** configurada por propiedad (`app.security.admin-emails`) determina quién recibe `ADMIN` en su primer inicio de sesión, comparando contra el correo verificado del proveedor. Cualquier otro usuario autenticado recibe `EMPLEADO`. El registro de usuario ocurre de forma implícita en el primer login correcto.
- Un `ADMIN` puede cambiar el rol y el estado (activo/inactivo) de cualquier usuario desde la aplicación.

### 1.3 Zona horaria

- El sistema opera en **una única zona horaria de empresa** configurable (`app.timezone`, por defecto `Europe/Madrid`).
- Todos los instantes se **almacenan en UTC** y se presentan en la zona de empresa. No hay selección de zona por usuario.

---

## 2. Modelo de datos

### 2.1 Entidades

#### `usuario`
| Campo | Tipo | Notas |
|-------|------|-------|
| `id` | BIGINT PK | |
| `email` | VARCHAR, único | Correo verificado; clave de vinculación entre proveedores y de la *allowlist* de ADMIN |
| `nombre` | VARCHAR | Nombre visible |
| `rol` | ENUM(`ADMIN`, `EMPLEADO`) | |
| `activo` | BOOLEAN | Un usuario inactivo no puede iniciar sesión ni ser añadido a reuniones |
| `creado_en` | TIMESTAMP | |

#### `identidad_oauth`
Identidades de proveedor asociadas a un `usuario` (permite login con GitHub y con Google sobre la misma cuenta).
| Campo | Tipo | Notas |
|-------|------|-------|
| `id` | BIGINT PK | |
| `usuario_id` | BIGINT FK → `usuario` | |
| `proveedor` | ENUM(`GITHUB`, `GOOGLE`) | |
| `proveedor_id` | VARCHAR | Identificador estable del usuario en ese proveedor (`sub` en Google, `id` en GitHub) |
| `handle` | VARCHAR | Dato informativo: `login` de GitHub o correo de Google |
| `creado_en` | TIMESTAMP | |
| Único `(proveedor, proveedor_id)` | | |

#### `cliente` — *reservable*
| Campo | Tipo | Notas |
|-------|------|-------|
| `id` | BIGINT PK | |
| `nombre` | VARCHAR | |
| `email` | VARCHAR | |
| `telefono` | VARCHAR | Opcional |
| `empresa` | VARCHAR | Opcional; organización del cliente |
| `activo` | BOOLEAN | |
| `version` | BIGINT | `@Version` — usado por la estrategia optimista |
| `creado_en` | TIMESTAMP | |

#### `sala` — *reservable*
| Campo | Tipo | Notas |
|-------|------|-------|
| `id` | BIGINT PK | |
| `nombre` | VARCHAR, único | |
| `ubicacion` | VARCHAR | Opcional (planta, edificio) |
| `capacidad` | INT | Opcional; informativa |
| `activa` | BOOLEAN | Una sala inactiva no admite nuevas reuniones |
| `version` | BIGINT | `@Version` — usado por la estrategia optimista |
| `creado_en` | TIMESTAMP | |

#### `reunion`
| Campo | Tipo | Notas |
|-------|------|-------|
| `id` | BIGINT PK | |
| `tipo` | ENUM(`CITA_CLIENTE`, `REUNION_EQUIPO`, `BLOQUEO_SALA`) | |
| `titulo` | VARCHAR | |
| `descripcion` | TEXT | Opcional |
| `sala_id` | BIGINT FK → `sala` | **Obligatorio** para todos los tipos |
| `cliente_id` | BIGINT FK → `cliente` | Obligatorio si `tipo = CITA_CLIENTE`; nulo en el resto |
| `organizador_id` | BIGINT FK → `usuario` | Quien crea la solicitud |
| `inicio` | DATETIME (UTC) | |
| `fin` | DATETIME (UTC) | |
| `estado` | ENUM(`PENDIENTE`, `CONFIRMADA`, `RECHAZADA`, `CANCELADA`) | |
| `motivo_rechazo` | VARCHAR | Opcional; se rellena al rechazar |
| `motivo_cancelacion` | VARCHAR | Opcional; se rellena al cancelar |
| `decidida_por_id` | BIGINT FK → `usuario` | Admin que aprobó/rechazó; nulo mientras PENDIENTE |
| `decidida_en` | TIMESTAMP | |
| `reprogramacion_de_id` | BIGINT FK → `reunion` | Enlaza la nueva solicitud con la reunión confirmada que sustituye (ver §3.7) |
| `version` | BIGINT | `@Version` |
| `creado_en` / `actualizado_en` | TIMESTAMP | |

#### `reunion_participante`
Relación N:M entre `reunion` y `usuario` (empleados asistentes).
| Campo | Tipo | Notas |
|-------|------|-------|
| `reunion_id` | BIGINT FK → `reunion` | |
| `usuario_id` | BIGINT FK → `usuario` | |
| PK compuesta `(reunion_id, usuario_id)` | | |

El **organizador se considera participante** a efectos de disponibilidad aunque no aparezca en esta tabla, o bien se inserta explícitamente en ella al crear la reunión (decisión de implementación; el PRD exige que su disponibilidad se compruebe).

#### `notificacion`
| Campo | Tipo | Notas |
|-------|------|-------|
| `id` | BIGINT PK | |
| `usuario_id` | BIGINT FK → `usuario` | Destinatario |
| `tipo` | ENUM(`SOLICITUD_CREADA`, `REUNION_CONFIRMADA`, `REUNION_RECHAZADA`, `REUNION_CANCELADA`, `RECHAZO_POR_CONFLICTO`, `REPROGRAMACION_SOLICITADA`) | |
| `mensaje` | VARCHAR | Texto renderizado |
| `reunion_id` | BIGINT FK → `reunion` | Opcional |
| `leida` | BOOLEAN | |
| `creado_en` | TIMESTAMP | |

#### `reunion_evento` (historial / auditoría)
Registro append-only de cada transición de estado de una reunión.
| Campo | Tipo | Notas |
|-------|------|-------|
| `id` | BIGINT PK | |
| `reunion_id` | BIGINT FK → `reunion` | |
| `estado_anterior` | ENUM / NULL | |
| `estado_nuevo` | ENUM | |
| `actor_id` | BIGINT FK → `usuario` | Quién provocó la transición (o sistema) |
| `detalle` | VARCHAR | Motivo, referencia a conflicto, etc. |
| `creado_en` | TIMESTAMP | |

### 2.2 Concepto de *reservable*

Un **reservable** es cualquier entidad cuya disponibilidad temporal es exclusiva. En este sistema los reservables de una reunión `R` son:

```
reservables(R) = { R.sala }
              ∪ { cada empleado en R.participantes } ∪ { R.organizador }
              ∪ ( { R.cliente } si R.cliente_id ≠ NULL )
```

### 2.3 Índices relevantes

- `reunion (sala_id, estado, inicio, fin)` — soporte para la consulta de solape por sala.
- `reunion_participante (usuario_id)` con acceso a `reunion(estado, inicio, fin)` — solape por empleado.
- `reunion (cliente_id, estado, inicio, fin)` — solape por cliente.
- `reunion (estado)` — bandeja de aprobación.

> **Nota:** MySQL/InnoDB no ofrece *exclusion constraints*. La garantía de no solape la aporta la lógica transaccional descrita en §6, **no** una restricción física de base de datos. No se añade trigger ni tabla de slots.

---

## 3. Reglas de negocio

### 3.1 Tipos de reunión

| Tipo | Cliente | Participantes empleados | Uso |
|------|---------|-------------------------|-----|
| `CITA_CLIENTE` | Obligatorio (1) | ≥ 1 (incl. organizador) | Reunión comercial con un cliente |
| `REUNION_EQUIPO` | — | ≥ 1 | Reunión interna sin cliente |
| `BLOQUEO_SALA` | — | 0 o más | Reservar una sala (mantenimiento, evento) sin agenda de personas |

### 3.2 Flujo de aprobación

- **Todos los tipos** de reunión requieren aprobación. Toda reunión nace en estado `PENDIENTE`.
- Solo un `ADMIN` ejecuta la acción de **aprobar** (`PENDIENTE → CONFIRMADA`) o **rechazar** (`PENDIENTE → RECHAZADA`).
- El **motivo de rechazo es opcional**.
- Si el organizador de la solicitud es `ADMIN`, puede aprobarla él mismo; la aprobación pasa por exactamente la misma ruta validada que cualquier otra (no hay atajo que salte la comprobación de solape).

### 3.3 Estado `PENDIENTE` y disponibilidad

- Una solicitud `PENDIENTE` **no reserva el hueco**. Pueden coexistir varias solicitudes `PENDIENTE` para la misma sala / empleado / cliente en franjas solapadas.
- El solape **solo se valida en el momento de la aprobación** y solo contra reuniones en estado `CONFIRMADA`.

### 3.4 Regla de no solape (invariante central)

> Para todo par de reuniones `A` y `B` con `A ≠ B`, si ambas están en estado `CONFIRMADA` y sus intervalos `[inicio, fin)` se intersecan, entonces `reservables(A) ∩ reservables(B) = ∅`.

- Los intervalos son **semiabiertos**: una reunión que termina a las 11:00 y otra que empieza a las 11:00 en la misma sala **no** se solapan.
- Intersección de intervalos: `A.inicio < B.fin AND B.inicio < A.fin`.
- La comprobación se aplica a **cada** reservable de la reunión que se intenta confirmar.

### 3.5 Auto-rechazo en cascada al confirmar

Cuando un `ADMIN` confirma una solicitud `S`:

1. Se valida y se confirma `S` bajo la estrategia de concurrencia activa (§6).
2. Tras confirmar `S`, el sistema busca **toda** solicitud en estado `PENDIENTE` que comparta al menos un reservable con `S` en una franja solapada y la marca `RECHAZADA` con `motivo_rechazo = "Conflicto con reunión confirmada #<id de S>"`.
3. Cada auto-rechazo genera una notificación de tipo `RECHAZO_POR_CONFLICTO` al organizador de la solicitud afectada y un registro en `reunion_evento`.

### 3.6 Reglas temporales (validadas al crear y al confirmar)

- `inicio < fin`.
- **Horario laboral:** la reunión debe estar íntegramente dentro de la franja `L–V`, `08:00–20:00` en la zona de empresa. Configurable (`app.business-hours.*`). No se permiten reuniones en sábado, domingo ni fuera de esa franja. No se permite que una reunión cruce el límite diario.
- **Duración:** mínimo 15 minutos, máximo 8 horas.
- **Alineación:** `inicio` y `fin` alineados a múltiplos de 15 minutos.
- **No pasado:** `inicio` debe ser posterior al instante de la petición. Esta regla se comprueba tanto al crear como al confirmar (una solicitud que ha quedado en el pasado sin aprobarse no puede confirmarse).

### 3.7 Ciclo de vida

```
                 crear
                   │
                   ▼
              ┌───────────┐   aprobar (admin, validado)   ┌────────────┐
              │ PENDIENTE │ ─────────────────────────────▶ │ CONFIRMADA │
              └───────────┘                                └────────────┘
                │   │  ▲                                      │      │
      rechazar  │   │  │ editar (organizador,                 │      │ cancelar
      (admin)   │   │  │  revalida al confirmar)              │      │ (organizador
                │   │  └─────────────────────────────────┐    │      │  o admin)
                ▼   ▼                                    │    │      ▼
           ┌───────────┐                                 │    │  ┌───────────┐
           │ RECHAZADA │                     reprogramar │    │  │ CANCELADA │
           └───────────┘         (crea nueva solicitud ──┘    │  └───────────┘
                                  PENDIENTE que enlaza        │
                                  reprogramacion_de = R) ◀────┘
```

**Acciones:**

| Acción | Quién | Transición | Reglas |
|--------|-------|-----------|--------|
| Crear solicitud | EMPLEADO / ADMIN | — → `PENDIENTE` | Valida §3.1, §3.6. No valida solape. |
| Editar solicitud | Organizador | `PENDIENTE` → `PENDIENTE` | Puede cambiar hora, sala y participantes. Se revalida §3.6 al editar y §3.4 al confirmar. |
| Retirar solicitud | Organizador | `PENDIENTE` → `CANCELADA` | |
| Aprobar | ADMIN | `PENDIENTE` → `CONFIRMADA` | Valida §3.4 y §3.6 bajo control de concurrencia. Dispara §3.5. |
| Rechazar | ADMIN | `PENDIENTE` → `RECHAZADA` | Motivo opcional. |
| Cancelar confirmada | Organizador / ADMIN | `CONFIRMADA` → `CANCELADA` | Libera los reservables de inmediato. Motivo opcional. |
| Reprogramar confirmada | Organizador / ADMIN | (ver abajo) | |

**Reprogramación de una reunión `CONFIRMADA` `R`:**

- Genera una **nueva solicitud** en estado `PENDIENTE` con los datos nuevos y `reprogramacion_de_id = R.id`.
- `R` **permanece `CONFIRMADA` y sigue reservando sus recursos con los datos antiguos** hasta que se resuelva la nueva solicitud.
- Si el `ADMIN` **aprueba** la reprogramación: la nueva solicitud pasa a `CONFIRMADA` (validando solape con los datos nuevos, ignorando a `R` en esa comprobación) y `R` pasa a `CANCELADA` con `motivo_cancelacion = "Reprogramada en #<nueva id>"`, en la **misma transacción**.
- Si el `ADMIN` **rechaza** la reprogramación: la nueva solicitud pasa a `RECHAZADA` y `R` sigue `CONFIRMADA` sin cambios.

### 3.8 Notificaciones

- Canales del MVP: **bandeja de notificaciones dentro de la aplicación** y **correo electrónico**. En desarrollo/Docker el correo se dirige a un servidor de captura (p. ej. MailHog); no hay integración con proveedores externos de correo en producción dentro de este alcance.
- Eventos que notifican:
  - `SOLICITUD_CREADA` → a todos los `ADMIN`.
  - `REUNION_CONFIRMADA` → al organizador y a los empleados participantes.
  - `REUNION_RECHAZADA` → al organizador.
  - `RECHAZO_POR_CONFLICTO` → al organizador de la solicitud auto-rechazada.
  - `REUNION_CANCELADA` → al organizador y participantes.
  - `REPROGRAMACION_SOLICITADA` → a todos los `ADMIN`.
- El envío de correo **no debe formar parte de la transacción** que cambia el estado de la reunión (se dispara tras el commit).

---

## 4. Requisitos funcionales

### Autenticación y usuarios
- **RF-01** — El sistema permite iniciar sesión mediante OAuth2 con **GitHub** o con **Google**, a elección del usuario, y crea el usuario en el primer acceso. Si un usuario inicia sesión con un proveedor distinto usando un correo verificado que ya existe en el sistema, la nueva identidad se vincula a la cuenta existente en lugar de crear un duplicado.
- **RF-02** — El sistema asigna rol `ADMIN` a los correos de la *allowlist* configurada (`app.security.admin-emails`) y `EMPLEADO` al resto.
- **RF-03** — Un `ADMIN` puede listar usuarios y cambiar su rol y su estado activo/inactivo.
- **RF-04** — Un usuario inactivo no puede iniciar sesión ni ser añadido como participante.

### Salas y clientes
- **RF-05** — Un `ADMIN` puede crear, editar, activar y desactivar salas.
- **RF-06** — Un `ADMIN` puede crear, editar, activar y desactivar clientes.
- **RF-07** — Las salas y clientes inactivos no aparecen como seleccionables en nuevas solicitudes.

### Solicitud de reuniones
- **RF-08** — Un `EMPLEADO` o `ADMIN` puede crear una solicitud de reunión indicando tipo, título, descripción opcional, sala (obligatoria), franja `inicio`–`fin`, empleados participantes y —solo en `CITA_CLIENTE`— un cliente.
- **RF-09** — El sistema valida al crear: composición según tipo (§3.1) y reglas temporales (§3.6). No valida solape en la creación.
- **RF-10** — Toda solicitud creada queda en estado `PENDIENTE`.
- **RF-11** — El organizador puede editar su solicitud mientras esté `PENDIENTE` (hora, sala, participantes, cliente), revalidando §3.6.
- **RF-12** — El organizador puede retirar su solicitud `PENDIENTE` (pasa a `CANCELADA`).
- **RF-13** — El formulario de nueva solicitud muestra una **comprobación de disponibilidad informativa** (no vinculante) que indica si la franja elegida ya choca con reuniones `CONFIRMADA` para la sala, los empleados o el cliente seleccionados.

### Aprobación
- **RF-14** — Un `ADMIN` puede ver la **bandeja de solicitudes `PENDIENTE`**, ordenada por fecha de creación, con indicación visual de si cada solicitud entra en conflicto con alguna reunión `CONFIRMADA`.
- **RF-15** — Un `ADMIN` puede **aprobar** una solicitud `PENDIENTE`. La aprobación valida la regla de no solape (§3.4) y las reglas temporales (§3.6) bajo la estrategia de concurrencia activa. Si hay conflicto, la aprobación se rechaza con un error de conflicto (HTTP 409) y la solicitud permanece `PENDIENTE`.
- **RF-16** — Al aprobar una solicitud, el sistema auto-rechaza en cascada las solicitudes `PENDIENTE` en conflicto (§3.5).
- **RF-17** — Un `ADMIN` puede **rechazar** una solicitud `PENDIENTE` con un motivo opcional.
- **RF-18** — La acción de aprobar es **idempotente**: reintentar la aprobación de una solicitud que ya está `CONFIRMADA` no crea una segunda reunión ni altera el estado; devuelve el resultado de la reunión ya confirmada.

### Reuniones confirmadas
- **RF-19** — El organizador o un `ADMIN` pueden **cancelar** una reunión `CONFIRMADA`; los reservables quedan libres inmediatamente.
- **RF-20** — El organizador o un `ADMIN` pueden **solicitar la reprogramación** de una reunión `CONFIRMADA`, generando una nueva solicitud `PENDIENTE` enlazada (§3.7). La reunión original mantiene su reserva hasta que la reprogramación se resuelve.
- **RF-21** — Aprobar una reprogramación confirma la nueva reunión y cancela la original en la misma transacción; rechazarla deja la original intacta.

### Visualización (frontend Vue)
- **RF-22** — **Calendario mensual global**: muestra todas las reuniones de la empresa, con filtros por sala, por empleado y por estado.
- **RF-23** — **Calendario semanal por sala**: rejilla día × hora de una sala concreta que muestra su ocupación `CONFIRMADA`, haciendo visible la ausencia de solapes.
- **RF-24** — **Mi agenda**: lista de las reuniones del usuario autenticado (como organizador o participante) agrupadas por estado (`PENDIENTE`, `CONFIRMADA`, `RECHAZADA`, `CANCELADA`).
- **RF-25** — **Formulario de nueva solicitud** (RF-08 + RF-13).
- **RF-26** — **Bandeja de aprobación del `ADMIN`** (RF-14) con acciones aprobar / rechazar.

### Notificaciones
- **RF-27** — El sistema genera notificaciones internas para los eventos de §3.8 y las expone en una bandeja consultable con marca de leído/no leído.
- **RF-28** — El sistema envía un correo por cada uno de esos eventos, fuera de la transacción de cambio de estado.

### Auditoría
- **RF-29** — Cada transición de estado de una reunión se registra en `reunion_evento` con actor, estados y detalle.

### Interfaz y presentación
- **RF-30** — El frontend Vue es **responsive**: todas las pantallas (RF-22 … RF-26, login y gestión) son usables en móvil, tablet y escritorio, sin *scroll* horizontal y con los objetivos táctiles y el tamaño de texto adecuados. Se accede desde el navegador; no hay aplicación empaquetada. Detalle en §5.6.1.

---

## 5. Requisito no funcional central: control de concurrencia

### 5.1 Propiedad a garantizar

Bajo **cualquier número de peticiones concurrentes** de aprobación (y de reprogramación, que confirma una reunión), el sistema debe mantener el invariante de §3.4: **jamás pueden existir dos reuniones `CONFIRMADA` que se solapen en el tiempo y compartan una sala, un empleado o un cliente**. La operación de aprobación debe ser además **idempotente** frente a reintentos concurrentes de la misma solicitud.

Comportamiento esperado ante contención: **exactamente una** de las operaciones concurrentes en conflicto tiene éxito (`CONFIRMADA`); las demás terminan de forma limpia, sin corromper estado, devolviendo un conflicto (HTTP 409) o —en el caso de reintento de la misma solicitud ya confirmada— el resultado idempotente de la reunión ya existente.

### 5.2 Alcance transaccional

- La aprobación de una solicitud es **una única transacción** que: (a) toma el control de concurrencia sobre los reservables implicados, (b) relee el estado actual de la solicitud, (c) comprueba el solape contra reuniones `CONFIRMADA`, (d) cambia el estado a `CONFIRMADA`, (e) ejecuta el auto-rechazo en cascada (§3.5). Si cualquier paso falla, se hace rollback completo.
- Nivel de aislamiento: **`READ COMMITTED`** (el propio de InnoDB por defecto es `REPEATABLE READ`; se fija explícitamente `READ COMMITTED` para el flujo de aprobación y se apoya la exclusión en el bloqueo explícito, no en el aislamiento).
- El envío de correos y cualquier trabajo no esencial se ejecuta **después del commit**.
- La **comprobación de disponibilidad informativa** del formulario (RF-13) es una lectura sin bloqueo y **no** ofrece ninguna garantía; sirve solo como ayuda de UX.

### 5.3 Estrategia intercambiable

El control de concurrencia se implementa tras una **interfaz común** (p. ej. `ReservationGuard` / `ConfirmationStrategy`) con **dos implementaciones seleccionables por configuración** (`app.concurrency.strategy = PESSIMISTIC | OPTIMISTIC`). El resto de la lógica de negocio es idéntica en ambos modos.

#### 5.3.1 Modo `PESSIMISTIC` — bloqueo pesimista

- Al aprobar, la transacción ejecuta `SELECT ... FOR UPDATE` sobre:
  - la fila de la **sala** de la solicitud, y
  - la fila de **cada empleado** participante (incluido el organizador), y
  - la fila del **cliente** si lo hay.
- Los bloqueos se adquieren en **orden determinista** (por tipo de entidad y luego por `id` ascendente) para evitar *deadlocks* entre transacciones que compiten por reservables solapados.
- Con todos los reservables bloqueados, la transacción comprueba el solape contra reuniones `CONFIRMADA`. Si no hay solape, cambia el estado y hace commit; los bloqueos se liberan al terminar la transacción. Las transacciones concurrentes que necesitan esos mismos reservables **esperan** hasta el commit y luego ven el estado ya actualizado, por lo que detectarán el conflicto y devolverán 409.
- Se configura un **timeout de espera de bloqueo** (`innodb_lock_wait_timeout` / `javax.persistence.lock.timeout`); si expira, la operación devuelve 409 (o 503 con reintento sugerido), sin dejar estado a medias.

#### 5.3.2 Modo `OPTIMISTIC` — bloqueo optimista con versión y reintentos

- Las entidades reservables (`sala`, `cliente`) y `usuario` llevan columna `@Version`.
- Al aprobar, la transacción lee los reservables y su `version`, comprueba el solape y, antes de confirmar, **incrementa la `version` de cada reservable implicado** (marcándolos como "tocados" por esta confirmación).
- Si otra transacción concurrente confirmó primero y modificó la `version` de un reservable compartido, el commit lanza `OptimisticLockException` (`ObjectOptimisticLockingFailureException`).
- La operación se **reintenta hasta 3 veces** con *backoff* corto (p. ej. 20–50 ms con jitter). En cada reintento se releen los datos y se revalida el solape (un reintento puede terminar en 409 legítimo si el ganador ya ocupó la franja).
- Agotados los 3 reintentos sin éxito, la operación devuelve **HTTP 409** (conflicto de concurrencia), sin estado parcial.
- **No** se añade ninguna red de seguridad a nivel de base de datos (ni *trigger* ni tabla de *slots*): la garantía la aporta esta estrategia.

### 5.4 Idempotencia de la aprobación

- La aprobación solo es válida desde el estado `PENDIENTE`. Al entrar en la transacción se **relee el estado** de la solicitud (con el bloqueo correspondiente en modo pesimista).
- Si la solicitud ya está `CONFIRMADA`, la operación **no** realiza cambios y devuelve el resultado de la reunión confirmada (respuesta equivalente a la de la confirmación original).
- Si está `RECHAZADA` o `CANCELADA`, la operación devuelve un error de estado no válido (HTTP 409) sin efectos.
- Dos peticiones concurrentes que aprueban **la misma** solicitud producen **una sola** reunión `CONFIRMADA` y un solo conjunto de notificaciones; la segunda petición observa el resultado idempotente.

### 5.5 Escenarios de verificación exigidos

El requisito no se considera cumplido hasta que se demuestra, mediante ejecución **concurrente real de múltiples hilos** contra la base de datos (no *mocks*), y **con cada una de las dos estrategias**, que:

- **VC-1 — Doble confirmación de la misma solicitud (idempotencia) — escenario destacado.**
  `N` hilos (`N ≥ 10`) invocan la aprobación de **la misma** solicitud `PENDIENTE` simultáneamente.
  *Resultado esperado:* existe **exactamente una** reunión `CONFIRMADA` para esa solicitud; ningún hilo provoca una segunda reunión, doble reserva ni estado corrupto; los hilos que no "ganan" reciben el resultado idempotente o un 409 limpio; se genera un único conjunto de notificaciones y un historial coherente en `reunion_evento`.

- **VC-2 — `N` confirmaciones distintas para el mismo hueco.**
  Se preparan `N` solicitudes `PENDIENTE` **diferentes** que comparten sala (y/o empleado y/o cliente) en la **misma franja**. `N` hilos aprueban una solicitud cada uno a la vez.
  *Resultado esperado:* **exactamente una** queda `CONFIRMADA`; las demás terminan `RECHAZADA`/409; no hay dos reuniones `CONFIRMADA` solapadas en ningún reservable.

- **VC-3 — Solape parcial de franjas.**
  Dos (o más) solicitudes para la misma sala con franjas que se **cruzan parcialmente** (p. ej. `10:00–11:00` y `10:30–11:30`), aprobadas concurrentemente.
  *Resultado esperado:* solo una queda `CONFIRMADA`; la detección de solape reconoce la intersección parcial de intervalos semiabiertos, no solo franjas idénticas.

En los tres escenarios: sin *deadlocks* no controlados, sin pérdida de actualizaciones, y el estado final de la base de datos satisface el invariante de §3.4. VC-1 es el escenario que se documenta y destaca; VC-2 y VC-3 son igualmente obligatorios.

### 5.6 Otros requisitos no funcionales

- **Despliegue:** todo el sistema (backend, MySQL, frontend, servidor de correo de captura) se levanta con `docker compose up`. Configuración por variables de entorno.
- **Migraciones de esquema** versionadas (Flyway o Liquibase).
- **Observabilidad mínima:** logs estructurados de cada intento de aprobación con resultado (confirmada / conflicto / reintento / timeout) y estrategia activa.
- **Rendimiento:** no hay objetivo de carga formal; el sistema debe comportarse de forma correcta y sin *deadlocks* con contención alta en los escenarios de §5.5.
- **API:** REST sobre JSON.

#### 5.6.1 Diseño responsive (RF-30)

- **Un solo frontend Vue** sirve a todos los tamaños de pantalla. No hay build ni cliente separado para móvil; no hay PWA ni empaquetado nativo.
- **Puntos de ruptura** de referencia: móvil `< 600 px`, tablet `600–1023 px`, escritorio `≥ 1024 px`. El layout se adapta con CSS *flex/grid* y unidades relativas.
- **Sin scroll horizontal de página** en ningún ancho. Las rejillas de calendario (mensual global, semanal por sala) y las tablas (bandeja de aprobación, Mi agenda) que no caben scrollan **dentro de su propio contenedor**, no arrastran la página.
- **Adaptaciones por vista en móvil:**
  - *Calendario mensual global*: se admite una vista de lista/agenda por día como alternativa a la rejilla de 7 columnas cuando el ancho no permite mostrarla con legibilidad.
  - *Calendario semanal por sala*: scroll horizontal interno de la franja horaria; cabecera de días fija.
  - *Filtros*: colapsados en un panel/acordeón desplegable en móvil, visibles en línea en escritorio.
  - *Navegación*: menú lateral en escritorio, menú hamburguesa o barra inferior en móvil.
- **Objetivos táctiles** de al menos 44×44 px; tamaño de fuente base ≥ 16 px para evitar zoom automático en iOS; formularios usables con teclado en pantalla.
- **Orientación:** usable en vertical y horizontal.
- **Compatibilidad:** últimas dos versiones mayores de Chrome, Firefox, Safari y Edge, incluidas sus variantes móviles (iOS Safari, Chrome Android).
- La verificación del comportamiento responsive (anchos concretos, ausencia de *overflow*, navegación móvil) se enumerará como criterios de aceptación de RF-30 en el backlog.

---

## 6. Fuera de alcance

Lo siguiente queda **explícitamente excluido** de este proyecto:

- **Reuniones recurrentes o series** ("todos los martes"). Cada reunión es un evento único.
- **Recursos reservables distintos de salas.** No se modelan proyectores, coches, portátiles ni ningún otro equipo. La única entidad física reservable es la sala.
- **Integraciones externas de calendario:** sin sincronización ni import/export con Google Calendar, Outlook, CalDAV ni ficheros `.ics`.
- **Portal de cliente:** el cliente no inicia sesión ni tiene ninguna interfaz; es solo un dato de la reunión.
- **Multi-empresa / multi-tenant:** la instalación sirve a una sola organización.
- **Autenticación con contraseña propia, SSO corporativo con verificación de dominio (Azure AD, restricción a un dominio de Google Workspace) y recuperación de cuenta.** Solo OAuth2 "iniciar sesión con GitHub" y "iniciar sesión con Google" a nivel de cuenta individual, sin restricción de dominio.
- **Zonas horarias por usuario** y reuniones que cruzan husos. Una única zona de empresa.
- **Aplicación móvil nativa o híbrida** (Android/iOS, empaquetado con Capacitor/React Native/Flutter, publicación en tiendas) y **PWA instalable / modo offline**. El soporte móvil se cubre mediante web responsive (RF-30, §5.6.1); el acceso es siempre desde el navegador.
- **Internacionalización (i18n) / multi-idioma.** Un solo idioma.
- **Informes, analítica, cuadros de mando de ocupación y exportaciones de datos.**
- **Facturación, cobros o gestión económica** de ninguna clase.
- **Gestión de asistencia** (confirmación de asistentes, check-in, actas de reunión, adjuntos).
- **Notificaciones push, SMS o integración con mensajería** (Slack, Teams). Solo bandeja interna y correo.
- **Lista de espera automática:** cuando una solicitud se rechaza por conflicto no se re-encola ni se sugiere hueco alternativo automáticamente.
- **Red de seguridad de no solape a nivel de base de datos** (triggers, tabla de slots con `UNIQUE`): la garantía la aportan las estrategias de concurrencia de §5.3.
- La **estrategia y el plan de pruebas** (más allá de enunciar los escenarios de verificación de §5.5 como criterio de aceptación del requisito de concurrencia) se definen en el backlog de issues, no en este PRD.
