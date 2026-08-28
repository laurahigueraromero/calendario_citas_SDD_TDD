package com.calendario.citas.usuario;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import com.calendario.citas.support.JpaIntegrationTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UsuarioPersistenceIT extends JpaIntegrationTest {

	@Autowired
	private UsuarioRepository usuarios;

	@Autowired
	private IdentidadOauthRepository identidades;

	@Test
	void guardaUnUsuarioConDosIdentidades() {
		Usuario usuario = usuarios.save(new Usuario("laura@example.com", "Laura", Rol.ADMIN));

		identidades.save(new IdentidadOauth(usuario, ProveedorOauth.GITHUB, "gh-123", "lauraghub"));
		identidades.save(new IdentidadOauth(usuario, ProveedorOauth.GOOGLE, "goog-456", "laura@example.com"));

		assertThat(usuario.getId()).isNotNull();
		assertThat(usuario.getCreadoEn()).isNotNull();

		IdentidadOauth porGithub = identidades
				.findByProveedorAndProveedorId(ProveedorOauth.GITHUB, "gh-123")
				.orElseThrow();
		IdentidadOauth porGoogle = identidades
				.findByProveedorAndProveedorId(ProveedorOauth.GOOGLE, "goog-456")
				.orElseThrow();

		assertThat(porGithub.getUsuario().getId()).isEqualTo(usuario.getId());
		assertThat(porGoogle.getUsuario().getId()).isEqualTo(usuario.getId());
	}

	@Test
	void encuentraElUsuarioPorEmail() {
		usuarios.save(new Usuario("comercial@example.com", "Comercial", Rol.EMPLEADO));

		Usuario encontrado = usuarios.findByEmail("comercial@example.com").orElseThrow();

		assertThat(encontrado.getNombre()).isEqualTo("Comercial");
		assertThat(encontrado.getRol()).isEqualTo(Rol.EMPLEADO);
		assertThat(encontrado.isActivo()).isTrue();
		assertThat(usuarios.findByEmail("nadie@example.com")).isEmpty();
	}

	@Test
	void elEmailEsUnico() {
		usuarios.save(new Usuario("dup@example.com", "Primero", Rol.EMPLEADO));

		assertThatThrownBy(() -> usuarios.saveAndFlush(new Usuario("dup@example.com", "Segundo", Rol.EMPLEADO)))
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void elParProveedorYProveedorIdEsUnico() {
		Usuario a = usuarios.save(new Usuario("a@example.com", "A", Rol.EMPLEADO));
		Usuario b = usuarios.save(new Usuario("b@example.com", "B", Rol.EMPLEADO));
		identidades.save(new IdentidadOauth(a, ProveedorOauth.GITHUB, "gh-999", "a"));

		assertThatThrownBy(() -> identidades
				.saveAndFlush(new IdentidadOauth(b, ProveedorOauth.GITHUB, "gh-999", "b")))
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void mismoProveedorIdEnProveedoresDistintosSiSePermite() {
		Usuario usuario = usuarios.save(new Usuario("c@example.com", "C", Rol.EMPLEADO));

		identidades.save(new IdentidadOauth(usuario, ProveedorOauth.GITHUB, "shared-id", "c"));
		identidades.saveAndFlush(new IdentidadOauth(usuario, ProveedorOauth.GOOGLE, "shared-id", "c@example.com"));

		assertThat(identidades.count()).isEqualTo(2);
	}
}
