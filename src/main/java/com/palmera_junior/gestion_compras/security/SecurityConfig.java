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

    // Bean para encriptar contraseñas usando el estándar seguro BCrypt
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // 1. Autorización de solicitudes
            .authorizeHttpRequests(auth -> auth
                // Permitimos que el navegador descargue CSS, JS, imágenes y fuentes sin loguearse
                .requestMatchers("/css/**", "/js/**", "/img/**", "/webjars/**").permitAll()
                // La página de login debe ser accesible para todos
                .requestMatchers("/login").permitAll()
                // Las páginas de administración (ej. crear sedes, usuarios) solo para el ADMIN
                .requestMatchers("/admin/**").hasRole("ADMINISTRADOR")
                // Cualquier otra pantalla del sistema (dashboard, ordenes, etc.) requiere login previo
                .anyRequest().authenticated()
            )
            // 2. Configuración del Login basado en Formulario HTML
            .formLogin(form -> form
                .loginPage("/login") // Le decimos a Spring que use nuestra propia ruta de login personalizada
                .defaultSuccessUrl("/dashboard", true) // Dirección de destino tras login exitoso
                .failureUrl("/login?error=true") // Redirección si la clave o usuario son incorrectos
                .usernameParameter("username") // El atributo 'name' del input de usuario en tu HTML
                .passwordParameter("password") // El atributo 'name' del input de contraseña en tu HTML
                .permitAll()
            )
            // 3. Configuración del Cierre de Sesión (Logout)
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout=true") // Redirección al salir
                .invalidateHttpSession(true) // Destruye la sesión en el servidor
                .deleteCookies("JSESSIONID") // Borra la cookie del navegador
                .permitAll()
            );

        return http.build();
    }
}