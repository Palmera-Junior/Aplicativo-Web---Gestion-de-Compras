package com.palmera_junior.gestion_compras.repository;

import java.util.Optional;

import org.springframework.data.domain.Pageable;
import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.palmera_junior.gestion_compras.entity.Empleado;
import com.palmera_junior.gestion_compras.entity.TipoDocumentoEmpleado;

public interface EmpleadoRepository extends JpaRepository<Empleado, Integer> {

    @EntityGraph(attributePaths = {"cargo", "area", "centroCosto"})
    java.util.List<Empleado> findAllByOrderByPrimerApellidoAscPrimerNombreAsc();

    @Query("""
            select empleado from Empleado empleado
            where lower(empleado.primerNombre) like lower(concat('%', :termino, '%'))
                or lower(coalesce(empleado.segundoNombre, '')) like lower(concat('%', :termino, '%'))
                or lower(empleado.primerApellido) like lower(concat('%', :termino, '%'))
                or lower(coalesce(empleado.segundoApellido, '')) like lower(concat('%', :termino, '%'))
                or lower(concat(concat(empleado.primerNombre, ' '), empleado.primerApellido))
                    like lower(concat('%', :termino, '%'))
            order by empleado.primerApellido, empleado.primerNombre
            """)
    java.util.List<Empleado> buscarPorNombre(@Param("termino") String termino, Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {"cargo", "area", "centroCosto", "nivelEducativo"})
    Optional<Empleado> findById(Integer id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select empleado from Empleado empleado where empleado.idEmpleado = :idEmpleado")
    Optional<Empleado> findByIdForUpdate(@Param("idEmpleado") Integer idEmpleado);

    Optional<Empleado> findByTipoDocumentoAndNumeroDocumento(
            TipoDocumentoEmpleado tipoDocumento, String numeroDocumento);

    boolean existsByTipoDocumentoAndNumeroDocumento(
            TipoDocumentoEmpleado tipoDocumento, String numeroDocumento);

    @Query(value = """
            SELECT EXISTS (
                SELECT 1
                FROM core.empleado
                WHERE correo_electronico IS NOT NULL
                  AND lower(btrim(correo_electronico)) = lower(btrim(:correoElectronico))
            )
            """, nativeQuery = true)
    boolean existsByCorreoElectronicoNormalizado(
            @Param("correoElectronico") String correoElectronico);

    @Query(value = """
            SELECT EXISTS (
                SELECT 1
                FROM core.empleado
                WHERE id_empleado <> :idEmpleado
                  AND correo_electronico IS NOT NULL
                  AND lower(btrim(correo_electronico)) = lower(btrim(:correoElectronico))
            )
            """, nativeQuery = true)
    boolean existsByCorreoElectronicoNormalizadoExcludingEmpleado(
            @Param("correoElectronico") String correoElectronico,
            @Param("idEmpleado") Integer idEmpleado);
}
