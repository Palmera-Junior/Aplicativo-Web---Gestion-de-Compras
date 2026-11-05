package com.palmera_junior.gestion_compras.rrhh.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.palmera_junior.gestion_compras.rrhh.entity.ContratoEmpleadoHistorialEstado;

public interface ContratoEmpleadoHistorialEstadoRepository
        extends JpaRepository<ContratoEmpleadoHistorialEstado, Long> {

    List<ContratoEmpleadoHistorialEstado>
            findAllByContrato_IdContratoEmpleadoOrderByFechaCambioDesc(Integer idContratoEmpleado);
}
