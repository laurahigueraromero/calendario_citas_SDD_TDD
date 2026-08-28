-- Baseline de esquema. Punto de partida para las migraciones de las entidades
-- (issues DATA-*). No crea ninguna tabla todavía: el modelo de datos del PRD
-- (§2) se irá añadiendo en V2, V3, ... conforme se implementen las issues.
--
-- Convención de nombres: V<n>__descripcion_en_snake_case.sql
-- Ubicación: src/main/resources/db/migration/
-- Las migraciones son inmutables una vez mergeadas: los cambios van en una nueva.

SELECT 1;
