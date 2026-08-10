package com.palmera_junior.gestion_compras.security;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import com.palmera_junior.gestion_compras.entity.Usuario;
import com.palmera_junior.gestion_compras.service.UsuarioService;

@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    @Autowired
    private UsuarioService usuarioService;

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