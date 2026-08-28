package com.calendario.citas.seguridad;

import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Component;

import com.calendario.citas.usuario.ProveedorOauth;
import com.calendario.citas.usuario.Usuario;

/**
 * Servicio de usuario para Google (OIDC): exige correo verificado, da de alta o
 * vincula el usuario y añade su rol como autoridad.
 */
@Component
public class AppOidcUserService implements OAuth2UserService<OidcUserRequest, OidcUser> {

	private final OidcUserService delegate = new OidcUserService();
	private final RegistroUsuarioService registro;

	public AppOidcUserService(RegistroUsuarioService registro) {
		this.registro = registro;
	}

	@Override
	public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
		OidcUser user = delegate.loadUser(userRequest);

		String email = user.getEmail();
		if (email == null || email.isBlank() || !Boolean.TRUE.equals(user.getEmailVerified())) {
			throw new OAuth2AuthenticationException(
					new OAuth2Error("email_no_verificado"),
					"El proveedor no ha facilitado un correo verificado.");
		}

		Usuario usuario = registro.registrarOVincular(new DatosIdentidad(
				ProveedorOauth.GOOGLE, user.getSubject(), email, user.getFullName(), email));

		List<GrantedAuthority> authorities = List.of(
				new SimpleGrantedAuthority("ROLE_" + usuario.getRol().name()));
		return new DefaultOidcUser(authorities, user.getIdToken(), user.getUserInfo(), "sub");
	}
}
