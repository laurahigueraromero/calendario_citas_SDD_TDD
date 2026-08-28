package com.calendario.citas.seguridad;

import com.calendario.citas.usuario.ProveedorOauth;

/**
 * Datos que un proveedor OAuth2 aporta sobre el usuario, ya normalizados y con
 * el correo verificado, listos para dar de alta o vincular la cuenta.
 */
public record DatosIdentidad(
		ProveedorOauth proveedor,
		String proveedorId,
		String emailVerificado,
		String nombre,
		String handle) {
}
