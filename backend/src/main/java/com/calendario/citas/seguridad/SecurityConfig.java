package com.calendario.citas.seguridad;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.authentication.logout.HttpStatusReturningLogoutSuccessHandler;

/**
 * Configuración de seguridad (PRD §1.2, RF-01).
 *
 * <ul>
 *   <li>Login mediante OAuth2 con GitHub y Google.</li>
 *   <li>Rutas públicas: raíz, errores, inicio y callback del login, y el health.</li>
 *   <li>Cualquier otra ruta requiere sesión; sin ella se responde 401 (no se
 *       redirige a una página de login: de eso se encarga el frontend).</li>
 *   <li>Cierre de sesión: {@code POST /logout} → 204.</li>
 *   <li>CSRF con cookie legible por el SPA ({@code XSRF-TOKEN}).</li>
 * </ul>
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

	@Bean
	SecurityFilterChain securityFilterChain(
			HttpSecurity http,
			OAuth2UserService<OidcUserRequest, OidcUser> oidcUserService,
			RestAuthenticationEntryPoint authenticationEntryPoint,
			AuthenticationFailureHandler oauthLoginFailureHandler) throws Exception {

		http
				.authorizeHttpRequests(auth -> auth
						.requestMatchers("/", "/error").permitAll()
						.requestMatchers("/actuator/health", "/actuator/info").permitAll()
						.requestMatchers("/login/**", "/oauth2/**").permitAll()
						.anyRequest().authenticated())
				.oauth2Login(login -> login
						.userInfoEndpoint(userInfo -> userInfo.oidcUserService(oidcUserService))
						.failureHandler(oauthLoginFailureHandler))
				.logout(logout -> logout
						.logoutUrl("/logout")
						.logoutSuccessHandler(new HttpStatusReturningLogoutSuccessHandler(HttpStatus.NO_CONTENT)))
				.exceptionHandling(ex -> ex.authenticationEntryPoint(authenticationEntryPoint))
				.csrf(csrf -> csrf.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse()));

		return http.build();
	}
}
