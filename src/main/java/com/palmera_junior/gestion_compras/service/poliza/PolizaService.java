package com.palmera_junior.gestion_compras.service.poliza;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

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

import com.palmera_junior.gestion_compras.dto.PolizaDTO;
import com.palmera_junior.gestion_compras.entity.EstadoPoliza;
import com.palmera_junior.gestion_compras.entity.Poliza;
import com.palmera_junior.gestion_compras.entity.Proveedor;
import com.palmera_junior.gestion_compras.entity.Rol;
import com.palmera_junior.gestion_compras.entity.Usuario;
import com.palmera_junior.gestion_compras.repository.PolizaRepository;
import com.palmera_junior.gestion_compras.repository.ProveedorRepository;
import com.palmera_junior.gestion_compras.events.PolizaAprobadaEvent;
import com.palmera_junior.gestion_compras.service.correo.CorreoPolizaOutboxService;
import com.palmera_junior.gestion_compras.service.usuario.IUsuarioService;

@Service
public class PolizaService implements IPolizaService {

    private static final DateTimeFormatter ISO_DATE = DateTimeFormatter.ISO_LOCAL_DATE;

    private final PolizaRepository polizaRepository;
    private final ProveedorRepository proveedorRepository;
    private final IUsuarioService usuarioService;
    private final IPolizaArchivoStorage archivoStorage;
    private final ApplicationEventPublisher eventPublisher;
    private final CorreoPolizaOutboxService correoPolizaOutboxService;

    @Autowired
    public PolizaService(PolizaRepository polizaRepository, ProveedorRepository proveedorRepository,
            IUsuarioService usuarioService, IPolizaArchivoStorage archivoStorage,
            ApplicationEventPublisher eventPublisher, CorreoPolizaOutboxService correoPolizaOutboxService) {
        this.polizaRepository = polizaRepository;
        this.proveedorRepository = proveedorRepository;
        this.usuarioService = usuarioService;
        this.archivoStorage = archivoStorage;
        this.eventPublisher = eventPublisher;
        this.correoPolizaOutboxService = correoPolizaOutboxService;
    }

    PolizaService(PolizaRepository polizaRepository, ProveedorRepository proveedorRepository,
            IUsuarioService usuarioService, IPolizaArchivoStorage archivoStorage) {
        this(polizaRepository, proveedorRepository, usuarioService, archivoStorage, null, null);
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('COMERCIAL', 'APROBADOR', 'ADMINISTRADOR', 'SUPERADMINISTRADOR')")
    public Page<Poliza> polizasPaginadas(Pageable pageable, String search, String fechaDesde,
            String fechaHasta, Integer idSede, boolean esNacional, String estado) {
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
        if (inicio != null) {
            specification = specification.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.greaterThanOrEqualTo(root.get("fechaCreacion"), inicio));
        }
        if (fin != null) {
            specification = specification.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.lessThanOrEqualTo(root.get("fechaCreacion"), fin));
        }

        if (!esNacional && idSede != null) {
            specification = specification.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.equal(root.get("sede").get("idSede"), idSede));
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
        return polizaRepository.findAll((root, query, criteriaBuilder) ->
                criteriaBuilder.equal(root.get("sede").get("idSede"), usuario.getSede().getIdSede()),
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
    @Transactional
    @PreAuthorize("hasAnyRole('COMERCIAL', 'APROBADOR', 'ADMINISTRADOR', 'SUPERADMINISTRADOR')")
    public Poliza guardarDesdeDTO(PolizaDTO dto) {
        Usuario usuario = usuarioService.obtenerUsuarioAutenticado();
        Poliza poliza = new Poliza();
        poliza.setUsuario(usuario);
        poliza.setSede(usuario.getSede());
        String adjuntoNuevo = archivoStorage.almacenar(dto == null ? null : dto.getContratoAdjunto());
        try {
            aplicarDatos(poliza, dto, usuario, adjuntoNuevo);
            poliza.setEstado(EstadoPoliza.BORRADOR);
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
        if (poliza.getEstado() != EstadoPoliza.BORRADOR) {
            throw new IllegalStateException("Solo se pueden editar pólizas en estado BORRADOR");
        }
        String adjuntoAnterior = poliza.getContratoAdjunto();
        String nombreAdjuntoAnterior = poliza.getContratoAdjuntoNombre();
        boolean eliminarAdjunto = dto != null && dto.isEliminarContratoAdjunto();
        boolean hayAdjuntoNuevo = dto != null && dto.getContratoAdjunto() != null
                && !dto.getContratoAdjunto().isEmpty();
        if (hayAdjuntoNuevo && adjuntoAnterior != null && !eliminarAdjunto) {
            throw new IllegalArgumentException("Debe eliminar el archivo actual antes de adjuntar otro");
        }
        String adjuntoNuevo = archivoStorage.almacenar(dto == null ? null : dto.getContratoAdjunto());
        try {
            aplicarDatos(poliza, dto, usuario, adjuntoNuevo);
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
        assertVisible(poliza, aprobador);
        poliza.aprobar(aprobador, LocalDate.now());
        Poliza aprobada = polizaRepository.save(poliza);
        Long idAuditoria = correoPolizaOutboxService.registrarPendiente(aprobada);
        eventPublisher.publishEvent(new PolizaAprobadaEvent(idAuditoria));
        return aprobada;
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

    private void aplicarDatos(Poliza poliza, PolizaDTO dto, Usuario usuario, String adjuntoNuevo) {
        if (dto == null) {
            throw new IllegalArgumentException("Los datos de la póliza son obligatorios");
        }
        if (dto.getIdProveedor() == null) {
            throw new IllegalArgumentException("El proveedor es obligatorio");
        }
        if (isBlank(dto.getCliente()) || isBlank(dto.getNumeroContrato())) {
            throw new IllegalArgumentException("El cliente y el número de contrato son obligatorios");
        }
        validarNumeroContratoUnico(dto.getNumeroContrato(), poliza.getIdPoliza());
        validarNoNegativo(dto.getValorPrima(), "La prima no puede ser negativa");
        validarNoNegativo(dto.getValorContrato(), "El valor del contrato no puede ser negativo");

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
        poliza.setValorPrima(dto.getValorPrima());
        poliza.setValorContrato(dto.getValorContrato());
        if (adjuntoNuevo != null) {
            poliza.setContratoAdjunto(adjuntoNuevo);
            poliza.setContratoAdjuntoNombre(dto.getContratoAdjunto().getOriginalFilename());
        }
    }

    private void assertVisible(Poliza poliza, Usuario usuario) {
        if (usuario == null) {
            throw new SecurityException("Debe estar autenticado para gestionar pólizas");
        }
        if (usuario.getRol() == Rol.SUPERADMINISTRADOR || usuario.getRol() == Rol.ADMINISTRADOR) {
            return;
        }
        if (usuario.getSede() == null || poliza.getSede() == null
                || !poliza.getSede().getIdSede().equals(usuario.getSede().getIdSede())) {
            throw new SecurityException("Solo puede gestionar pólizas de su sede");
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
