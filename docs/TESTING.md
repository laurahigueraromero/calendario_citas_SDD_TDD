# Estrategia de testing

Documento vivo. Define cómo se prueba el proyecto. Complementa al PRD (que
deliberadamente no fija la estrategia de testing) y a los criterios de
aceptación de cada issue del backlog.

## Política TDD

- Cada issue del backlog incluye una sección **"Tests (primero)"** con los casos
  concretos. Se escriben **antes** que el código de producción.
- Un Pull Request no se mergea si los tests de su issue no están y no pasan en CI.
- Los tests describen comportamiento observable, no detalles de implementación.

## Niveles

### 1. Unitario (backend)

- Dominio puro: reglas de negocio sin Spring ni base de datos
  (validación temporal, detección de solape, backoff de reintentos…).
- JUnit 5 + AssertJ. Rápidos, deterministas, sin E/S.
- **Convención de nombres:** `*Test.java`. Los ejecuta **surefire** en la fase
  `test`.
- Ejemplo de andamiaje: `support/ConcurrencyHarnessTest`.

### 2. Integración (backend)

- Contexto de Spring (completo o *slice*) contra **MySQL 8.4 real** gestionado
  con **Testcontainers** — nunca H2 ni base de datos en memoria: el núcleo del
  proyecto depende del comportamiento de bloqueo de InnoDB.
- Clase base: **`support/IntegrationTest`** (`@SpringBootTest` +
  `@Import(TestcontainersConfiguration.class)`). Todas las subclases que no
  añadan configuración comparten un único contexto cacheado y **un único
  contenedor** para toda la suite.
- La imagen (`mysql:8.4`) está fijada en `TestcontainersConfiguration` y coincide
  con la de `docker-compose.yml`.
- **Convención de nombres:** `*IT.java`. Los ejecuta **failsafe** en la fase
  `verify`.
- Requiere Docker en la máquina (local y runner de CI).
- Ejemplos: `CalendarioCitasApplicationIT` (carga de contexto),
  `DatabaseConnectivityIT` (datasource + versión de MySQL).

### 3. Concurrencia (backend)

- Varios **hilos reales** ejecutando la misma operación contra la base de datos,
  **sin mocks**. Es el nivel que valida el requisito no funcional central
  (§5 del PRD).
- Herramienta: **`support/ConcurrencyHarness`**
  - `runInParallel(n, action)` lanza `n` hilos que esperan en una barrera y se
    liberan a la vez, para estrechar al máximo la ventana de carrera.
  - Devuelve un `Result` con los éxitos y las excepciones **por hilo**
    (`successCount()`, `failureCount()`, `exactlyOneSucceeded()`,
    `failuresOfType(...)`).
- Se ejecutan como tests de integración (`*IT`, failsafe) porque necesitan la
  base de datos.
- Cada escenario del PRD (VC-1, VC-2, VC-3) se prueba **con las dos estrategias**
  de concurrencia. Patrón previsto cuando exista `app.concurrency.strategy`:

  ```java
  @ParameterizedTest
  @ValueSource(strings = {"PESSIMISTIC", "OPTIMISTIC"})
  void ningunSolapeBajoConcurrencia(String estrategia) { ... }
  ```

  reiniciando el contexto por parámetro (`@DynamicPropertySource` /
  `@TestPropertySource`), o con una clase de test por estrategia.
- Los tests de concurrencia deben pasar de forma **estable en ejecución
  repetida** (p. ej. anotados para repetirse, o ejecutados N veces en CI antes
  de darlos por buenos).
- Ejemplo de andamiaje: `ConcurrentAccessSmokeIT` (N hilos compiten por una clave
  primaria; exactamente uno gana).

### Frontend

- **Vitest** + `@vue/test-utils`, entorno `jsdom`.
- Se prueban: lógica de *stores* (Pinia), *composables*, guardas de router,
  formateo/validación de datos y render condicional de componentes.
- **Convención de nombres:** `*.spec.js`, junto al código en `__tests__/`.
- Cobertura: `pnpm test:coverage` (proveedor v8, solo reporte; se fijarán
  umbrales cuando haya código real).
- Ejemplos: `src/stores/__tests__/counter.spec.js`,
  `src/components/__tests__/HelloWorld.spec.js`.
- Criterios de "hecho" por vista: se definen en la issue de cada vista y en la
  issue transversal de responsive (FE-11 / RF-30).

## Cómo ejecutar

| Qué | Comando |
|-----|---------|
| Backend, unitarios | `cd backend && ./mvnw test` |
| Backend, todo (unit + integración + concurrencia) | `cd backend && ./mvnw verify` |
| Frontend, unitarios | `cd frontend && pnpm test:unit` |
| Frontend, watch | `cd frontend && pnpm test:unit:watch` |
| Frontend, cobertura | `cd frontend && pnpm test:coverage` |

## En CI

`.github/workflows/ci.yml` ejecuta en cada Pull Request y push a `main`:

- **backend:** `./mvnw -B -ntp verify` (surefire + failsafe; Testcontainers
  levanta MySQL en el runner, que ya trae Docker).
- **frontend:** `pnpm lint` + `pnpm test:unit` + `pnpm build`.

## Andamiaje disponible (issue TEST-0)

| Pieza | Ubicación | Uso |
|-------|-----------|-----|
| `IntegrationTest` | `backend/.../support/` | Clase base para tests `*IT` con MySQL real |
| `TestcontainersConfiguration` | `backend/.../citas/` | Definición del contenedor `mysql:8.4` |
| `ConcurrencyHarness` | `backend/.../support/` | Lanzar N hilos a la vez y recoger resultados/excepciones |

Pendiente de crear cuando lleguen las entidades (issues DATA-*): *builders* de
`Sala`, `Cliente`, `Usuario` y `Reunion` para tests, y una clase base
`@DataJpaTest` contra el contenedor.
