package com.palmera_junior.gestion_compras.controller;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.time.LocalDate;

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
import com.palmera_junior.gestion_compras.dto.DetallePrimaDTO;
import com.palmera_junior.gestion_compras.dto.ProveedorAutocompleteDTO;
import com.palmera_junior.gestion_compras.entity.EstadoPoliza;
import com.palmera_junior.gestion_compras.entity.EstadoEnvioCorreo;
import com.palmera_junior.gestion_compras.entity.Poliza;
import com.palmera_junior.gestion_compras.entity.EstadoAprobacionSede;
import com.palmera_junior.gestion_compras.entity.Usuario;
import com.palmera_junior.gestion_compras.entity.Proveedor;
import com.palmera_junior.gestion_compras.service.catalogo.IProveedorService;
import com.palmera_junior.gestion_compras.service.correo.CorreoPolizaOutboxService;
import com.palmera_junior.gestion_compras.service.organizacion.ISedeService;
import com.palmera_junior.gestion_compras.service.poliza.IPolizaService;
import com.palmera_junior.gestion_compras.service.usuario.IUsuarioService;

/**
 * Controlador de gestión de pólizas del sistema.
 * Centraliza el listado, búsqueda, aprobación, activación y edición de pólizas, así como la
 * consulta de datos auxiliares necesarios para la pantalla de administración.
 */
@Controller
@RequestMapping("/polizas")
public class PolizaController {

    private final IPolizaService polizaService;
    private final IProveedorService proveedorService;
    private final IUsuarioService usuarioService;
    private final ISedeService sedeService;
    private final CorreoPolizaOutboxService correoPolizaOutboxService;

    /**
     * Construye el controlador con los servicios de pólizas, proveedores, usuarios, sedes y correos.
     */
    public PolizaController(IPolizaService polizaService, IProveedorService proveedorService,
            IUsuarioService usuarioService, ISedeService sedeService,
            CorreoPolizaOutboxService correoPolizaOutboxService) {
        this.polizaService = polizaService;
        this.proveedorService = proveedorService;
        this.usuarioService = usuarioService;
        this.sedeService = sedeService;
        this.correoPolizaOutboxService = correoPolizaOutboxService;
    }

    /**
     * Muestra la vista principal de pólizas con filtros, paginación y totales por estado.
     * La consulta se ajusta según la sede del usuario para respetar el alcance funcional del rol.
     */
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

        if ((fechaDesde == null || fechaDesde.isBlank())
                && (fechaHasta == null || fechaHasta.isBlank())) {
            LocalDate ahora = LocalDate.now();
            fechaDesde = ahora.withDayOfMonth(1).toString();
            fechaHasta = ahora.withDayOfMonth(ahora.lengthOfMonth()).toString();
        }

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
        long totalPolizas = polizasResumen.stream()
            .filter(this::esPolizaValidaParaTotales)
            .count();

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
        model.addAttribute("sedes", sedeService.listarTodos());
        model.addAttribute("sedeActualId", usuario.getSede() != null ? usuario.getSede().getIdSede() : null);
        model.addAttribute("polizaSedeNacional", esNacional);
        model.addAttribute("totalPolizas", totalPolizas);
        model.addAttribute("polizasAnuladas", polizasResumen.stream().filter(p -> p.getEstado() == EstadoPoliza.ANULADA).count());
        model.addAttribute("polizasBorrador", polizasResumen.stream().filter(p -> p.getEstado() == EstadoPoliza.BORRADOR).count());
        model.addAttribute("polizasAprobadas", polizasResumen.stream().filter(p -> p.getEstado() == EstadoPoliza.APROBADA).count());
        model.addAttribute("polizasVigentes", polizasResumen.stream().filter(p -> p.getEstado() == EstadoPoliza.VIGENTE).count());
        model.addAttribute("polizasVencidas", polizasResumen.stream().filter(p -> p.getEstado() == EstadoPoliza.VENCIDA).count());
        model.addAttribute("polizasTerminadas", polizasResumen.stream().filter(p -> p.getEstado() == EstadoPoliza.TERMINADA).count());
        model.addAttribute("usuarioActual", authentication.getName());
        model.addAttribute("usuarioActualId", usuario.getIdUsuario());
        model.addAttribute("rolUsuarioActual", usuario.getRol().name());
        model.addAttribute("polizaCreadorActual", nombreCompleto(usuario));
        model.addAttribute("polizaSedeActual", usuario.getSede() == null ? "Sin información" : usuario.getSede().getNombre());

        return "polizas";
    }

    /**
     * Busca proveedores por texto para alimentar autocompletados y sugerencias en los formularios de pólizas.
     */
    @GetMapping("/proveedores/buscar")
    @ResponseBody
    @PreAuthorize("hasAnyRole('COMERCIAL', 'APROBADOR', 'ADMINISTRADOR', 'SUPERADMINISTRADOR')")
    public List<ProveedorAutocompleteDTO> buscarProveedores(@RequestParam(required = false) String query) {
        if (query == null || query.trim().length() < 3) {
            return List.of();
        }
        return proveedorService.buscarPorTerminoNacional(query).stream()
                .map(this::aProveedorAutocompleteDTO)
                .toList();
    }

    /**
     * Convierte una entidad de proveedor en el DTO compacto usado por el front-end de autocompletado.
     */
    private ProveedorAutocompleteDTO aProveedorAutocompleteDTO(Proveedor proveedor) {
        return new ProveedorAutocompleteDTO(proveedor.getIdProv(), proveedor.getNit(), proveedor.getNombre(),
                proveedor.getCiudad(), proveedor.getDireccion(), proveedor.getTelefono(), proveedor.getCorreo());
    }

    /**
     * Determina si una póliza debe contabilizarse en los totales generales del resumen.
     */
    private boolean esPolizaValidaParaTotales(Poliza poliza) {
        return poliza != null && poliza.getEstado() != EstadoPoliza.ANULADA;
    }

    /**
     * Consulta en tiempo real el estado de envío de correo de las pólizas visibles por el usuario.
     */
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

    /**
     * Devuelve el estado de aprobación de la póliza por sede para renderizar el historial de revisiones.
     */
    @GetMapping("/{id}/aprobaciones-sedes")
    @ResponseBody
    @PreAuthorize("hasAnyRole('COMERCIAL', 'APROBADOR', 'ADMINISTRADOR', 'SUPERADMINISTRADOR')")
    public List<Map<String, Object>> aprobacionesPorSede(@PathVariable Integer id) {
        return polizaService.listarAprobacionesPorSede(id).stream().map(aprobacion -> {
            Map<String, Object> item = new java.util.HashMap<>();
            item.put("idSede", aprobacion.getSede() != null ? aprobacion.getSede().getIdSede() : null);
            item.put("nombreSede", aprobacion.getSede() != null ? aprobacion.getSede().getNombre() : "Sin información");
            item.put("estado", aprobacion.getEstado() != null ? aprobacion.getEstado().name() : EstadoAprobacionSede.PENDIENTE.name());
                item.put("aprobador", aprobacion.getUsuarioAprobacion() == null
                    ? null
                    : nombreCompleto(aprobacion.getUsuarioAprobacion()));
                item.put("fechaAprobacion", aprobacion.getFechaAprobacion());
            return item;
        }).toList();
    }

    /**
     * Recupera los detalles de prima asociados a una póliza para mostrarlos en una vista de detalle.
     */
    @GetMapping("/{id}/detalles-prima")
    @ResponseBody
    @PreAuthorize("hasAnyRole('COMERCIAL', 'APROBADOR', 'ADMINISTRADOR', 'SUPERADMINISTRADOR')")
    public List<DetallePrimaDTO> detallesPrima(@PathVariable Integer id) {
        return polizaService.obtenerPorId(id).getDetallesPrima().stream().map(detalle -> {
            DetallePrimaDTO dto = new DetallePrimaDTO();
            dto.setValor(detalle.getValor());
            dto.setDescripcion(detalle.getDescripcion());
            return dto;
        }).toList();
    }

    /**
     * Crea una nueva póliza a partir del DTO del formulario y responde de forma compatible con AJAX y submit tradicional.
     */
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

    /**
     * Actualiza una póliza existente y mantiene la misma respuesta dual para formularios convencionales y peticiones AJAX.
     */
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

    /**
     * Aprueba la póliza y deja el flujo listo para continuar con la activación o seguimiento posterior.
     */
    @PostMapping("/{id}/aprobar")
    @PreAuthorize("hasAnyRole('APROBADOR', 'ADMINISTRADOR', 'SUPERADMINISTRADOR')")
    public String aprobarPoliza(@PathVariable Integer id, RedirectAttributes redirectAttributes) {
        polizaService.aprobar(id);
        redirectAttributes.addFlashAttribute("mensajeExito", "Póliza aprobada correctamente.");
        return "redirect:/polizas";
    }

    /**
     * Activa la póliza como vigente, validando la fecha de vencimiento y el archivo físico si aplica.
     */
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

    /**
     * Anula una póliza cuando se confirma que ya no debe seguir vigente en el sistema.
     */
    @PostMapping("/{id}/anular")
    @PreAuthorize("hasAnyRole('APROBADOR', 'SUPERADMINISTRADOR')")
    public String anularPoliza(@PathVariable Integer id, RedirectAttributes redirectAttributes) {
        polizaService.anular(id);
        redirectAttributes.addFlashAttribute("mensajeExito", "Póliza anulada correctamente.");
        return "redirect:/polizas";
    }

    /**
     * Finaliza la póliza con un motivo explícito cuando la cobertura ya no está vigente para la operación.
     */
    @PostMapping("/{id}/terminar")
    @PreAuthorize("hasAnyRole('COMERCIAL', 'APROBADOR', 'ADMINISTRADOR', 'SUPERADMINISTRADOR')")
    public String terminarPoliza(@PathVariable Integer id,
            @RequestParam String motivoTerminacion,
            RedirectAttributes redirectAttributes) {
        polizaService.terminar(id, motivoTerminacion);
        redirectAttributes.addFlashAttribute("mensajeExito", "Póliza terminada correctamente.");
        return "redirect:/polizas";
    }

    /**
     * Detecta si la petición entrante es una llamada AJAX para responder con JSON en lugar de redirección.
     */
    private boolean esSolicitudAjax(HttpServletRequest request) {
        return "XMLHttpRequest".equalsIgnoreCase(request.getHeader("X-Requested-With"));
    }

    /**
     * Normaliza el nombre completo del usuario para mostrarlo en la UI sin campos nulos o vacíos.
     */
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
