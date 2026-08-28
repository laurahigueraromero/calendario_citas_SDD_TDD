package com.calendario.citas.seguridad;

import java.io.IOException;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Cuando el login OAuth2 se rechaza (p. ej. el proveedor no facilita un correo
 * verificado) devuelve <b>403</b> con un mensaje claro para el usuario.
 */
@Component
public class OAuthLoginFailureHandler implements AuthenticationFailureHandler {

	@Override
	public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
			AuthenticationException exception) throws IOException {
		response.setStatus(HttpStatus.FORBIDDEN.value());
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		response.getWriter().write(
				"{\"error\":\"login_rechazado\","
						+ "\"mensaje\":\"No se pudo iniciar sesión: el proveedor no facilitó un correo verificado.\"}");
	}
}
