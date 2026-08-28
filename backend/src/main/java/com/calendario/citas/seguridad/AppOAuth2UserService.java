package com.calendario.citas.seguridad;

import java.util.List;
import java.util.Map;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.calendario.citas.usuario.ProveedorOauth;
import com.calendario.citas.usuario.Usuario;

/**
 * Servicio de usuario para GitHub: obtiene un correo verificado (consultando
 * {@code /user/emails} si el perfil no lo expone), da de alta o vincula el
 * usuario y añade su rol como autoridad.
 */
@Component
public class AppOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

	private final DefaultOAuth2UserService delegate = new DefaultOAuth2UserService();
	private final RegistroUsuarioService registro;
	private final RestClient github = RestClient.create();

	public AppOAuth2UserService(RegistroUsuarioService registro) {
		this.registro = registro;
	}

	@Override
	public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
		OAuth2User user = delegate.loadUser(userRequest);
		Map<String, Object> attrs = user.getAttributes();

		String email = (String) attrs.get("email");
		if (email == null || email.isBlank()) {
			email = emailVerificadoDesdeApi(userRequest.getAccessToken().getTokenValue());
		}
		if (email == null || email.isBlank()) {
			throw new OAuth2AuthenticationException(
					new OAuth2Error("email_no_verificado"),
					"Tu cuenta de GitHub no tiene un correo verificado accesible.");
		}

		String proveedorId = String.valueOf(attrs.get("id"));
		String login = (String) attrs.get("login");
		String nombre = (String) attrs.get("name");

		Usuario usuario = registro.registrarOVincular(new DatosIdentidad(
				ProveedorOauth.GITHUB, proveedorId, email, nombre, login));

		List<GrantedAuthority> authorities = List.of(
				new SimpleGrantedAuthority("ROLE_" + usuario.getRol().name()));
		String nameKey = userRequest.getClientRegistration()
				.getProviderDetails().getUserInfoEndpoint().getUserNameAttributeName();
		return new DefaultOAuth2User(authorities, attrs, nameKey);
	}

	private String emailVerificadoDesdeApi(String accessToken) {
		try {
			List<Map<String, Object>> emails = github.get()
					.uri("https://api.github.com/user/emails")
					.header("Authorization", "Bearer " + accessToken)
					.header("Accept", "application/vnd.github+json")
					.retrieve()
					.body(new ParameterizedTypeReference<List<Map<String, Object>>>() { });
			return GithubEmails.seleccionarVerificado(emails);
		}
		catch (RuntimeException ex) {
			return null;
		}
	}
}
