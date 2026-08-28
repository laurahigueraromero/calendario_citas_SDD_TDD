package com.calendario.citas.seguridad;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import com.calendario.citas.support.IntegrationTest;

import org.springframework.security.core.authority.SimpleGrantedAuthority;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oauth2Login;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AutenticacionIT extends IntegrationTest {

	@Autowired
	private MockMvc mvc;

	@Test
	void sinSesionApiMeDevuelve401() throws Exception {
		mvc.perform(get("/api/me"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.error").value("no_autenticado"));
	}

	@Test
	void sinSesionCualquierRutaProtegidaDevuelve401() throws Exception {
		mvc.perform(get("/api/cualquier-cosa"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void elHealthEsPublico() throws Exception {
		mvc.perform(get("/actuator/health"))
				.andExpect(status().isOk());
	}

	@Test
	void usuarioDeGithubVeSusDatos() throws Exception {
		mvc.perform(get("/api/me").with(oauth2Login()
						.authorities(new SimpleGrantedAuthority("ROLE_EMPLEADO"))
						.attributes(attrs -> {
							attrs.put("login", "laurah");
							attrs.put("name", "Laura Higuera");
							attrs.put("email", "laura@example.com");
						})))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.autenticado").value(true))
				.andExpect(jsonPath("$.nombre").value("Laura Higuera"))
				.andExpect(jsonPath("$.email").value("laura@example.com"))
				.andExpect(jsonPath("$.proveedor").value("test"))
				.andExpect(jsonPath("$.rol").value("EMPLEADO"));
	}

	@Test
	void usuarioDeGithubSinNombreCaeAlLogin() throws Exception {
		mvc.perform(get("/api/me").with(oauth2Login().attributes(attrs -> {
					attrs.put("login", "laurah");
					attrs.put("email", "laura@example.com");
				})))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.nombre").value("laurah"));
	}

	@Test
	void usuarioDeGoogleVeSusDatos() throws Exception {
		mvc.perform(get("/api/me").with(oidcLogin().idToken(token -> token
						.claim("email", "laura@gmail.com")
						.claim("email_verified", true)
						.claim("name", "Laura"))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.email").value("laura@gmail.com"))
				.andExpect(jsonPath("$.nombre").value("Laura"));
	}

	@Test
	void logoutDevuelve204() throws Exception {
		mvc.perform(post("/logout").with(oauth2Login()).with(csrf()))
				.andExpect(status().isNoContent());
	}
}
