package com.palmera_junior.gestion_compras.service.poliza;

import java.io.IOException;
import java.util.Locale;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/** Valida PDFs para persistirlos como contenido binario en la base de datos. */
@Service
public class PolizaArchivoStorage implements IPolizaArchivoStorage {

    private static final long TAMANO_MAXIMO_BYTES = 10L * 1024 * 1024;
    private static final String EXTENSION_PDF = "pdf";
    private static final String TIPO_PDF = "application/pdf";

    @Override
    public byte[] almacenar(MultipartFile archivo) {
        if (archivo == null || archivo.isEmpty()) {
            return null;
        }
        if (archivo.getSize() > TAMANO_MAXIMO_BYTES) {
            throw new IllegalArgumentException("El adjunto no puede superar 10 MB");
        }

        String extension = obtenerExtension(archivo.getOriginalFilename());
        String tipo = archivo.getContentType() == null ? "" : archivo.getContentType().toLowerCase(Locale.ROOT);
        if (!EXTENSION_PDF.equals(extension) || !TIPO_PDF.equals(tipo)) {
            throw new IllegalArgumentException("El contrato adjunto debe ser un PDF válido");
        }

        try {
            byte[] contenido = archivo.getBytes();
            if (!esPdf(contenido)) {
                throw new IllegalArgumentException("El contrato adjunto debe ser un PDF válido");
            }
            return contenido;
        } catch (IOException exception) {
            throw new IllegalStateException("No fue posible leer el archivo adjunto", exception);
        }
    }

    @Override
    public byte[] leer(byte[] contenido) throws IOException {
        if (contenido == null || contenido.length == 0) {
            throw new IOException("El adjunto no tiene contenido almacenado en la base de datos");
        }
        return contenido;
    }

    private String obtenerExtension(String nombreOriginal) {
        if (nombreOriginal == null) {
            return "";
        }
        int ultimoPunto = nombreOriginal.lastIndexOf('.');
        return ultimoPunto < 1 ? "" : nombreOriginal.substring(ultimoPunto + 1).toLowerCase(Locale.ROOT);
    }

    private boolean esPdf(byte[] contenido) {
        return contenido != null && contenido.length >= 5
                && contenido[0] == '%'
                && contenido[1] == 'P'
                && contenido[2] == 'D'
                && contenido[3] == 'F'
                && contenido[4] == '-';
    }
}
