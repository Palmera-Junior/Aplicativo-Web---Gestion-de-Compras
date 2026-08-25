package com.palmera_junior.gestion_compras.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Configuración del codificador de contraseñas de la aplicación.
 */
@Configuration
public class PasswordEncoderConfig {

    /**
     * Qué hace:
     * Provee el bean {@link PasswordEncoder} basado en el algoritmo hash adaptativo BCrypt.
     * 
     * @return Instancia de {@link BCryptPasswordEncoder}.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}

