package com.calendario.citas.seguridad;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Datos del usuario autenticado (RF-01). Si no hay sesión, la petición ni
 * siquiera llega aquí: el filtro de seguridad responde 401.
 */
@RestController
public class MeController {

	@GetMapping("/api/me")
	public Map<String, Object> me(@AuthenticationPrincipal OAuth2User principal, OAuth2AuthenticationToken token) {
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("autenticado", true);
		body.put("nombre", nombre(principal));
		body.put("email", principal.getAttribute("email"));
		body.put("proveedor", token.getAuthorizedClientRegistrationId());
		return body;
	}

	private static String nombre(OAuth2User principal) {
		String name = principal.getAttribute("name");
		if (name != null && !name.isBlank()) {
			return name;
		}
		// GitHub: si "name" es nulo, cae al "login"
		String login = principal.getAttribute("login");
		return login != null ? login : principal.getName();
	}
}
