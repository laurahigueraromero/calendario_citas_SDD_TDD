package com.calendario.citas.seguridad;

import java.io.IOException;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Ante una petición sin autenticar devuelve <b>401</b> con un cuerpo JSON, en
 * lugar de redirigir a una página de login. El frontend (SPA) es quien decide
 * qué hacer con ese 401 (mostrar la pantalla de login).
 */
@Component
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

	@Override
	public void commence(HttpServletRequest request, HttpServletResponse response,
			AuthenticationException authException) throws IOException {
		response.setStatus(HttpStatus.UNAUTHORIZED.value());
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		response.getWriter().write("{\"error\":\"no_autenticado\"}");
	}
}
