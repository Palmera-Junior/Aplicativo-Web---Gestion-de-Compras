package com.palmera_junior.gestion_compras.service.poliza;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/** Almacenamiento local de los contratos adjuntos de pólizas. */
@Service
public class PolizaArchivoStorage implements IPolizaArchivoStorage {

    private static final long TAMANO_MAXIMO_BYTES = 10L * 1024 * 1024;
    private static final String EXTENSION_PDF = "pdf";
    private static final String TIPO_PDF = "application/pdf";

    private final Path directorioBase;

    public PolizaArchivoStorage(@Value("${polizas.adjuntos.directorio:./uploads/polizas}") String directorio) {
        this.directorioBase = Path.of(directorio).toAbsolutePath().normalize();
    }

    @Override
    public String almacenar(MultipartFile archivo) {
        if (archivo == null || archivo.isEmpty()) {
            return null;
        }
        if (archivo.getSize() > TAMANO_MAXIMO_BYTES) {
            throw new IllegalArgumentException("El adjunto no puede superar 10 MB");
        }

        String extension = obtenerExtension(archivo.getOriginalFilename());
        String tipo = archivo.getContentType() == null ? "" : archivo.getContentType().toLowerCase(Locale.ROOT);
        if (!EXTENSION_PDF.equals(extension) || !TIPO_PDF.equals(tipo) || !esPdf(archivo)) {
            throw new IllegalArgumentException("El contrato adjunto debe ser un PDF válido");
        }

        String nombreSeguro = UUID.randomUUID() + "." + extension;
        Path destino = directorioBase.resolve(nombreSeguro).normalize();
        if (!destino.startsWith(directorioBase)) {
            throw new IllegalArgumentException("Nombre de archivo no válido");
        }

        try {
            Files.createDirectories(directorioBase);
            try (InputStream contenido = archivo.getInputStream()) {
                Files.copy(contenido, destino, StandardCopyOption.REPLACE_EXISTING);
            }
            return nombreSeguro;
        } catch (IOException exception) {
            throw new IllegalStateException("No fue posible guardar el archivo adjunto", exception);
        }
    }

    @Override
    public void eliminar(String rutaRelativa) {
        if (rutaRelativa == null || rutaRelativa.isBlank()) {
            return;
        }
        Path destino = directorioBase.resolve(rutaRelativa).normalize();
        if (!destino.startsWith(directorioBase)) {
            return;
        }
        try {
            Files.deleteIfExists(destino);
        } catch (IOException ignored) {
            // Un fallo de limpieza no debe ocultar el error original de la operación.
        }
    }

    @Override
    public byte[] leer(String rutaRelativa) throws IOException {
        if (rutaRelativa == null || rutaRelativa.isBlank()) {
            throw new IOException("La póliza no tiene un contrato adjunto");
        }
        Path archivo = directorioBase.resolve(rutaRelativa).normalize();
        if (!archivo.startsWith(directorioBase)) {
            throw new IOException("Ruta de adjunto no válida");
        }
        return Files.readAllBytes(archivo);
    }

    private String obtenerExtension(String nombreOriginal) {
        if (nombreOriginal == null) {
            return "";
        }
        int ultimoPunto = nombreOriginal.lastIndexOf('.');
        return ultimoPunto < 1 ? "" : nombreOriginal.substring(ultimoPunto + 1).toLowerCase(Locale.ROOT);
    }

    private boolean esPdf(MultipartFile archivo) {
        try (InputStream contenido = archivo.getInputStream()) {
            byte[] cabecera = contenido.readNBytes(5);
            return cabecera.length == 5
                    && cabecera[0] == '%'
                    && cabecera[1] == 'P'
                    && cabecera[2] == 'D'
                    && cabecera[3] == 'F'
                    && cabecera[4] == '-';
        } catch (IOException exception) {
            throw new IllegalArgumentException("No fue posible validar el PDF adjunto", exception);
        }
    }
}
