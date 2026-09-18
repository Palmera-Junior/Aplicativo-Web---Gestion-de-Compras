package com.palmera_junior.gestion_compras.service.poliza;

import java.io.IOException;

import org.springframework.web.multipart.MultipartFile;

public interface IPolizaArchivoStorage {

    /** Almacena un adjunto y retorna la ruta relativa persistible, o {@code null} si está vacío. */
    String almacenar(MultipartFile archivo);

    byte[] leer(String rutaRelativa) throws IOException;

    /** Elimina un archivo previamente almacenado. */
    void eliminar(String rutaRelativa);
}
