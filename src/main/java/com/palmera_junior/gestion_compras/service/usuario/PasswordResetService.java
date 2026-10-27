package com.palmera_junior.gestion_compras.service.usuario;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.palmera_junior.gestion_compras.entity.Usuario;
import com.palmera_junior.gestion_compras.repository.UsuarioRepository;
import com.palmera_junior.gestion_compras.service.correo.EmailTemplateService;
import com.palmera_junior.gestion_compras.service.correo.IEmailService;

import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;

@Service
public class PasswordResetService {

    private static final Logger log = LoggerFactory.getLogger(PasswordResetService.class);
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final JdbcTemplate jdbcTemplate;
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordResetRateLimiter rateLimiter;
    private final IEmailService emailService;
    private final EmailTemplateService emailTemplateService;

    public PasswordResetService(JdbcTemplate jdbcTemplate, UsuarioRepository usuarioRepository,
            PasswordEncoder passwordEncoder, PasswordResetRateLimiter rateLimiter,
            IEmailService emailService, EmailTemplateService emailTemplateService) {
        this.jdbcTemplate = jdbcTemplate;
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.rateLimiter = rateLimiter;
        this.emailService = emailService;
        this.emailTemplateService = emailTemplateService;
    }

    @Transactional
    public void solicitarRecuperacion(String email, String ipAddress) {
        if (!rateLimiter.allow("ip-reset-request", valorSeguro(ipAddress), 20)) {
            return;
        }
        String emailNormalizado = normalizarEmail(email);
        if (emailNormalizado == null || !rateLimiter.allow("email-reset-request", emailNormalizado, 5)) {
            return;
        }

        Integer userId = jdbcTemplate.query("""
                SELECT id_usuario FROM core.usuario
                WHERE LOWER(email) = ? FOR UPDATE
                """, rs -> rs.next() ? rs.getInt("id_usuario") : null, emailNormalizado);
        if (userId == null) {
            return;
        }
        Usuario usuario = usuarioRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException("La cuenta asociada al correo ya no existe."));
        if (!StringUtils.hasText(usuario.getEmail())) {
            return;
        }
        jdbcTemplate.update(
                "DELETE FROM core.password_reset_token WHERE id_usuario = ? AND usado_en IS NULL",
                usuario.getIdUsuario());
        jdbcTemplate.update("""
                DELETE FROM core.password_reset_token
                WHERE expira_en < CURRENT_TIMESTAMP - INTERVAL '30 days'
                """);

        byte[] randomToken = new byte[32];
        SECURE_RANDOM.nextBytes(randomToken);
        String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(randomToken);
        String tokenHash = hash(rawToken);
        jdbcTemplate.update("""
                INSERT INTO core.password_reset_token (id_usuario, token_hash, expira_en)
                VALUES (?, ?, CURRENT_TIMESTAMP + INTERVAL '20 minutes')
                """, usuario.getIdUsuario(), tokenHash);

        String url = emailTemplateService.urlRestablecimiento(rawToken);
        String html = emailTemplateService.generarCorreoRecuperacion(
                usuario.getNombre(), url);
        try {
            emailService.enviarNotificacionHtml(usuario.getEmail(), "Recuperación de contraseña", html);
        } catch (Exception ex) {
            log.error("No se pudo enviar un correo de recuperación de contraseña.", ex);
            throw new DeliveryException(ex);
        }
    }

    @Transactional(readOnly = true)
    public boolean tokenValido(String rawToken) {
        if (!StringUtils.hasText(rawToken) || rawToken.length() > 100) {
            return false;
        }
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM core.password_reset_token
                WHERE token_hash = ? AND usado_en IS NULL AND expira_en > CURRENT_TIMESTAMP
                """, Integer.class, hash(rawToken));
        return count != null && count == 1;
    }

    @Transactional
    public ResultadoRestablecimiento restablecer(String rawToken, String nuevaContrasena,
            String confirmacion, String ipAddress) {
        if (!rateLimiter.allow("ip-reset-complete", valorSeguro(ipAddress), 20)) {
            return ResultadoRestablecimiento.LIMITADO;
        }
        if (!StringUtils.hasText(rawToken) || rawToken.length() > 100) {
            return ResultadoRestablecimiento.TOKEN_INVALIDO;
        }

        TokenRecuperacion token = jdbcTemplate.query("""
                SELECT id_password_reset_token, id_usuario, usado_en, expira_en
                FROM core.password_reset_token
                WHERE token_hash = ? AND expira_en > CURRENT_TIMESTAMP
                FOR UPDATE
                """, rs -> rs.next()
                ? new TokenRecuperacion(rs.getLong("id_password_reset_token"),
                        rs.getInt("id_usuario"), rs.getTimestamp("usado_en"))
                : null, hash(rawToken));
        if (token == null || token.usadoEn() != null) {
            return ResultadoRestablecimiento.TOKEN_INVALIDO;
        }
        if (nuevaContrasena == null || !nuevaContrasena.equals(confirmacion)) {
            return ResultadoRestablecimiento.CONFIRMACION_INVALIDA;
        }
        if (!contrasenaCumplePolitica(nuevaContrasena)) {
            return ResultadoRestablecimiento.CONTRASENA_INVALIDA;
        }

        Usuario usuario = usuarioRepository.findById(token.idUsuario())
                .orElseThrow(() -> new IllegalStateException("La cuenta asociada al token ya no existe."));
        usuario.setContraseña(passwordEncoder.encode(nuevaContrasena));
        usuarioRepository.save(usuario);
        jdbcTemplate.update("""
                UPDATE core.password_reset_token
                SET usado_en = CURRENT_TIMESTAMP
                WHERE id_password_reset_token = ? AND usado_en IS NULL
                """, token.id());
        jdbcTemplate.update("""
                DELETE FROM core.password_reset_token
                WHERE id_usuario = ? AND usado_en IS NULL
                """, token.idUsuario());
        return ResultadoRestablecimiento.EXITOSO;
    }

    public static boolean contrasenaCumplePolitica(String password) {
        return password != null
                && password.codePointCount(0, password.length()) >= 12
                && password.getBytes(StandardCharsets.UTF_8).length <= 72;
    }

    private String normalizarEmail(String email) {
        if (!StringUtils.hasText(email)) {
            return null;
        }
        String normalized = email.trim().toLowerCase(Locale.ROOT);
        if (normalized.length() > 100) {
            return null;
        }
        try {
            InternetAddress address = new InternetAddress(normalized, true);
            address.validate();
            return normalized.equals(address.getAddress()) ? normalized : null;
        } catch (AddressException ex) {
            return null;
        }
    }

    private String valorSeguro(String value) {
        return StringUtils.hasText(value) ? value : "unknown";
    }

    private String hash(String value) {
        try {
            return java.util.HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is unavailable.", ex);
        }
    }

    public enum ResultadoRestablecimiento {
        EXITOSO,
        TOKEN_INVALIDO,
        LIMITADO,
        CONFIRMACION_INVALIDA,
        CONTRASENA_INVALIDA
    }

    public static class DeliveryException extends RuntimeException {
        public DeliveryException(Throwable cause) {
            super("No se pudo enviar el correo de recuperación.", cause);
        }
    }

    private record TokenRecuperacion(long id, int idUsuario, java.sql.Timestamp usadoEn) {
    }
}
