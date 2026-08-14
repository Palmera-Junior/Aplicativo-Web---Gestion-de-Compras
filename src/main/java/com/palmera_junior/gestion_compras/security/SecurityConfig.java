package com.palmera_junior.gestion_compras.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final CustomOAuth2UserService customOAuth2UserService;

    public SecurityConfig(CustomOAuth2UserService customOAuth2UserService) {
        this.customOAuth2UserService = customOAuth2UserService;
    }

    // Handler compartido: garantiza que el login por formulario Y por Google
    // redirijan siguiendo la MISMA regla de roles
    @Bean
    public AuthenticationSuccessHandler successHandler() {
        return (request, response, authentication) -> {
            boolean isAdmin = authentication.getAuthorities().stream()
                    .map(auth -> auth.getAuthority())
                    .anyMatch(authRole -> authRole.equals("ROLE_ADMINISTRADOR"));
            response.sendRedirect(isAdmin ? "/admin" : "/dashboard");
        };
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/login", "/login.css", "/imgs/**", "/static/**",
                                "/oauth2/**", "/login/oauth2/**" // rutas del flujo de Google
                        ).permitAll()

                        .requestMatchers("/admin/**").hasRole("ADMINISTRADOR")

                        .requestMatchers("/dashboard", "/dashboard/**", "/ordenes/**", "/orden_compra/**", "/api/orden_compra/**", "/api/ordenes/**")
                        .hasAnyRole("SOLICITANTE", "APROBADOR")

                        .anyRequest().authenticated())
                .formLogin(form -> form
                        .loginPage("/login")
                        .successHandler(successHandler())
                        .permitAll())
                .oauth2Login(oauth2 -> oauth2
                        .loginPage("/login")
                        .userInfoEndpoint(userInfo -> userInfo.userService(customOAuth2UserService))
                        .successHandler(successHandler())
                        .failureUrl("/login?error") // mismo comportamiento que un login fallido normal
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout")
                        .permitAll());

        return http.build();
    }
}