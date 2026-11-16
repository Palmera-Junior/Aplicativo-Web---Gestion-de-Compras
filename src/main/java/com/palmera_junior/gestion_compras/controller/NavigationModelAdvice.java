package com.palmera_junior.gestion_compras.controller;

import com.palmera_junior.gestion_compras.entity.EstadoOrdenCompra;
import com.palmera_junior.gestion_compras.entity.EstadoAprobacionSede;
import com.palmera_junior.gestion_compras.entity.EstadoPoliza;
import com.palmera_junior.gestion_compras.entity.Usuario;
import com.palmera_junior.gestion_compras.repository.OrdenCompraRepository;
import com.palmera_junior.gestion_compras.repository.PolizaRepository;
import com.palmera_junior.gestion_compras.service.usuario.IUsuarioService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/** Datos de navegación compartidos por las vistas autenticadas. */
@ControllerAdvice(annotations = Controller.class)
public class NavigationModelAdvice {

    private final IUsuarioService usuarioService;
    private final OrdenCompraRepository ordenCompraRepository;
    private final PolizaRepository polizaRepository;

    /**
     * Construye el advice con los servicios necesarios para consultar el estado de aprobación
     * por sede y completar los datos que se reutilizan en todas las vistas autenticadas.
     */
    public NavigationModelAdvice(IUsuarioService usuarioService, OrdenCompraRepository ordenCompraRepository,
            PolizaRepository polizaRepository) {
        this.usuarioService = usuarioService;
        this.ordenCompraRepository = ordenCompraRepository;
        this.polizaRepository = polizaRepository;
    }

    /**
     * Carga en el modelo los conteos de pendientes de aprobación para el usuario autenticado,
     * de forma que el layout y los fragmentos de navegación puedan mostrar alertas contextualizadas.
     */
    @ModelAttribute
    public void agregarPendientesDeAprobacion(Model model, Authentication authentication) {
        model.addAttribute("borradoresOrdenesSede", 0L);
        model.addAttribute("borradoresPolizasSede", 0L);
        if (authentication == null || !authentication.isAuthenticated()) {
            return;
        }

        Usuario usuario = usuarioService.obtenerUsuarioAutenticado(authentication);
        if (usuario == null || usuario.getPermisos() == null || usuario.getIdsSedesAsignadas().isEmpty()) {
            return;
        }

        var permisos = usuario.getPermisos().stream()
                .map(permiso -> permiso.getCodigo())
                .collect(java.util.stream.Collectors.toSet());
        var sedes = usuario.getIdsSedesAsignadas().stream().toList();
        if (permisos.contains("ORDENES_APROBAR")) {
            model.addAttribute("borradoresOrdenesSede",
                    ordenCompraRepository.countPendientesAprobacionPorSedes(EstadoOrdenCompra.BORRADOR, sedes));
        }
        if (permisos.contains("POLIZAS_APROBAR")) {
            model.addAttribute("borradoresPolizasSede",
                    polizaRepository.countPendientesAprobacionPorSedes(
                            EstadoPoliza.BORRADOR, sedes, EstadoAprobacionSede.PENDIENTE));
        }
    }
}
