package com.calendario.citas.usuario;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

/**
 * Usuario de la aplicación (PRD §2.1).
 *
 * <p>La identidad se resuelve por {@code email} verificado; el enlace con cada
 * proveedor OAuth vive en {@link IdentidadOauth}.
 */
@Entity
@Table(name = "usuario")
public class Usuario {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true, length = 320)
	private String email;

	@Column(nullable = false, length = 200)
	private String nombre;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private Rol rol;

	@Column(nullable = false)
	private boolean activo = true;

	@Column(name = "creado_en", nullable = false, updatable = false)
	private Instant creadoEn;

	protected Usuario() {
		// requerido por JPA
	}

	public Usuario(String email, String nombre, Rol rol) {
		this.email = email;
		this.nombre = nombre;
		this.rol = rol;
		this.activo = true;
	}

	@PrePersist
	void alCrear() {
		if (creadoEn == null) {
			creadoEn = Instant.now();
		}
	}

	public Long getId() {
		return id;
	}

	public String getEmail() {
		return email;
	}

	public String getNombre() {
		return nombre;
	}

	public void setNombre(String nombre) {
		this.nombre = nombre;
	}

	public Rol getRol() {
		return rol;
	}

	public void setRol(Rol rol) {
		this.rol = rol;
	}

	public boolean isActivo() {
		return activo;
	}

	public void setActivo(boolean activo) {
		this.activo = activo;
	}

	public Instant getCreadoEn() {
		return creadoEn;
	}
}
