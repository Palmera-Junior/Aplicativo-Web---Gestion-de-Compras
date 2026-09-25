package com.palmera_junior.gestion_compras.service.poliza;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
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
import com.palmera_junior.gestion_compras.entity.EstadoAprobacionSede;
import com.palmera_junior.gestion_compras.entity.EstadoPoliza;
import com.palmera_junior.gestion_compras.entity.DetallePrima;
import com.palmera_junior.gestion_compras.entity.Poliza;
import com.palmera_junior.gestion_compras.entity.PolizaSedeAprobacion;
import com.palmera_junior.gestion_compras.entity.Proveedor;
import com.palmera_junior.gestion_compras.entity.Rol;
import com.palmera_junior.gestion_compras.entity.Sede;
import com.palmera_junior.gestion_compras.entity.Usuario;
import com.palmera_junior.gestion_compras.repository.PolizaRepository;
import com.palmera_junior.gestion_compras.repository.PolizaSedeAprobacionRepository;
import com.palmera_junior.gestion_compras.repository.ProveedorRepository;
import com.palmera_junior.gestion_compras.events.PolizaAprobadaEvent;
import com.palmera_junior.gestion_compras.service.correo.CorreoPolizaOutboxService;
import com.palmera_junior.gestion_compras.service.usuario.IUsuarioService;
import org.springframework.web.multipart.MultipartFile;

@Service
public class PolizaService implements IPolizaService {

    private static final DateTimeFormatter ISO_DATE = DateTimeFormatter.ISO_LOCAL_DATE;

    private final PolizaRepository polizaRepository;
    private final ProveedorRepository proveedorRepository;
    private final IUsuarioService usuarioService;
    private final IPolizaArchivoStorage archivoStorage;
    private final ApplicationEventPublisher eventPublisher;
    private final CorreoPolizaOutboxService correoPolizaOutboxService;
    private final PolizaSedeAprobacionRepository polizaSedeAprobacionRepository;

    @Autowired
    public PolizaService(PolizaRepository polizaRepository, ProveedorRepository proveedorRepository,
            IUsuarioService usuarioService, IPolizaArchivoStorage archivoStorage,
            ApplicationEventPublisher eventPublisher, CorreoPolizaOutboxService correoPolizaOutboxService,
            PolizaSedeAprobacionRepository polizaSedeAprobacionRepository) {
        this.polizaRepository = polizaRepository;
        this.proveedorRepository = proveedorRepository;
        this.usuarioService = usuarioService;
        this.archivoStorage = archivoStorage;
        this.eventPublisher = eventPublisher;
        this.correoPolizaOutboxService = correoPolizaOutboxService;
        this.polizaSedeAprobacionRepository = polizaSedeAprobacionRepository;
    }

    public PolizaService(PolizaRepository polizaRepository, ProveedorRepository proveedorRepository,
            IUsuarioService usuarioService, IPolizaArchivoStorage archivoStorage,
            ApplicationEventPublisher eventPublisher, CorreoPolizaOutboxService correoPolizaOutboxService) {
        this(polizaRepository, proveedorRepository, usuarioService, archivoStorage,
                eventPublisher, correoPolizaOutboxService, null);
    }

    PolizaService(PolizaRepository polizaRepository, ProveedorRepository proveedorRepository,
            IUsuarioService usuarioService, IPolizaArchivoStorage archivoStorage) {
        this(polizaRepository, proveedorRepository, usuarioService, archivoStorage, null, null, null);
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
        return polizaRepository.findAll(specification, pageableOrdenado);
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
        String adjuntoNuevo = archivoStorage.almacenar(dto == null ? null : dto.getContratoAdjunto());
        try {
            aplicarDatos(poliza, dto, usuario, adjuntoNuevo);
            poliza.setEstado(EstadoPoliza.BORRADOR);
            actualizarAprobacionesPorSede(poliza, dto.getIdsSedes(), usuario);
            return polizaRepository.save(poliza);
        } catch (RuntimeException exception) {
            archivoStorage.eliminar(adjuntoNuevo);
            throw exception;
        }
    }

    @Override
    @Transactional
    @PreAuthorize("hasAnyRole('COMERCIAL', 'APROBADOR', 'ADMINISTRADOR', 'SUPERADMINISTRADOR')")
    public Poliza actualizarDesdeDTO(Integer idPoliza, PolizaDTO dto) {
        Usuario usuario = usuarioService.obtenerUsuarioAutenticado();
        Poliza poliza = polizaRepository.findById(idPoliza)
                .orElseThrow(() -> new IllegalArgumentException("Póliza no encontrada"));
        assertVisible(poliza, usuario);
        assertPuedeEditar(poliza, usuario);
        if (poliza.getEstado() != EstadoPoliza.BORRADOR) {
            throw new IllegalStateException("Solo se pueden editar pólizas en estado BORRADOR");
        }
        String adjuntoAnterior = poliza.getContratoAdjunto();
        String nombreAdjuntoAnterior = poliza.getContratoAdjuntoNombre();
        boolean eliminarAdjunto = dto != null && dto.isEliminarContratoAdjunto();
        boolean hayAdjuntoNuevo = dto != null && dto.getContratoAdjunto() != null
                && !dto.getContratoAdjunto().isEmpty();
        if (!hayAdjuntoNuevo && (eliminarAdjunto || isBlank(adjuntoAnterior))) {
            throw new IllegalArgumentException("Debe adjuntar el contrato de la póliza en formato PDF");
        }
        if (hayAdjuntoNuevo && adjuntoAnterior != null && !eliminarAdjunto) {
            throw new IllegalArgumentException("Debe eliminar el archivo actual antes de adjuntar otro");
        }
        String adjuntoNuevo = archivoStorage.almacenar(dto == null ? null : dto.getContratoAdjunto());
        try {
            aplicarDatos(poliza, dto, usuario, adjuntoNuevo);
            actualizarAprobacionesPorSede(poliza, dto.getIdsSedes(), usuario);
            if (eliminarAdjunto && adjuntoNuevo == null) {
                poliza.setContratoAdjunto(null);
                poliza.setContratoAdjuntoNombre(null);
            } else if (adjuntoNuevo != null) {
                poliza.setContratoAdjuntoNombre(dto.getContratoAdjunto().getOriginalFilename());
            } else {
                poliza.setContratoAdjuntoNombre(nombreAdjuntoAnterior);
            }
            Poliza guardada = polizaRepository.save(poliza);
            if (eliminarAdjunto || adjuntoNuevo != null) {
                archivoStorage.eliminar(adjuntoAnterior);
            }
            return guardada;
        } catch (RuntimeException exception) {
            archivoStorage.eliminar(adjuntoNuevo);
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
    public Poliza activarVigente(Integer idPoliza, String fechaVencimiento, MultipartFile polizaFisica) {
        Usuario usuario = usuarioService.obtenerUsuarioAutenticado();
        Poliza poliza = polizaRepository.findById(idPoliza)
                .orElseThrow(() -> new IllegalArgumentException("Póliza no encontrada"));
        assertVisible(poliza, usuario);
        if (polizaFisica == null || polizaFisica.isEmpty()) {
            throw new IllegalArgumentException("Debe adjuntar el PDF de la póliza física");
        }

        LocalDate nuevaFechaVencimiento = parseDate(fechaVencimiento);
        if (nuevaFechaVencimiento == null || nuevaFechaVencimiento.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("La fecha de vencimiento debe ser hoy o posterior");
        }

        String adjuntoNuevo = archivoStorage.almacenar(polizaFisica);
        try {
            poliza.activar(usuario, nuevaFechaVencimiento, adjuntoNuevo, polizaFisica.getOriginalFilename());
            return polizaRepository.save(poliza);
        } catch (RuntimeException exception) {
            archivoStorage.eliminar(adjuntoNuevo);
            throw exception;
        }
    }

    @Override
    @Transactional
    public int marcarVencidas() {
        return polizaRepository.marcarVigentesVencidas(LocalDate.now());
    }

    @Override
    @Transactional
    @PreAuthorize("hasAnyRole('APROBADOR', 'ADMINISTRADOR', 'SUPERADMINISTRADOR')")
    public Poliza anular(Integer idPoliza) {
        Usuario usuario = usuarioService.obtenerUsuarioAutenticado();
        Poliza poliza = polizaRepository.findById(idPoliza)
                .orElseThrow(() -> new IllegalArgumentException("Póliza no encontrada"));
        assertVisible(poliza, usuario);
        poliza.anular();
        return polizaRepository.save(poliza);
    }

    @Override
    @Transactional
    @PreAuthorize("hasAnyRole('APROBADOR', 'SUPERADMINISTRADOR')")
    public Poliza terminar(Integer idPoliza, String motivoTerminacion) {
        Usuario usuario = usuarioService.obtenerUsuarioAutenticado();
        Poliza poliza = polizaRepository.findById(idPoliza)
                .orElseThrow(() -> new IllegalArgumentException("Póliza no encontrada"));
        assertVisible(poliza, usuario);
        poliza.terminar(usuario, LocalDate.now(), motivoTerminacion);
        return polizaRepository.save(poliza);
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

    private void aplicarDatos(Poliza poliza, PolizaDTO dto, Usuario usuario, String adjuntoNuevo) {
        if (dto == null) {
            throw new IllegalArgumentException("Los datos de la póliza son obligatorios");
        }
        if (dto.getIdProveedor() == null) {
            throw new IllegalArgumentException("El proveedor es obligatorio");
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

        Proveedor proveedor = proveedorRepository.findById(dto.getIdProveedor().intValue())
                .orElseThrow(() -> new IllegalArgumentException("El proveedor no existe"));
        if (!esNacional(usuario) && (usuario.getSede() == null || proveedor.getSedes().stream()
                .noneMatch(sede -> sede.getIdSede().equals(usuario.getSede().getIdSede())))) {
            throw new IllegalArgumentException("El proveedor no está asociado a la sede del usuario");
        }

        poliza.setFechaCreacion(fecha);
        poliza.setFechaVencimiento(fechaVencimiento);
        poliza.setProveedor(proveedor);
        poliza.setCliente(dto.getCliente().trim());
        poliza.setDescripcion(trimToNull(dto.getDescripcion()));
        poliza.setNumeroContrato(dto.getNumeroContrato().trim());
        poliza.setValorPrima(totalPrima);
        poliza.setValorContrato(dto.getValorContrato());
        if (adjuntoNuevo != null) {
            poliza.setContratoAdjunto(adjuntoNuevo);
            poliza.setContratoAdjuntoNombre(dto.getContratoAdjunto().getOriginalFilename());
        }
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

    private void assertPuedeEditar(Poliza poliza, Usuario usuario) {
        boolean esCreador = usuario != null && poliza.getUsuario() != null
                && usuario.getIdUsuario().equals(poliza.getUsuario().getIdUsuario());
        boolean esSuperAdministrador = usuario != null && usuario.getRol() == Rol.SUPERADMINISTRADOR;
        boolean perteneceASedePrincipal = usuario != null && usuario.getSede() != null
            && poliza.getSede() != null
            && usuario.getSede().getIdSede().equals(poliza.getSede().getIdSede());
        if (!esCreador && !esSuperAdministrador && !perteneceASedePrincipal) {
            throw new SecurityException(
                "Solo el creador, un usuario de la sede principal o un superadministrador pueden editar la póliza.");
        }
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
