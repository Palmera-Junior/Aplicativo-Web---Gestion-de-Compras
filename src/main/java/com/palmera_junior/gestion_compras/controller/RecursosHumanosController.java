package com.palmera_junior.gestion_compras.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
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
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

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
import com.palmera_junior.gestion_compras.rrhh.web.EmpleadoRegistroForm;
import com.palmera_junior.gestion_compras.repository.CentroCostoRepository;
import com.palmera_junior.gestion_compras.service.usuario.IUsuarioService;

/** Vistas y acciones del módulo de Recursos Humanos. */
@Controller
public class RecursosHumanosController {

    private final IEmpleadoService empleadoService;
    private final IEmpleadoAdjuntoService adjuntoService;
    private final EmpleadoRegistroWorkflowService registroWorkflow;
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

    public RecursosHumanosController(
            IEmpleadoService empleadoService,
            IEmpleadoAdjuntoService adjuntoService,
            EmpleadoRegistroWorkflowService registroWorkflow,
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
        this.adjuntoService = adjuntoService;
        this.registroWorkflow = registroWorkflow;
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
            Authentication authentication,
            Model model) {
        boolean formularioEnFlash = model.containsAttribute("registroEmpleado");
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
        model.addAttribute("puedeGestionar", permisos.contains("RRHH_GESTIONAR"));
        model.addAttribute("puedeEditarRegistro", permisos.contains("RRHH_REGISTRO_EDITAR"));
        model.addAttribute("puedeAprobar", permisos.contains("RRHH_APROBAR"));
        model.addAttribute("esEdicionContratacion", esEdicionContratacion);
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
        model.addAttribute("historialEstados", List.of());
        model.addAttribute("empleadoEditar", null);
        model.addAttribute("adjuntosEmpleado", List.of());
        model.addAttribute("tiposAdjuntosVigentes", "");
        if (!formularioEnFlash && editarEmpleadoId == null) {
            model.addAttribute("registroEmpleado", new EmpleadoRegistroForm());
        }

        if (empleadoId != null) {
            Empleado empleado = empleadoService.buscarPorId(empleadoId)
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.NOT_FOUND, "No existe el empleado solicitado"));
            List<EmpleadoHistorialEstado> historial =
                    empleadoService.listarHistorialEstado(empleadoId);
            model.addAttribute("empleadoSeleccionado", empleado);
            model.addAttribute("historialEstados", historial);
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
                form.setTiposAdjuntoRequeridos(requisitos.stream()
                        .filter(requisito -> requisito.getEtapaRequerida()
                                == (esEdicionContratacion
                                        ? EtapaAdjuntoEmpleado.CONTRATACION
                                        : EtapaAdjuntoEmpleado.REGISTRO_INICIAL))
                        .map(requisito -> requisito.getTipoAdjunto().getIdTipoAdjuntoEmpleado())
                        .collect(Collectors.toCollection(ArrayList::new)));
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
        EmpleadoRegistroForm registro = (EmpleadoRegistroForm) model.getAttribute("registroEmpleado");
        List<Integer> tiposSeleccionados = registro == null || registro.getTiposAdjuntoRequeridos() == null
                ? List.of() : registro.getTiposAdjuntoRequeridos();
        model.addAttribute("tiposSeleccionados", tiposSeleccionados);
        model.addAttribute("tiposAdjuntoSeleccionados", tiposAdjuntoIniciales.stream()
                .filter(tipo -> tiposSeleccionados.contains(tipo.getIdTipoAdjuntoEmpleado()))
                .toList());
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

    private long contarPorEstado(List<Empleado> empleados, EstadoEmpleado estado) {
        return empleados.stream()
                .filter(empleado -> empleado.getEstado() == estado)
                .count();
    }
}
