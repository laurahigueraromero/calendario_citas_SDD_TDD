package com.calendario.citas.usuario;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * Identidad de un usuario en un proveedor OAuth2 concreto (PRD §2.1).
 *
 * <p>Un {@link Usuario} puede tener varias: una por proveedor con el que ha
 * iniciado sesión. El par {@code (proveedor, proveedorId)} es único.
 */
@Entity
@Table(
		name = "identidad_oauth",
		uniqueConstraints = @UniqueConstraint(
				name = "uk_identidad_proveedor",
				columnNames = {"proveedor", "proveedor_id"}))
public class IdentidadOauth {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(optional = false, fetch = FetchType.LAZY)
	@JoinColumn(name = "usuario_id", nullable = false)
	private Usuario usuario;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private ProveedorOauth proveedor;

	/** Identificador estable del usuario en el proveedor ({@code sub} en Google, {@code id} en GitHub). */
	@Column(name = "proveedor_id", nullable = false)
	private String proveedorId;

	/** Dato informativo: {@code login} de GitHub o correo de Google. */
	@Column(length = 320)
	private String handle;

	@Column(name = "creado_en", nullable = false, updatable = false)
	private Instant creadoEn;

	protected IdentidadOauth() {
		// requerido por JPA
	}

	public IdentidadOauth(Usuario usuario, ProveedorOauth proveedor, String proveedorId, String handle) {
		this.usuario = usuario;
		this.proveedor = proveedor;
		this.proveedorId = proveedorId;
		this.handle = handle;
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

	public Usuario getUsuario() {
		return usuario;
	}

	public ProveedorOauth getProveedor() {
		return proveedor;
	}

	public String getProveedorId() {
		return proveedorId;
	}

	public String getHandle() {
		return handle;
	}

	public void setHandle(String handle) {
		this.handle = handle;
	}

	public Instant getCreadoEn() {
		return creadoEn;
	}
}
