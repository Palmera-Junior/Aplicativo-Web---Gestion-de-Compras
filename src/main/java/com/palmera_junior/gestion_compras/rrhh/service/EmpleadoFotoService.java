package com.palmera_junior.gestion_compras.rrhh.service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Optional;
import javax.imageio.ImageIO;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.palmera_junior.gestion_compras.entity.Empleado;
import com.palmera_junior.gestion_compras.rrhh.entity.EmpleadoFoto;
import com.palmera_junior.gestion_compras.rrhh.repository.EmpleadoFotoRepository;

@Service
public class EmpleadoFotoService {

    private static final long TAMANO_MAXIMO = 5L * 1024 * 1024;

    private final EmpleadoFotoRepository fotoRepository;

    public EmpleadoFotoService(EmpleadoFotoRepository fotoRepository) {
        this.fotoRepository = fotoRepository;
    }

    @Transactional(readOnly = true)
    public Optional<String> obtenerNombre(Integer idEmpleado) {
        return fotoRepository.findById(idEmpleado).map(EmpleadoFoto::getNombreOriginal);
    }

    @Transactional
    public void guardar(Empleado empleado, MultipartFile archivo) {
        if (archivo == null || archivo.isEmpty()) {
            return;
        }
        if (archivo.getSize() > TAMANO_MAXIMO) {
            throw new IllegalArgumentException("La fotografía no puede superar 5 MB");
        }
        try {
            byte[] contenido = archivo.getBytes();
            if (ImageIO.read(new ByteArrayInputStream(contenido)) == null) {
                throw new IllegalArgumentException("El archivo no es una imagen válida");
            }
            String tipo = switch (archivo.getContentType() == null ? "" : archivo.getContentType()) {
                case "image/jpeg" -> "image/jpeg";
                case "image/png" -> "image/png";
                default -> throw new IllegalArgumentException("Usa una fotografía PNG o JPEG");
            };
            String nombre = archivo.getOriginalFilename() == null
                    ? "foto-empleado" : archivo.getOriginalFilename().replace('\\', '/');
            nombre = nombre.substring(nombre.lastIndexOf('/') + 1).trim();
            EmpleadoFoto foto = fotoRepository.findById(empleado.getIdEmpleado())
                    .orElseGet(EmpleadoFoto::new);
            foto.setEmpleado(empleado);
            foto.setNombreOriginal(nombre.isBlank()
                    ? "foto-empleado" : nombre.substring(0, Math.min(255, nombre.length())));
            foto.setTipoMime(tipo);
            foto.setTamanoBytes((long) contenido.length);
            foto.setContenido(contenido);
            fotoRepository.save(foto);
        } catch (IOException ex) {
            throw new IllegalArgumentException("No fue posible leer la fotografía", ex);
        }
    }
}
