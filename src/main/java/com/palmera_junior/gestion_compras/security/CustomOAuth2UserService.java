package com.palmera_junior.gestion_compras.security;

import java.util.List;
import java.util.Optional;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import com.palmera_junior.gestion_compras.entity.Usuario;
import com.palmera_junior.gestion_compras.service.usuario.IUsuarioService;

/**
 * Servicio personalizado para la carga y mapeo de usuarios federados vía OAuth2 (Google / Microsoft).
 * Extrae claims del token de identidad, verifica la preexistencia del usuario en base de datos por correo
 * y vincula la cuenta de proveedor con las autoridades y roles locales del sistema.
 */
@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final IUsuarioService usuarioService;

    /**
     * Constructor para inyección del servicio de usuarios.
     */
    public CustomOAuth2UserService(IUsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    /**
     * Qué hace:
     * Carga el perfil OAuth2 del proveedor federado, extrae el email y el ID de usuario único (`sub`/`oid`),
     * valida que el correo esté pre-registrado en la base de datos de Palmera Junior,
     * vincula el proveedor si es la primera vez que inicia sesión con OAuth2, y mapea el rol local ("ROLE_...").
     * 
     * A dónde apunta:
     * - Proveedores OAuth2 externos (Google Identity Platform).
     * - Base de datos interna: {@link IUsuarioService#buscarPorEmail(String)} y {@link IUsuarioService#guardar(Usuario)}.
     * 
     * @param userRequest Petición de usuario de Spring OAuth2 Client.
     * @return {@link OAuth2User} enriquecido con la entidad {@link Usuario} local.
     * @throws OAuth2AuthenticationException Si el correo no existe en el sistema o el proveedor no coincide.
     */
    @Override
    public OAuth2User loadUser(
            OAuth2UserRequest userRequest)
            throws OAuth2AuthenticationException {


        OAuth2User oAuth2User =
                super.loadUser(userRequest);

        String registrationId =
                userRequest.getClientRegistration()
                        .getRegistrationId();

        String email;
        String proveedorId;

        switch (registrationId.toLowerCase()) {

            case "google":

                email =
                        oAuth2User.getAttribute("email");

                proveedorId =
                        oAuth2User.getAttribute("sub");

                break;

            case "microsoft":

                email =
                        oAuth2User.getAttribute("email");

                if (email == null || email.isBlank()) {

                    email =
                            oAuth2User.getAttribute(
                                    "preferred_username");
                }

                proveedorId =
                        oAuth2User.getAttribute("oid");

                break;

            default:

                throw new OAuth2AuthenticationException(
                        "Proveedor OAuth no soportado: "
                                + registrationId);
        }

        if (email == null || email.isBlank()) {

            throw new OAuth2AuthenticationException(
                    "No fue posible obtener el correo electrónico.");
        }

        Optional<Usuario> usuarioOpt =
                usuarioService.buscarPorEmail(email);

        if (usuarioOpt.isEmpty()) {

            throw new OAuth2AuthenticationException(
                    "No existe una cuenta registrada con este correo.");
        }

        Usuario usuario = usuarioOpt.get();

        if (usuario.getProveedor() == null) {

            usuario.setProveedor(
                    registrationId);

            usuario.setProveedorId(
                    proveedorId);

            usuario =
                    usuarioService.guardar(usuario);

        } else if (!usuario.getProveedor()
                .equalsIgnoreCase(registrationId)) {

            throw new OAuth2AuthenticationException(
                    "Esta cuenta ya está vinculada con "
                            + usuario.getProveedor());
        }

        GrantedAuthority authority =
                new SimpleGrantedAuthority(
                        "ROLE_" + usuario.getRol().name());

        OAuth2User oauthUser =
                new DefaultOAuth2User(
                        List.of(authority),
                        oAuth2User.getAttributes(),
                        "email");

        return new CustomOAuth2User(
                oauthUser,
                usuario);
    }
}