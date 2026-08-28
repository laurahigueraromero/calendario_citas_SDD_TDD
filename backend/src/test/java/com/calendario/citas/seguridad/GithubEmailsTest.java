package com.calendario.citas.seguridad;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GithubEmailsTest {

	@Test
	void prefiereElPrimarioVerificado() {
		List<Map<String, Object>> emails = List.of(
				Map.of("email", "otro@example.com", "primary", false, "verified", true),
				Map.of("email", "principal@example.com", "primary", true, "verified", true));

		assertThat(GithubEmails.seleccionarVerificado(emails)).isEqualTo("principal@example.com");
	}

	@Test
	void siElPrimarioNoEstaVerificadoCogeElPrimerVerificado() {
		List<Map<String, Object>> emails = List.of(
				Map.of("email", "principal@example.com", "primary", true, "verified", false),
				Map.of("email", "secundario@example.com", "primary", false, "verified", true));

		assertThat(GithubEmails.seleccionarVerificado(emails)).isEqualTo("secundario@example.com");
	}

	@Test
	void sinNingunoVerificadoDevuelveNull() {
		List<Map<String, Object>> emails = List.of(
				Map.of("email", "a@example.com", "primary", true, "verified", false),
				Map.of("email", "b@example.com", "primary", false, "verified", false));

		assertThat(GithubEmails.seleccionarVerificado(emails)).isNull();
	}

	@Test
	void listaNulaOVaciaDevuelveNull() {
		assertThat(GithubEmails.seleccionarVerificado(null)).isNull();
		assertThat(GithubEmails.seleccionarVerificado(List.of())).isNull();
	}
}
