package com.palmera_junior.gestion_compras.controller;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.palmera_junior.gestion_compras.dto.PolizaDTO;
import com.palmera_junior.gestion_compras.dto.ActivarPolizaDTO;
import com.palmera_junior.gestion_compras.entity.EstadoPoliza;
import com.palmera_junior.gestion_compras.entity.EstadoEnvioCorreo;
import com.palmera_junior.gestion_compras.entity.Poliza;
import com.palmera_junior.gestion_compras.entity.Usuario;
import com.palmera_junior.gestion_compras.service.catalogo.IProveedorService;
import com.palmera_junior.gestion_compras.service.correo.CorreoPolizaOutboxService;
import com.palmera_junior.gestion_compras.service.poliza.IPolizaService;
import com.palmera_junior.gestion_compras.service.usuario.IUsuarioService;

@Controller
@RequestMapping("/polizas")
public class PolizaController {

    private final IPolizaService polizaService;
    private final IProveedorService proveedorService;
    private final IUsuarioService usuarioService;
    private final CorreoPolizaOutboxService correoPolizaOutboxService;

    public PolizaController(IPolizaService polizaService, IProveedorService proveedorService,
            IUsuarioService usuarioService, CorreoPolizaOutboxService correoPolizaOutboxService) {
        this.polizaService = polizaService;
        this.proveedorService = proveedorService;
        this.usuarioService = usuarioService;
        this.correoPolizaOutboxService = correoPolizaOutboxService;
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

        int paginaSegura = Math.max(page, 0);
        int tamanoSeguro = Math.min(Math.max(size, 1), 100);
        Pageable pageable = PageRequest.of(paginaSegura, tamanoSeguro, Sort.by(Sort.Direction.DESC, "idPoliza"));
        Page<Poliza> polizasPage = polizaService.polizasPaginadas(
                pageable,
                q,
                fechaDesde,
                fechaHasta,
                idSede,
                esNacional,
                estado);

        Page<Poliza> polizasResumenPage = polizaService.polizasPaginadas(
                PageRequest.of(0, 10000),
                q,
                fechaDesde,
                fechaHasta,
                idSede,
                esNacional,
                estado);
        List<Poliza> polizasResumen = polizasResumenPage.getContent();

        model.addAttribute("polizasPage", polizasPage);
        model.addAttribute("estadosCorreoPoliza", correoPolizaOutboxService.obtenerEstadosPorPolizas(
            polizasPage.getContent().stream().map(Poliza::getIdPoliza).toList()));
        model.addAttribute("estadosPoliza", EstadoPoliza.values());
        model.addAttribute("estadoSeleccionado", estado);
        model.addAttribute("q", q);
        model.addAttribute("fechaDesde", fechaDesde);
        model.addAttribute("fechaHasta", fechaHasta);
        model.addAttribute("tamanoPagina", tamanoSeguro);
        model.addAttribute("paginaActual", Math.max(polizasPage.getNumber(), 0) + 1);
        model.addAttribute("polizasResumen", polizasResumen);
        model.addAttribute("proveedores", esNacional
            ? proveedorService.listarTodos()
            : proveedorService.listarPorSede(usuario.getSede().getIdSede()));
        model.addAttribute("polizasAnuladas", polizasResumen.stream().filter(p -> p.getEstado() == EstadoPoliza.ANULADA).count());
        model.addAttribute("polizasBorrador", polizasResumen.stream().filter(p -> p.getEstado() == EstadoPoliza.BORRADOR).count());
        model.addAttribute("polizasAprobadas", polizasResumen.stream().filter(p -> p.getEstado() == EstadoPoliza.APROBADA).count());
        model.addAttribute("polizasVigentes", polizasResumen.stream().filter(p -> p.getEstado() == EstadoPoliza.VIGENTE).count());
        model.addAttribute("polizasVencidas", polizasResumen.stream().filter(p -> p.getEstado() == EstadoPoliza.VENCIDA).count());
        model.addAttribute("usuarioActual", authentication.getName());
        model.addAttribute("polizaCreadorActual", nombreCompleto(usuario));
        model.addAttribute("polizaSedeActual", usuario.getSede() == null ? "Sin información" : usuario.getSede().getNombre());

        return "polizas";
    }

    @GetMapping("/correo-estados")
    @ResponseBody
    @PreAuthorize("hasAnyRole('COMERCIAL', 'APROBADOR', 'ADMINISTRADOR', 'SUPERADMINISTRADOR')")
    public Map<Integer, EstadoEnvioCorreo> estadosCorreoEnVivo(@RequestParam List<Integer> ids) {
        Set<Integer> idsVisibles = polizaService.listarPolizas().stream()
                .map(Poliza::getIdPoliza)
                .collect(Collectors.toSet());
        List<Integer> idsAutorizados = ids.stream()
                .filter(idsVisibles::contains)
                .distinct()
                .toList();
        return correoPolizaOutboxService.obtenerEstadosPorPolizas(idsAutorizados);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('COMERCIAL', 'APROBADOR', 'ADMINISTRADOR', 'SUPERADMINISTRADOR')")
    public Object guardarPoliza(@ModelAttribute PolizaDTO dto, RedirectAttributes redirectAttributes,
            HttpServletRequest request) {
        polizaService.guardarDesdeDTO(dto);
        if (esSolicitudAjax(request)) {
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(Map.of("message", "Póliza creada correctamente."));
        }
        redirectAttributes.addFlashAttribute("mensajeExito", "Póliza creada correctamente.");
        return "redirect:/polizas";
    }

    @PostMapping("/{id}/editar")
    @PreAuthorize("hasAnyRole('COMERCIAL', 'APROBADOR', 'ADMINISTRADOR', 'SUPERADMINISTRADOR')")
    public Object actualizarPoliza(
            @PathVariable Integer id,
            @ModelAttribute PolizaDTO dto,
            RedirectAttributes redirectAttributes,
            HttpServletRequest request) {
        polizaService.actualizarDesdeDTO(id, dto);
        if (esSolicitudAjax(request)) {
            return ResponseEntity.ok(Map.of("message", "Póliza actualizada correctamente."));
        }
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

    @PostMapping(value = "/{id}/activar", consumes = "multipart/form-data")
    @PreAuthorize("hasAnyRole('COMERCIAL', 'APROBADOR', 'ADMINISTRADOR', 'SUPERADMINISTRADOR')")
    public String activarPoliza(
            @PathVariable Integer id,
            @ModelAttribute ActivarPolizaDTO dto,
            RedirectAttributes redirectAttributes) {
        polizaService.activarVigente(id, dto.getFechaVencimiento(), dto.getPolizaFisica());
        redirectAttributes.addFlashAttribute("mensajeExito", "Póliza activada como vigente correctamente.");
        return "redirect:/polizas";
    }

    @PostMapping("/{id}/anular")
    @PreAuthorize("hasAnyRole('APROBADOR', 'ADMINISTRADOR', 'SUPERADMINISTRADOR')")
    public String anularPoliza(@PathVariable Integer id, RedirectAttributes redirectAttributes) {
        polizaService.anular(id);
        redirectAttributes.addFlashAttribute("mensajeExito", "Póliza anulada correctamente.");
        return "redirect:/polizas";
    }

    private boolean esSolicitudAjax(HttpServletRequest request) {
        return "XMLHttpRequest".equalsIgnoreCase(request.getHeader("X-Requested-With"));
    }

    private String nombreCompleto(Usuario usuario) {
        if (usuario == null) {
            return "Sin información";
        }
        String nombre = usuario.getNombre() == null ? "" : usuario.getNombre().trim();
        String apellido = usuario.getApellido() == null ? "" : usuario.getApellido().trim();
        String resultado = (nombre + " " + apellido).trim();
        return resultado.isEmpty() ? "Sin información" : resultado;
    }
}
