package com.palmera_junior.gestion_compras.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.GrantedAuthority;
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

                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        // Permitimos acceso público al login y a recursos estáticos
                        .requestMatchers("/login", "/login.css", "/imgs/**", "/static/**").permitAll()

                        // Solo ADMIN puede acceder a /admin/**
                        .requestMatchers("/admin/**").hasRole("ADMINISTRADOR")

                        // Dashboard y APIs/paths de órdenes solo para SOLICITANTE y APROBADOR
                        .requestMatchers("/dashboard", "/dashboard/**", "/ordenes/**", "/orden_compra/**", "/api/orden_compra/**", "/api/ordenes/**")
                        .hasAnyRole("SOLICITANTE", "APROBADOR")

                        // Cualquier otra ruta autenticada por defecto
                        .anyRequest().authenticated())
                .formLogin(form -> form
                        // Especificamos que la ruta de nuestra vista de login es /login
                        .loginPage("/login")
                        // Redirección de acuerdo al rol del usuario
                        .successHandler((request, response, authentication) -> {
                            boolean isAdmin = authentication.getAuthorities().stream()
                                    .map(GrantedAuthority::getAuthority)
                                    .anyMatch(authRole -> authRole.equals("ROLE_ADMINISTRADOR"));
                            response.sendRedirect(isAdmin ? "/admin" : "/dashboard");
                        })
                        .permitAll())
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        // Al salir, redirigimos al login enviando el parámetro ?logout
                        .logoutSuccessUrl("/login?logout")
                        .permitAll());

        return http.build();
    }
}
