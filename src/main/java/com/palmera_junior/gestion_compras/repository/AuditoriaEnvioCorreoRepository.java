package com.palmera_junior.gestion_compras.repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.palmera_junior.gestion_compras.entity.AuditoriaEnvioCorreo;
import com.palmera_junior.gestion_compras.entity.EstadoEnvioCorreo;

public interface AuditoriaEnvioCorreoRepository extends JpaRepository<AuditoriaEnvioCorreo, Long> {

    List<AuditoriaEnvioCorreo> findTop50ByEstadoInAndProximoIntentoLessThanEqualOrderByCreadoEnAsc(
            Collection<EstadoEnvioCorreo> estados, LocalDateTime ahora);

    // Ordenado por id descendente para poder quedarnos con el intento más reciente por orden
    List<AuditoriaEnvioCorreo> findByOrdenCompra_IdOrdenInOrderByIdDesc(Collection<Integer> idsOrdenes);

    // Último intento de envío de una orden puntual (para la acción manual de marcar como enviado)
    java.util.Optional<AuditoriaEnvioCorreo> findFirstByOrdenCompra_IdOrdenOrderByIdDesc(Integer idOrden);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update AuditoriaEnvioCorreo a
               set a.estado = com.palmera_junior.gestion_compras.entity.EstadoEnvioCorreo.PROCESANDO,
                   a.intentos = a.intentos + 1,
                   a.bloqueadoEn = :ahora,
                   a.actualizadoEn = :ahora
             where a.id = :id
               and a.estado in :estados
               and a.proximoIntento <= :ahora
            """)
    int reclamarParaEnvio(@Param("id") Long id,
            @Param("estados") Collection<EstadoEnvioCorreo> estados,
            @Param("ahora") LocalDateTime ahora);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update AuditoriaEnvioCorreo a
               set a.estado = com.palmera_junior.gestion_compras.entity.EstadoEnvioCorreo.REINTENTAR,
                   a.proximoIntento = :ahora,
                   a.bloqueadoEn = null,
                   a.actualizadoEn = :ahora,
                   a.ultimoError = 'Proceso interrumpido antes de terminar el envío'
             where a.estado = com.palmera_junior.gestion_compras.entity.EstadoEnvioCorreo.PROCESANDO
               and a.bloqueadoEn < :limite
            """)
    int liberarProcesamientosAtascados(@Param("limite") LocalDateTime limite,
            @Param("ahora") LocalDateTime ahora);
}
