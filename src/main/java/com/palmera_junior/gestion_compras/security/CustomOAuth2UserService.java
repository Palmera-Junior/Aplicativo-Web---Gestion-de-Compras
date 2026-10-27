package com.palmera_junior.gestion_compras.security;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

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


        OAuth2User oAuth2User = loadProviderUser(userRequest);

        String registrationId =
                userRequest.getClientRegistration()
                        .getRegistrationId()
                        .toLowerCase(Locale.ROOT);

        String email;
        String proveedorId;

        switch (registrationId.toLowerCase()) {

            case "google":
                email = texto(oAuth2User.getAttribute("email"));
                proveedorId = texto(oAuth2User.getAttribute("sub"));
                break;

            case "microsoft":
                email = extraerCorreoMicrosoft(oAuth2User.getAttributes());
                proveedorId = texto(oAuth2User.getAttribute("oid"));
                if (!StringUtils.hasText(proveedorId)) {
                    proveedorId = texto(oAuth2User.getAttribute("sub"));
                }
                break;

            default:

                throw new OAuth2AuthenticationException(
                        "Proveedor OAuth no soportado: "
                                + registrationId);
        }

        if (!StringUtils.hasText(email)) {

            throw new OAuth2AuthenticationException(
                    "No fue posible obtener el correo electrónico.");
        }

        Usuario usuario = validarYVincularUsuario(registrationId, email, proveedorId);

        String nameAttributeKey;
        if (oAuth2User.getAttributes().containsKey("sub")) {
            nameAttributeKey = "sub";
        } else if (oAuth2User.getAttributes().containsKey("oid")) {
            nameAttributeKey = "oid";
        } else if (oAuth2User.getAttributes().containsKey("preferred_username")) {
            nameAttributeKey = "preferred_username";
        } else {
            nameAttributeKey = "email";
        }

        List<GrantedAuthority> authorities = new java.util.ArrayList<>();
        if (usuario.getRol() != null) {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + usuario.getRol().getCodigo()));
        }
        usuario.getPermisos().stream()
                .map(permiso -> new SimpleGrantedAuthority("PERM_" + permiso.getCodigo()))
                .forEach(authorities::add);

        OAuth2User oauthUser =
                new DefaultOAuth2User(
                        authorities,
                        oAuth2User.getAttributes(),
                        nameAttributeKey);

        return new CustomOAuth2User(
                oauthUser,
                usuario);
    }

    /**
     * Valida y vincula el usuario local a partir de los claims que entrega el proveedor.
     * Se comparte con el flujo OIDC de Microsoft, que Spring procesa mediante OidcUserService.
     */
    Usuario validarYVincularUsuario(String registrationId, String email, String proveedorId) {
        if (!StringUtils.hasText(email)) {
            throw new OAuth2AuthenticationException(
                    "No fue posible obtener el correo electrónico.");
        }

        Optional<Usuario> usuarioOpt = usuarioService.buscarPorEmail(email);

        if (usuarioOpt.isEmpty()) {

            throw new OAuth2AuthenticationException(
                    "No existe una cuenta registrada con este correo.");
        }

        Usuario usuario = usuarioOpt.get();

        if (!StringUtils.hasText(usuario.getEmail())) {
            throw new OAuth2AuthenticationException(
                    "La cuenta registrada no tiene un correo electrónico válido.");
        }

        if (!StringUtils.hasText(usuario.getProveedor())) {

            usuario.setProveedor(
                    registrationId);

            usuario.setProveedorId(
                    proveedorId);

            usuario =
                    usuarioService.guardar(usuario);

        } else if (!usuario.getProveedor().equalsIgnoreCase(registrationId)) {

            throw new OAuth2AuthenticationException(
                    "Esta cuenta ya está vinculada con "
                            + usuario.getProveedor());
        }

        return usuario;
    }

    static String extraerCorreoMicrosoft(Map<String, Object> atributos) {
        return List.of("preferred_username", "email", "mail", "userPrincipalName", "upn")
                .stream()
                .map(atributos::get)
                .map(CustomOAuth2UserService::texto)
                .filter(StringUtils::hasText)
                .findFirst()
                .orElse(null);
    }

    protected OAuth2User loadProviderUser(OAuth2UserRequest userRequest) {
        return super.loadUser(userRequest);
    }

    private static String texto(Object valor) {
        return valor instanceof String texto && StringUtils.hasText(texto)
                ? texto.trim()
                : null;
    }
}
