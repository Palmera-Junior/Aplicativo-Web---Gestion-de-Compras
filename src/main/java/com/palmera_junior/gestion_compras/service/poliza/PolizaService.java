package com.palmera_junior.gestion_compras.service.poliza;

import java.math.BigDecimal;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import jakarta.persistence.criteria.JoinType;

import com.palmera_junior.gestion_compras.dto.PolizaDTO;
import com.palmera_junior.gestion_compras.dto.DetallePrimaDTO;
import com.palmera_junior.gestion_compras.dto.RenovarPolizaDTO;
import com.palmera_junior.gestion_compras.entity.EstadoAprobacionSede;
import com.palmera_junior.gestion_compras.entity.EstadoPoliza;
import com.palmera_junior.gestion_compras.entity.DetallePrima;
import com.palmera_junior.gestion_compras.entity.Poliza;
import com.palmera_junior.gestion_compras.entity.PolizaAdjunto;
import com.palmera_junior.gestion_compras.entity.PolizaSedeAprobacion;
import com.palmera_junior.gestion_compras.entity.PolizaRenovacion;
import com.palmera_junior.gestion_compras.entity.Proveedor;
import com.palmera_junior.gestion_compras.entity.Rol;
import com.palmera_junior.gestion_compras.entity.Sede;
import com.palmera_junior.gestion_compras.entity.TipoAdjuntoPoliza;
import com.palmera_junior.gestion_compras.entity.Usuario;
import com.palmera_junior.gestion_compras.repository.PolizaRepository;
import com.palmera_junior.gestion_compras.repository.PolizaSedeAprobacionRepository;
import com.palmera_junior.gestion_compras.repository.PolizaRenovacionRepository;
import com.palmera_junior.gestion_compras.repository.ProveedorRepository;
import com.palmera_junior.gestion_compras.events.PolizaAprobadaEvent;
import com.palmera_junior.gestion_compras.events.NuevaPolizaCreadaEvent;
import com.palmera_junior.gestion_compras.events.PolizaProximaAVencerEvent;
import com.palmera_junior.gestion_compras.events.PolizaTerminadaEvent;
import com.palmera_junior.gestion_compras.service.correo.CorreoPolizaOutboxService;
import com.palmera_junior.gestion_compras.service.usuario.IUsuarioService;
import org.springframework.web.multipart.MultipartFile;

@Service
public class PolizaService implements IPolizaService {

    private static final DateTimeFormatter ISO_DATE = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final int MAX_ADJUNTOS_POLIZA = 5;

    private final PolizaRepository polizaRepository;
    private final ProveedorRepository proveedorRepository;
    private final IUsuarioService usuarioService;
    private final IPolizaArchivoStorage archivoStorage;
    private final ApplicationEventPublisher eventPublisher;
    private final CorreoPolizaOutboxService correoPolizaOutboxService;
    private final PolizaSedeAprobacionRepository polizaSedeAprobacionRepository;
    private final PolizaRenovacionRepository polizaRenovacionRepository;

    @Autowired
    public PolizaService(PolizaRepository polizaRepository, ProveedorRepository proveedorRepository,
            IUsuarioService usuarioService, IPolizaArchivoStorage archivoStorage,
            ApplicationEventPublisher eventPublisher, CorreoPolizaOutboxService correoPolizaOutboxService,
            PolizaSedeAprobacionRepository polizaSedeAprobacionRepository,
            PolizaRenovacionRepository polizaRenovacionRepository) {
        this.polizaRepository = polizaRepository;
        this.proveedorRepository = proveedorRepository;
        this.usuarioService = usuarioService;
        this.archivoStorage = archivoStorage;
        this.eventPublisher = eventPublisher;
        this.correoPolizaOutboxService = correoPolizaOutboxService;
        this.polizaSedeAprobacionRepository = polizaSedeAprobacionRepository;
        this.polizaRenovacionRepository = polizaRenovacionRepository;
        }

        public PolizaService(PolizaRepository polizaRepository, ProveedorRepository proveedorRepository,
            IUsuarioService usuarioService, IPolizaArchivoStorage archivoStorage,
            ApplicationEventPublisher eventPublisher, CorreoPolizaOutboxService correoPolizaOutboxService,
            PolizaSedeAprobacionRepository polizaSedeAprobacionRepository) {
        this(polizaRepository, proveedorRepository, usuarioService, archivoStorage, eventPublisher,
            correoPolizaOutboxService, polizaSedeAprobacionRepository, null);
    }

    public PolizaService(PolizaRepository polizaRepository, ProveedorRepository proveedorRepository,
            IUsuarioService usuarioService, IPolizaArchivoStorage archivoStorage,
            ApplicationEventPublisher eventPublisher, CorreoPolizaOutboxService correoPolizaOutboxService) {
        this(polizaRepository, proveedorRepository, usuarioService, archivoStorage,
            eventPublisher, correoPolizaOutboxService, null, null);
    }

    PolizaService(PolizaRepository polizaRepository, ProveedorRepository proveedorRepository,
            IUsuarioService usuarioService, IPolizaArchivoStorage archivoStorage) {
        this(polizaRepository, proveedorRepository, usuarioService, archivoStorage, null, null, null, null);
    }

    @Override
    @Transactional
    @PreAuthorize("hasAnyRole('COMERCIAL', 'APROBADOR', 'ADMINISTRADOR', 'SUPERADMINISTRADOR')")
    public Page<Poliza> polizasPaginadas(Pageable pageable, String search, String fechaDesde,
            String fechaHasta, Integer idSede, boolean esNacional, String estado) {
        marcarVencidas();
        Specification<Poliza> specification = Specification.where((root, query, criteriaBuilder) ->
                criteriaBuilder.conjunction());

        if (search != null && !search.isBlank()) {
            String termino = "%" + search.trim().toLowerCase() + "%";
            specification = specification.and((root, query, criteriaBuilder) -> {
                var proveedor = root.join("proveedor");
                return criteriaBuilder.or(
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("numeroContrato")), termino),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("cliente")), termino),
                        criteriaBuilder.like(criteriaBuilder.lower(proveedor.get("nombre")), termino));
            });
        }

        LocalDate inicio = parseDate(fechaDesde);
        LocalDate fin = parseDate(fechaHasta);
        if (inicio != null && fin != null && inicio.isAfter(fin)) {
            throw new IllegalArgumentException("El rango de fechas de inicio no es válido");
        }
        if (inicio != null) {
            specification = specification.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.greaterThanOrEqualTo(root.get("fechaCreacion"), inicio));
        }
        if (fin != null) {
            specification = specification.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.lessThanOrEqualTo(root.get("fechaCreacion"), fin));
        }

        if (!esNacional && idSede != null) {
            specification = specification.and((root, query, criteriaBuilder) -> {
                query.distinct(true);
                var aprobaciones = root.join("aprobacionesPorSede", JoinType.LEFT);
                return criteriaBuilder.or(
                        criteriaBuilder.equal(root.get("sede").get("idSede"), idSede),
                        criteriaBuilder.equal(aprobaciones.get("sede").get("idSede"), idSede));
            });
        }

        if (estado != null && !estado.isBlank()) {
            try {
                EstadoPoliza estadoPoliza = EstadoPoliza.valueOf(estado.trim().toUpperCase());
                specification = specification.and((root, query, criteriaBuilder) ->
                        criteriaBuilder.equal(root.get("estado"), estadoPoliza));
            } catch (IllegalArgumentException exception) {
                throw new IllegalArgumentException("El estado de póliza no es válido");
            }
        }

        Pageable pageableOrdenado = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(),
                Sort.by(Sort.Direction.DESC, "idPoliza"));
        Page<Poliza> pagina = polizaRepository.findAll(specification, pageableOrdenado);
        // La plantilla consulta el nombre del contrato desde la nueva relación. Se
        // inicializa aquí, dentro de la transacción, para que la vista no dependa de
        // Open Session in View y no falle al renderizar las acciones de cada fila.
        pagina.getContent().forEach(poliza -> poliza.getAdjuntos().size());
        return pagina;
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('COMERCIAL', 'APROBADOR', 'ADMINISTRADOR', 'SUPERADMINISTRADOR')")
    public List<Poliza> listarPolizas() {
        Usuario usuario = usuarioService.obtenerUsuarioAutenticado();
        if (esNacional(usuario)) {
            return polizaRepository.findAll(Sort.by(Sort.Direction.DESC, "idPoliza"));
        }
        Integer idSedeUsuario = usuario.getSede().getIdSede();
        return polizaRepository.findAll((root, query, criteriaBuilder) -> {
                query.distinct(true);
                var aprobaciones = root.join("aprobacionesPorSede", JoinType.LEFT);
                return criteriaBuilder.or(
                    criteriaBuilder.equal(root.get("sede").get("idSede"), idSedeUsuario),
                    criteriaBuilder.equal(aprobaciones.get("sede").get("idSede"), idSedeUsuario));
            },
                Sort.by(Sort.Direction.DESC, "idPoliza"));
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('COMERCIAL', 'APROBADOR', 'ADMINISTRADOR', 'SUPERADMINISTRADOR')")
    public Poliza obtenerPorId(Integer idPoliza) {
        Poliza poliza = polizaRepository.findWithRelationsByIdPoliza(idPoliza)
                .orElseThrow(() -> new IllegalArgumentException("Póliza no encontrada"));
        poliza.getAdjuntos().size();
        assertVisible(poliza, usuarioService.obtenerUsuarioAutenticado());
        return poliza;
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('COMERCIAL', 'APROBADOR', 'ADMINISTRADOR', 'SUPERADMINISTRADOR')")
    public List<PolizaSedeAprobacion> listarAprobacionesPorSede(Integer idPoliza) {
        Poliza poliza = polizaRepository.findWithAprobacionesPorSedeByIdPoliza(idPoliza)
                .orElseThrow(() -> new IllegalArgumentException("Póliza no encontrada"));
        assertVisible(poliza, usuarioService.obtenerUsuarioAutenticado());
        return polizaSedeAprobacionRepository.findByPolizaIdPoliza(idPoliza);
    }

    @Override
    @Transactional
    @PreAuthorize("hasAnyRole('COMERCIAL', 'APROBADOR', 'ADMINISTRADOR', 'SUPERADMINISTRADOR')")
    public Poliza guardarDesdeDTO(PolizaDTO dto) {
        if (dto == null || dto.getContratoAdjunto() == null || dto.getContratoAdjunto().isEmpty()) {
            throw new IllegalArgumentException("Debe adjuntar el contrato de la póliza en formato PDF");
        }
        Usuario usuario = usuarioService.obtenerUsuarioAutenticado();
        Poliza poliza = new Poliza();
        poliza.setUsuario(usuario);
        poliza.setSede(usuario.getSede());
        byte[] contenidoAdjunto = archivoStorage.almacenar(dto.getContratoAdjunto());
        try {
            aplicarDatos(poliza, dto);
            poliza.addAdjunto(crearAdjunto(contenidoAdjunto, dto.getContratoAdjunto().getOriginalFilename(),
                    TipoAdjuntoPoliza.CONTRATO));
            poliza.setEstado(EstadoPoliza.BORRADOR);
            actualizarAprobacionesPorSede(poliza, dto.getIdsSedes(), usuario);
            Poliza polizaGuardada = polizaRepository.save(poliza);
            publicarNotificacionNuevaPoliza(polizaGuardada, usuario);
            return polizaGuardada;
        } catch (RuntimeException exception) {
            throw exception;
        }
    }

    @Override
    @Transactional
    @PreAuthorize("hasAnyRole('COMERCIAL', 'APROBADOR', 'ADMINISTRADOR', 'SUPERADMINISTRADOR')")
    public Poliza actualizarDesdeDTO(Integer idPoliza, PolizaDTO dto) {
        if (dto == null) {
            throw new IllegalArgumentException("Los datos de la póliza son obligatorios");
        }
        Usuario usuario = usuarioService.obtenerUsuarioAutenticado();
        Poliza poliza = polizaRepository.findById(idPoliza)
                .orElseThrow(() -> new IllegalArgumentException("Póliza no encontrada"));
        assertPuedeModificar(poliza, usuario);
        if (poliza.getEstado() != EstadoPoliza.BORRADOR) {
            throw new IllegalStateException("Solo se pueden editar pólizas en estado BORRADOR");
        }
        PolizaAdjunto adjuntoAnterior = poliza.obtenerAdjuntoContrato().orElse(null);
        boolean eliminarAdjunto = dto != null && dto.isEliminarContratoAdjunto();
        boolean hayAdjuntoNuevo = dto != null && dto.getContratoAdjunto() != null
                && !dto.getContratoAdjunto().isEmpty();
        if (!hayAdjuntoNuevo && (eliminarAdjunto || adjuntoAnterior == null)) {
            throw new IllegalArgumentException("Debe adjuntar el contrato de la póliza en formato PDF");
        }
        if (hayAdjuntoNuevo && adjuntoAnterior != null && !eliminarAdjunto) {
            throw new IllegalArgumentException("Debe eliminar el archivo actual antes de adjuntar otro");
        }
        boolean habiaSedeAprobada = poliza.getAprobacionesPorSede().stream()
                .anyMatch(aprobacion -> aprobacion.getEstado() == EstadoAprobacionSede.APROBADA);
        boolean eraMultiSede = poliza.getAprobacionesPorSede().size() > 1;
        byte[] contenidoAdjunto = hayAdjuntoNuevo ? archivoStorage.almacenar(dto.getContratoAdjunto()) : null;
        try {
            aplicarDatos(poliza, dto);
            actualizarAprobacionesPorSede(poliza, dto.getIdsSedes(), usuario);
            if (habiaSedeAprobada && (eraMultiSede || poliza.getAprobacionesPorSede().size() > 1)) {
                poliza.getAprobacionesPorSede().forEach(PolizaSedeAprobacion::reiniciar);
            }
            if (eliminarAdjunto && adjuntoAnterior != null) {
                poliza.removeAdjunto(adjuntoAnterior);
            }
            if (contenidoAdjunto != null) {
                poliza.addAdjunto(crearAdjunto(contenidoAdjunto, dto.getContratoAdjunto().getOriginalFilename(),
                        TipoAdjuntoPoliza.CONTRATO));
            }
            return polizaRepository.save(poliza);
        } catch (RuntimeException exception) {
            throw exception;
        }
    }

    @Override
    @Transactional
    @PreAuthorize("hasAnyRole('APROBADOR', 'ADMINISTRADOR', 'SUPERADMINISTRADOR')")
    public Poliza aprobar(Integer idPoliza) {
        Usuario aprobador = usuarioService.obtenerUsuarioAutenticado();
        Poliza poliza = polizaRepository.findById(idPoliza)
                .orElseThrow(() -> new IllegalArgumentException("Póliza no encontrada"));
        assertPuedeAprobar(poliza, aprobador);
        Integer idSedeAprobador = aprobador.getSede() != null ? aprobador.getSede().getIdSede() : null;
        return aprobarPorSede(idPoliza, idSedeAprobador);
    }

    @Override
    @Transactional
    @PreAuthorize("hasAnyRole('APROBADOR', 'ADMINISTRADOR', 'SUPERADMINISTRADOR')")
    public Poliza aprobarPorSede(Integer idPoliza, Integer idSede) {
        Usuario aprobador = usuarioService.obtenerUsuarioAutenticado();
        Poliza poliza = polizaRepository.findById(idPoliza)
                .orElseThrow(() -> new IllegalArgumentException("Póliza no encontrada"));

        Integer idSedeAprobacion = idSede;
        if (idSedeAprobacion == null) {
            idSedeAprobacion = aprobador != null && aprobador.getSede() != null ? aprobador.getSede().getIdSede() : null;
        }

        if (idSedeAprobacion == null) {
            throw new IllegalArgumentException("La sede de aprobación es obligatoria");
        }

        final Integer idSedeFinal = idSedeAprobacion;

        PolizaSedeAprobacion aprobacion = polizaSedeAprobacionRepository != null
                ? polizaSedeAprobacionRepository.findByPolizaIdPolizaAndSedeIdSede(idPoliza, idSedeFinal)
                        .orElseGet(() -> crearAprobacionPorSede(poliza, idSedeFinal))
                : null;

        if (aprobacion == null) {
            throw new IllegalArgumentException("No existe una aprobación para la sede indicada");
        }

        if (aprobacion.getEstado() == EstadoAprobacionSede.APROBADA) {
            return poliza;
        }

        assertPuedeAprobar(poliza, aprobador, idSedeFinal);
        aprobacion.aprobar(aprobador, LocalDate.now());
        if (polizaSedeAprobacionRepository != null) {
            polizaSedeAprobacionRepository.save(aprobacion);
        }

        if (poliza.obtenerAprobacionPorSede(idSedeFinal).map(a -> a.getEstado() == EstadoAprobacionSede.APROBADA).orElse(false)
                && poliza.getAprobacionesPorSede() != null && poliza.getAprobacionesPorSede().stream().allMatch(a -> a.getEstado() == EstadoAprobacionSede.APROBADA)) {
            poliza.aprobar(aprobador, LocalDate.now());
            Poliza aprobada = polizaRepository.save(poliza);
            if (correoPolizaOutboxService != null && eventPublisher != null) {
                Long idAuditoria = correoPolizaOutboxService.registrarPendiente(aprobada);
                eventPublisher.publishEvent(new PolizaAprobadaEvent(idAuditoria));
            }
            return aprobada;
        }

        return polizaRepository.save(poliza);
    }

    @Override
    @Transactional
    @PreAuthorize("hasAnyRole('COMERCIAL', 'APROBADOR', 'ADMINISTRADOR', 'SUPERADMINISTRADOR')")
    public Poliza activarVigente(Integer idPoliza, String numeroPoliza, List<MultipartFile> polizasFisicas) {
        Usuario usuario = usuarioService.obtenerUsuarioAutenticado();
        Poliza poliza = polizaRepository.findById(idPoliza)
                .orElseThrow(() -> new IllegalArgumentException("Póliza no encontrada"));
        assertPuedeModificar(poliza, usuario);
        if (numeroPoliza == null || numeroPoliza.isBlank()) {
            throw new IllegalArgumentException("El número de póliza es obligatorio");
        }
        if (polizasFisicas == null || polizasFisicas.isEmpty()) {
            throw new IllegalArgumentException("Debe adjuntar al menos un PDF de la póliza física");
        }
        if (polizasFisicas.size() > MAX_ADJUNTOS_POLIZA) {
            throw new IllegalArgumentException("Puede adjuntar máximo 5 archivos PDF por póliza");
        }
        String numeroNormalizado = numeroPoliza.trim();
        validarNumeroPolizaUnico(numeroNormalizado, poliza.getIdPoliza());

        List<PolizaAdjunto> adjuntosNuevos = new ArrayList<>();
        for (MultipartFile archivo : polizasFisicas) {
            if (archivo == null || archivo.isEmpty()) {
                throw new IllegalArgumentException("Cada archivo seleccionado debe contener un PDF");
            }
            byte[] contenido = archivoStorage.almacenar(archivo);

            adjuntosNuevos.add(crearAdjunto(contenido, archivo.getOriginalFilename(),
                TipoAdjuntoPoliza.POLIZA_FISICA));
        }
        poliza.activar(numeroNormalizado, adjuntosNuevos);
        poliza.setUsuarioActivacion(usuario);
        return polizaRepository.save(poliza);
    }

    @Override
    @Transactional
    @PreAuthorize("hasAnyRole('COMERCIAL', 'APROBADOR', 'ADMINISTRADOR', 'SUPERADMINISTRADOR')")
    public Poliza renovar(Integer idPoliza, RenovarPolizaDTO dto) {
        Usuario usuario = usuarioService.obtenerUsuarioAutenticado();
        Poliza poliza = polizaRepository.findWithAprobacionesPorSedeByIdPoliza(idPoliza)
                .orElseThrow(() -> new IllegalArgumentException("Póliza no encontrada"));
        assertPuedeModificar(poliza, usuario);
        if (poliza.getEstado() != EstadoPoliza.VIGENTE || !poliza.estaProximaAVencer()) {
            throw new IllegalStateException("Solo se pueden renovar pólizas vigentes próximas a vencer en menos de 30 días");
        }
        if (dto == null || dto.getFechaInicio() == null || dto.getFechaFin() == null) {
            throw new IllegalArgumentException("Debe indicar las fechas de inicio y fin de la renovación");
        }
        if (poliza.getFechaVencimiento() == null || !dto.getFechaInicio().isAfter(poliza.getFechaVencimiento())) {
            throw new IllegalArgumentException("La fecha de inicio de la renovación debe ser posterior al vencimiento vigente");
        }
        if (!dto.getFechaFin().isAfter(dto.getFechaInicio())) {
            throw new IllegalArgumentException("La fecha fin debe ser posterior a la fecha de inicio");
        }
        List<MultipartFile> archivos = dto.getArchivos() == null ? List.of()
                : dto.getArchivos().stream().filter(archivo -> archivo != null && !archivo.isEmpty()).toList();
        if (archivos.isEmpty()) {
            throw new IllegalArgumentException("Debe adjuntar al menos un PDF de la renovación");
        }
        if (archivos.size() > MAX_ADJUNTOS_POLIZA) {
            throw new IllegalArgumentException("Puede adjuntar máximo 5 archivos PDF por renovación");
        }

        PolizaRenovacion renovacion = new PolizaRenovacion();
        renovacion.setPoliza(poliza);
        renovacion.setFechaInicioAnterior(poliza.getFechaCreacion());
        renovacion.setFechaFinAnterior(poliza.getFechaVencimiento());
        renovacion.setFechaInicio(dto.getFechaInicio());
        renovacion.setFechaFin(dto.getFechaFin());
        renovacion.setFechaRegistro(LocalDateTime.now());
        renovacion.setUsuario(usuario);
        for (MultipartFile archivo : archivos) {
            byte[] contenido = archivoStorage.almacenar(archivo);
            PolizaAdjunto adjunto = new PolizaAdjunto();
            adjunto.setContenido(contenido);
            adjunto.setNombreOriginal(archivo.getOriginalFilename());
            adjunto.setFechaCarga(LocalDateTime.now());
            renovacion.addAdjunto(adjunto);
        }
        poliza.getRenovaciones().add(renovacion);
        poliza.setFechaCreacion(dto.getFechaInicio());
        poliza.setFechaVencimiento(dto.getFechaFin());
        return polizaRepository.save(poliza);
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('COMERCIAL', 'APROBADOR', 'ADMINISTRADOR', 'SUPERADMINISTRADOR')")
    public List<PolizaRenovacion> listarRenovaciones(Integer idPoliza) {
        Poliza poliza = polizaRepository.findWithAprobacionesPorSedeByIdPoliza(idPoliza)
                .orElseThrow(() -> new IllegalArgumentException("Póliza no encontrada"));
        assertVisible(poliza, usuarioService.obtenerUsuarioAutenticado());
        return polizaRenovacionRepository.findByPolizaIdPolizaOrderByFechaInicioAsc(idPoliza);
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('COMERCIAL', 'APROBADOR', 'ADMINISTRADOR', 'SUPERADMINISTRADOR')")
    public PolizaAdjunto obtenerAdjuntoRenovacion(Integer idPoliza, Integer idRenovacion,
            Integer idAdjunto) {
        PolizaRenovacion renovacion = polizaRenovacionRepository
                .findByIdRenovacionAndPolizaIdPoliza(idRenovacion, idPoliza)
                .orElseThrow(() -> new IllegalArgumentException("Renovación no encontrada"));
        Poliza poliza = polizaRepository.findWithAprobacionesPorSedeByIdPoliza(idPoliza)
            .orElseThrow(() -> new IllegalArgumentException("Póliza no encontrada"));
        assertVisible(poliza, usuarioService.obtenerUsuarioAutenticado());
        return renovacion.getAdjuntos().stream()
            .filter(adjunto -> adjunto.getTipoAdjunto() == TipoAdjuntoPoliza.RENOVACION)
            .filter(adjunto -> idAdjunto.equals(adjunto.getIdAdjuntoPoliza()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Adjunto de renovación no encontrado"));
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] leerAdjuntoRenovacion(PolizaAdjunto adjunto) throws IOException {
        if (adjunto == null) {
            throw new IllegalArgumentException("Adjunto de renovación no encontrado");
        }
        return archivoStorage.leer(adjunto.getContenido());
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('COMERCIAL', 'APROBADOR', 'ADMINISTRADOR', 'SUPERADMINISTRADOR')")
    public PolizaAdjunto obtenerContratoAdjunto(Integer idPoliza) {
        Poliza poliza = polizaRepository.findWithRelationsByIdPoliza(idPoliza)
                .orElseThrow(() -> new IllegalArgumentException("Póliza no encontrada"));
        assertVisible(poliza, usuarioService.obtenerUsuarioAutenticado());
        return poliza.obtenerAdjuntoContrato()
                .orElseThrow(() -> new IllegalArgumentException("La póliza no tiene contrato adjunto"));
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] leerContratoAdjunto(PolizaAdjunto adjunto) throws IOException {
        if (adjunto == null) {
            throw new IllegalArgumentException("Contrato adjunto no encontrado");
        }
        return archivoStorage.leer(adjunto.getContenido());
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('COMERCIAL', 'APROBADOR', 'ADMINISTRADOR', 'SUPERADMINISTRADOR')")
    public PolizaAdjunto obtenerAdjuntoFisico(Integer idPoliza, Integer idAdjunto) {
        Poliza poliza = polizaRepository.findWithRelationsByIdPoliza(idPoliza)
                .orElseThrow(() -> new IllegalArgumentException("Póliza no encontrada"));
        assertVisible(poliza, usuarioService.obtenerUsuarioAutenticado());
        return poliza.getAdjuntos().stream()
                .filter(adjunto -> adjunto.getTipoAdjunto() == TipoAdjuntoPoliza.POLIZA_FISICA)
                .filter(adjunto -> idAdjunto.equals(adjunto.getIdAdjuntoPoliza()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Documento de póliza no encontrado"));
    }

    @Override
    @Transactional
    public int marcarVencidas() {
        LocalDate hoy = LocalDate.now();
        int vencidas = polizaRepository.marcarVigentesVencidas(hoy);
        polizaRepository.findPendientesNotificacionProximoVencimiento(hoy, hoy.plusDays(30))
                .forEach(poliza -> {
                    poliza.setNotificacionProximoVencimientoEn(java.time.LocalDateTime.now());
                    if (eventPublisher != null) {
                        eventPublisher.publishEvent(new PolizaProximaAVencerEvent(poliza.getIdPoliza()));
                    }
                });
        return vencidas;
    }

    @Override
    @Transactional
    @PreAuthorize("hasAnyRole('APROBADOR', 'SUPERADMINISTRADOR')")
    public Poliza anular(Integer idPoliza) {
        Usuario usuario = usuarioService.obtenerUsuarioAutenticado();
        Poliza poliza = polizaRepository.findById(idPoliza)
                .orElseThrow(() -> new IllegalArgumentException("Póliza no encontrada"));
        assertPuedeAnular(poliza, usuario);
        poliza.anular();
        poliza.setUsuarioAnulacion(usuario);
        poliza.setFechaAnulacion(LocalDate.now());
        return polizaRepository.save(poliza);
    }

    @Override
    @Transactional
    @PreAuthorize("hasAnyRole('APROBADOR', 'SUPERADMINISTRADOR', 'COMERCIAL', 'ADMINISTRADOR')")
    public Poliza terminar(Integer idPoliza, String motivoTerminacion) {
        Usuario usuario = usuarioService.obtenerUsuarioAutenticado();
        Poliza poliza = polizaRepository.findById(idPoliza)
                .orElseThrow(() -> new IllegalArgumentException("Póliza no encontrada"));
        assertPuedeModificar(poliza, usuario);
        poliza.terminar(usuario, LocalDate.now(), motivoTerminacion);
        Poliza polizaTerminada = polizaRepository.save(poliza);
        publicarNotificacionPolizaTerminada(polizaTerminada);
        return polizaTerminada;
    }

    private void actualizarAprobacionesPorSede(Poliza poliza, List<Integer> idsSedes, Usuario creador) {
        if (poliza == null) {
            return;
        }
        List<Integer> sedes = resolverSedesInvolucradas(idsSedes, creador);
        Set<Integer> idsSeleccionados = new HashSet<>(sedes);

        // Se conservan las aprobaciones existentes. Borrarlas y crearlas de nuevo
        // en la misma transacción puede intentar insertar antes de ejecutar el
        // orphan removal y violar la restricción única (id_poliza, id_sede).
        poliza.getAprobacionesPorSede().removeIf(aprobacion -> aprobacion.getSede() == null
                || !idsSeleccionados.contains(aprobacion.getSede().getIdSede()));

        Set<Integer> idsExistentes = new HashSet<>();
        poliza.getAprobacionesPorSede().forEach(aprobacion -> {
            if (aprobacion.getSede() != null && aprobacion.getSede().getIdSede() != null) {
                idsExistentes.add(aprobacion.getSede().getIdSede());
            }
        });
        for (Integer idSede : sedes) {
            if (idsExistentes.contains(idSede)) {
                continue;
            }
            Sede sede = new Sede();
            sede.setIdSede(idSede);
            PolizaSedeAprobacion aprobacion = new PolizaSedeAprobacion();
            aprobacion.setSede(sede);
            aprobacion.setEstado(EstadoAprobacionSede.PENDIENTE);
            poliza.addAprobacionPorSede(aprobacion);
        }
    }

    private List<Integer> resolverSedesInvolucradas(List<Integer> idsSedes, Usuario usuario) {
        if (usuario == null || usuario.getSede() == null || usuario.getSede().getIdSede() == null) {
            throw new IllegalArgumentException("El usuario debe tener una sede asignada para gestionar pólizas");
        }

        Integer idSedeUsuario = usuario.getSede().getIdSede();
        List<Integer> sedes = idsSedes == null ? List.of() : idsSedes.stream()
                .filter(id -> id != null)
                .distinct()
                .toList();
        if (sedes.isEmpty()) {
            return List.of(idSedeUsuario);
        }
        if (!esSedeNacional(usuario) && (sedes.size() != 1 || !idSedeUsuario.equals(sedes.get(0)))) {
            throw new IllegalArgumentException(
                    "Solo Sede Nacional puede crear o editar pólizas con varias sedes involucradas");
        }
        return sedes;
    }

    private PolizaSedeAprobacion crearAprobacionPorSede(Poliza poliza, Integer idSede) {
        Sede sede = new Sede();
        sede.setIdSede(idSede);
        PolizaSedeAprobacion aprobacion = new PolizaSedeAprobacion();
        aprobacion.setPoliza(poliza);
        aprobacion.setSede(sede);
        aprobacion.setEstado(EstadoAprobacionSede.PENDIENTE);
        poliza.addAprobacionPorSede(aprobacion);
        return aprobacion;
    }

    private void publicarNotificacionNuevaPoliza(Poliza poliza, Usuario creador) {
        if (eventPublisher == null || poliza == null || poliza.getAprobacionesPorSede() == null) {
            return;
        }
        List<Integer> idsSedes = poliza.getAprobacionesPorSede().stream()
                .map(PolizaSedeAprobacion::getSede)
                .filter(java.util.Objects::nonNull)
                .map(Sede::getIdSede)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .toList();
        eventPublisher.publishEvent(new NuevaPolizaCreadaEvent(poliza.getIdPoliza(), idsSedes));
    }

    private void publicarNotificacionPolizaTerminada(Poliza poliza) {
        if (eventPublisher == null || poliza == null || poliza.getAprobacionesPorSede() == null) {
            return;
        }
        List<Integer> idsSedes = poliza.getAprobacionesPorSede().stream()
                .map(PolizaSedeAprobacion::getSede)
                .filter(java.util.Objects::nonNull)
                .map(Sede::getIdSede)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .toList();
        eventPublisher.publishEvent(new PolizaTerminadaEvent(poliza.getIdPoliza(), idsSedes));
    }

    private void aplicarDatos(Poliza poliza, PolizaDTO dto) {
        if (dto == null) {
            throw new IllegalArgumentException("Los datos de la póliza son obligatorios");
        }
        if (isBlank(dto.getCliente()) || isBlank(dto.getNumeroContrato()) || isBlank(dto.getDescripcion())) {
            throw new IllegalArgumentException("El cliente, el número de contrato y la descripción son obligatorios");
        }
        validarNumeroContratoUnico(dto.getNumeroContrato(), poliza.getIdPoliza());
        validarNoNegativo(dto.getValorContrato(), "El valor del contrato no puede ser negativo");
        BigDecimal totalPrima = aplicarDetallesPrima(poliza, dto);

        LocalDate fecha = parseDate(dto.getFechaCreacion());
        if (fecha == null) {
            throw new IllegalArgumentException("La fecha de creación es obligatoria y debe ser válida");
        }
        LocalDate fechaVencimiento = parseDate(dto.getFechaVencimiento());
        if (fechaVencimiento == null) {
            throw new IllegalArgumentException("La fecha de vencimiento es obligatoria y debe ser válida");
        }
        if (fechaVencimiento.isBefore(fecha)) {
            throw new IllegalArgumentException("La fecha de vencimiento no puede ser anterior a la fecha de creación");
        }

        Proveedor proveedor = resolverProveedor(dto);

        poliza.setFechaCreacion(fecha);
        poliza.setFechaVencimiento(fechaVencimiento);
        poliza.setProveedor(proveedor);
        poliza.setCliente(dto.getCliente().trim());
        poliza.setDescripcion(trimToNull(dto.getDescripcion()));
        poliza.setNumeroContrato(dto.getNumeroContrato().trim());
        poliza.setValorPrima(totalPrima);
        poliza.setValorContrato(dto.getValorContrato());
    }

    private PolizaAdjunto crearAdjunto(byte[] contenido, String nombreOriginal, TipoAdjuntoPoliza tipo) {
        PolizaAdjunto adjunto = new PolizaAdjunto();
        adjunto.setContenido(contenido);
        adjunto.setNombreOriginal(nombreOriginal);
        adjunto.setFechaCarga(LocalDateTime.now());
        adjunto.setTipoAdjunto(tipo);
        return adjunto;
    }

    /**
     * En pólizas los proveedores son globales: nunca se crea ni modifica una relación proveedor_sede.
     */
    private Proveedor resolverProveedor(PolizaDTO dto) {
        if (dto.getIdProveedor() != null) {
            return proveedorRepository.findById(dto.getIdProveedor().intValue())
                    .orElseThrow(() -> new IllegalArgumentException("El proveedor no existe"));
        }
        if (dto.getNitProveedor() == null || isBlank(dto.getNombreProveedor()) || isBlank(dto.getCiudadProveedor())
                || isBlank(dto.getDireccionProveedor()) || isBlank(dto.getTelefonoProveedor())
                || isBlank(dto.getCorreoProveedor())) {
            throw new IllegalArgumentException(
                    "Seleccione un proveedor o registre NIT, nombre, ciudad, dirección, teléfono y correo.");
        }

        return proveedorRepository.findByNit(dto.getNitProveedor()).orElseGet(() -> {
            Proveedor proveedor = new Proveedor();
            proveedor.setNit(dto.getNitProveedor());
            proveedor.setNombre(dto.getNombreProveedor().trim());
            proveedor.setCiudad(dto.getCiudadProveedor().trim());
            proveedor.setDireccion(dto.getDireccionProveedor().trim());
            proveedor.setTelefono(dto.getTelefonoProveedor().trim());
            proveedor.setCorreo(dto.getCorreoProveedor().trim());
            return proveedorRepository.saveAndFlush(proveedor);
        });
    }

    private BigDecimal aplicarDetallesPrima(Poliza poliza, PolizaDTO dto) {
        if (dto.getDetallesPrima() == null || dto.getDetallesPrima().isEmpty()) {
            throw new IllegalArgumentException("Debe registrar al menos un detalle de prima");
        }

        BigDecimal total = BigDecimal.ZERO;
        poliza.getDetallesPrima().clear();
        for (DetallePrimaDTO detalleDTO : dto.getDetallesPrima()) {
            if (detalleDTO == null) {
                throw new IllegalArgumentException("El detalle de prima no es válido");
            }
            validarNoNegativo(detalleDTO.getValor(), "La prima no puede ser negativa");
            String descripcion = trimToNull(detalleDTO.getDescripcion());
            if (descripcion == null) {
                throw new IllegalArgumentException("La descripción de cada prima es obligatoria");
            }

            DetallePrima detalle = new DetallePrima();
            detalle.setValor(detalleDTO.getValor());
            detalle.setDescripcion(descripcion);
            poliza.addDetallePrima(detalle);
            total = total.add(detalleDTO.getValor());
        }
        return total;
    }

    private void assertVisible(Poliza poliza, Usuario usuario) {
        if (usuario == null) {
            throw new SecurityException("Debe estar autenticado para gestionar pólizas");
        }
        if (usuario.getRol() == Rol.SUPERADMINISTRADOR || usuario.getRol() == Rol.ADMINISTRADOR) {
            return;
        }
        if (usuario.getSede() == null) {
            throw new SecurityException("Solo puede gestionar pólizas de su sede");
        }
        Integer idSedeUsuario = usuario.getSede().getIdSede();
        boolean esSedePrincipal = poliza.getSede() != null
            && idSedeUsuario.equals(poliza.getSede().getIdSede());
        boolean esSedeInvolucrada = poliza.getAprobacionesPorSede() != null
            && poliza.getAprobacionesPorSede().stream()
                .anyMatch(aprobacion -> aprobacion.getSede() != null
                    && idSedeUsuario.equals(aprobacion.getSede().getIdSede()));
        if (!esSedePrincipal && !esSedeInvolucrada) {
            throw new SecurityException("Solo puede gestionar pólizas de su sede");
        }
    }

    private void assertPuedeModificar(Poliza poliza, Usuario usuario) {
        if (usuario != null && usuario.getRol() == Rol.SUPERADMINISTRADOR) {
            return;
        }
        boolean esCreador = usuario != null && usuario.getIdUsuario() != null
                && poliza.getUsuario() != null && usuario.getIdUsuario().equals(poliza.getUsuario().getIdUsuario());
        if (!esCreador && !esAprobadorDeSedeInvolucrada(poliza, usuario)) {
            throw new SecurityException(
                "Solo el creador o el aprobador de una sede involucrada pueden modificar o terminar la póliza.");
        }
    }

    private void assertPuedeAnular(Poliza poliza, Usuario usuario) {
        if (usuario == null || (usuario.getRol() != Rol.SUPERADMINISTRADOR
                && !esAprobadorDeSedeInvolucrada(poliza, usuario))) {
            throw new SecurityException(
                "Solo un superadministrador o el aprobador de una sede involucrada puede anular la póliza.");
        }
    }

    private boolean esAprobadorDeSedeInvolucrada(Poliza poliza, Usuario usuario) {
        if (poliza == null || usuario == null || usuario.getRol() != Rol.APROBADOR
                || usuario.getSede() == null || usuario.getSede().getIdSede() == null) {
            return false;
        }
        Integer idSedeUsuario = usuario.getSede().getIdSede();
        if (poliza.getAprobacionesPorSede() != null && !poliza.getAprobacionesPorSede().isEmpty()) {
            return poliza.getAprobacionesPorSede().stream()
                    .anyMatch(aprobacion -> aprobacion.getSede() != null
                            && idSedeUsuario.equals(aprobacion.getSede().getIdSede()));
        }
        return poliza.getSede() != null && idSedeUsuario.equals(poliza.getSede().getIdSede());
    }

    private void assertPuedeAprobar(Poliza poliza, Usuario usuario) {
        assertPuedeAprobar(poliza, usuario, usuario != null && usuario.getSede() != null ? usuario.getSede().getIdSede() : null);
    }

    private void assertPuedeAprobar(Poliza poliza, Usuario usuario, Integer idSedeAprobador) {
        if (usuario != null && usuario.getRol() == Rol.SUPERADMINISTRADOR) {
            return;
        }
        if (usuario == null || usuario.getSede() == null || idSedeAprobador == null) {
            throw new SecurityException("No tiene permisos para aprobar polizas de otras sedes");
        }
        if (poliza == null || poliza.getAprobacionesPorSede() == null || poliza.getAprobacionesPorSede().isEmpty()) {
            if (poliza != null && poliza.getSede() != null && !poliza.getSede().getIdSede().equals(idSedeAprobador)) {
                throw new SecurityException("No tiene permisos para aprobar polizas de otras sedes");
            }
            return;
        }
        boolean perteneceASedeInvolucrada = poliza.getAprobacionesPorSede().stream()
                .anyMatch(aprobacion -> aprobacion.getSede() != null
                        && idSedeAprobador.equals(aprobacion.getSede().getIdSede()));
        if (!perteneceASedeInvolucrada) {
            throw new SecurityException("No tiene permisos para aprobar polizas de otras sedes");
        }
    }

    private void validarNumeroContratoUnico(String numeroContrato, Integer idActual) {
        String contrato = trimToNull(numeroContrato);
        if (contrato == null) {
            return;
        }

        polizaRepository.findByNumeroContrato(contrato)
                .ifPresent(polizaExistente -> {
                    if (idActual == null || !idActual.equals(polizaExistente.getIdPoliza())) {
                        throw new IllegalArgumentException("El número de contrato ya existe");
                    }
                });
    }

    private void validarNumeroPolizaUnico(String numeroPoliza, Integer idActual) {
        polizaRepository.findByNumeroPolizaIgnoreCase(numeroPoliza)
                .ifPresent(polizaExistente -> {
                    if (idActual == null || !idActual.equals(polizaExistente.getIdPoliza())) {
                        throw new IllegalArgumentException("No se puede activar.Ya existe una póliza con ese número.");
                    }
                });
    }

    private boolean esNacional(Usuario usuario) {
        return usuario != null && (usuario.getRol() == Rol.SUPERADMINISTRADOR
                || usuario.getRol() == Rol.ADMINISTRADOR);
    }

    private boolean esSedeNacional(Usuario usuario) {
        return usuario != null
                && usuario.getSede() != null
                && "Sede Nacional".equalsIgnoreCase(usuario.getSede().getNombre());
    }

    private LocalDate parseDate(String value) {
        if (isBlank(value)) {
            return null;
        }
        try {
            return LocalDate.parse(value.trim(), ISO_DATE);
        } catch (DateTimeParseException exception) {
            throw new IllegalArgumentException("La fecha de póliza no es válida");
        }
    }

    private void validarNoNegativo(BigDecimal value, String message) {
        if (value == null || value.signum() < 0) {
            throw new IllegalArgumentException(message.replace("no puede ser negativa", "es obligatoria y no puede ser negativa"));
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String trimToNull(String value) {
        return isBlank(value) ? null : value.trim();
    }
}
