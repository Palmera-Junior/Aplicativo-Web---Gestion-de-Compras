package com.palmera_junior.gestion_compras.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity // Nos permite proteger métodos individuales con anotaciones como @PreAuthorize
public class SecurityConfig {
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // Bean para encriptar contraseñas usando el estándar seguro BCrypt
    @Bean
public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http

        .csrf(csrf -> csrf
                .ignoringRequestMatchers("/api/**") 
            )
        .authorizeHttpRequests(auth -> auth
            // ⚠️ CRÍTICO: Permitimos acceso público al login y a todos los recursos estáticos 
            // (tu CSS, tus imágenes dentro de /imgs/, favicon, etc.)
            .requestMatchers("/login", "/login.css", "/imgs/**", "/static/**", "/api/ordenes/**").permitAll()
            
            // Cualquier otra ruta requiere que el usuario esté autenticado
            .anyRequest().authenticated()
        )
        .formLogin(form -> form
            // Especificamos que la ruta de nuestra vista de login es /login
            .loginPage("/login")
            // Redirección exitosa por defecto
            .defaultSuccessUrl("/dashboard", true)
            .permitAll()
        )
        .logout(logout -> logout
            .logoutUrl("/logout")
            // Al salir, redirigimos al login enviando el parámetro ?logout
            .logoutSuccessUrl("/login?logout")
            .permitAll()
        );

    return http.build();
}
}

