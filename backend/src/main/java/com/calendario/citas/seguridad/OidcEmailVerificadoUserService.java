package com.calendario.citas.seguridad;

import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Component;

/**
 * Servicio de usuario para proveedores OIDC (Google). Exige que el proveedor
 * facilite un correo <b>verificado</b>; si no, rechaza el login (PRD §1.2).
 *
 * <p>El alta del usuario y la vinculación por correo se implementan en AUTH-3;
 * aquí solo se valida la precondición.
 */
@Component
public class OidcEmailVerificadoUserService implements OAuth2UserService<OidcUserRequest, OidcUser> {

	private final OidcUserService delegate = new OidcUserService();

	@Override
	public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
		OidcUser user = delegate.loadUser(userRequest);

		boolean emailVerificado = user.getEmail() != null && Boolean.TRUE.equals(user.getEmailVerified());
		if (!emailVerificado) {
			throw new OAuth2AuthenticationException(
					new OAuth2Error("email_no_verificado"),
					"El proveedor no ha facilitado un correo verificado.");
		}
		return user;
	}
}
