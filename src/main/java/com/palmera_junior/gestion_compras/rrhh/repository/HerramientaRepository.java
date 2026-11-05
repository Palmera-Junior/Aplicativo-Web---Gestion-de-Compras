package com.palmera_junior.gestion_compras.rrhh.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.palmera_junior.gestion_compras.rrhh.entity.Herramienta;

public interface HerramientaRepository extends JpaRepository<Herramienta, Integer> {

    Optional<Herramienta> findByCodigo(String codigo);

    List<Herramienta> findAllByActivaTrueOrderByNombreAsc();
}
