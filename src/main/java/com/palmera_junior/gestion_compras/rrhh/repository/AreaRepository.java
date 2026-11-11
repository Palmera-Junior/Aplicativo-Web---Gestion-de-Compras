package com.palmera_junior.gestion_compras.rrhh.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.palmera_junior.gestion_compras.rrhh.entity.Area;

public interface AreaRepository extends JpaRepository<Area, Integer> {

    java.util.Optional<Area> findByIdAndActivoTrue(Integer id);
}
