package com.palmera_junior.gestion_compras.rrhh.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.palmera_junior.gestion_compras.rrhh.entity.Cargo;

public interface CargoRepository extends JpaRepository<Cargo, Integer> {

    java.util.Optional<Cargo> findByIdAndActivoTrue(Integer id);
}
