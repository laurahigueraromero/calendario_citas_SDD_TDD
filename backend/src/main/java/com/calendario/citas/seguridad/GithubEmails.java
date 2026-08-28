package com.calendario.citas.seguridad;

import java.util.List;
import java.util.Map;

/**
 * Selección del correo a usar a partir de la respuesta de
 * {@code GET https://api.github.com/user/emails}.
 *
 * <p>GitHub solo incluye el correo en {@code /user} si el usuario lo ha hecho
 * público, así que a veces hay que consultar este endpoint aparte.
 */
final class GithubEmails {

	private GithubEmails() {
	}

	/**
	 * Devuelve el correo primario y verificado; si no lo hay, el primero
	 * verificado; si ninguno está verificado, {@code null}.
	 */
	static String seleccionarVerificado(List<Map<String, Object>> emails) {
		if (emails == null) {
			return null;
		}
		String primeroVerificado = null;
		for (Map<String, Object> entrada : emails) {
			boolean verificado = Boolean.TRUE.equals(entrada.get("verified"));
			if (!verificado) {
				continue;
			}
			String email = (String) entrada.get("email");
			if (Boolean.TRUE.equals(entrada.get("primary"))) {
				return email;
			}
			if (primeroVerificado == null) {
				primeroVerificado = email;
			}
		}
		return primeroVerificado;
	}
}
