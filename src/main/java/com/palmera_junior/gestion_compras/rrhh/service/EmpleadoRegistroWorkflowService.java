package com.palmera_junior.gestion_compras.rrhh.service;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.MultiValueMap;
import org.springframework.web.multipart.MultipartFile;

import com.palmera_junior.gestion_compras.entity.Empleado;
import com.palmera_junior.gestion_compras.entity.Permiso;
import com.palmera_junior.gestion_compras.entity.Usuario;
import com.palmera_junior.gestion_compras.repository.CentroCostoRepository;
import com.palmera_junior.gestion_compras.repository.EmpleadoRepository;
import com.palmera_junior.gestion_compras.rrhh.entity.EmpleadoAdjunto;
import com.palmera_junior.gestion_compras.rrhh.entity.EmpleadoAdjuntoRequerido;
import com.palmera_junior.gestion_compras.rrhh.entity.EmpleadoAdjuntoRequeridoId;
import com.palmera_junior.gestion_compras.rrhh.entity.EtapaAdjuntoEmpleado;
import com.palmera_junior.gestion_compras.rrhh.entity.ContratoEmpleado;
import com.palmera_junior.gestion_compras.rrhh.entity.CatalogoRrhh;
import com.palmera_junior.gestion_compras.rrhh.entity.TipoAdjuntoEmpleado;
import com.palmera_junior.gestion_compras.rrhh.entity.EstadoEmpleado;
import com.palmera_junior.gestion_compras.rrhh.entity.EstadoContratoEmpleado;
import com.palmera_junior.gestion_compras.rrhh.repository.AfpRepository;
import com.palmera_junior.gestion_compras.rrhh.repository.ArlRepository;
import com.palmera_junior.gestion_compras.rrhh.repository.AreaRepository;
import com.palmera_junior.gestion_compras.rrhh.repository.CajaCompensacionRepository;
import com.palmera_junior.gestion_compras.rrhh.repository.CargoRepository;
import com.palmera_junior.gestion_compras.rrhh.repository.EntidadBancariaRepository;
import com.palmera_junior.gestion_compras.rrhh.repository.EpsRepository;
import com.palmera_junior.gestion_compras.rrhh.repository.ContratoEmpleadoRepository;
import com.palmera_junior.gestion_compras.rrhh.repository.EmpleadoAdjuntoRepository;
import com.palmera_junior.gestion_compras.rrhh.repository.EmpleadoAdjuntoRequeridoRepository;
import com.palmera_junior.gestion_compras.rrhh.repository.NivelEducativoRepository;
import com.palmera_junior.gestion_compras.rrhh.repository.TipoContratoRepository;
import com.palmera_junior.gestion_compras.rrhh.repository.TipoAdjuntoEmpleadoRepository;
import com.palmera_junior.gestion_compras.rrhh.repository.TipoPagoRepository;
import com.palmera_junior.gestion_compras.rrhh.web.EmpleadoRegistroForm;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EmpleadoRegistroWorkflowService {

    private static final long MAX_TAMANO_ADJUNTO = 10L * 1024 * 1024;
    private static final long MAX_TAMANO_TOTAL_ADJUNTOS = 50L * 1024 * 1024;
    private static final BigDecimal MAX_DECIMAL_10_2 = new BigDecimal("99999999.99");
    private static final Set<String> GRUPOS_INICIALES = Set.of("INGRESO", "VEHICULO");
    private static final Set<String> GRUPOS_CONTRATACION = Set.of("AFILIACIONES", "EVALUACIONES");

    private final IEmpleadoService empleadoService;
    private final IEmpleadoAdjuntoService adjuntoService;
    private final AsignacionHerramientaService asignacionHerramientaService;
    private final EmpleadoRepository empleadoRepository;
    private final ContratoEmpleadoRepository contratoRepository;
    private final EmpleadoAdjuntoRepository adjuntoRepository;
    private final EmpleadoAdjuntoRequeridoRepository requeridoRepository;
    private final TipoAdjuntoEmpleadoRepository tipoAdjuntoRepository;
    private final AreaRepository areaRepository;
    private final CargoRepository cargoRepository;
    private final CentroCostoRepository centroCostoRepository;
    private final NivelEducativoRepository nivelEducativoRepository;
    private final CorreoEmpleadoOutboxService correoOutboxService;
    private final EmpleadoFotoService empleadoFotoService;
    private final TipoContratoRepository tipoContratoRepository;
    private final TipoPagoRepository tipoPagoRepository;
    private final EntidadBancariaRepository entidadBancariaRepository;
    private final EpsRepository epsRepository;
    private final AfpRepository afpRepository;
    private final ArlRepository arlRepository;
    private final CajaCompensacionRepository cajaCompensacionRepository;

    @Transactional
    public Empleado guardar(
            EmpleadoRegistroForm form,
            MultiValueMap<String, MultipartFile> archivos,
            Usuario actor) {
        if (form == null) {
            throw new IllegalArgumentException("No se recibieron datos del empleado");
        }
        validarTamanoTotalAdjuntos(archivos);
        boolean nuevo = form.getIdEmpleado() == null;
        if (!nuevo) {
            Empleado actual = empleadoRepository.findById(form.getIdEmpleado())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "El empleado que intentas editar ya no existe"));
            if (form.getVersion() == null || !form.getVersion().equals(actual.getVersion())) {
                throw new IllegalStateException(
                        "El registro cambió desde que lo abriste. Recarga la página antes de guardar.");
            }
            if (actual.getEstado() == EstadoEmpleado.PROCESO_CONTRATACION) {
                throw new IllegalStateException(
                        "La contratación debe guardarse o completarse desde su acción específica");
            }
            if (actual.getEstado() != EstadoEmpleado.REGISTRO_INICIAL) {
                throw new IllegalStateException(
                        "Solo se puede editar un empleado en Registro inicial");
            }
        }
        validarFormulario(form);
        validarCorreoNoDuplicado(form.getCorreoElectronico(), form.getIdEmpleado());
        Empleado empleado = mapearEmpleado(form);
        if (!nuevo) {
            validarDocumentoNoDuplicado(empleado, form.getIdEmpleado());
            empleado.setIdEmpleado(form.getIdEmpleado());
            empleado = empleadoService.editarRegistroInicial(form.getIdEmpleado(), empleado, actor);
        } else {
            validarDocumentoNoDuplicado(empleado, null);
            empleado = empleadoService.crearRegistroInicial(empleado, actor);
        }
        empleadoFotoService.guardar(empleado, form.getFoto());
        asignacionHerramientaService.sincronizarPendientes(
                empleado, form.getHerramientasSolicitadasIds());
        guardarRequisitos(
                empleado, form.getTiposAdjuntoRequeridos(), EtapaAdjuntoEmpleado.REGISTRO_INICIAL);
        guardarAdjuntos(
                empleado, archivos, form.getTiposAdjuntoRequeridos(),
                EtapaAdjuntoEmpleado.REGISTRO_INICIAL);
        if (nuevo) {
            correoOutboxService.registrarNotificacionesRegistroInicial(empleado);
        }
        return empleado;
    }

    @Transactional(readOnly = true)
    public void cargarDatosContratacionPendiente(Integer idEmpleado, EmpleadoRegistroForm form) {
        contratoRepository
                .findFirstByEmpleado_IdEmpleadoAndEstadoOrderByIdContratoEmpleadoDesc(
                        idEmpleado, EstadoContratoEmpleado.BORRADOR)
                .ifPresent(contrato -> copiarContratoAlFormulario(contrato, form));
    }

    @Transactional
    public Empleado guardarAvanceContratacion(
            Integer idEmpleado,
            EmpleadoRegistroForm form,
            MultiValueMap<String, MultipartFile> archivos,
            Usuario actor) {
        Empleado empleado = validarEmpleadoEnContratacion(idEmpleado, form, actor);
        validarTamanoTotalAdjuntos(archivos);
        ContratoEmpleado contrato = obtenerContratoBorrador(idEmpleado, form);
        mapearContrato(form, contrato);
        contrato.setCargo(empleado.getCargo());
        contrato.setArea(empleado.getArea());
        validarMinimosBorrador(contrato);
        contrato.setEmpleado(empleado);
        contrato.setEstado(EstadoContratoEmpleado.BORRADOR);
        guardarRequisitos(
                empleado, form.getTiposAdjuntoRequeridos(),
                EtapaAdjuntoEmpleado.CONTRATACION);
        guardarAdjuntos(
                empleado, archivos, form.getTiposAdjuntoRequeridos(),
                EtapaAdjuntoEmpleado.CONTRATACION);
        contratoRepository.save(contrato);
        return empleado;
    }

    @Transactional
    public Empleado completarContratacion(
            Integer idEmpleado,
            EmpleadoRegistroForm form,
            MultiValueMap<String, MultipartFile> archivos,
            Usuario actor) {
        Empleado empleado = validarEmpleadoEnContratacion(idEmpleado, form, actor);
        validarTamanoTotalAdjuntos(archivos);
        ContratoEmpleado contrato = obtenerContratoBorrador(idEmpleado, form);
        mapearContrato(form, contrato);
        contrato.setCargo(empleado.getCargo());
        contrato.setArea(empleado.getArea());
        contrato.setEmpleado(empleado);
        guardarRequisitos(
                empleado, form.getTiposAdjuntoRequeridos(), EtapaAdjuntoEmpleado.CONTRATACION);
        guardarAdjuntos(
                empleado, archivos, form.getTiposAdjuntoRequeridos(),
                EtapaAdjuntoEmpleado.CONTRATACION);
        Empleado completado =
                empleadoService.completarRegistro(empleado.getIdEmpleado(), contrato, actor);
        asociarAdjuntosAlContrato(empleado, contrato, form.getTiposAdjuntoRequeridos());
        return completado;
    }

    private ContratoEmpleado mapearContrato(
            EmpleadoRegistroForm form, ContratoEmpleado contrato) {
        ValidacionDatosEmpleado.validarFechaIngresoNoPasada(form.getFechaIngreso());
        ValidacionDatosEmpleado.validarSoloDigitos(
                form.getNumeroCuentaBancaria(), "El número de cuenta bancaria");
        ValidacionDatosEmpleado.validarNoNegativo("salario básico", form.getSalarioBasico());
        ValidacionDatosEmpleado.validarNoNegativo("tarifa ARL", form.getTarifaArlPorcentaje());
        ValidacionDatosEmpleado.validarNoNegativo(
                "auxilio de transporte", form.getAuxilioTransporteValor());
        ValidacionDatosEmpleado.validarNoNegativo(
                "auxilio de rodamiento", form.getAuxilioRodamientoValor());
        ValidacionDatosEmpleado.validarNoNegativo(
                "bonificación no salarial", form.getBonificacionNoSalarialValor());
        contrato.setFechaIngreso(form.getFechaIngreso());
        contrato.setFechaFin(form.getFechaFin());
        contrato.setSalarioBasico(validarDecimal10_2("salario básico", form.getSalarioBasico()));
        contrato.setTipoContrato(buscarCatalogoOpcional(
                form.getTipoContratoId(), tipoContratoRepository, "tipo de contrato"));
        contrato.setTipoPago(buscarCatalogoOpcional(
                form.getTipoPagoId(), tipoPagoRepository, "tipo de pago"));
        contrato.setEntidadBancaria(buscarCatalogoOpcional(
                form.getEntidadBancariaId(), entidadBancariaRepository, "entidad bancaria"));
        contrato.setNumeroCuentaBancaria(normalizar(form.getNumeroCuentaBancaria()));
        contrato.setEps(buscarCatalogoOpcional(form.getEpsId(), epsRepository, "EPS"));
        contrato.setAfp(buscarCatalogoOpcional(form.getAfpId(), afpRepository, "AFP"));
        contrato.setArl(buscarCatalogoOpcional(form.getArlId(), arlRepository, "ARL"));
        contrato.setCajaCompensacion(buscarCatalogoOpcional(
                form.getCajaCompensacionId(), cajaCompensacionRepository, "caja de compensación"));
        contrato.setTarifaArlPorcentaje(validarDecimal10_2(
                "tarifa ARL", form.getTarifaArlPorcentaje()));
        contrato.setAuxilioTransporteAplica(form.isAuxilioTransporteAplica());
        contrato.setAuxilioTransporteValor(valorAplicable(
                "auxilio de transporte", form.isAuxilioTransporteAplica(),
                form.getAuxilioTransporteValor()));
        contrato.setAuxilioRodamientoAplica(form.isAuxilioRodamientoAplica());
        contrato.setAuxilioRodamientoValor(valorAplicable(
                "auxilio de rodamiento", form.isAuxilioRodamientoAplica(),
                form.getAuxilioRodamientoValor()));
        contrato.setBonificacionNoSalarialAplica(form.isBonificacionNoSalarialAplica());
        contrato.setBonificacionNoSalarialValor(valorAplicable(
                "bonificación no salarial", form.isBonificacionNoSalarialAplica(),
                form.getBonificacionNoSalarialValor()));
        contrato.setCondicionesEspecialesAplica(form.isCondicionesEspecialesAplica());
        contrato.setCondicionesEspeciales(form.isCondicionesEspecialesAplica()
                ? normalizar(form.getCondicionesEspeciales()) : null);
        if (form.getTarifaArlPorcentaje() != null
                && (form.getTarifaArlPorcentaje().signum() < 0
                        || form.getTarifaArlPorcentaje().compareTo(BigDecimal.valueOf(100)) > 0)) {
            throw new IllegalArgumentException("La tarifa ARL debe estar entre 0 y 100");
        }
        if (form.isCondicionesEspecialesAplica() && vacio(form.getCondicionesEspeciales())) {
            throw new IllegalArgumentException(
                    "Describe las condiciones especiales del contrato");
        }
        return contrato;
    }

    private Empleado validarEmpleadoEnContratacion(
            Integer idEmpleado, EmpleadoRegistroForm form, Usuario actor) {
        exigirPermiso(actor, "RRHH_GESTIONAR");
        if (form == null || !Objects.equals(idEmpleado, form.getIdEmpleado())) {
            throw new IllegalArgumentException(
                    "Los datos de contratación no corresponden al empleado indicado");
        }
        Empleado empleado = empleadoRepository.findByIdForUpdate(idEmpleado)
                .orElseThrow(() -> new IllegalArgumentException(
                        "El empleado que intentas editar ya no existe"));
        if (form.getVersion() == null || !form.getVersion().equals(empleado.getVersion())) {
            throw new IllegalStateException(
                    "El registro cambió desde que lo abriste. Recarga la página antes de guardar.");
        }
        if (empleado.getEstado() != EstadoEmpleado.PROCESO_CONTRATACION) {
            throw new IllegalStateException(
                    "Solo se puede guardar contratación mientras el empleado esté en Proceso de contratación");
        }
        return empleado;
    }

    private ContratoEmpleado obtenerContratoBorrador(
            Integer idEmpleado, EmpleadoRegistroForm form) {
        ContratoEmpleado borrador = contratoRepository
                .findFirstByEmpleado_IdEmpleadoAndEstadoOrderByIdContratoEmpleadoDesc(
                        idEmpleado, EstadoContratoEmpleado.BORRADOR)
                .orElse(null);
        if (borrador == null) {
            if (form.getIdContratoEmpleado() != null || form.getVersionContrato() != null) {
                throw new IllegalStateException(
                        "El borrador contractual cambió. Recarga la página antes de continuar.");
            }
            return new ContratoEmpleado();
        }
        if (!Objects.equals(form.getIdContratoEmpleado(), borrador.getIdContratoEmpleado())
                || form.getVersionContrato() == null
                || !form.getVersionContrato().equals(borrador.getVersion())) {
            throw new IllegalStateException(
                    "El borrador contractual cambió. Recarga la página antes de continuar.");
        }
        return borrador;
    }

    private static void validarMinimosBorrador(ContratoEmpleado contrato) {
        if (contrato.getFechaIngreso() == null) {
            throw new IllegalArgumentException(
                    "La fecha de ingreso es necesaria para guardar el avance contractual");
        }
        if (contrato.getSalarioBasico() == null || contrato.getSalarioBasico().signum() < 0) {
            throw new IllegalArgumentException(
                    "Ingresa un salario básico válido para guardar el avance contractual");
        }
        ValidacionDatosEmpleado.validarFechaFinPosterior(
                contrato.getFechaIngreso(), contrato.getFechaFin());
    }

    private void copiarContratoAlFormulario(
            ContratoEmpleado contrato, EmpleadoRegistroForm form) {
        form.setIdContratoEmpleado(contrato.getIdContratoEmpleado());
        form.setVersionContrato(contrato.getVersion());
        form.setTipoContratoId(contrato.getTipoContrato() == null
                ? null : contrato.getTipoContrato().getId());
        form.setTipoPagoId(contrato.getTipoPago() == null
                ? null : contrato.getTipoPago().getId());
        form.setEntidadBancariaId(contrato.getEntidadBancaria() == null
                ? null : contrato.getEntidadBancaria().getId());
        form.setNumeroCuentaBancaria(contrato.getNumeroCuentaBancaria());
        form.setFechaIngreso(contrato.getFechaIngreso());
        form.setFechaFin(contrato.getFechaFin());
        form.setSalarioBasico(contrato.getSalarioBasico());
        form.setAuxilioTransporteAplica(contrato.isAuxilioTransporteAplica());
        form.setAuxilioTransporteValor(contrato.getAuxilioTransporteValor());
        form.setAuxilioRodamientoAplica(contrato.isAuxilioRodamientoAplica());
        form.setAuxilioRodamientoValor(contrato.getAuxilioRodamientoValor());
        form.setBonificacionNoSalarialAplica(contrato.isBonificacionNoSalarialAplica());
        form.setBonificacionNoSalarialValor(contrato.getBonificacionNoSalarialValor());
        form.setEpsId(contrato.getEps() == null ? null : contrato.getEps().getId());
        form.setAfpId(contrato.getAfp() == null ? null : contrato.getAfp().getId());
        form.setArlId(contrato.getArl() == null ? null : contrato.getArl().getId());
        form.setTarifaArlPorcentaje(contrato.getTarifaArlPorcentaje());
        form.setCajaCompensacionId(contrato.getCajaCompensacion() == null
                ? null : contrato.getCajaCompensacion().getId());
        form.setCondicionesEspecialesAplica(contrato.isCondicionesEspecialesAplica());
        form.setCondicionesEspeciales(contrato.getCondicionesEspeciales());
    }

    private void asociarAdjuntosAlContrato(
            Empleado empleado, ContratoEmpleado contrato, List<Integer> tiposRequeridos) {
        Set<Integer> idsTiposRequeridos = tiposRequeridos == null
                ? Set.of() : new HashSet<>(tiposRequeridos);
        List<EmpleadoAdjunto> adjuntos = adjuntoRepository
                .findAllByEmpleado_IdEmpleadoOrderByFechaCargaDesc(empleado.getIdEmpleado());
        List<EmpleadoAdjunto> adjuntosContratacion = adjuntos.stream()
                .filter(EmpleadoAdjunto::isVigente)
                .filter(adjunto -> idsTiposRequeridos.contains(
                        adjunto.getTipoAdjunto().getIdTipoAdjuntoEmpleado()))
                .filter(adjunto -> GRUPOS_CONTRATACION.contains(
                        adjunto.getTipoAdjunto().getGrupo().name()))
                .toList();
        if (!adjuntosContratacion.isEmpty()) {
            adjuntosContratacion.forEach(adjunto -> adjunto.setContrato(contrato));
            adjuntoRepository.saveAll(adjuntosContratacion);
        }
    }

    private static void exigirPermiso(Usuario actor, String permiso) {
        Set<String> permisos = actor == null || actor.getPermisos() == null
                ? Set.of()
                : actor.getPermisos().stream()
                        .map(Permiso::getCodigo)
                        .collect(java.util.stream.Collectors.toSet());
        if (!permisos.contains(permiso)) {
            throw new SecurityException("No tiene permiso para esta operación");
        }
    }

    private Empleado mapearEmpleado(EmpleadoRegistroForm form) {
        Empleado empleado = new Empleado();
        empleado.setTipoDocumento(form.getTipoDocumento());
        empleado.setNumeroDocumento(normalizar(form.getNumeroDocumento()));
        empleado.setFechaExpedicion(form.getFechaExpedicion());
        empleado.setLugarExpedicion(normalizar(form.getLugarExpedicion()));
        empleado.setPrimerNombre(normalizar(form.getPrimerNombre()));
        empleado.setSegundoNombre(normalizar(form.getSegundoNombre()));
        empleado.setPrimerApellido(normalizar(form.getPrimerApellido()));
        empleado.setSegundoApellido(normalizar(form.getSegundoApellido()));
        empleado.setFechaNacimiento(form.getFechaNacimiento());
        empleado.setLugarNacimiento(normalizar(form.getLugarNacimiento()));
        empleado.setTipoSangre(form.getTipoSangre());
        empleado.setRh(form.getRh());
        empleado.setCelular(normalizar(form.getCelular()));
        empleado.setCorreoElectronico(normalizar(form.getCorreoElectronico()));
        empleado.setDireccionResidencia(normalizar(form.getDireccionResidencia()));
        empleado.setBarrio(normalizar(form.getBarrio()));
        empleado.setMunicipio(normalizar(form.getMunicipio()));
        empleado.setDepartamento(normalizar(form.getDepartamento()));
        empleado.setProfesion(normalizar(form.getProfesion()));
        empleado.setSubcentro(normalizar(form.getSubcentro()));
        empleado.setArea(buscarCatalogo(areaRepository.findById(form.getAreaId()), "área"));
        empleado.setCargo(buscarCatalogo(cargoRepository.findById(form.getCargoId()), "cargo"));
        empleado.setCentroCosto(buscarCatalogo(
                centroCostoRepository.findById(form.getCentroCostoId()), "centro de costo"));
        if (form.getNivelEducativoId() != null) {
            empleado.setNivelEducativo(buscarCatalogo(
                    nivelEducativoRepository.findById(form.getNivelEducativoId()), "nivel educativo"));
        }
        return empleado;
    }

    private void guardarRequisitos(
            Empleado empleado, List<Integer> idsTipos, EtapaAdjuntoEmpleado etapa) {
        List<Integer> seleccionados = idsTipos == null ? List.of() : idsTipos.stream().distinct().toList();
        List<TipoAdjuntoEmpleado> tipos = seleccionados.stream()
                .map(id -> tipoAdjuntoRepository.findById(id)
                        .orElseThrow(() -> new IllegalArgumentException(
                                "El tipo de adjunto seleccionado no existe")))
                .toList();
        Set<String> gruposPermitidos = etapa == EtapaAdjuntoEmpleado.REGISTRO_INICIAL
                ? GRUPOS_INICIALES : GRUPOS_CONTRATACION;
        if (tipos.stream().anyMatch(tipo -> !tipo.isActivo()
                || !gruposPermitidos.contains(tipo.getGrupo().name()))) {
            throw new IllegalArgumentException(
                    etapa == EtapaAdjuntoEmpleado.REGISTRO_INICIAL
                            ? "Solo se pueden configurar como iniciales adjuntos de ingreso o vehículo"
                            : "Solo se pueden configurar para contratación adjuntos de afiliaciones o evaluaciones");
        }

        requeridoRepository.deleteAllByEmpleado_IdEmpleadoAndEtapaRequerida(
                empleado.getIdEmpleado(), etapa);
        for (TipoAdjuntoEmpleado tipo : tipos) {
            EmpleadoAdjuntoRequerido requerido = new EmpleadoAdjuntoRequerido();
            EmpleadoAdjuntoRequeridoId id = new EmpleadoAdjuntoRequeridoId();
            id.setIdEmpleado(empleado.getIdEmpleado());
            id.setIdTipoAdjuntoEmpleado(tipo.getIdTipoAdjuntoEmpleado());
            requerido.setId(id);
            requerido.setEmpleado(empleado);
            requerido.setTipoAdjunto(tipo);
            requerido.setEtapaRequerida(etapa);
            adjuntoService.guardarRequisito(requerido);
        }
    }

    private void guardarAdjuntos(
            Empleado empleado,
            MultiValueMap<String, MultipartFile> archivos,
            List<Integer> tiposRequeridos,
            EtapaAdjuntoEmpleado etapa) {
        if (archivos == null || archivos.isEmpty()) {
            return;
        }
        Set<Integer> idsTiposRequeridos = tiposRequeridos == null
                ? Set.of() : new HashSet<>(tiposRequeridos);
        Set<String> gruposPermitidos = etapa == EtapaAdjuntoEmpleado.REGISTRO_INICIAL
                ? GRUPOS_INICIALES : GRUPOS_CONTRATACION;
        for (Map.Entry<String, List<MultipartFile>> entry : archivos.entrySet()) {
            String nombreCampo = entry.getKey();
            if (!nombreCampo.startsWith("archivo-")) {
                continue;
            }
            Integer idTipo = parsearIdTipo(nombreCampo.substring("archivo-".length()));
            TipoAdjuntoEmpleado tipo = tipoAdjuntoRepository.findById(idTipo)
                    .orElseThrow(() -> new IllegalArgumentException(
                            "El tipo de adjunto seleccionado no existe"));
            if (!tipo.isActivo() || !gruposPermitidos.contains(tipo.getGrupo().name())) {
                throw new IllegalArgumentException(
                        "El tipo de adjunto no corresponde a la etapa actual del registro");
            }
            List<MultipartFile> archivosTipo = entry.getValue().stream()
                    .filter(archivo -> archivo != null && !archivo.isEmpty())
                    .toList();
            if (!archivosTipo.isEmpty() && !idsTiposRequeridos.contains(idTipo)) {
                throw new IllegalArgumentException(
                        "Selecciona el concepto " + tipo.getDescripcion()
                                + " en Configuración antes de adjuntar archivos");
            }
            if (!tipo.isPermiteMultiplesVigentes() && archivosTipo.size() > 1) {
                throw new IllegalArgumentException(
                        "Solo se permite un archivo vigente para " + tipo.getDescripcion());
            }
            if (!tipo.isPermiteMultiplesVigentes() && !archivosTipo.isEmpty()) {
                List<EmpleadoAdjunto> anteriores = adjuntoRepository
                        .findAllByEmpleado_IdEmpleadoAndTipoAdjunto_IdTipoAdjuntoEmpleadoAndVigenteTrueOrderByFechaCargaDesc(
                                empleado.getIdEmpleado(), idTipo);
                anteriores.forEach(adjunto -> adjunto.setVigente(false));
                adjuntoRepository.saveAllAndFlush(anteriores);
            }
            for (MultipartFile archivo : archivosTipo) {
                guardarAdjunto(empleado, tipo, archivo);
            }
        }
    }

    private void guardarAdjunto(
            Empleado empleado, TipoAdjuntoEmpleado tipo, MultipartFile archivo) {
        if (archivo.getSize() > MAX_TAMANO_ADJUNTO) {
            throw new IllegalArgumentException(
                    "El archivo " + archivo.getOriginalFilename() + " supera el límite de 10 MB");
        }
        byte[] contenido;
        try {
            contenido = archivo.getBytes();
        } catch (IOException ex) {
            throw new IllegalStateException("No se pudo leer el archivo adjunto", ex);
        }
        if (contenido.length < 5
                || contenido[0] != '%' || contenido[1] != 'P'
                || contenido[2] != 'D' || contenido[3] != 'F' || contenido[4] != '-') {
            throw new IllegalArgumentException(
                    "El archivo " + archivo.getOriginalFilename() + " no es un PDF válido");
        }
        String nombre = nombreSeguro(archivo.getOriginalFilename());
        EmpleadoAdjunto adjunto = new EmpleadoAdjunto();
        adjunto.setEmpleado(empleado);
        adjunto.setTipoAdjunto(tipo);
        adjunto.setNombreOriginal(nombre);
        adjunto.setTipoMime("application/pdf");
        adjunto.setTamanoBytes((long) contenido.length);
        adjunto.setContenido(contenido);
        adjunto.setFechaCarga(OffsetDateTime.now());
        adjunto.setVigente(true);
        adjunto.setPublicoQr(false);
        adjuntoService.guardar(adjunto);
    }

    private void validarTamanoTotalAdjuntos(MultiValueMap<String, MultipartFile> archivos) {
        if (archivos == null) {
            return;
        }
        long total = archivos.values().stream()
                .flatMap(List::stream)
                .filter(archivo -> archivo != null && !archivo.isEmpty())
                .mapToLong(MultipartFile::getSize)
                .sum();
        if (total > MAX_TAMANO_TOTAL_ADJUNTOS) {
            throw new IllegalArgumentException(
                    "El total de archivos adjuntos no puede superar 50 MB");
        }
    }

    private void validarFormulario(EmpleadoRegistroForm form) {
        if (form == null) {
            throw new IllegalArgumentException("No se recibieron datos del empleado");
        }
        if (form.getTipoDocumento() == null
                || vacio(form.getNumeroDocumento())
                || vacio(form.getPrimerNombre())
                || vacio(form.getPrimerApellido())
                || form.getAreaId() == null
                || form.getCargoId() == null
                || form.getCentroCostoId() == null) {
            throw new IllegalArgumentException(
                    "Completa tipo y número de documento, nombres, apellidos, área, cargo y centro de costo");
        }
        ValidacionDatosEmpleado.validarSoloDigitos(
                form.getNumeroDocumento(), "El número de documento");
        ValidacionDatosEmpleado.validarSoloDigitos(form.getCelular(), "El celular");
        ValidacionDatosEmpleado.validarCorreo(form.getCorreoElectronico());
        ValidacionDatosEmpleado.validarFechasIdentificacion(
                form.getFechaExpedicion(), form.getFechaNacimiento());
    }

    private void validarDocumentoNoDuplicado(Empleado empleado, Integer idActual) {
        Empleado existente = empleadoRepository.findByTipoDocumentoAndNumeroDocumento(
                empleado.getTipoDocumento(), empleado.getNumeroDocumento()).orElse(null);
        if (existente != null && !existente.getIdEmpleado().equals(idActual)) {
            throw new IllegalArgumentException("Ya existe un empleado con ese documento");
        }
    }

    private void validarCorreoNoDuplicado(String correo, Integer idActual) {
        String normalizado = normalizar(correo);
        if (normalizado == null) {
            return;
        }
        boolean existe = idActual == null
                ? empleadoRepository.existsByCorreoElectronicoNormalizado(normalizado)
                : empleadoRepository.existsByCorreoElectronicoNormalizadoExcludingEmpleado(
                        normalizado, idActual);
        if (existe) {
            throw new IllegalArgumentException(
                    "Ya existe un empleado registrado con ese correo electrónico");
        }
    }

    private static Integer parsearIdTipo(String valor) {
        try {
            return Integer.valueOf(valor);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Identificador de adjunto inválido", ex);
        }
    }

    private static String nombreSeguro(String nombreOriginal) {
        if (nombreOriginal == null || nombreOriginal.isBlank()) {
            throw new IllegalArgumentException("El archivo debe tener un nombre válido");
        }
        String nombre = nombreOriginal.replace('\\', '/');
        nombre = nombre.substring(nombre.lastIndexOf('/') + 1).trim();
        if (nombre.isEmpty() || nombre.length() > 255) {
            throw new IllegalArgumentException("El nombre del archivo no es válido");
        }
        return nombre;
    }

    private static boolean vacio(String valor) {
        return valor == null || valor.isBlank();
    }

    private static BigDecimal valorAplicable(
            String concepto, boolean aplica, BigDecimal valor) {
        if (!aplica) {
            return null;
        }
        if (valor == null || valor.signum() < 0) {
            throw new IllegalArgumentException(
                    "Ingresa un valor válido para " + concepto);
        }
        return validarDecimal10_2(concepto, valor);
    }

    private static BigDecimal validarDecimal10_2(String concepto, BigDecimal valor) {
        if (valor == null) {
            return null;
        }
        if (valor.abs().compareTo(MAX_DECIMAL_10_2) > 0
                || valor.stripTrailingZeros().scale() > 2) {
            throw new IllegalArgumentException(
                    concepto + " debe tener máximo 8 enteros y 2 decimales");
        }
        return valor;
    }

    private static <T extends CatalogoRrhh> T buscarCatalogoOpcional(
            Integer id, JpaRepository<T, Integer> repository, String etiqueta) {
        if (id == null) {
            return null;
        }
        T catalogo = repository.findById(id).orElseThrow(() ->
                new IllegalArgumentException("El " + etiqueta + " seleccionado no existe"));
        if (!catalogo.isActivo()) {
            throw new IllegalArgumentException("El " + etiqueta + " seleccionado está inactivo");
        }
        return catalogo;
    }

    private static String normalizar(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }

    private static <T> T buscarCatalogo(java.util.Optional<T> resultado, String etiqueta) {
        return resultado.orElseThrow(() ->
                new IllegalArgumentException("El " + etiqueta + " seleccionado no existe"));
    }
}
