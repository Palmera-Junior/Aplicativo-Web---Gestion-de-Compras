package com.palmera_junior.gestion_compras.service;

import com.palmera_junior.gestion_compras.entity.OrdenCompra;

public interface IPdfService {
    byte[] generarPdfOrdenCompra(OrdenCompra orden) throws Exception;
}
