package com.palmera_junior.gestion_compras.controller;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.palmera_junior.gestion_compras.dto.PolizaDTO;
import com.palmera_junior.gestion_compras.entity.EstadoPoliza;
import com.palmera_junior.gestion_compras.entity.Poliza;
import com.palmera_junior.gestion_compras.entity.Usuario;
import com.palmera_junior.gestion_compras.service.catalogo.IProveedorService;
import com.palmera_junior.gestion_compras.service.poliza.IPolizaService;
import com.palmera_junior.gestion_compras.service.usuario.IUsuarioService;

@Controller
@RequestMapping("/polizas")
public class PolizaController {

    private final IPolizaService polizaService;
    private final IProveedorService proveedorService;
    private final IUsuarioService usuarioService;

    public PolizaController(IPolizaService polizaService, IProveedorService proveedorService,
            IUsuarioService usuarioService) {
        this.polizaService = polizaService;
        this.proveedorService = proveedorService;
        this.usuarioService = usuarioService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('COMERCIAL', 'APROBADOR', 'ADMINISTRADOR', 'SUPERADMINISTRADOR')")
    public String listarPolizas(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(defaultValue = "") String q,
            @RequestParam(defaultValue = "") String fechaDesde,
            @RequestParam(defaultValue = "") String fechaHasta,
            @RequestParam(required = false) String estado,
            Model model,
            Authentication authentication) {

        Usuario usuario = usuarioService.obtenerUsuarioAutenticado();
        Integer idSede = usuario.getSede() != null ? usuario.getSede().getIdSede() : null;
        boolean esNacional = usuario.getSede() != null
            && "Sede Nacional".equalsIgnoreCase(usuario.getSede().getNombre());

        Pageable pageable = PageRequest.of(Math.max(page, 0), size, Sort.by(Sort.Direction.DESC, "idPoliza"));
        Page<Poliza> polizasPage = polizaService.polizasPaginadas(
                pageable,
                q,
                fechaDesde,
                fechaHasta,
                idSede,
                esNacional,
                estado);

        Page<Poliza> polizasResumenPage = polizaService.polizasPaginadas(
                PageRequest.of(0, Integer.MAX_VALUE),
                q,
                fechaDesde,
                fechaHasta,
                idSede,
                esNacional,
                estado);
        List<Poliza> polizasResumen = polizasResumenPage.getContent();

        model.addAttribute("polizasPage", polizasPage);
        model.addAttribute("estadosPoliza", EstadoPoliza.values());
        model.addAttribute("estadoSeleccionado", estado);
        model.addAttribute("q", q);
        model.addAttribute("fechaDesde", fechaDesde);
        model.addAttribute("fechaHasta", fechaHasta);
        model.addAttribute("tamanoPagina", size);
        model.addAttribute("paginaActual", Math.max(polizasPage.getNumber(), 0) + 1);
        model.addAttribute("polizasResumen", polizasResumen);
        model.addAttribute("proveedores", esNacional
            ? proveedorService.listarTodos()
            : proveedorService.listarPorSede(usuario.getSede().getIdSede()));
        model.addAttribute("polizasAnuladas", polizasResumen.stream().filter(p -> p.getEstado() == EstadoPoliza.ANULADA).count());
        model.addAttribute("polizasBorrador", polizasResumen.stream().filter(p -> p.getEstado() == EstadoPoliza.BORRADOR).count());
        model.addAttribute("polizasAprobadas", polizasResumen.stream().filter(p -> p.getEstado() == EstadoPoliza.APROBADA).count());
        model.addAttribute("usuarioActual", authentication.getName());

        return "polizas";
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('COMERCIAL', 'APROBADOR', 'ADMINISTRADOR', 'SUPERADMINISTRADOR')")
    public String guardarPoliza(@ModelAttribute PolizaDTO dto, RedirectAttributes redirectAttributes) {
        polizaService.guardarDesdeDTO(dto);
        redirectAttributes.addFlashAttribute("mensajeExito", "Póliza creada correctamente.");
        return "redirect:/polizas";
    }

    @PostMapping("/{id}/editar")
    @PreAuthorize("hasAnyRole('COMERCIAL', 'APROBADOR', 'ADMINISTRADOR', 'SUPERADMINISTRADOR')")
    public String actualizarPoliza(
            @PathVariable Integer id,
            @ModelAttribute PolizaDTO dto,
            RedirectAttributes redirectAttributes) {
        polizaService.actualizarDesdeDTO(id, dto);
        redirectAttributes.addFlashAttribute("mensajeExito", "Póliza actualizada correctamente.");
        return "redirect:/polizas";
    }

    @PostMapping("/{id}/aprobar")
    @PreAuthorize("hasAnyRole('APROBADOR', 'ADMINISTRADOR', 'SUPERADMINISTRADOR')")
    public String aprobarPoliza(@PathVariable Integer id, RedirectAttributes redirectAttributes) {
        polizaService.aprobar(id);
        redirectAttributes.addFlashAttribute("mensajeExito", "Póliza aprobada correctamente.");
        return "redirect:/polizas";
    }

    @PostMapping("/{id}/anular")
    @PreAuthorize("hasAnyRole('APROBADOR', 'ADMINISTRADOR', 'SUPERADMINISTRADOR')")
    public String anularPoliza(@PathVariable Integer id, RedirectAttributes redirectAttributes) {
        polizaService.anular(id);
        redirectAttributes.addFlashAttribute("mensajeExito", "Póliza anulada correctamente.");
        return "redirect:/polizas";
    }
}
