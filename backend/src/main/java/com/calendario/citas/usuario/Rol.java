package com.calendario.citas.usuario;

/**
 * Roles de la aplicación (PRD §1.1). Solo dos.
 */
public enum Rol {

	/** Aprueba/rechaza solicitudes y gestiona recursos y usuarios. */
	ADMIN,

	/** Comercial o trabajador de la empresa. Crea y gestiona sus solicitudes. */
	EMPLEADO
}
