package com.calendario.citas.seguridad;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import com.calendario.citas.support.IntegrationTest;
import com.calendario.citas.usuario.IdentidadOauthRepository;
import com.calendario.citas.usuario.ProveedorOauth;
import com.calendario.citas.usuario.Rol;
import com.calendario.citas.usuario.Usuario;
import com.calendario.citas.usuario.UsuarioRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Transactional
class RegistroUsuarioServiceIT extends IntegrationTest {

	@Autowired
	private RegistroUsuarioService registro;

	@Autowired
	private UsuarioRepository usuarios;

	@Autowired
	private IdentidadOauthRepository identidades;

	@Test
	void primerLoginCreaUsuarioEIdentidad() {
		Usuario usuario = registro.registrarOVincular(new DatosIdentidad(
				ProveedorOauth.GITHUB, "gh-1", "laura@example.com", "Laura", "laurah"));

		assertThat(usuario.getId()).isNotNull();
		assertThat(usuario.getEmail()).isEqualTo("laura@example.com");
		assertThat(usuario.getRol()).isEqualTo(Rol.EMPLEADO);
		assertThat(identidades.existsByProveedorAndProveedorId(ProveedorOauth.GITHUB, "gh-1")).isTrue();
	}

	@Test
	void identidadConocidaDevuelveElMismoUsuarioSinDuplicar() {
		Usuario primero = registro.registrarOVincular(new DatosIdentidad(
				ProveedorOauth.GITHUB, "gh-2", "a@example.com", "A", "a"));
		Usuario segundo = registro.registrarOVincular(new DatosIdentidad(
				ProveedorOauth.GITHUB, "gh-2", "a@example.com", "A", "a"));

		assertThat(segundo.getId()).isEqualTo(primero.getId());
		assertThat(usuarios.count()).isEqualTo(1);
		assertThat(identidades.count()).isEqualTo(1);
	}

	@Test
	void segundoProveedorConMismoEmailVerificadoVinculaSinCrearUsuario() {
		Usuario porGithub = registro.registrarOVincular(new DatosIdentidad(
				ProveedorOauth.GITHUB, "gh-3", "misma@example.com", "Nombre", "gh"));
		Usuario porGoogle = registro.registrarOVincular(new DatosIdentidad(
				ProveedorOauth.GOOGLE, "goog-3", "misma@example.com", "Nombre", "misma@example.com"));

		assertThat(porGoogle.getId()).isEqualTo(porGithub.getId());
		assertThat(usuarios.count()).isEqualTo(1);
		assertThat(identidades.count()).isEqualTo(2);
	}

	@Test
	void emailDistintoCreaUsuarioNuevo() {
		registro.registrarOVincular(new DatosIdentidad(
				ProveedorOauth.GITHUB, "gh-4", "uno@example.com", "Uno", "uno"));
		registro.registrarOVincular(new DatosIdentidad(
				ProveedorOauth.GOOGLE, "goog-4", "dos@example.com", "Dos", "dos@example.com"));

		assertThat(usuarios.count()).isEqualTo(2);
	}

	@Test
	void sinEmailVerificadoLanzaExcepcion() {
		assertThatThrownBy(() -> registro.registrarOVincular(new DatosIdentidad(
				ProveedorOauth.GITHUB, "gh-5", "  ", "X", "x")))
				.isInstanceOf(IllegalArgumentException.class);

		assertThat(usuarios.count()).isZero();
	}

	@Test
	void usaElHandleComoNombreSiElProveedorNoDaNombre() {
		Usuario usuario = registro.registrarOVincular(new DatosIdentidad(
				ProveedorOauth.GITHUB, "gh-6", "sinnombre@example.com", null, "elhandle"));

		assertThat(usuario.getNombre()).isEqualTo("elhandle");
	}
}
