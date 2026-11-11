package com.palmera_junior.gestion_compras.rrhh.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HashMap;
import java.util.Set;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.palmera_junior.gestion_compras.entity.Empleado;
import com.palmera_junior.gestion_compras.entity.Permiso;
import com.palmera_junior.gestion_compras.entity.Usuario;
import com.palmera_junior.gestion_compras.rrhh.entity.EmpleadoAdjunto;
import com.palmera_junior.gestion_compras.rrhh.entity.EmpleadoFoto;
import com.palmera_junior.gestion_compras.rrhh.entity.EmpleadoQrToken;
import com.palmera_junior.gestion_compras.rrhh.entity.EstadoEmpleado;
import com.palmera_junior.gestion_compras.rrhh.repository.EmpleadoAdjuntoRepository;
import com.palmera_junior.gestion_compras.rrhh.repository.EmpleadoFotoRepository;
import com.palmera_junior.gestion_compras.rrhh.repository.EmpleadoQrTokenRepository;
import com.palmera_junior.gestion_compras.repository.EmpleadoRepository;

@Service
public class EmpleadoQrService {

    private final EmpleadoRepository empleadoRepository;
    private final EmpleadoQrTokenRepository tokenRepository;
    private final EmpleadoFotoRepository fotoRepository;
    private final EmpleadoAdjuntoRepository adjuntoRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    public EmpleadoQrService(EmpleadoRepository empleadoRepository,
            EmpleadoQrTokenRepository tokenRepository, EmpleadoFotoRepository fotoRepository,
            EmpleadoAdjuntoRepository adjuntoRepository) {
        this.empleadoRepository = empleadoRepository;
        this.tokenRepository = tokenRepository;
        this.fotoRepository = fotoRepository;
        this.adjuntoRepository = adjuntoRepository;
    }

    @Transactional
    public void renovar(Integer idEmpleado, Usuario actor, String urlBasePublica) {
        exigirPermiso(actor, "RRHH_QR_GESTIONAR");
        if (urlBasePublica == null || urlBasePublica.isBlank()) {
            throw new IllegalArgumentException("La URL pública de la tarjeta es obligatoria");
        }
        Empleado empleado = empleadoRepository.findByIdForUpdate(idEmpleado)
                .orElseThrow(() -> new IllegalArgumentException("No existe el empleado"));
        if (empleado.getEstado() != EstadoEmpleado.ACTIVO) {
            throw new IllegalStateException("Solo se puede emitir una tarjeta para un empleado activo");
        }
        tokenRepository.findByEmpleado_IdEmpleadoAndActivoTrue(idEmpleado).ifPresent(token -> {
            token.invalidar();
            tokenRepository.saveAndFlush(token);
        });
        byte[] aleatorio = new byte[32];
        secureRandom.nextBytes(aleatorio);
        String secreto = Base64.getUrlEncoder().withoutPadding().encodeToString(aleatorio);
        EmpleadoQrToken token = new EmpleadoQrToken();
        token.setEmpleado(empleado);
        token.setEmitidoPor(actor);
        token.setTokenHash(hash(secreto));
        token.setImagenQr(generarImagenQr(urlBasePublica + secreto));
        tokenRepository.save(token);
    }

    @Transactional
    public void revocar(Integer idEmpleado, Usuario actor) {
        exigirPermiso(actor, "RRHH_QR_GESTIONAR");
        empleadoRepository.findByIdForUpdate(idEmpleado)
                .orElseThrow(() -> new IllegalArgumentException("No existe el empleado"));
        EmpleadoQrToken token = tokenRepository.findByEmpleado_IdEmpleadoAndActivoTrue(idEmpleado)
                .orElseThrow(() -> new IllegalStateException("El empleado no tiene un QR activo"));
        token.invalidar();
        tokenRepository.save(token);
    }

    @Transactional(readOnly = true)
    public boolean tieneQrActivo(Integer idEmpleado) {
        return tokenRepository.findByEmpleado_IdEmpleadoAndActivoTrue(idEmpleado).isPresent();
    }

    @Transactional(readOnly = true)
    public boolean tieneImagenQrActivo(Integer idEmpleado) {
        return tokenRepository.findByEmpleado_IdEmpleadoAndActivoTrue(idEmpleado)
                .map(EmpleadoQrToken::getImagenQr)
                .map(imagen -> imagen.length > 0)
                .orElse(false);
    }

    @Transactional(readOnly = true)
    public byte[] obtenerImagenQrActivo(Integer idEmpleado, Usuario actor) {
        exigirPermiso(actor, "RRHH_QR_GESTIONAR");
        return tokenRepository.findByEmpleado_IdEmpleadoAndActivoTrue(idEmpleado)
                .map(EmpleadoQrToken::getImagenQr)
                .filter(imagen -> imagen != null && imagen.length > 0)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No hay una imagen QR disponible para este empleado"));
    }

    @Transactional(readOnly = true)
    public boolean tieneFoto(Integer idEmpleado) {
        return fotoRepository.existsById(idEmpleado);
    }

    @Transactional(readOnly = true)
    public String nombreArlPublico(Integer idEmpleado) {
        return adjuntoRepository
                .findAllByEmpleado_IdEmpleadoAndPublicoQrTrueAndVigenteTrueOrderByFechaCargaDesc(
                        idEmpleado).stream()
                .filter(adjunto -> "3.1".equals(adjunto.getTipoAdjunto().getCodigo()))
                .map(EmpleadoAdjunto::getNombreOriginal)
                .findFirst().orElse(null);
    }

    @Transactional(readOnly = true)
    public ArchivoPublico fotoEmpleado(Integer idEmpleado, Usuario actor) {
        exigirPermiso(actor, "RRHH_QR_GESTIONAR");
        EmpleadoFoto foto = fotoRepository.findById(idEmpleado)
                .orElseThrow(() -> new IllegalArgumentException("La tarjeta no tiene fotografía"));
        return new ArchivoPublico(foto.getNombreOriginal(), foto.getTipoMime(), foto.getContenido());
    }

    @Transactional(readOnly = true)
    public TarjetaPublica consultar(String secreto) {
        Empleado empleado = validarToken(secreto).getEmpleado();
        String archivoArl = adjuntoRepository
                .findAllByEmpleado_IdEmpleadoAndPublicoQrTrueAndVigenteTrueOrderByFechaCargaDesc(
                        empleado.getIdEmpleado()).stream()
                .filter(adjunto -> "3.1".equals(adjunto.getTipoAdjunto().getCodigo()))
                .map(EmpleadoAdjunto::getNombreOriginal)
                .findFirst().orElse(null);
        String sede = empleado.getCentroCosto() == null || empleado.getCentroCosto().getSede() == null
                ? null : empleado.getCentroCosto().getSede().getNombre();
        String fotoUrl = fotoRepository.existsById(empleado.getIdEmpleado())
                ? "/recursos-humanos/tarjeta/" + secreto + "/foto"
                : null;
        return new TarjetaPublica(empleado.getPrimerNombre(), empleado.getSegundoNombre(),
                empleado.getPrimerApellido(), empleado.getSegundoApellido(),
                empleado.getCargo() == null ? null : empleado.getCargo().getNombre(),
                empleado.getArea() == null ? null : empleado.getArea().getNombre(),
                sede, empleado.getEstado().getEtiqueta(),
                empleado.getRh() == null ? null : empleado.getRh().name(), archivoArl,
                fotoUrl,
                archivoArl == null ? null : "/recursos-humanos/tarjeta/" + secreto + "/arl");
    }

    @Transactional(readOnly = true)
    public ArchivoPublico foto(String secreto) {
        Integer idEmpleado = validarToken(secreto).getEmpleado().getIdEmpleado();
        EmpleadoFoto foto = fotoRepository.findById(idEmpleado)
                .orElseThrow(() -> new IllegalArgumentException("La tarjeta no tiene fotografía"));
        return new ArchivoPublico(foto.getNombreOriginal(), foto.getTipoMime(), foto.getContenido());
    }

    @Transactional(readOnly = true)
    public ArchivoPublico certificadoArl(String secreto) {
        Integer idEmpleado = validarToken(secreto).getEmpleado().getIdEmpleado();
        EmpleadoAdjunto adjunto = adjuntoRepository
                .findAllByEmpleado_IdEmpleadoAndPublicoQrTrueAndVigenteTrueOrderByFechaCargaDesc(
                        idEmpleado).stream()
                .filter(a -> "3.1".equals(a.getTipoAdjunto().getCodigo()))
                .findFirst().orElseThrow(() -> new IllegalArgumentException(
                        "No hay certificado ARL autorizado para consulta pública"));
        if (!"application/pdf".equalsIgnoreCase(adjunto.getTipoMime())) {
            throw new IllegalArgumentException("El certificado ARL publicado no tiene un formato permitido");
        }
        return new ArchivoPublico(adjunto.getNombreOriginal(), adjunto.getTipoMime(), adjunto.getContenido());
    }

    private EmpleadoQrToken validarToken(String secreto) {
        if (secreto == null || secreto.length() < 40 || secreto.length() > 64) {
            throw new IllegalArgumentException("La tarjeta solicitada no existe o fue renovada");
        }
        return tokenRepository.findByTokenHashAndActivoTrue(hash(secreto))
                .orElseThrow(() -> new IllegalArgumentException(
                        "La tarjeta solicitada no existe o fue renovada"));
    }

    private static String hash(String secreto) {
        try {
            byte[] bytes = MessageDigest.getInstance("SHA-256")
                    .digest(secreto.getBytes(StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(bytes);
        } catch (java.security.NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 no está disponible", ex);
        }
    }

    private static byte[] generarImagenQr(String url) {
        try {
            var hints = new HashMap<EncodeHintType, Object>();
            hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
            hints.put(EncodeHintType.MARGIN, 2);
            BitMatrix matrix = new QRCodeWriter().encode(
                    url, BarcodeFormat.QR_CODE, 320, 320, hints);
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matrix, "PNG", output);
            return output.toByteArray();
        } catch (WriterException | IOException ex) {
            throw new IllegalStateException("No fue posible generar la imagen del QR", ex);
        }
    }

    private static void exigirPermiso(Usuario actor, String permiso) {
        Set<String> permisos = actor == null || actor.getPermisos() == null ? Set.of()
                : actor.getPermisos().stream().map(Permiso::getCodigo)
                        .collect(java.util.stream.Collectors.toSet());
        if (!permisos.contains(permiso)) {
            throw new SecurityException("No tiene permiso para gestionar tarjetas QR");
        }
    }

    public record TarjetaPublica(String primerNombre, String segundoNombre, String primerApellido,
            String segundoApellido, String cargo, String area, String sede, String estado,
            String rh, String arlNombre, String fotoUrl, String arlUrl) { }

    public record ArchivoPublico(String nombre, String mime, byte[] contenido) { }
}
