package com.palmera_junior.gestion_compras.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.palmera_junior.gestion_compras.entity.Empleado;
import com.palmera_junior.gestion_compras.entity.TipoDocumentoEmpleado;

public interface EmpleadoRepository extends JpaRepository<Empleado, Integer> {

    @EntityGraph(attributePaths = {"cargo", "area", "centroCosto"})
    java.util.List<Empleado> findAllByOrderByPrimerApellidoAscPrimerNombreAsc();

    Optional<Empleado> findByTipoDocumentoAndNumeroDocumento(
            TipoDocumentoEmpleado tipoDocumento, String numeroDocumento);

    boolean existsByTipoDocumentoAndNumeroDocumento(
            TipoDocumentoEmpleado tipoDocumento, String numeroDocumento);
}
