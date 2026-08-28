package com.calendario.citas.seguridad;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.calendario.citas.usuario.IdentidadOauth;
import com.calendario.citas.usuario.IdentidadOauthRepository;
import com.calendario.citas.usuario.Rol;
import com.calendario.citas.usuario.Usuario;
import com.calendario.citas.usuario.UsuarioRepository;

/**
 * Da de alta el usuario en su primer inicio de sesión y vincula identidades de
 * varios proveedores a la misma cuenta cuando comparten correo verificado
 * (PRD §1.2, RF-01).
 */
@Service
public class RegistroUsuarioService {

	private final UsuarioRepository usuarios;
	private final IdentidadOauthRepository identidades;

	public RegistroUsuarioService(UsuarioRepository usuarios, IdentidadOauthRepository identidades) {
		this.usuarios = usuarios;
		this.identidades = identidades;
	}

	/**
	 * Resuelve el {@link Usuario} para una identidad de proveedor:
	 * <ol>
	 *   <li>Si la identidad {@code (proveedor, proveedorId)} ya se conoce, devuelve su usuario.</li>
	 *   <li>Si existe un usuario con ese correo verificado, le añade la nueva identidad.</li>
	 *   <li>Si no, crea el usuario (rol {@link Rol#EMPLEADO}) y la identidad.</li>
	 * </ol>
	 */
	@Transactional
	public Usuario registrarOVincular(DatosIdentidad datos) {
		return identidades
				.findByProveedorAndProveedorId(datos.proveedor(), datos.proveedorId())
				.map(IdentidadOauth::getUsuario)
				.orElseGet(() -> vincularOCrear(datos));
	}

	private Usuario vincularOCrear(DatosIdentidad datos) {
		String email = datos.emailVerificado();
		if (email == null || email.isBlank()) {
			throw new IllegalArgumentException("El proveedor no facilitó un correo verificado");
		}

		Usuario usuario = usuarios
				.findByEmail(email)
				.orElseGet(() -> usuarios.save(new Usuario(email, nombrePara(datos, email), Rol.EMPLEADO)));

		identidades.save(new IdentidadOauth(usuario, datos.proveedor(), datos.proveedorId(), datos.handle()));
		return usuario;
	}

	private static String nombrePara(DatosIdentidad datos, String email) {
		if (datos.nombre() != null && !datos.nombre().isBlank()) {
			return datos.nombre();
		}
		if (datos.handle() != null && !datos.handle().isBlank()) {
			return datos.handle();
		}
		return email;
	}
}
