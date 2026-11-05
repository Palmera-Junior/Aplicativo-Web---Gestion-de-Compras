package com.palmera_junior.gestion_compras.rrhh.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.palmera_junior.gestion_compras.rrhh.entity.EmpleadoQrToken;

public interface EmpleadoQrTokenRepository extends JpaRepository<EmpleadoQrToken, Long> {

    Optional<EmpleadoQrToken> findByTokenHashAndActivoTrue(String tokenHash);

    Optional<EmpleadoQrToken> findByEmpleado_IdEmpleadoAndActivoTrue(Integer idEmpleado);
}
