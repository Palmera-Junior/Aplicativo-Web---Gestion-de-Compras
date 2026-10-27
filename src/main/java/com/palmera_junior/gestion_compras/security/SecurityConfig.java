package com.palmera_junior.gestion_compras.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
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

    private static final Logger log = LoggerFactory.getLogger(SecurityConfig.class);

    private final CustomOAuth2UserService customOAuth2UserService;

    @Value("${security.oauth2.microsoft.enabled:false}")
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
     * y redirige a /admin solo para SUPERADMINISTRADOR; el rol ADMINISTRADOR entra al dashboard
     * y los demás roles siguen a /dashboard.
     * 
     * A dónde apunta:
     * - Redirecciones HTTP: /admin o /dashboard
     * 
     * @return {@link AuthenticationSuccessHandler} configurado.
     */
    @Bean
    public AuthenticationSuccessHandler successHandler() {
        return (request, response, authentication) -> {
            String destino = RutaInicialUsuario.resolver(authentication).orElse("/acceso-denegado");
            response.sendRedirect(request.getContextPath() + destino);
        };
    }

    /** Conserva el detalle del fallo OAuth2 únicamente durante la redirección al login. */
    @Bean
    public AuthenticationFailureHandler oauth2FailureHandler() {
        return (request, response, exception) -> {
            String detalle = detalleOAuth2(exception);
            log.warn("Falló la autenticación OAuth2: {}", detalle, exception);
            request.getSession(true).setAttribute("OAUTH2_ERROR_DETAIL", detalle);
            response.sendRedirect(request.getContextPath() + "/login?oauth2Error");
        };
    }

    private String detalleOAuth2(Exception exception) {
        Throwable causa = exception;
        while (causa.getCause() != null && causa.getCause() != causa) {
            causa = causa.getCause();
        }

        String mensaje = causa.getMessage();
        if (mensaje == null || mensaje.isBlank()) {
            mensaje = exception.getMessage();
        }
        if (mensaje == null || mensaje.isBlank()) {
            mensaje = "Sin mensaje proporcionado por el proveedor.";
        }
        return exception.getClass().getSimpleName() + ": " + mensaje;
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
                                "/login",
                                "/css/**",
                                "/imgs/**",
                                "/js/**",
                                "/static/**",
                                "/oauth2/**",
                                "/login/oauth2/**",
                                "/error"
                        ).permitAll()

                        .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                        .requestMatchers("/actuator/**").denyAll()

                        .requestMatchers("/admin/**")
                        .hasAnyAuthority(
                                "PERM_PANEL_ADMINISTRACION",
                                "PERM_PRODUCTOS_GESTIONAR", "PERM_PROVEEDORES_GESTIONAR",
                                "PERM_SEDES_GESTIONAR", "PERM_CENTROS_COSTO_GESTIONAR")

                        .requestMatchers("/polizas", "/polizas/**")
                        .hasAnyAuthority(
                                "PERM_POLIZAS_VER", "PERM_POLIZAS_GESTIONAR", "PERM_POLIZAS_APROBAR")

                        .requestMatchers("/orden-compra/*/aprobar")
                        .hasAuthority("PERM_ORDENES_APROBAR")

                        .requestMatchers(
                                "/orden-compra/*/detalles",
                                "/orden-compra/*/has-diferencias-recepcion",
                                "/orden-compra/has-diferencias",
                                "/orden-compra/*/pdf")
                        .hasAnyAuthority(
                                "PERM_ORDENES_VER", "PERM_ORDENES_GESTIONAR", "PERM_ORDENES_APROBAR")

                        .requestMatchers("/dashboard", "/dashboard/**")
                        .hasAnyAuthority(
                                "PERM_ORDENES_VER", "PERM_ORDENES_GESTIONAR", "PERM_ORDENES_APROBAR")

                        .requestMatchers("/ordenes/**", "/orden-compra/**")
                        .hasAnyAuthority(
                                "PERM_ORDENES_GESTIONAR", "PERM_ORDENES_APROBAR")

                        .requestMatchers("/api/usuario/**").authenticated()

                        .anyRequest().authenticated())
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
