package com.palmera_junior.gestion_compras.rrhh.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.palmera_junior.gestion_compras.rrhh.entity.NivelEducativo;

public interface NivelEducativoRepository extends JpaRepository<NivelEducativo, Integer> {

    java.util.Optional<NivelEducativo> findByIdAndActivoTrue(Integer id);
}
