package com.palmera_junior.gestion_compras.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.multipart.MultipartFile;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import com.palmera_junior.gestion_compras.entity.Empleado;
import com.palmera_junior.gestion_compras.entity.Usuario;
import com.palmera_junior.gestion_compras.rrhh.entity.Area;
import com.palmera_junior.gestion_compras.rrhh.entity.Afp;
import com.palmera_junior.gestion_compras.rrhh.entity.Arl;
import com.palmera_junior.gestion_compras.rrhh.entity.CajaCompensacion;
import com.palmera_junior.gestion_compras.rrhh.entity.Cargo;
import com.palmera_junior.gestion_compras.rrhh.entity.EntidadBancaria;
import com.palmera_junior.gestion_compras.rrhh.entity.Eps;
import com.palmera_junior.gestion_compras.rrhh.entity.EmpleadoAdjunto;
import com.palmera_junior.gestion_compras.rrhh.entity.ContratoEmpleado;
import com.palmera_junior.gestion_compras.rrhh.entity.EmpleadoAdjuntoRequerido;
import com.palmera_junior.gestion_compras.rrhh.entity.EmpleadoHistorialEstado;
import com.palmera_junior.gestion_compras.rrhh.entity.EtapaAdjuntoEmpleado;
import com.palmera_junior.gestion_compras.rrhh.entity.EstadoEmpleado;
import com.palmera_junior.gestion_compras.rrhh.entity.GrupoAdjuntoEmpleado;
import com.palmera_junior.gestion_compras.rrhh.entity.NivelEducativo;
import com.palmera_junior.gestion_compras.rrhh.entity.TipoContrato;
import com.palmera_junior.gestion_compras.rrhh.entity.TipoAdjuntoEmpleado;
import com.palmera_junior.gestion_compras.rrhh.entity.TipoPago;
import com.palmera_junior.gestion_compras.rrhh.repository.AfpRepository;
import com.palmera_junior.gestion_compras.rrhh.repository.ArlRepository;
import com.palmera_junior.gestion_compras.rrhh.service.AsignacionHerramientaService;
import com.palmera_junior.gestion_compras.rrhh.repository.AreaRepository;
import com.palmera_junior.gestion_compras.rrhh.repository.CajaCompensacionRepository;
import com.palmera_junior.gestion_compras.rrhh.repository.CargoRepository;
import com.palmera_junior.gestion_compras.rrhh.repository.EntidadBancariaRepository;
import com.palmera_junior.gestion_compras.rrhh.repository.EpsRepository;
import com.palmera_junior.gestion_compras.rrhh.repository.NivelEducativoRepository;
import com.palmera_junior.gestion_compras.rrhh.repository.TipoContratoRepository;
import com.palmera_junior.gestion_compras.rrhh.repository.TipoPagoRepository;
import com.palmera_junior.gestion_compras.rrhh.service.EmpleadoRegistroWorkflowService;
import com.palmera_junior.gestion_compras.rrhh.service.IEmpleadoAdjuntoService;
import com.palmera_junior.gestion_compras.rrhh.service.IEmpleadoService;
import com.palmera_junior.gestion_compras.rrhh.service.IContratoEmpleadoService;
import com.palmera_junior.gestion_compras.rrhh.service.EmpleadoQrService;
import com.palmera_junior.gestion_compras.rrhh.service.EmpleadoPerfilService;
import com.palmera_junior.gestion_compras.rrhh.web.EmpleadoPerfilForm;
import com.palmera_junior.gestion_compras.rrhh.web.EmpleadoRegistroForm;
import com.palmera_junior.gestion_compras.repository.CentroCostoRepository;
import com.palmera_junior.gestion_compras.service.usuario.IUsuarioService;

/** Vistas y acciones del módulo de Recursos Humanos. */
@Controller
public class RecursosHumanosController {

    private final IEmpleadoService empleadoService;
    private final IContratoEmpleadoService contratoEmpleadoService;
    private final EmpleadoQrService empleadoQrService;
    private final EmpleadoPerfilService empleadoPerfilService;
    private final IEmpleadoAdjuntoService adjuntoService;
    private final EmpleadoRegistroWorkflowService registroWorkflow;
    private final AsignacionHerramientaService asignacionHerramientaService;
    private final IUsuarioService usuarioService;
    private final AreaRepository areaRepository;
    private final CargoRepository cargoRepository;
    private final CentroCostoRepository centroCostoRepository;
    private final NivelEducativoRepository nivelEducativoRepository;
    private final TipoContratoRepository tipoContratoRepository;
    private final TipoPagoRepository tipoPagoRepository;
    private final EntidadBancariaRepository entidadBancariaRepository;
    private final EpsRepository epsRepository;
    private final AfpRepository afpRepository;
    private final ArlRepository arlRepository;
    private final CajaCompensacionRepository cajaCompensacionRepository;

    @Autowired
    public RecursosHumanosController(
            IEmpleadoService empleadoService,
            IContratoEmpleadoService contratoEmpleadoService,
            EmpleadoQrService empleadoQrService,
            EmpleadoPerfilService empleadoPerfilService,
            IEmpleadoAdjuntoService adjuntoService,
            EmpleadoRegistroWorkflowService registroWorkflow,
            AsignacionHerramientaService asignacionHerramientaService,
            IUsuarioService usuarioService,
            AreaRepository areaRepository,
            CargoRepository cargoRepository,
            CentroCostoRepository centroCostoRepository,
            NivelEducativoRepository nivelEducativoRepository,
            TipoContratoRepository tipoContratoRepository,
            TipoPagoRepository tipoPagoRepository,
            EntidadBancariaRepository entidadBancariaRepository,
            EpsRepository epsRepository,
            AfpRepository afpRepository,
            ArlRepository arlRepository,
            CajaCompensacionRepository cajaCompensacionRepository) {
        this.empleadoService = empleadoService;
        this.contratoEmpleadoService = contratoEmpleadoService;
        this.empleadoQrService = empleadoQrService;
        this.empleadoPerfilService = empleadoPerfilService;
        this.adjuntoService = adjuntoService;
        this.registroWorkflow = registroWorkflow;
        this.asignacionHerramientaService = asignacionHerramientaService;
        this.usuarioService = usuarioService;
        this.areaRepository = areaRepository;
        this.cargoRepository = cargoRepository;
        this.centroCostoRepository = centroCostoRepository;
        this.nivelEducativoRepository = nivelEducativoRepository;
        this.tipoContratoRepository = tipoContratoRepository;
        this.tipoPagoRepository = tipoPagoRepository;
        this.entidadBancariaRepository = entidadBancariaRepository;
        this.epsRepository = epsRepository;
        this.afpRepository = afpRepository;
        this.arlRepository = arlRepository;
        this.cajaCompensacionRepository = cajaCompensacionRepository;
    }

    /** Constructor compatible con pruebas y extensiones que usan el contrato previo. */
    public RecursosHumanosController(
            IEmpleadoService empleadoService,
            IEmpleadoAdjuntoService adjuntoService,
            EmpleadoRegistroWorkflowService registroWorkflow,
            AsignacionHerramientaService asignacionHerramientaService,
            IUsuarioService usuarioService,
            AreaRepository areaRepository,
            CargoRepository cargoRepository,
            CentroCostoRepository centroCostoRepository,
            NivelEducativoRepository nivelEducativoRepository,
            TipoContratoRepository tipoContratoRepository,
            TipoPagoRepository tipoPagoRepository,
            EntidadBancariaRepository entidadBancariaRepository,
            EpsRepository epsRepository,
            AfpRepository afpRepository,
            ArlRepository arlRepository,
            CajaCompensacionRepository cajaCompensacionRepository) {
        this(empleadoService, null, null, null, adjuntoService, registroWorkflow, asignacionHerramientaService,
                usuarioService, areaRepository, cargoRepository, centroCostoRepository,
                nivelEducativoRepository, tipoContratoRepository, tipoPagoRepository,
                entidadBancariaRepository, epsRepository, afpRepository, arlRepository,
                cajaCompensacionRepository);
    }

    @GetMapping("/recursos-humanos/dashboard")
    public String dashboard() {
        return "recursos-humanos-dashboard";
    }

    @GetMapping("/recursos-humanos/principal")
    @PreAuthorize("hasAnyAuthority('PERM_RRHH_VER', 'PERM_RRHH_GESTIONAR', 'PERM_RRHH_APROBAR', 'PERM_RRHH_REGISTRO_EDITAR')")
    public String principal(
            @RequestParam(required = false) Integer empleadoId,
            @RequestParam(required = false) Integer editarEmpleadoId,
            @RequestParam(required = false) String error,
            @RequestParam(required = false, defaultValue = "false") boolean nuevoEmpleado,
            Authentication authentication,
            Model model) {
        if (nuevoEmpleado) {
            empleadoId = null;
            editarEmpleadoId = null;
        }
        boolean formularioEnFlash = !nuevoEmpleado && model.containsAttribute("registroEmpleado");
        List<Empleado> empleados = empleadoService.listar();
        Usuario actor = usuarioService.obtenerUsuarioAutenticado(authentication);
        Set<String> permisos = actor.getPermisos().stream()
                .map(permiso -> permiso.getCodigo())
                .collect(Collectors.toSet());
        Empleado empleadoEnEdicion = editarEmpleadoId == null ? null
                : empleadoService.buscarPorId(editarEmpleadoId)
                        .orElseThrow(() -> new ResponseStatusException(
                                HttpStatus.NOT_FOUND, "No existe el empleado solicitado"));
        boolean esEdicionContratacion = empleadoEnEdicion != null
                && empleadoEnEdicion.getEstado() == EstadoEmpleado.PROCESO_CONTRATACION;

        model.addAttribute("empleados", empleados);
        model.addAttribute("herramientasCatalogo",
                asignacionHerramientaService.listarCatalogoActivo());
        model.addAttribute("asignacionesHerramientas", List.of());
        model.addAttribute("puedeGestionar", permisos.contains("RRHH_GESTIONAR"));
        model.addAttribute("puedeGestionarAsignaciones",
                permisos.contains("RRHH_ASIGNACIONES_GESTIONAR"));
        model.addAttribute("puedeEditarRegistro", permisos.contains("RRHH_REGISTRO_EDITAR"));
        model.addAttribute("puedeAprobar", permisos.contains("RRHH_APROBAR"));
        model.addAttribute("puedeAnularContrato", permisos.contains("RRHH_CONTRATO_ANULAR"));
        model.addAttribute("puedeGestionarQr", permisos.contains("RRHH_QR_GESTIONAR"));
        model.addAttribute("puedeEditarPerfil", permisos.contains("RRHH_PERFIL_EDITAR"));
        model.addAttribute("esEdicionContratacion", esEdicionContratacion);
        model.addAttribute("esNuevoEmpleado", nuevoEmpleado && editarEmpleadoId == null);
        model.addAttribute("tiposDocumento",
                com.palmera_junior.gestion_compras.entity.TipoDocumentoEmpleado.values());
        model.addAttribute("tiposSangre",
                com.palmera_junior.gestion_compras.entity.TipoSangre.values());
        model.addAttribute("factoresRh", com.palmera_junior.gestion_compras.entity.Rh.values());
        model.addAttribute("areas", areaRepository.findAll().stream()
                .filter(Area::isActivo).toList());
        model.addAttribute("cargos", cargoRepository.findAll().stream()
                .filter(Cargo::isActivo).toList());
        model.addAttribute("centrosCosto", centroCostoRepository.findAll());
        model.addAttribute("nivelesEducativos", nivelEducativoRepository.findAll().stream()
                .filter(NivelEducativo::isActivo).toList());
        model.addAttribute("tiposContrato", esEdicionContratacion
                ? tipoContratoRepository.findAll().stream().filter(TipoContrato::isActivo).toList()
                : List.of());
        model.addAttribute("tiposContratoRenovacion", tipoContratoRepository.findAll().stream()
                .filter(TipoContrato::isActivo).toList());
        model.addAttribute("tiposPago", esEdicionContratacion
                ? tipoPagoRepository.findAll().stream().filter(TipoPago::isActivo).toList()
                : List.of());
        model.addAttribute("entidadesBancarias", esEdicionContratacion
                ? entidadBancariaRepository.findAll().stream()
                        .filter(EntidadBancaria::isActivo).toList()
                : List.of());
        model.addAttribute("epsCatalogo", esEdicionContratacion
                ? epsRepository.findAll().stream().filter(Eps::isActivo).toList()
                : List.of());
        model.addAttribute("afpCatalogo", esEdicionContratacion
                ? afpRepository.findAll().stream().filter(Afp::isActivo).toList()
                : List.of());
        model.addAttribute("arlCatalogo", esEdicionContratacion
                ? arlRepository.findAll().stream().filter(Arl::isActivo).toList()
                : List.of());
        model.addAttribute("cajasCompensacion", esEdicionContratacion
                ? cajaCompensacionRepository.findAll().stream()
                        .filter(CajaCompensacion::isActivo).toList()
                : List.of());
        List<TipoAdjuntoEmpleado> tiposAdjuntoActivos = adjuntoService.listarTiposActivos();
        Set<GrupoAdjuntoEmpleado> gruposAdjuntos = esEdicionContratacion
                ? Set.of(GrupoAdjuntoEmpleado.AFILIACIONES, GrupoAdjuntoEmpleado.EVALUACIONES)
                : Set.of(GrupoAdjuntoEmpleado.INGRESO, GrupoAdjuntoEmpleado.VEHICULO);
        List<TipoAdjuntoEmpleado> tiposAdjuntoIniciales = tiposAdjuntoActivos.stream()
                .filter(tipo -> gruposAdjuntos.contains(tipo.getGrupo()))
                .toList();
        model.addAttribute("tiposAdjuntoIniciales", tiposAdjuntoIniciales);
        model.addAttribute("tiposAdjuntoIngreso", tiposAdjuntoIniciales.stream()
                .filter(tipo -> tipo.getGrupo() == GrupoAdjuntoEmpleado.INGRESO).toList());
        model.addAttribute("tiposAdjuntoVehiculo", tiposAdjuntoIniciales.stream()
                .filter(tipo -> tipo.getGrupo() == GrupoAdjuntoEmpleado.VEHICULO).toList());
        model.addAttribute("tiposAdjuntoContratacion", tiposAdjuntoActivos.stream()
                .filter(tipo -> Set.of(
                        GrupoAdjuntoEmpleado.AFILIACIONES, GrupoAdjuntoEmpleado.EVALUACIONES)
                        .contains(tipo.getGrupo()))
                .toList());
        if (error != null && !error.isBlank()) {
            model.addAttribute("mensajeError", error);
        }
        model.addAttribute("totalEmpleados", empleados.size());
        model.addAttribute("cantidadRegistroInicial",
                contarPorEstado(empleados, EstadoEmpleado.REGISTRO_INICIAL));
        model.addAttribute("cantidadProcesoContratacion",
                contarPorEstado(empleados, EstadoEmpleado.PROCESO_CONTRATACION));
        model.addAttribute("cantidadActivos", contarPorEstado(empleados, EstadoEmpleado.ACTIVO));
        model.addAttribute("cantidadRetirados", contarPorEstado(empleados, EstadoEmpleado.RETIRADO));
        model.addAttribute("empleadoSeleccionado", null);
        model.addAttribute("perfilForm", new EmpleadoPerfilForm());
        model.addAttribute("nombreFotoEmpleado", null);
        model.addAttribute("contratosEmpleado", List.of());
        model.addAttribute("contratoVigente", null);
        model.addAttribute("adjuntosFicha", List.of());
        model.addAttribute("tiposDocumentoExpediente", adjuntoService.listarTiposActivos().stream()
                .filter(tipo -> !"3.8".equals(tipo.getCodigo())).toList());
        model.addAttribute("contratosDocumento", List.of());
        model.addAttribute("documentosContrato", java.util.Map.of());
        model.addAttribute("qrActivo", false);
        model.addAttribute("qrImagenDisponible", false);
        model.addAttribute("empleadoTieneFoto", false);
        model.addAttribute("nombreArlPublico", null);
        model.addAttribute("historialEstados", List.of());
        model.addAttribute("empleadoEditar", null);
        model.addAttribute("adjuntosEmpleado", List.of());
        model.addAttribute("tiposAdjuntosVigentes", "");
        if (!formularioEnFlash && editarEmpleadoId == null) {
            model.addAttribute("registroEmpleado", new EmpleadoRegistroForm());
        }

        if (empleadoId != null) {
            if (contratoEmpleadoService != null) {
                contratoEmpleadoService.sincronizarEstados();
            }
            Empleado empleado = empleadoService.buscarPorId(empleadoId)
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.NOT_FOUND, "No existe el empleado solicitado"));
            List<EmpleadoHistorialEstado> historial =
                    empleadoService.listarHistorialEstado(empleadoId);
            model.addAttribute("empleadoSeleccionado", empleado);
            model.addAttribute("perfilForm", formDesdePerfil(empleado));
            if (empleadoPerfilService != null) {
                model.addAttribute("nombreFotoEmpleado",
                        empleadoPerfilService.obtenerNombreFoto(empleadoId).orElse(null));
            }
            model.addAttribute("historialEstados", historial);
            List<ContratoEmpleado> contratos = contratoEmpleadoService == null
                    ? List.of() : contratoEmpleadoService.listarPorEmpleado(empleadoId);
            java.util.Map<Integer, Long> documentosContrato = new java.util.HashMap<>();
            for (ContratoEmpleado contrato : contratos) {
                adjuntoService.buscarContratoFirmado(empleadoId, contrato.getIdContratoEmpleado())
                        .ifPresent(adjunto -> documentosContrato.put(
                                contrato.getIdContratoEmpleado(), adjunto.getIdEmpleadoAdjunto()));
            }
            model.addAttribute("contratosEmpleado", contratos);
            model.addAttribute("contratoVigente", contratos.stream()
                    .filter(contrato -> contrato.getEstado().name().equals("VIGENTE"))
                    .findFirst().orElse(null));
            model.addAttribute("adjuntosFicha", adjuntoService.listarPorEmpleado(empleadoId));
            model.addAttribute("contratosDocumento", contratos);
            model.addAttribute("documentosContrato", documentosContrato);
            boolean qrActivo = empleadoQrService != null
                    && empleadoQrService.tieneQrActivo(empleadoId);
            model.addAttribute("qrActivo", qrActivo);
            model.addAttribute("qrImagenDisponible", qrActivo
                    && empleadoQrService.tieneImagenQrActivo(empleadoId));
            if (empleadoQrService != null && permisos.contains("RRHH_QR_GESTIONAR")) {
                model.addAttribute("empleadoTieneFoto", empleadoQrService.tieneFoto(empleadoId));
                model.addAttribute("nombreArlPublico",
                        empleadoQrService.nombreArlPublico(empleadoId));
            }
        }

        if (editarEmpleadoId != null) {
            Empleado empleado = empleadoEnEdicion;
            boolean puedeEditarInicial = permisos.contains("RRHH_REGISTRO_EDITAR")
                    || permisos.contains("RRHH_GESTIONAR");
            boolean puedeCompletarContratacion = permisos.contains("RRHH_GESTIONAR");
            if (empleado.getEstado() == EstadoEmpleado.REGISTRO_INICIAL && !puedeEditarInicial
                    || esEdicionContratacion && !puedeCompletarContratacion
                    || empleado.getEstado() != EstadoEmpleado.REGISTRO_INICIAL
                            && !esEdicionContratacion) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "El empleado no está disponible para esta etapa de edición");
            }
            model.addAttribute("empleadoEditar", empleado);
            List<EmpleadoAdjuntoRequerido> requisitos =
                    adjuntoService.listarRequeridosPorEmpleado(editarEmpleadoId);
            if (!formularioEnFlash) {
                EmpleadoRegistroForm form = formDesdeEmpleado(empleado);
                if (esEdicionContratacion) {
                    registroWorkflow.cargarDatosContratacionPendiente(editarEmpleadoId, form);
                }
                form.setTiposAdjuntoRequeridos(requisitos.stream()
                        .filter(requisito -> requisito.getEtapaRequerida()
                                == (esEdicionContratacion
                                        ? EtapaAdjuntoEmpleado.CONTRATACION
                                        : EtapaAdjuntoEmpleado.REGISTRO_INICIAL))
                        .map(requisito -> requisito.getTipoAdjunto().getIdTipoAdjuntoEmpleado())
                        .collect(Collectors.toCollection(ArrayList::new)));
                if (!esEdicionContratacion
                        && empleado.getEstado() == EstadoEmpleado.REGISTRO_INICIAL) {
                    form.setHerramientasSolicitadasIds(
                            asignacionHerramientaService.listarIdsPendientes(editarEmpleadoId));
                }
                model.addAttribute("registroEmpleado", form);
            }
            List<EmpleadoAdjunto> adjuntos = adjuntoService.listarPorEmpleado(editarEmpleadoId);
            model.addAttribute("adjuntosEmpleado", adjuntos);
            model.addAttribute("tiposAdjuntosVigentes", adjuntos.stream()
                    .filter(EmpleadoAdjunto::isVigente)
                    .map(adjunto -> adjunto.getTipoAdjunto().getIdTipoAdjuntoEmpleado())
                    .distinct()
                    .map(String::valueOf)
                    .collect(Collectors.joining(",")));
        }
        if (empleadoId != null) {
            model.addAttribute(
                    "asignacionesHerramientas",
                    asignacionHerramientaService.listarPorEmpleado(empleadoId));
        }
        EmpleadoRegistroForm registro = (EmpleadoRegistroForm) model.getAttribute("registroEmpleado");
        List<Integer> tiposSeleccionados = registro == null || registro.getTiposAdjuntoRequeridos() == null
                ? List.of() : registro.getTiposAdjuntoRequeridos();
        model.addAttribute("tiposSeleccionados", tiposSeleccionados);
        model.addAttribute("tiposAdjuntoSeleccionados", tiposAdjuntoIniciales.stream()
                .filter(tipo -> tiposSeleccionados.contains(tipo.getIdTipoAdjuntoEmpleado()))
                .toList());
        model.addAttribute(
                "herramientasSolicitadasSeleccionadas",
                registro == null || registro.getHerramientasSolicitadasIds() == null
                        ? List.of() : registro.getHerramientasSolicitadasIds());
        return "recursos-humanos-principal";
    }

    @PostMapping("/recursos-humanos/empleados")
    @PreAuthorize("hasAnyAuthority('PERM_RRHH_GESTIONAR', 'PERM_RRHH_REGISTRO_EDITAR')")
    public String guardarRegistroInicial(
            @ModelAttribute("registroEmpleado") EmpleadoRegistroForm form,
            MultipartHttpServletRequest request,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {
        try {
            Usuario actor = usuarioService.obtenerUsuarioAutenticado(authentication);
            registroWorkflow.guardar(form, request.getMultiFileMap(), actor);
            redirectAttributes.addFlashAttribute("mensajeExito",
                    form.getIdEmpleado() == null
                            ? "Empleado creado en Registro inicial."
                            : "Registro inicial actualizado.");
            return "redirect:/recursos-humanos/principal";
        } catch (IllegalArgumentException | IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("mensajeError", ex.getMessage());
            redirectAttributes.addFlashAttribute("registroEmpleado", form);
            return "redirect:/recursos-humanos/principal"
                    + (form.getIdEmpleado() == null
                            ? "" : "?editarEmpleadoId=" + form.getIdEmpleado())
                    + "#registro-empleado";
        }
    }

    @PostMapping("/recursos-humanos/empleados/{id}/contratacion")
    @PreAuthorize("hasAuthority('PERM_RRHH_GESTIONAR')")
    public String guardarAvanceContratacion(
            @PathVariable Integer id,
            @ModelAttribute("registroEmpleado") EmpleadoRegistroForm form,
            MultipartHttpServletRequest request,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {
        try {
            registroWorkflow.guardarAvanceContratacion(
                    id, form, request.getMultiFileMap(),
                    usuarioService.obtenerUsuarioAutenticado(authentication));
            redirectAttributes.addFlashAttribute("mensajeExito",
                    "Avance contractual guardado. El empleado continúa en Proceso de contratación.");
        } catch (IllegalArgumentException | IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("mensajeError", ex.getMessage());
            redirectAttributes.addFlashAttribute("registroEmpleado", form);
        }
        return "redirect:/recursos-humanos/principal?editarEmpleadoId=" + id
                + "#registro-empleado";
    }

    @PostMapping("/recursos-humanos/empleados/{id}/completar")
    @PreAuthorize("hasAuthority('PERM_RRHH_GESTIONAR')")
    public String completarContratacion(
            @PathVariable Integer id,
            @ModelAttribute("registroEmpleado") EmpleadoRegistroForm form,
            MultipartHttpServletRequest request,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {
        try {
            registroWorkflow.completarContratacion(
                    id, form, request.getMultiFileMap(),
                    usuarioService.obtenerUsuarioAutenticado(authentication));
            redirectAttributes.addFlashAttribute("mensajeExito",
                    "Empleado activado y primer contrato creado correctamente.");
            return "redirect:/recursos-humanos/principal";
        } catch (IllegalArgumentException | IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("mensajeError", ex.getMessage());
            redirectAttributes.addFlashAttribute("registroEmpleado", form);
            return "redirect:/recursos-humanos/principal?editarEmpleadoId=" + id
                    + "#registro-empleado";
        }
    }

    @PostMapping("/recursos-humanos/empleados/{id}/aprobar")
    @PreAuthorize("hasAuthority('PERM_RRHH_APROBAR')")
    public String aprobarRegistroInicial(
            @PathVariable Integer id,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {
        try {
            empleadoService.aprobarRegistroInicial(
                    id, usuarioService.obtenerUsuarioAutenticado(authentication));
            redirectAttributes.addFlashAttribute("mensajeExito",
                    "Empleado aprobado y trasladado a Proceso de contratación.");
        } catch (IllegalArgumentException | IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("mensajeError", ex.getMessage());
        }
        return "redirect:/recursos-humanos/principal";
    }

    @PostMapping("/recursos-humanos/empleados/{id}/asignaciones/{idAsignacion}/entregar")
    @PreAuthorize("hasAuthority('PERM_RRHH_ASIGNACIONES_GESTIONAR')")
    public String entregarHerramienta(
            @PathVariable Integer id,
            @PathVariable Long idAsignacion,
            @RequestParam("acta") MultipartFile acta,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {
        try {
            asignacionHerramientaService.asignar(
                    id, idAsignacion, acta,
                    usuarioService.obtenerUsuarioAutenticado(authentication));
            redirectAttributes.addFlashAttribute(
                    "mensajeExito", "Herramienta asignada y acta registrada correctamente.");
        } catch (IllegalArgumentException | IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("mensajeError", ex.getMessage());
        }
        return "redirect:/recursos-humanos/principal?empleadoId=" + id
                + "&pestana=asignaciones#ficha-empleado";
    }

    @PostMapping("/recursos-humanos/empleados/{id}/asignaciones/{idAsignacion}/recibir")
    @PreAuthorize("hasAuthority('PERM_RRHH_ASIGNACIONES_GESTIONAR')")
    public String recibirHerramienta(
            @PathVariable Integer id,
            @PathVariable Long idAsignacion,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {
        try {
            asignacionHerramientaService.marcarRecibida(
                    id, idAsignacion,
                    usuarioService.obtenerUsuarioAutenticado(authentication));
            redirectAttributes.addFlashAttribute(
                    "mensajeExito", "La herramienta quedó marcada como recibida.");
        } catch (IllegalArgumentException | IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("mensajeError", ex.getMessage());
        }
        return "redirect:/recursos-humanos/principal?empleadoId=" + id
                + "&pestana=asignaciones#ficha-empleado";
    }

    @PostMapping("/recursos-humanos/empleados/{id}/contratos/{idContrato}/terminar")
    @PreAuthorize("hasAuthority('PERM_RRHH_CONTRATO_TERMINAR')")
    public String terminarContrato(
            @PathVariable Integer id,
            @PathVariable Integer idContrato,
            @RequestParam String motivo,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {
        try {
            ContratoEmpleado contrato = contratoEmpleadoService.buscarPorId(idContrato)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
            if (contrato.getEmpleado().getIdEmpleado().intValue() != id) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND);
            }
            contratoEmpleadoService.terminar(idContrato,
                    usuarioService.obtenerUsuarioAutenticado(authentication), motivo);
            redirectAttributes.addFlashAttribute("mensajeExito", "Contrato terminado correctamente.");
        } catch (IllegalArgumentException | IllegalStateException | SecurityException ex) {
            redirectAttributes.addFlashAttribute("mensajeError", ex.getMessage());
        }
        return "redirect:/recursos-humanos/principal?empleadoId=" + id + "#ficha-empleado";
    }

    @PostMapping("/recursos-humanos/empleados/{id}/contratos/{idContrato}/anular")
    @PreAuthorize("hasAuthority('PERM_RRHH_CONTRATO_ANULAR')")
    public String anularContrato(
            @PathVariable Integer id,
            @PathVariable Integer idContrato,
            @RequestParam String motivo,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {
        try {
            ContratoEmpleado contrato = contratoEmpleadoService.buscarPorId(idContrato)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
            if (!contrato.getEmpleado().getIdEmpleado().equals(id)) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND);
            }
            contratoEmpleadoService.anular(idContrato,
                    usuarioService.obtenerUsuarioAutenticado(authentication), motivo);
            redirectAttributes.addFlashAttribute("mensajeExito", "Contrato anulado correctamente.");
        } catch (IllegalArgumentException | IllegalStateException | SecurityException ex) {
            redirectAttributes.addFlashAttribute("mensajeError", ex.getMessage());
        }
        return "redirect:/recursos-humanos/principal?empleadoId=" + id + "#ficha-empleado";
    }

    @PostMapping("/recursos-humanos/empleados/{id}/contratos/{idContrato}/renovar")
    @PreAuthorize("hasAuthority('PERM_RRHH_GESTIONAR')")
    public String renovarContrato(
            @PathVariable Integer id,
            @PathVariable Integer idContrato,
            @RequestParam java.time.LocalDate fechaInicio,
            @RequestParam(required = false) java.time.LocalDate fechaFin,
            @RequestParam java.math.BigDecimal salario,
            @RequestParam Integer idTipoContrato,
            @RequestParam Integer idCargo,
            @RequestParam Integer idArea,
            @RequestParam MultipartFile contratoFirmado,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {
        try {
            ContratoEmpleado contrato = contratoEmpleadoService.buscarPorId(idContrato)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
            if (!contrato.getEmpleado().getIdEmpleado().equals(id)) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND);
            }
            contratoEmpleadoService.renovar(idContrato, fechaInicio, fechaFin, salario,
                    idTipoContrato, idCargo, idArea, contratoFirmado,
                    usuarioService.obtenerUsuarioAutenticado(authentication));
            redirectAttributes.addFlashAttribute("mensajeExito", "Contrato renovado; se conservó el historial anterior.");
        } catch (IllegalArgumentException | IllegalStateException | SecurityException ex) {
            redirectAttributes.addFlashAttribute("mensajeError", ex.getMessage());
        }
        return "redirect:/recursos-humanos/principal?empleadoId=" + id + "#rrhh-detail-contracts";
    }

    @PostMapping("/recursos-humanos/empleados/{id}/documentos")
    @PreAuthorize("hasAuthority('PERM_RRHH_GESTIONAR')")
    public String agregarDocumento(
            @PathVariable Integer id,
            @RequestParam Integer idTipoAdjunto,
            @RequestParam(required = false) Integer idContrato,
            @RequestParam MultipartFile archivo,
            RedirectAttributes redirectAttributes) {
        try {
            adjuntoService.agregarDocumento(id, idTipoAdjunto, idContrato, archivo);
            redirectAttributes.addFlashAttribute("mensajeExito", "Documento agregado al expediente.");
        } catch (IllegalArgumentException | IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("mensajeError", ex.getMessage());
        }
        return "redirect:/recursos-humanos/principal?empleadoId=" + id + "&pestana=documentos#ficha-empleado";
    }

    @PostMapping("/recursos-humanos/empleados/{id}/documentos/{idAdjunto}/reemplazar")
    @PreAuthorize("hasAnyAuthority('PERM_RRHH_GESTIONAR', 'PERM_RRHH_REGISTRO_EDITAR')")
    public String reemplazarDocumentoInicial(
            @PathVariable Integer id,
            @PathVariable Long idAdjunto,
            @RequestParam MultipartFile archivo,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {
        try {
            validarPermisoGestionDocumentosIniciales(id, authentication);
            adjuntoService.reemplazarDocumentoInicial(id, idAdjunto, archivo);
            redirectAttributes.addFlashAttribute("mensajeExito", "Documento reemplazado correctamente.");
        } catch (IllegalArgumentException | IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("mensajeError", ex.getMessage());
        }
        return "redirect:/recursos-humanos/principal?empleadoId=" + id
                + "&pestana=documentos#ficha-empleado";
    }

    @PostMapping("/recursos-humanos/empleados/{id}/documentos/{idAdjunto}/retirar")
    @PreAuthorize("hasAnyAuthority('PERM_RRHH_GESTIONAR', 'PERM_RRHH_REGISTRO_EDITAR')")
    public String retirarDocumentoInicial(
            @PathVariable Integer id,
            @PathVariable Long idAdjunto,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {
        try {
            validarPermisoGestionDocumentosIniciales(id, authentication);
            adjuntoService.retirarDocumentoInicial(id, idAdjunto);
            redirectAttributes.addFlashAttribute("mensajeExito", "Documento retirado del expediente.");
        } catch (IllegalArgumentException | IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("mensajeError", ex.getMessage());
        }
        return "redirect:/recursos-humanos/principal?empleadoId=" + id
                + "&pestana=documentos#ficha-empleado";
    }

    private void validarPermisoGestionDocumentosIniciales(Integer idEmpleado, Authentication authentication) {
        Empleado empleado = empleadoService.buscarPorId(idEmpleado)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        boolean puedeGestionar = usuarioService.obtenerUsuarioAutenticado(authentication)
                .getPermisos().stream()
                .anyMatch(permiso -> "RRHH_GESTIONAR".equals(permiso.getCodigo()));
        if (empleado.getEstado() != EstadoEmpleado.REGISTRO_INICIAL && !puedeGestionar) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
    }

    @GetMapping("/recursos-humanos/empleados/{id}/contratos/{idContrato}/documento")
    @PreAuthorize("hasAnyAuthority('PERM_RRHH_VER', 'PERM_RRHH_GESTIONAR', 'PERM_RRHH_APROBAR', 'PERM_RRHH_REGISTRO_EDITAR')")
    public ResponseEntity<ByteArrayResource> verContratoFirmado(
            @PathVariable Integer id, @PathVariable Integer idContrato) {
        return respuestaContratoFirmado(id, idContrato, false);
    }

    @GetMapping("/recursos-humanos/empleados/{id}/contratos/{idContrato}/documento/descargar")
    @PreAuthorize("hasAnyAuthority('PERM_RRHH_VER', 'PERM_RRHH_GESTIONAR', 'PERM_RRHH_APROBAR', 'PERM_RRHH_REGISTRO_EDITAR')")
    public ResponseEntity<ByteArrayResource> descargarContratoFirmado(
            @PathVariable Integer id, @PathVariable Integer idContrato) {
        return respuestaContratoFirmado(id, idContrato, true);
    }

    private ResponseEntity<ByteArrayResource> respuestaContratoFirmado(
            Integer idEmpleado, Integer idContrato, boolean descargar) {
        IEmpleadoAdjuntoService.DocumentoContratoFirmado adjunto =
                adjuntoService.obtenerDocumentoContratoFirmado(idEmpleado, idContrato)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        ContentDisposition disposicion = (descargar
                ? ContentDisposition.attachment()
                : ContentDisposition.inline())
                .filename(adjunto.nombreOriginal(), java.nio.charset.StandardCharsets.UTF_8)
                .build();
        ResponseEntity.BodyBuilder respuesta = ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, disposicion.toString())
                .header(HttpHeaders.CACHE_CONTROL, "private, no-store")
                .header("X-Content-Type-Options", "nosniff");
        return respuesta.body(new ByteArrayResource(adjunto.contenido()));
    }

    @PostMapping("/recursos-humanos/empleados/{id}/qr")
    @PreAuthorize("hasAuthority('PERM_RRHH_QR_GESTIONAR')")
    public String renovarQr(@PathVariable Integer id, Authentication authentication,
            HttpServletRequest request, RedirectAttributes redirectAttributes) {
        try {
            String urlBase = request.getScheme() + "://" + request.getServerName()
                    + (request.getServerPort() == 80 || request.getServerPort() == 443
                            ? "" : ":" + request.getServerPort())
                    + request.getContextPath() + "/recursos-humanos/tarjeta/";
            boolean yaTeniaQr = empleadoQrService.tieneQrActivo(id);
            empleadoQrService.renovar(id,
                    usuarioService.obtenerUsuarioAutenticado(authentication), urlBase);
            redirectAttributes.addFlashAttribute("mensajeExito", yaTeniaQr
                    ? "QR renovado. El anterior quedó completamente invalidado."
                    : "QR generado correctamente.");
        } catch (IllegalArgumentException | IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("mensajeError", ex.getMessage());
        }
        return "redirect:/recursos-humanos/principal?empleadoId=" + id + "#modal-qr";
    }

    @GetMapping("/recursos-humanos/empleados/{id}/qr/imagen")
    @PreAuthorize("hasAuthority('PERM_RRHH_QR_GESTIONAR')")
    public ResponseEntity<ByteArrayResource> imagenQrEmpleado(
            @PathVariable Integer id, @RequestParam(defaultValue = "false") boolean descargar,
            Authentication authentication) {
        byte[] imagen;
        try {
            imagen = empleadoQrService.obtenerImagenQrActivo(
                    id, usuarioService.obtenerUsuarioAutenticado(authentication));
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage());
        }
        ContentDisposition disposicion = (descargar
                ? ContentDisposition.attachment()
                : ContentDisposition.inline())
                .filename("tarjeta-empleado-" + id + ".png",
                        java.nio.charset.StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .header(HttpHeaders.CONTENT_DISPOSITION, disposicion.toString())
                .header(HttpHeaders.CACHE_CONTROL, "private, no-store")
                .header("X-Content-Type-Options", "nosniff")
                .body(new ByteArrayResource(imagen));
    }

    @GetMapping("/recursos-humanos/empleados/{id}/qr/foto")
    @PreAuthorize("hasAuthority('PERM_RRHH_QR_GESTIONAR')")
    public ResponseEntity<ByteArrayResource> fotoEmpleadoQr(
            @PathVariable Integer id, Authentication authentication) {
        try {
            EmpleadoQrService.ArchivoPublico foto = empleadoQrService.fotoEmpleado(
                    id, usuarioService.obtenerUsuarioAutenticado(authentication));
            return respuestaPublica(MediaType.parseMediaType(foto.mime()), foto.contenido());
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage());
        }
    }

    @PostMapping("/recursos-humanos/empleados/{id}/qr/revocar")
    @PreAuthorize("hasAuthority('PERM_RRHH_QR_GESTIONAR')")
    public String revocarQr(@PathVariable Integer id, Authentication authentication,
            RedirectAttributes redirectAttributes) {
        try {
            empleadoQrService.revocar(id, usuarioService.obtenerUsuarioAutenticado(authentication));
            redirectAttributes.addFlashAttribute("mensajeExito", "QR revocado correctamente.");
        } catch (IllegalArgumentException | IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("mensajeError", ex.getMessage());
        }
        return "redirect:/recursos-humanos/principal?empleadoId=" + id + "#modal-qr";
    }

    @PostMapping("/recursos-humanos/empleados/{id}/perfil")
    @PreAuthorize("hasAuthority('PERM_RRHH_PERFIL_EDITAR')")
    public String actualizarPerfil(@PathVariable Integer id,
            @ModelAttribute("perfilForm") EmpleadoPerfilForm form,
            Authentication authentication, RedirectAttributes redirectAttributes) {
        try {
            empleadoPerfilService.actualizar(id, form,
                    usuarioService.obtenerUsuarioAutenticado(authentication));
            redirectAttributes.addFlashAttribute("mensajeExito", "Perfil actualizado y cambios auditados.");
        } catch (IllegalArgumentException | IllegalStateException | SecurityException ex) {
            redirectAttributes.addFlashAttribute("mensajeError", ex.getMessage());
        }
        return "redirect:/recursos-humanos/principal?empleadoId=" + id + "#ficha-empleado";
    }

    @GetMapping("/recursos-humanos/tarjeta/{token}")
    public String tarjetaPublica(@PathVariable String token, Model model,
            HttpServletResponse response) {
        try {
            model.addAttribute("tarjeta", empleadoQrService.consultar(token));
            response.setHeader("X-Robots-Tag", "noindex, nofollow, noarchive");
            response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store, no-cache, must-revalidate");
            return "tarjeta-empleado-publica";
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
    }

    @GetMapping("/recursos-humanos/tarjeta/{token}/qr.png")
    public ResponseEntity<ByteArrayResource> qrPublico(@PathVariable String token,
            HttpServletRequest request) {
        try {
            empleadoQrService.consultar(token);
            String url = request.getScheme() + "://" + request.getServerName()
                    + (request.getServerPort() == 80 || request.getServerPort() == 443
                            ? "" : ":" + request.getServerPort())
                    + request.getContextPath() + "/recursos-humanos/tarjeta/" + token;
            var hints = new java.util.HashMap<EncodeHintType, Object>();
            hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
            hints.put(EncodeHintType.MARGIN, 2);
            BitMatrix matrix = new QRCodeWriter().encode(url, BarcodeFormat.QR_CODE, 320, 320, hints);
            java.io.ByteArrayOutputStream output = new java.io.ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matrix, "PNG", output);
            return respuestaPublica(MediaType.IMAGE_PNG, output.toByteArray());
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        } catch (WriterException | java.io.IOException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "No fue posible generar el código QR");
        }
    }

    @GetMapping("/recursos-humanos/tarjeta/{token}/foto")
    public ResponseEntity<ByteArrayResource> fotoPublica(@PathVariable String token) {
        try {
            EmpleadoQrService.ArchivoPublico foto = empleadoQrService.foto(token);
            MediaType tipo = MediaType.parseMediaType(foto.mime());
            if (!Set.of("image/jpeg", "image/png", "image/webp").contains(tipo.toString())) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND);
            }
            return respuestaPublica(tipo, foto.contenido());
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
    }

    @GetMapping("/recursos-humanos/tarjeta/{token}/arl")
    public ResponseEntity<ByteArrayResource> arlPublica(@PathVariable String token) {
        try {
            EmpleadoQrService.ArchivoPublico arl = empleadoQrService.certificadoArl(token);
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_PDF)
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            ContentDisposition.inline()
                                    .filename(arl.nombre(), java.nio.charset.StandardCharsets.UTF_8)
                                    .build().toString())
                    .header("X-Robots-Tag", "noindex, nofollow, noarchive")
                    .header(HttpHeaders.CACHE_CONTROL, "no-store, no-cache, must-revalidate")
                    .header("X-Content-Type-Options", "nosniff")
                    .body(new ByteArrayResource(arl.contenido()));
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
    }

    private static ResponseEntity<ByteArrayResource> respuestaPublica(MediaType mediaType, byte[] bytes) {
        return ResponseEntity.ok().contentType(mediaType)
                .header("X-Robots-Tag", "noindex, nofollow, noarchive")
                .header(HttpHeaders.CACHE_CONTROL, "no-store, no-cache, must-revalidate")
                .body(new ByteArrayResource(bytes));
    }

    @GetMapping({
            "/recursos-humanos/empleados/{id}/documentos/{idAdjunto}",
            "/recursos-humanos/empleados/{id}/documentos/{idAdjunto}/descargar"
    })
    @PreAuthorize("hasAnyAuthority('PERM_RRHH_VER', 'PERM_RRHH_GESTIONAR', 'PERM_RRHH_APROBAR', 'PERM_RRHH_REGISTRO_EDITAR')")
    public ResponseEntity<ByteArrayResource> descargarAdjunto(
            @PathVariable Integer id, @PathVariable Long idAdjunto) {
        EmpleadoAdjunto adjunto = adjuntoService.buscarAdjunto(id, idAdjunto);
        MediaType mediaType = MediaType.APPLICATION_OCTET_STREAM;
        try {
            mediaType = MediaType.parseMediaType(adjunto.getTipoMime());
        } catch (IllegalArgumentException ignored) {
            // Se sirve como binario si el MIME guardado no es válido.
        }
        return ResponseEntity.ok().contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment()
                                .filename(adjunto.getNombreOriginal(), java.nio.charset.StandardCharsets.UTF_8)
                                .build().toString())
                .body(new ByteArrayResource(adjunto.getContenido()));
    }

    @GetMapping("/recursos-humanos/empleados/{id}/documentos/{idAdjunto}/ver")
    @PreAuthorize("hasAnyAuthority('PERM_RRHH_VER', 'PERM_RRHH_GESTIONAR', 'PERM_RRHH_APROBAR', 'PERM_RRHH_REGISTRO_EDITAR')")
    public ResponseEntity<ByteArrayResource> verAdjunto(
            @PathVariable Integer id, @PathVariable Long idAdjunto) {
        EmpleadoAdjunto adjunto = adjuntoService.buscarAdjunto(id, idAdjunto);
        return respuestaPdfInline(adjunto.getNombreOriginal(), adjunto.getContenido());
    }

    @PostMapping("/recursos-humanos/empleados/{id}/documentos/{idAdjunto}/publicar-qr")
    @PreAuthorize("hasAuthority('PERM_RRHH_QR_GESTIONAR')")
    public String publicarAdjuntoQr(@PathVariable Integer id, @PathVariable Long idAdjunto,
            @RequestParam boolean publicar, RedirectAttributes redirectAttributes) {
        try {
            adjuntoService.actualizarPublicacionQr(id, idAdjunto, publicar);
            redirectAttributes.addFlashAttribute("mensajeExito", publicar
                    ? "Certificado ARL publicado para la tarjeta QR."
                    : "Certificado ARL retirado de la tarjeta QR.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("mensajeError", ex.getMessage());
        }
        return "redirect:/recursos-humanos/principal?empleadoId=" + id + "&pestana=documentos#ficha-empleado";
    }

    @GetMapping("/recursos-humanos/empleados/{id}/asignaciones/{idAsignacion}/acta")
    @PreAuthorize("hasAnyAuthority('PERM_RRHH_VER', 'PERM_RRHH_GESTIONAR', 'PERM_RRHH_APROBAR', 'PERM_RRHH_REGISTRO_EDITAR')")
    public ResponseEntity<ByteArrayResource> verActaHerramienta(
            @PathVariable Integer id, @PathVariable Long idAsignacion) {
        AsignacionHerramientaService.ActaHerramienta acta =
                asignacionHerramientaService.obtenerActa(id, idAsignacion);
        return respuestaPdfInline(acta.nombre(), acta.contenido());
    }

    @GetMapping("/recursos-humanos/empleados/{id}/asignaciones/{idAsignacion}/acta/descargar")
    @PreAuthorize("hasAnyAuthority('PERM_RRHH_VER', 'PERM_RRHH_GESTIONAR', 'PERM_RRHH_APROBAR', 'PERM_RRHH_REGISTRO_EDITAR')")
    public ResponseEntity<ByteArrayResource> descargarActaHerramientaArchivo(
            @PathVariable Integer id, @PathVariable Long idAsignacion) {
        AsignacionHerramientaService.ActaHerramienta acta =
                asignacionHerramientaService.obtenerActa(id, idAsignacion);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment()
                                .filename(acta.nombre(), java.nio.charset.StandardCharsets.UTF_8)
                                .build()
                                .toString())
                .body(new ByteArrayResource(acta.contenido()));
    }

    private static ResponseEntity<ByteArrayResource> respuestaPdfInline(String nombre, byte[] contenido) {
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.inline()
                                .filename(nombre, java.nio.charset.StandardCharsets.UTF_8)
                                .build().toString())
                .header(HttpHeaders.CACHE_CONTROL, "private, no-store")
                .header("X-Content-Type-Options", "nosniff")
                .body(new ByteArrayResource(contenido));
    }

    private EmpleadoRegistroForm formDesdeEmpleado(Empleado empleado) {
        EmpleadoRegistroForm form = new EmpleadoRegistroForm();
        form.setIdEmpleado(empleado.getIdEmpleado());
        form.setVersion(empleado.getVersion());
        form.setTipoDocumento(empleado.getTipoDocumento());
        form.setNumeroDocumento(empleado.getNumeroDocumento());
        form.setFechaExpedicion(empleado.getFechaExpedicion());
        form.setLugarExpedicion(empleado.getLugarExpedicion());
        form.setPrimerNombre(empleado.getPrimerNombre());
        form.setSegundoNombre(empleado.getSegundoNombre());
        form.setPrimerApellido(empleado.getPrimerApellido());
        form.setSegundoApellido(empleado.getSegundoApellido());
        form.setFechaNacimiento(empleado.getFechaNacimiento());
        form.setLugarNacimiento(empleado.getLugarNacimiento());
        form.setTipoSangre(empleado.getTipoSangre());
        form.setRh(empleado.getRh());
        form.setCelular(empleado.getCelular());
        form.setCorreoElectronico(empleado.getCorreoElectronico());
        form.setDireccionResidencia(empleado.getDireccionResidencia());
        form.setBarrio(empleado.getBarrio());
        form.setMunicipio(empleado.getMunicipio());
        form.setDepartamento(empleado.getDepartamento());
        form.setNivelEducativoId(empleado.getNivelEducativo() == null
                ? null : empleado.getNivelEducativo().getId());
        form.setProfesion(empleado.getProfesion());
        form.setAreaId(empleado.getArea() == null ? null : empleado.getArea().getId());
        form.setCargoId(empleado.getCargo() == null ? null : empleado.getCargo().getId());
        form.setCentroCostoId(empleado.getCentroCosto() == null
                ? null : empleado.getCentroCosto().getIdCentroCosto());
        form.setSubcentro(empleado.getSubcentro());
        return form;
    }

    private EmpleadoPerfilForm formDesdePerfil(Empleado empleado) {
        EmpleadoPerfilForm form = new EmpleadoPerfilForm();
        form.setTipoDocumento(empleado.getTipoDocumento());
        form.setNumeroDocumento(empleado.getNumeroDocumento());
        form.setFechaExpedicion(empleado.getFechaExpedicion());
        form.setLugarExpedicion(empleado.getLugarExpedicion());
        form.setPrimerNombre(empleado.getPrimerNombre());
        form.setSegundoNombre(empleado.getSegundoNombre());
        form.setPrimerApellido(empleado.getPrimerApellido());
        form.setSegundoApellido(empleado.getSegundoApellido());
        form.setFechaNacimiento(empleado.getFechaNacimiento());
        form.setLugarNacimiento(empleado.getLugarNacimiento());
        form.setTipoSangre(empleado.getTipoSangre());
        form.setRh(empleado.getRh());
        form.setCelular(empleado.getCelular());
        form.setCorreoElectronico(empleado.getCorreoElectronico());
        form.setDireccionResidencia(empleado.getDireccionResidencia());
        form.setBarrio(empleado.getBarrio());
        form.setMunicipio(empleado.getMunicipio());
        form.setDepartamento(empleado.getDepartamento());
        form.setProfesion(empleado.getProfesion());
        form.setNivelEducativoId(empleado.getNivelEducativo() == null
                ? null : empleado.getNivelEducativo().getId());
        return form;
    }

    private long contarPorEstado(List<Empleado> empleados, EstadoEmpleado estado) {
        return empleados.stream()
                .filter(empleado -> empleado.getEstado() == estado)
                .count();
    }
}
