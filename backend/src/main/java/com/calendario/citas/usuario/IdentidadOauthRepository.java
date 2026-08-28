package com.calendario.citas.usuario;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface IdentidadOauthRepository extends JpaRepository<IdentidadOauth, Long> {

	Optional<IdentidadOauth> findByProveedorAndProveedorId(ProveedorOauth proveedor, String proveedorId);

	boolean existsByProveedorAndProveedorId(ProveedorOauth proveedor, String proveedorId);
}
