package com.palmera_junior.gestion_compras.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

/**
 * Configuración central de seguridad de Spring Security.
 * Define la cadena de filtros HTTP, políticas de autorización basadas en roles (RBAC),
 * login híbrido (formulario local + Google OAuth2), protección CSRF con cookies y control de sesiones.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final CustomOAuth2UserService customOAuth2UserService;

    @Value("${security.oauth2.microsoft.enabled:true}")
    private boolean microsoftOAuth2Enabled;

    /**
     * Constructor para inyección del servicio OAuth2 personalizado.
     */
    public SecurityConfig(CustomOAuth2UserService customOAuth2UserService) {
        this.customOAuth2UserService = customOAuth2UserService;
    }

    /**
     * Qué hace:
     * Retorna un handler de éxito unificado que inspecciona las autoridades del usuario autenticado
     * y lo redirige a /admin (si tiene rol ROLE_ADMINISTRADOR) o a /dashboard (para los demás roles).
     * 
     * A dónde apunta:
     * - Redirecciones HTTP: /admin o /dashboard
     * 
     * @return {@link AuthenticationSuccessHandler} configurado.
     */
    @Bean
    public AuthenticationSuccessHandler successHandler() {
        return (request, response, authentication) -> {
            boolean isAdmin = authentication.getAuthorities().stream()
                    .map(auth -> auth.getAuthority())
                    .anyMatch(authRole -> authRole.equals("ROLE_ADMINISTRADOR"));
            response.sendRedirect(isAdmin ? "/admin" : "/dashboard");
        };
    }

    /**
     * Qué hace:
     * Construye la cadena de filtros de seguridad HTTP, definiendo:
     * - Recursos públicos (login, estilos css, imágenes, endpoints OAuth2, healthchecks).
     * - Rutas restringidas a administradores (/admin/**).
     * - Rutas de compras y dashboard (/dashboard, /orden-compra/**, /api/usuario/**).
     * - Repositorio de tokens CSRF vía cookie accesible por JavaScript (CookieCsrfTokenRepository).
     * - Cierre de sesión y protección de fijación de sesión (changeSessionId).
     * 
     * A dónde apunta:
     * - Filtros de Spring Security para todas las peticiones HTTP entrantes.
     * 
     * @param http Constructor de seguridad HTTP.
     * @return Cadena de filtros {@link SecurityFilterChain}.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/login", "/login.css", "/imgs/**", "/static/**",
                                "/oauth2/**", "/login/oauth2/**" // rutas del flujo de Google
                        ).permitAll()

                        .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                        .requestMatchers("/actuator/**").denyAll()

                        .requestMatchers("/admin/**").hasRole("ADMINISTRADOR")

                        .requestMatchers("/dashboard", "/dashboard/**", "/ordenes/**", "/orden-compra/**", "/api/usuario/**")
                        .hasAnyRole("SOLICITANTE", "APROBADOR", "ADMINISTRADOR")

                        .requestMatchers("/dashboard.js", "/index.css", "/admin.css", "/admin.js", "/security.js")
                        .authenticated()

                        .anyRequest().denyAll())
                .csrf(csrf -> csrf
                        .csrfTokenRepository(org.springframework.security.web.csrf.CookieCsrfTokenRepository.withHttpOnlyFalse())
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .successHandler(successHandler())
                        .permitAll())
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout")
                        .permitAll())
                .sessionManagement(session -> session
                        .sessionFixation(fixation -> fixation.changeSessionId()));

        // Permite desplegar sin credenciales de Microsoft configuradas.
        if (microsoftOAuth2Enabled) {
            http.oauth2Login(oauth2 -> oauth2
                    .loginPage("/login")
                    .userInfoEndpoint(userInfo -> userInfo.userService(customOAuth2UserService))
                    .successHandler(successHandler())
                    .failureUrl("/login?error") // mismo comportamiento que un login fallido normal
            );
        }

        return http.build();
    }
}

