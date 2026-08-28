package com.calendario.citas;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import com.calendario.citas.support.ConcurrencyHarness;
import com.calendario.citas.support.IntegrationTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Smoke del nivel de tests de concurrencia: valida que el andamiaje
 * ({@link ConcurrencyHarness} + Testcontainers + pool de conexiones) sirve para
 * observar una carrera real contra MySQL.
 *
 * <p>Es una versión en miniatura del patrón de los escenarios VC del PRD: varios
 * hilos compiten por el mismo recurso (aquí, una clave primaria) y exactamente
 * uno gana. La exclusión aquí la da una restricción de la base de datos; en el
 * dominio real la darán las estrategias de bloqueo (issues APRV-3 / APRV-4).
 */
class ConcurrentAccessSmokeIT extends IntegrationTest {

	private static final int THREADS = 12;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@BeforeEach
	void createScratchTable() {
		jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS concurrency_smoke (id INT PRIMARY KEY)");
		jdbcTemplate.execute("TRUNCATE TABLE concurrency_smoke");
	}

	@AfterEach
	void dropScratchTable() {
		jdbcTemplate.execute("DROP TABLE IF EXISTS concurrency_smoke");
	}

	@Test
	void exactlyOneThreadInsertsTheContestedRow() throws InterruptedException {
		ConcurrencyHarness.Result<Integer> result = ConcurrencyHarness.runInParallel(
				THREADS,
				() -> jdbcTemplate.update("INSERT INTO concurrency_smoke (id) VALUES (1)"));

		assertThat(result.exactlyOneSucceeded())
				.as("solo un hilo debería insertar la fila")
				.isTrue();
		assertThat(result.failureCount()).isEqualTo(THREADS - 1);
		assertThat(result.failuresOfType(DataIntegrityViolationException.class))
				.as("el resto falla por clave duplicada")
				.isEqualTo(THREADS - 1L);

		Integer rows = jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM concurrency_smoke", Integer.class);
		assertThat(rows).isEqualTo(1);
	}
}
