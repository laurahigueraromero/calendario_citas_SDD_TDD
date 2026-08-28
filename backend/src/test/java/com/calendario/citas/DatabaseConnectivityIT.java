package com.calendario.citas;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import com.calendario.citas.support.IntegrationTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Smoke del nivel de integración: hay un {@link DataSource} y responde, y la
 * base de datos es la versión esperada (MySQL 8.4, la misma imagen que fija
 * {@code docker-compose.yml} y {@code TestcontainersConfiguration}).
 */
class DatabaseConnectivityIT extends IntegrationTest {

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private DataSource dataSource;

	@Test
	void dataSourceAnswers() {
		Integer one = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
		assertThat(one).isEqualTo(1);
		assertThat(dataSource).isNotNull();
	}

	@Test
	void runsExpectedMySqlVersion() {
		String version = jdbcTemplate.queryForObject("SELECT VERSION()", String.class);
		assertThat(version).startsWith("8.4");
	}
}
