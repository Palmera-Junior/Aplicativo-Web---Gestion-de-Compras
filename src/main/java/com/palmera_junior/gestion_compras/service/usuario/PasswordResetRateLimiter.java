package com.palmera_junior.gestion_compras.service.usuario;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PasswordResetRateLimiter {

    private final JdbcTemplate jdbcTemplate;
    private final AtomicInteger requests = new AtomicInteger();

    public PasswordResetRateLimiter(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean allow(String scope, String value, int limit) {
        String keyHash = hash(scope + ":" + value);
        Integer count = jdbcTemplate.queryForObject("""
                WITH current_window AS (
                    SELECT date_bin(
                        INTERVAL '15 minutes',
                        CURRENT_TIMESTAMP,
                        TIMESTAMPTZ '2000-01-01 00:00:00+00'
                    ) AS starts_at
                )
                INSERT INTO core.password_reset_rate_limit (key_hash, window_start, request_count)
                SELECT ?, starts_at, 1 FROM current_window
                ON CONFLICT (key_hash) DO UPDATE
                SET window_start = EXCLUDED.window_start,
                    request_count = CASE
                        WHEN core.password_reset_rate_limit.window_start = EXCLUDED.window_start
                            THEN LEAST(core.password_reset_rate_limit.request_count + 1, 100000)
                        ELSE 1
                    END
                RETURNING request_count
                """, Integer.class, keyHash);

        if (requests.incrementAndGet() % 100 == 0) {
            jdbcTemplate.update("""
                    DELETE FROM core.password_reset_rate_limit
                    WHERE window_start < CURRENT_TIMESTAMP - INTERVAL '1 day'
                    """);
        }
        return count != null && count <= limit;
    }

    private String hash(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is unavailable.", ex);
        }
    }
}
