package com.palmera_junior.gestion_compras.security;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.security.core.Authentication;

/** Resuelve una página principal solo cuando el usuario tiene acceso a ella. */
public final class RutaInicialUsuario {

    private RutaInicialUsuario() {
    }

    public static Optional<String> resolver(Authentication authentication) {
        Set<String> autoridades = authentication.getAuthorities().stream()
                .map(authority -> authority.getAuthority())
                .collect(Collectors.toSet());

        if (autoridades.contains("PERM_PANEL_ADMINISTRACION")) {
            return Optional.of("/admin");
        }
        if (tieneAlguna(autoridades, "PERM_ORDENES_VER", "PERM_ORDENES_GESTIONAR", "PERM_ORDENES_APROBAR")) {
            return Optional.of("/dashboard");
        }
        if (tieneAlguna(autoridades, "PERM_POLIZAS_VER", "PERM_POLIZAS_GESTIONAR", "PERM_POLIZAS_APROBAR")) {
            return Optional.of("/polizas");
        }

        return Optional.empty();
    }

    private static boolean tieneAlguna(Set<String> autoridades, String... candidatas) {
        for (String candidata : candidatas) {
            if (autoridades.contains(candidata)) {
                return true;
            }
        }
        return false;
    }
}
