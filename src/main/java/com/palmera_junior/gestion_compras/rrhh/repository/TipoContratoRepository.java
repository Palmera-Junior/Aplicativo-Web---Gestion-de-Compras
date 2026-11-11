package com.palmera_junior.gestion_compras.rrhh.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.palmera_junior.gestion_compras.rrhh.entity.TipoContrato;

public interface TipoContratoRepository extends JpaRepository<TipoContrato, Integer> {

    java.util.Optional<TipoContrato> findByIdAndActivoTrue(Integer id);
}
