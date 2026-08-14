package com.palmera_junior.gestion_compras.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.palmera_junior.gestion_compras.entity.PresentacionProducto;

public interface PresentacionProductoRepository extends JpaRepository<PresentacionProducto, Integer> {

    List<PresentacionProducto> findByProductoIdProducto(Integer idProducto);

    Optional<PresentacionProducto> findByIdPresentacion(Integer idPresentacion);

    void deleteByProductoIdProducto(Integer idProducto);
}
