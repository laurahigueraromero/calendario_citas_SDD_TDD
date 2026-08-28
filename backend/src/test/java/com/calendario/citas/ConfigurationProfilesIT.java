package com.calendario.citas;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;

import com.calendario.citas.support.IntegrationTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifica que el perfil {@code test} está activo y que las propiedades clave de
 * configuración tienen el valor esperado.
 */
class ConfigurationProfilesIT extends IntegrationTest {

	@Autowired
	private Environment environment;

	@Test
	void testProfileIsActive() {
		assertThat(environment.getActiveProfiles()).contains("test");
	}

	@Test
	void keyPropertiesHaveExpectedValues() {
		assertThat(environment.getProperty("app.timezone")).isEqualTo("Europe/Madrid");
		assertThat(environment.getProperty("spring.jpa.open-in-view", Boolean.class)).isFalse();
		assertThat(environment.getProperty("spring.jpa.properties.hibernate.jdbc.time_zone"))
				.isEqualTo("UTC");
	}
}
