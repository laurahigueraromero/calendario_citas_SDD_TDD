package com.calendario.citas;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import com.calendario.citas.support.IntegrationTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifica que Flyway está cableado y aplica las migraciones al arrancar el
 * contexto (también en los tests de integración).
 */
class FlywayMigrationIT extends IntegrationTest {

	@Autowired
	private Flyway flyway;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Test
	void flywayHistoryTableExists() {
		Integer count = jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM information_schema.tables "
						+ "WHERE table_schema = DATABASE() AND table_name = 'flyway_schema_history'",
				Integer.class);
		assertThat(count).isEqualTo(1);
	}

	@Test
	void baselineMigrationHasBeenApplied() {
		assertThat(flyway.info().applied()).isNotEmpty();

		Integer success = jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM flyway_schema_history WHERE version = '1' AND success = 1",
				Integer.class);
		assertThat(success).isEqualTo(1);
	}

	@Test
	void migrationsAreAppliedWithoutFailures() {
		Integer failed = jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM flyway_schema_history WHERE success = 0", Integer.class);
		assertThat(failed).isZero();
		assertThat(flyway.info().current()).isNotNull();
	}
}
