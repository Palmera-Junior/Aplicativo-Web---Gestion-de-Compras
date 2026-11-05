package com.palmera_junior.gestion_compras.rrhh.repository;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.palmera_junior.gestion_compras.rrhh.entity.AuditoriaEnvioCorreoEmpleado;
import com.palmera_junior.gestion_compras.entity.EstadoEnvioCorreo;

public interface AuditoriaEnvioCorreoEmpleadoRepository
        extends JpaRepository<AuditoriaEnvioCorreoEmpleado, Long> {

    boolean existsByEmpleado_IdEmpleadoAndDestinatarioIgnoreCaseAndClaveEvento(
            Integer idEmpleado, String destinatario, String claveEvento);

    List<AuditoriaEnvioCorreoEmpleado> findTop50ByEstadoInAndProximoIntentoLessThanEqualOrderByCreadoEnAsc(
            Collection<EstadoEnvioCorreo> estados, OffsetDateTime ahora);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update AuditoriaEnvioCorreoEmpleado a
               set a.estado = com.palmera_junior.gestion_compras.entity.EstadoEnvioCorreo.PROCESANDO,
                   a.intentos = a.intentos + 1,
                   a.bloqueadoEn = :ahora
             where a.idAuditoriaCorreoEmpleado = :id
               and a.estado in :estados
               and a.proximoIntento <= :ahora
            """)
    int reclamarParaEnvio(
            @Param("id") Long id,
            @Param("estados") Collection<EstadoEnvioCorreo> estados,
            @Param("ahora") OffsetDateTime ahora);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update AuditoriaEnvioCorreoEmpleado a
               set a.estado = com.palmera_junior.gestion_compras.entity.EstadoEnvioCorreo.REINTENTAR,
                   a.proximoIntento = :ahora,
                   a.bloqueadoEn = null,
                   a.ultimoError = 'Proceso interrumpido antes de terminar el envío'
             where a.estado = com.palmera_junior.gestion_compras.entity.EstadoEnvioCorreo.PROCESANDO
               and a.bloqueadoEn < :limite
            """)
    int liberarProcesamientosAtascados(
            @Param("limite") OffsetDateTime limite,
            @Param("ahora") OffsetDateTime ahora);
}
