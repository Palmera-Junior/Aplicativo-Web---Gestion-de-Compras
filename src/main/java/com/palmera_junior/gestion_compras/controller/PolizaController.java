package com.palmera_junior.gestion_compras.controller;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
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
import org.springframework.http.HttpHeaders;
import org.springframework.http.ContentDisposition;
import org.springframework.http.MediaType;
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
import com.palmera_junior.gestion_compras.dto.RenovarPolizaDTO;
import com.palmera_junior.gestion_compras.dto.DetallePrimaDTO;
import com.palmera_junior.gestion_compras.dto.ProveedorAutocompleteDTO;
import com.palmera_junior.gestion_compras.entity.EstadoPoliza;
import com.palmera_junior.gestion_compras.entity.EstadoEnvioCorreo;
import com.palmera_junior.gestion_compras.entity.Poliza;
import com.palmera_junior.gestion_compras.entity.PolizaAdjunto;
import com.palmera_junior.gestion_compras.entity.PolizaRenovacion;
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
    @PreAuthorize("hasAnyAuthority('PERM_POLIZAS_VER', 'PERM_POLIZAS_GESTIONAR', 'PERM_POLIZAS_APROBAR')")
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
        model.addAttribute("rolUsuarioActual", usuario.getRol().getCodigo());
        model.addAttribute("polizaCreadorActual", nombreCompleto(usuario));
        model.addAttribute("polizaSedeActual", usuario.getSede() == null ? "Sin información" : usuario.getSede().getNombre());

        return "polizas";
    }

    /**
     * Busca proveedores por texto para alimentar autocompletados y sugerencias en los formularios de pólizas.
     */
    @GetMapping("/proveedores/buscar")
    @ResponseBody
    @PreAuthorize("hasAnyAuthority('PERM_POLIZAS_VER', 'PERM_POLIZAS_GESTIONAR', 'PERM_POLIZAS_APROBAR')")
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
    @PreAuthorize("hasAnyAuthority('PERM_POLIZAS_VER', 'PERM_POLIZAS_GESTIONAR', 'PERM_POLIZAS_APROBAR')")
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
    @PreAuthorize("hasAnyAuthority('PERM_POLIZAS_VER', 'PERM_POLIZAS_GESTIONAR', 'PERM_POLIZAS_APROBAR')")
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
    @PreAuthorize("hasAnyAuthority('PERM_POLIZAS_VER', 'PERM_POLIZAS_GESTIONAR', 'PERM_POLIZAS_APROBAR')")
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
    @PreAuthorize("hasAuthority('PERM_POLIZAS_GESTIONAR')")
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
    @PreAuthorize("hasAuthority('PERM_POLIZAS_GESTIONAR')")
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
    @PreAuthorize("hasAuthority('PERM_POLIZAS_APROBAR')")
    public String aprobarPoliza(@PathVariable Integer id, RedirectAttributes redirectAttributes) {
        polizaService.aprobar(id);
        redirectAttributes.addFlashAttribute("mensajeExito", "Póliza aprobada correctamente.");
        return "redirect:/polizas";
    }

    /**
    * Activa la póliza como vigente, adjuntando sus documentos PDF físicos.
     */
    @PostMapping(value = "/{id}/activar", consumes = "multipart/form-data")
    @PreAuthorize("hasAuthority('PERM_POLIZAS_GESTIONAR')")
    public String activarPoliza(
            @PathVariable Integer id,
            @ModelAttribute ActivarPolizaDTO dto,
            RedirectAttributes redirectAttributes) {
        polizaService.activarVigente(id, dto.getNumeroPoliza(), dto.getPolizasFisicas());
        redirectAttributes.addFlashAttribute("mensajeExito", "Póliza activada como vigente correctamente.");
        return "redirect:/polizas";
    }

    @PostMapping(value = "/{id}/renovar", consumes = "multipart/form-data")
    @ResponseBody
    @PreAuthorize("hasAuthority('PERM_POLIZAS_GESTIONAR')")
    public ResponseEntity<Map<String, String>> renovarPoliza(
            @PathVariable Integer id,
            @ModelAttribute RenovarPolizaDTO dto) {
        polizaService.renovar(id, dto);
        return ResponseEntity.ok(Map.of("message", "Póliza renovada correctamente."));
    }

    @GetMapping("/{id}/renovaciones")
    @ResponseBody
    @PreAuthorize("hasAnyAuthority('PERM_POLIZAS_VER', 'PERM_POLIZAS_GESTIONAR', 'PERM_POLIZAS_APROBAR')")
    public Map<String, Object> renovaciones(@PathVariable Integer id) {
        Poliza poliza = polizaService.obtenerPorId(id);
        List<PolizaRenovacion> renovaciones = polizaService.listarRenovaciones(id);
        Map<String, Object> periodoInicial = new java.util.HashMap<>();
        PolizaRenovacion primeraRenovacion = renovaciones.isEmpty() ? null : renovaciones.get(0);
        periodoInicial.put("fechaInicio", primeraRenovacion == null
                ? poliza.getFechaCreacion() : primeraRenovacion.getFechaInicioAnterior());
        periodoInicial.put("fechaFin", primeraRenovacion == null
                ? poliza.getFechaVencimiento() : primeraRenovacion.getFechaFinAnterior());
        periodoInicial.put("usuario", poliza.getUsuario() == null ? "Sin información" : nombreCompleto(poliza.getUsuario()));
        periodoInicial.put("archivos", poliza.getAdjuntos().stream()
            .filter(adjunto -> adjunto.getTipoAdjunto() == com.palmera_junior.gestion_compras.entity.TipoAdjuntoPoliza.POLIZA_FISICA)
            .map(adjunto -> Map.of(
                "id", adjunto.getIdAdjuntoPoliza(),
                "nombre", adjunto.getNombreOriginal(),
                "url", "/polizas/" + id + "/adjuntos/" + adjunto.getIdAdjuntoPoliza()))
            .toList());

        List<Map<String, Object>> itemsRenovacion = renovaciones.stream().map(renovacion -> {
            Map<String, Object> item = new java.util.HashMap<>();
            item.put("numero", renovaciones.indexOf(renovacion) + 1);
            item.put("id", renovacion.getIdRenovacion());
            item.put("fechaInicio", renovacion.getFechaInicio());
            item.put("fechaFin", renovacion.getFechaFin());
            item.put("fechaInicioAnterior", renovacion.getFechaInicioAnterior());
            item.put("fechaFinAnterior", renovacion.getFechaFinAnterior());
            item.put("fechaRegistro", renovacion.getFechaRegistro());
            item.put("usuario", renovacion.getUsuario() == null ? "Sin información" : nombreCompleto(renovacion.getUsuario()));
            item.put("archivos", renovacion.getAdjuntos().stream().map(adjunto -> Map.of(
                    "id", adjunto.getIdAdjuntoPoliza(),
                    "nombre", adjunto.getNombreOriginal(),
                    "url", "/polizas/" + id + "/renovaciones/" + renovacion.getIdRenovacion()
                            + "/adjuntos/" + adjunto.getIdAdjuntoPoliza())).toList());
            return item;
        }).toList();
        Map<String, Object> historial = new java.util.HashMap<>();
        historial.put("periodoInicial", periodoInicial);
        historial.put("renovaciones", itemsRenovacion);
        return historial;
    }

    @GetMapping("/{id}/contrato-adjunto")
    @PreAuthorize("hasAnyAuthority('PERM_POLIZAS_VER', 'PERM_POLIZAS_GESTIONAR', 'PERM_POLIZAS_APROBAR')")
    public ResponseEntity<byte[]> descargarContratoAdjunto(@PathVariable Integer id) throws IOException {
        PolizaAdjunto adjunto = polizaService.obtenerContratoAdjunto(id);
        byte[] contenido = polizaService.leerContratoAdjunto(adjunto);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(ContentDisposition.attachment()
                .filename(adjunto.getNombreOriginal(), StandardCharsets.UTF_8).build());
        return new ResponseEntity<>(contenido, headers, HttpStatus.OK);
    }

    @GetMapping("/{id}/adjuntos/{idAdjunto}")
    @PreAuthorize("hasAnyAuthority('PERM_POLIZAS_VER', 'PERM_POLIZAS_GESTIONAR', 'PERM_POLIZAS_APROBAR')")
    public ResponseEntity<byte[]> descargarAdjuntoFisico(@PathVariable Integer id,
            @PathVariable Integer idAdjunto) throws IOException {
        PolizaAdjunto adjunto = polizaService.obtenerAdjuntoFisico(id, idAdjunto);
        byte[] contenido = polizaService.leerContratoAdjunto(adjunto);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(ContentDisposition.attachment()
                .filename(adjunto.getNombreOriginal(), StandardCharsets.UTF_8).build());
        return new ResponseEntity<>(contenido, headers, HttpStatus.OK);
    }

    @GetMapping("/{id}/renovaciones/{idRenovacion}/adjuntos/{idAdjunto}")
    @PreAuthorize("hasAnyAuthority('PERM_POLIZAS_VER', 'PERM_POLIZAS_GESTIONAR', 'PERM_POLIZAS_APROBAR')")
    public ResponseEntity<byte[]> descargarAdjuntoRenovacion(@PathVariable Integer id,
            @PathVariable Integer idRenovacion, @PathVariable Integer idAdjunto) throws IOException {
        PolizaAdjunto adjunto = polizaService.obtenerAdjuntoRenovacion(id, idRenovacion, idAdjunto);
        byte[] contenido = polizaService.leerAdjuntoRenovacion(adjunto);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(ContentDisposition.attachment()
            .filename(adjunto.getNombreOriginal(), StandardCharsets.UTF_8).build());
        return new ResponseEntity<>(contenido, headers, HttpStatus.OK);
    }

    /**
     * Anula una póliza cuando se confirma que ya no debe seguir vigente en el sistema.
     */
    @PostMapping("/{id}/anular")
    @PreAuthorize("hasAuthority('PERM_POLIZAS_APROBAR')")
    public String anularPoliza(@PathVariable Integer id, RedirectAttributes redirectAttributes) {
        polizaService.anular(id);
        redirectAttributes.addFlashAttribute("mensajeExito", "Póliza anulada correctamente.");
        return "redirect:/polizas";
    }

    /**
     * Finaliza la póliza con un motivo explícito cuando la cobertura ya no está vigente para la operación.
     */
    @PostMapping("/{id}/terminar")
    @PreAuthorize("hasAuthority('PERM_POLIZAS_GESTIONAR')")
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
