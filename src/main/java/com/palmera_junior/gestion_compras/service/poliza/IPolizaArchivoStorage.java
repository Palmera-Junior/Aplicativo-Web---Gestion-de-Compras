package com.palmera_junior.gestion_compras.service.poliza;

import java.io.IOException;

import org.springframework.web.multipart.MultipartFile;

public interface IPolizaArchivoStorage {

    /** Valida un adjunto y retorna su contenido binario, o {@code null} si está vacío. */
    byte[] almacenar(MultipartFile archivo);

    byte[] leer(byte[] contenido) throws IOException;
}
