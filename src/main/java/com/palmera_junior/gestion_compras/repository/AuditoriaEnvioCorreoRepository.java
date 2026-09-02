package com.palmera_junior.gestion_compras.repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.palmera_junior.gestion_compras.entity.AuditoriaEnvioCorreo;
import com.palmera_junior.gestion_compras.entity.EstadoEnvioCorreo;
import com.palmera_junior.gestion_compras.entity.TipoEnvioCorreo;

/**
 * Repositorio JPA para la entidad {@link AuditoriaEnvioCorreo}.
 * Soporta operaciones de consulta y bloqueo atómico del patrón Transactional Outbox.
 */
public interface AuditoriaEnvioCorreoRepository extends JpaRepository<AuditoriaEnvioCorreo, Long> {

    /**
     * Qué hace: Retorna hasta 50 registros outbox en los estados especificados cuya fecha de próximo intento sea menor o igual a la actual.
     * A dónde apunta: Tabla `auditoria_envio_correo`.
     */
    List<AuditoriaEnvioCorreo> findTop50ByEstadoInAndProximoIntentoLessThanEqualOrderByCreadoEnAsc(
            Collection<EstadoEnvioCorreo> estados, LocalDateTime ahora);

    // Ordenado por id descendente para poder quedarnos con el intento más reciente por orden
    /**
     * Qué hace: Retorna el historial de auditoría de envío para una lista de órdenes y un tipo de envío puntual, ordenado descendentemente por ID.
     * A dónde apunta: Tabla `auditoria_envio_correo`.
     */
    List<AuditoriaEnvioCorreo> findByOrdenCompra_IdOrdenInAndTipoEnvioOrderByIdDesc(
            Collection<Integer> idsOrdenes, TipoEnvioCorreo tipoEnvio);

    /**
     * Qué hace: Obtiene el registro de intento de envío más reciente de un tipo puntual (APROBACION/FACTURACION) para una orden.
     * A dónde apunta: Tabla `auditoria_envio_correo`.
     */
    Optional<AuditoriaEnvioCorreo> findFirstByOrdenCompra_IdOrdenAndTipoEnvioOrderByIdDesc(
            Integer idOrden, TipoEnvioCorreo tipoEnvio);

    /**
     * Qué hace: Bloquea atómicamente un registro outbox pasando su estado a PROCESANDO e incrementando los intentos.
     * A dónde apunta: Sentencia UPDATE sobre tabla `auditoria_envio_correo`.
     */
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

    /**
     * Qué hace: Restablece a REINTENTAR los registros que llevan más de 15 minutos en estado PROCESANDO.
     * A dónde apunta: Sentencia UPDATE sobre tabla `auditoria_envio_correo`.
     */
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

