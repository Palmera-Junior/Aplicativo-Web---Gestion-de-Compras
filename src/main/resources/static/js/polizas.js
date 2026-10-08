function mostrarToast(mensaje, tipo = 'success') {
    window.mostrarAppToast(mensaje, tipo);
}

async function obtenerMensajeError(response, mensajePorDefecto) {
    const texto = await response.text();
    if (!texto) return mensajePorDefecto;

    try {
        const cuerpo = JSON.parse(texto);
        return cuerpo.error || cuerpo.message || mensajePorDefecto;
    } catch (error) {
        return texto;
    }
}

function pintarEstadoCorreo(celda, estado) {
    const estados = {
        ENVIADO: ['enviado', 'check', 'Correo enviado correctamente a contabilidad'],
        FALLIDO: ['fallido', 'close', 'El correo a contabilidad no pudo enviarse'],
        PENDIENTE: ['pendiente', 'schedule', 'El correo a contabilidad está en proceso de envío'],
        PROCESANDO: ['pendiente', 'schedule', 'El correo a contabilidad está en proceso de envío'],
        REINTENTAR: ['pendiente', 'schedule', 'El correo a contabilidad está en proceso de envío']
    };
    const detalle = estados[estado];
    const indicador = document.createElement('span');
    indicador.className = `correo-estado-icon ${detalle ? detalle[0] : 'sin-envio'}`;
    indicador.title = detalle ? detalle[2] : 'No se ha generado un envío de correo a contabilidad';
    if (detalle) {
        const icono = document.createElement('span');
        icono.className = 'material-symbols';
        icono.textContent = detalle[1];
        indicador.appendChild(icono);
    } else {
        indicador.textContent = '-';
    }
    celda.replaceChildren(indicador);
}

const formatearPesos = (valor) => {
    const numero = Number(valor);
    if (!Number.isFinite(numero)) return '';
    return numero.toLocaleString('es-CO', {
        style: 'currency',
        currency: 'COP',
        minimumFractionDigits: 0,
        maximumFractionDigits: 0
    });
};

const valorNumerico = (valor) => valor.replace(/[^0-9]/g, '');

function configurarCampoMoneda(campoVisible) {
    const nombre = campoVisible.dataset.moneyField;
    const campoOculto = campoVisible.form?.elements[nombre];
    if (!campoOculto) return;

    const sincronizar = () => {
        campoOculto.value = valorNumerico(campoVisible.value);
    };
    campoVisible.addEventListener('focus', () => {
        campoVisible.value = valorNumerico(campoVisible.value);
        campoVisible.select();
    });
    campoVisible.addEventListener('input', sincronizar);
    campoVisible.addEventListener('blur', () => {
        sincronizar();
        campoVisible.value = formatearPesos(campoOculto.value);
    });
}

function actualizarTotalDetallesPrima() {
    const cuerpo = document.getElementById('tbody-detalles-prima');
    const totalOculto = document.querySelector('#poliza-form input[name="valorPrima"]');
    const totalVisible = document.getElementById('valor-prima-total');
    if (!cuerpo || !totalOculto || !totalVisible) return;

    const total = [...cuerpo.querySelectorAll('.prima-detalle-valor-oculto')]
        .reduce((acumulado, campo) => acumulado + (Number(campo.value) || 0), 0);
    totalOculto.value = total;
    totalVisible.value = formatearPesos(total);
}

function obtenerSedesSeleccionadas() {
    return [...document.querySelectorAll('input[name="idsSedes"]:checked')]
        .map((checkbox) => Number(checkbox.value))
        .filter((valor) => Number.isFinite(valor));
}

function actualizarResumenSedes() {
    const contenedor = document.getElementById('sede-aprobacion-resumen');
    if (!contenedor) return;

    const seleccionadas = obtenerSedesSeleccionadas();
    if (!seleccionadas.length) {
        contenedor.innerHTML = '<h4 class="sede-aprobacion-titulo">APROBACIONES</h4><div class="sede-aprobacion-contenido"><span class="material-symbols">fact_check</span><span>Selecciona las sedes que deben aprobar la póliza.</span></div>';
        return;
    }

    const etiquetas = seleccionadas.length === 1
        ? '1 sede seleccionada'
        : `${seleccionadas.length} sedes seleccionadas`;
    contenedor.innerHTML = `<h4 class="sede-aprobacion-titulo">APROBACIONES</h4><div class="sede-aprobacion-contenido"><span class="material-symbols">check_circle</span><span>${etiquetas}</span></div>`;
}

function escaparTexto(valor) {
    const elemento = document.createElement('span');
    elemento.textContent = valor == null ? '' : String(valor);
    return elemento.innerHTML;
}

function renderizarAprobacionesPorSede(aprobaciones) {
    const contenedor = document.getElementById('sede-aprobacion-resumen');
    if (!contenedor || !Array.isArray(aprobaciones) || !aprobaciones.length) {
        actualizarResumenSedes();
        return;
    }

    const chips = aprobaciones.map((aprobacion) => {
        const estado = (aprobacion.estado || 'PENDIENTE').toUpperCase();
        const className = estado === 'APROBADA' ? 'estado-aprobada' : 'estado-pendiente';
        const label = estado === 'APROBADA' ? 'Aprobada' : 'Pendiente';
        const detalle = estado === 'APROBADA'
            ? `Aprobó: ${escaparTexto(aprobacion.aprobador || 'Sin información')}<br><small>${escaparTexto(aprobacion.fechaAprobacion || 'Sin fecha')}</small>`
            : 'Aún no ha aprobado';
        return `<span class="sede-approval-chip ${className}"><strong>${escaparTexto(aprobacion.nombreSede || 'Sede')}</strong><small>${label}</small><span>${detalle}</span></span>`;
    }).join('');

    contenedor.innerHTML = `<h4 class="sede-aprobacion-titulo">APROBACIONES</h4><div class="sede-aprobacion-contenido"><span class="material-symbols">fact_check</span><div class="sede-approval-list">${chips}</div></div>`;
}

function renumerarDetallesPrima() {
    document.querySelectorAll('#tbody-detalles-prima tr').forEach((fila, indice) => {
        fila.querySelector('.prima-detalle-valor-oculto').name = `detallesPrima[${indice}].valor`;
        fila.querySelector('.prima-detalle-descripcion').name = `detallesPrima[${indice}].descripcion`;
    });
}

function agregarDetallePrima(detalle = {}) {
    const cuerpo = document.getElementById('tbody-detalles-prima');
    if (!cuerpo) return;

    const fila = document.createElement('tr');
    const valor = detalle.valor ?? '';
    fila.innerHTML = `
        <td>
            <input class="input-control td-input prima-detalle-descripcion" type="text" maxlength="255" placeholder="Descripción del detalle de prima" required>
        </td>
        <td>
            <input class="input-control td-input prima-detalle-valor" type="text" inputmode="numeric" autocomplete="off" placeholder="Ej: 1.000.000" required>
            <input class="prima-detalle-valor-oculto" type="hidden">
        </td>
        <td>
            <button title="Eliminar fila" type="button" class="btn-icon delete" aria-label="Eliminar fila">
                <span class="material-symbols">delete</span>
            </button>
        </td>`;

    const campoVisible = fila.querySelector('.prima-detalle-valor');
    const campoOculto = fila.querySelector('.prima-detalle-valor-oculto');
    const campoDescripcion = fila.querySelector('.prima-detalle-descripcion');
    campoOculto.value = valor;
    campoVisible.value = valor === '' ? '' : formatearPesos(valor);
    campoDescripcion.value = detalle.descripcion || '';
    campoVisible.addEventListener('focus', () => {
        campoVisible.value = valorNumerico(campoVisible.value);
        campoVisible.select();
    });
    campoVisible.addEventListener('input', () => {
        campoOculto.value = valorNumerico(campoVisible.value);
        actualizarTotalDetallesPrima();
    });
    campoVisible.addEventListener('blur', () => {
        campoVisible.value = campoOculto.value ? formatearPesos(campoOculto.value) : '';
    });
    cuerpo.appendChild(fila);
    renumerarDetallesPrima();
    actualizarTotalDetallesPrima();
}

function reiniciarDetallesPrima(detalles = [{}]) {
    const cuerpo = document.getElementById('tbody-detalles-prima');
    if (!cuerpo) return;
    cuerpo.replaceChildren();
    detalles.forEach(agregarDetallePrima);
}

function configurarEdicionDetallesPrima(editable) {
    const botonAgregar = document.getElementById('btn-agregar-detalle-prima');
    if (botonAgregar) botonAgregar.disabled = !editable;
    document.querySelectorAll('#tbody-detalles-prima .delete').forEach((boton) => {
        boton.disabled = !editable;
    });
}

document.addEventListener('DOMContentLoaded', () => {
    const dialog = document.getElementById('poliza-modal');
    const detailDialog = document.getElementById('poliza-detalle-modal');
    const activarDialog = document.getElementById('poliza-activar-modal');
    const activarForm = document.getElementById('poliza-activar-form');
    const activarNumero = document.getElementById('poliza-activar-numero');
    const activarArchivo = document.getElementById('poliza-fisica-input');
    const terminarDialog = document.getElementById('poliza-terminar-modal');
    const terminarForm = document.getElementById('poliza-terminar-form');
    const terminarMotivo = terminarForm?.elements.motivoTerminacion;
    const renovarDialog = document.getElementById('poliza-renovar-modal');
    const renovarForm = document.getElementById('poliza-renovar-form');
    const renovarInicio = document.getElementById('poliza-renovacion-inicio');
    const renovarFin = document.getElementById('poliza-renovacion-fin');
    const renovarArchivos = document.getElementById('poliza-renovacion-archivos');
    const renovarSubmit = document.getElementById('btn-confirmar-renovacion');
    const renovacionesAuditoria = document.getElementById('poliza-renovaciones-auditoria');
    const renovacionesBody = document.getElementById('poliza-renovaciones-body');
    const openButton = document.getElementById('btn-nueva-poliza');
    const closeButton = document.querySelector('#poliza-modal .modal-close');
    const cancelButton = document.querySelector('#poliza-modal .btn-cancel');
    const saveButton = document.getElementById('btn-guardar-poliza');
    const form = document.getElementById('poliza-form');
    const usuarioActualId = form?.dataset.usuarioActualId || '';
    const sedeActualId = form?.dataset.sedeActualId || '';
    const rolUsuarioActual = form?.dataset.rolActual || '';
    const esSedeNacional = form?.dataset.sedeNacional === 'true';
    const archivoInput = form?.elements.contratoAdjunto;
    const archivoActual = document.getElementById('poliza-archivo-actual');
    const nombreArchivoActual = document.getElementById('poliza-archivo-nombre');
    const enlaceArchivoActual = document.getElementById('poliza-archivo-enlace');
    const eliminarArchivoButton = document.getElementById('btn-eliminar-archivo-poliza');
    const eliminarArchivoInput = form?.elements.eliminarContratoAdjunto;
    const agregarDetalleButton = document.getElementById('btn-agregar-detalle-prima');
    const detallesPrimaBody = document.getElementById('tbody-detalles-prima');
    const formTitle = document.getElementById('poliza-modal-title');
    const formDescription = document.getElementById('poliza-modal-description');
    const proveedorSearch = document.getElementById('buscar-proveedor-poliza');
    const proveedorId = document.getElementById('id-proveedor-poliza');
    const proveedorSearchContainer = document.querySelector('.proveedor-poliza-search');
    const proveedorDropdown = document.getElementById('proveedor-poliza-resultados');
    const proveedorCard = document.getElementById('proveedor-poliza-card');
    const cambiarProveedorButton = document.getElementById('btn-cambiar-proveedor-poliza');
    const createAction = form?.getAttribute('action');
    const fechaDesde = document.getElementById('poliza-fecha-desde');
    const fechaHasta = document.getElementById('poliza-fecha-hasta');
    const estadoSelect = document.querySelector('.polizas-filters .estado-select');
    const auditoria = {
        creador: document.getElementById('poliza-creado-por'),
        aprobador: document.getElementById('poliza-aprobado-por'),
        fechaAprobacion: document.getElementById('poliza-fecha-aprobacion'),
        terminadoPor: document.getElementById('poliza-terminado-por'),
        fechaTerminacion: document.getElementById('poliza-fecha-terminacion'),
        motivoTerminacion: document.getElementById('poliza-motivo-terminacion'),
        terminacion: document.querySelectorAll('.auditoria-terminacion'),
        activador: document.getElementById('poliza-activado-por'),
        anulador: document.getElementById('poliza-anulado-por'),
        fechaAnulacion: document.getElementById('poliza-fecha-anulacion'),
        anulacion: document.querySelectorAll('.auditoria-anulacion-poliza')
    };
    ['poliza-aprobado-por', 'poliza-fecha-aprobacion',
        'poliza-terminado-por', 'poliza-fecha-terminacion', 'poliza-motivo-terminacion']
        .forEach((id) => document.getElementById(id)?.closest('.auditoria-item')?.setAttribute('hidden', 'hidden'));
    let scrollOriginal = null;
    let proveedorSearchTimer = null;
    let proveedorSearchController = null;
    let proveedorOpcionActiva = -1;

    const cerrarResultadosProveedor = (abortar = false) => {
        window.clearTimeout(proveedorSearchTimer);
        if (abortar) {
            proveedorSearchController?.abort();
            proveedorSearchController = null;
        }
        proveedorDropdown?.replaceChildren();
        if (proveedorDropdown) proveedorDropdown.style.display = 'none';
        proveedorSearch?.setAttribute('aria-expanded', 'false');
        proveedorSearch?.removeAttribute('aria-activedescendant');
        proveedorOpcionActiva = -1;
    };

    const mostrarTarjetaProveedor = (proveedor) => {
        proveedorId.value = proveedor.idProv || '';
        proveedorSearch.value = '';
        proveedorSearch.required = false;
        document.getElementById('proveedor-poliza-nombre').textContent = proveedor.nombre || '';
        document.getElementById('proveedor-poliza-identificacion').textContent =
            `NIT ${proveedor.nit || ''}${proveedor.ciudad ? ` · ${proveedor.ciudad}` : ''}`;
        document.getElementById('proveedor-poliza-direccion').textContent = proveedor.direccion || '';
        document.getElementById('proveedor-poliza-contacto').textContent =
            [proveedor.telefono, proveedor.correo].filter(Boolean).join(' · ');
        cerrarResultadosProveedor(true);
        proveedorSearchContainer.hidden = true;
        proveedorCard.hidden = false;
    };

    const restablecerProveedorPoliza = () => {
        cerrarResultadosProveedor(true);
        proveedorId.value = '';
        proveedorSearch.value = '';
        proveedorSearch.required = true;
        proveedorSearch.disabled = false;
        proveedorSearchContainer.hidden = false;
        proveedorCard.hidden = true;
        cambiarProveedorButton.disabled = false;
    };

    proveedorSearch?.addEventListener('input', () => {
        const termino = proveedorSearch.value.trim();
        cerrarResultadosProveedor(true);
        proveedorId.value = '';
        proveedorSearch.required = true;
        proveedorCard.hidden = true;
        if (termino.length < 3) return;

        proveedorSearchTimer = window.setTimeout(async () => {
            const controller = new AbortController();
            proveedorSearchController = controller;
            try {
                const response = await fetch(
                    `/polizas/proveedores/buscar?query=${encodeURIComponent(termino)}`,
                    { signal: controller.signal, headers: { Accept: 'application/json' } }
                );
                if (!response.ok) throw new Error('No fue posible buscar proveedores.');
                const proveedores = await response.json();
                if (controller.signal.aborted || proveedorSearch.value.trim() !== termino) return;
                proveedorDropdown.replaceChildren();

                if (!proveedores.length) {
                    const sinResultados = document.createElement('div');
                    sinResultados.className = 'proveedor-poliza-no-results';
                    sinResultados.textContent = 'No se encontraron proveedores.';
                    proveedorDropdown.appendChild(sinResultados);
                } else {
                    proveedores.slice(0, 8).forEach((proveedor, index) => {
                        const opcion = document.createElement('button');
                        opcion.type = 'button';
                        opcion.className = 'autocomplete-item proveedor-poliza-option';
                        opcion.id = `proveedor-poliza-opcion-${index}`;
                        opcion.setAttribute('role', 'option');
                        opcion.setAttribute('aria-selected', 'false');
                        const nombreProveedor = document.createElement('span');
                        nombreProveedor.textContent = proveedor.nombre || '';
                        const detalleProveedor = document.createElement('small');
                        detalleProveedor.textContent = `NIT: ${proveedor.nit || ''}${proveedor.ciudad ? ` · ${proveedor.ciudad}` : ''}`;
                        opcion.append(nombreProveedor, detalleProveedor);
                        opcion.addEventListener('mousedown', (event) => event.preventDefault());
                        opcion.addEventListener('click', () => mostrarTarjetaProveedor(proveedor));
                        proveedorDropdown.appendChild(opcion);
                    });
                }
                proveedorDropdown.style.display = 'block';
                proveedorSearch.setAttribute('aria-expanded', 'true');
            } catch (error) {
                if (error.name !== 'AbortError') {
                    mostrarToast('No fue posible cargar las sugerencias de proveedor.', 'error');
                }
                proveedorDropdown.replaceChildren();
                proveedorDropdown.style.display = 'none';
            } finally {
                if (proveedorSearchController === controller) proveedorSearchController = null;
            }
        }, 250);
    });

    proveedorSearch?.addEventListener('keydown', (event) => {
        const opciones = [...(proveedorDropdown?.querySelectorAll('[role="option"]') || [])];
        if (event.key === 'Escape' && proveedorDropdown?.style.display === 'block') {
            event.preventDefault();
            event.stopPropagation();
            cerrarResultadosProveedor(true);
            return;
        }
        if ((event.key === 'ArrowDown' || event.key === 'ArrowUp') && opciones.length) {
            event.preventDefault();
            const delta = event.key === 'ArrowDown' ? 1 : -1;
            proveedorOpcionActiva = proveedorOpcionActiva < 0
                ? (delta > 0 ? 0 : opciones.length - 1)
                : (proveedorOpcionActiva + delta + opciones.length) % opciones.length;
            opciones.forEach((opcion, index) => {
                const activa = index === proveedorOpcionActiva;
                opcion.classList.toggle('active', activa);
                opcion.setAttribute('aria-selected', String(activa));
            });
            proveedorSearch.setAttribute('aria-activedescendant', opciones[proveedorOpcionActiva].id);
        } else if (event.key === 'Enter' && opciones.length) {
            const indice = proveedorOpcionActiva >= 0
                ? proveedorOpcionActiva
                : opciones.length === 1 ? 0 : -1;
            if (indice >= 0) {
                event.preventDefault();
                opciones[indice].click();
            }
        }
    });

    cambiarProveedorButton?.addEventListener('click', () => {
        restablecerProveedorPoliza();
        proveedorSearch.focus();
    });
    document.addEventListener('click', (event) => {
        if (!event.target.closest('.proveedor-poliza-field')) cerrarResultadosProveedor(true);
    });

    const actualizarAuditoria = ({ creador, aprobador, sede, fechaAprobacion, terminadoPor, fechaTerminacion, motivoTerminacion, activador, anulador, fechaAnulacion }) => {
        if (auditoria.creador) auditoria.creador.textContent = creador || 'Sin información';
        if (auditoria.aprobador) auditoria.aprobador.textContent = aprobador || 'Sin aprobación';
        if (auditoria.fechaAprobacion) auditoria.fechaAprobacion.textContent = fechaAprobacion || 'Sin aprobación';
        if (auditoria.terminadoPor) auditoria.terminadoPor.textContent = terminadoPor || 'Sin terminación';
        if (auditoria.fechaTerminacion) auditoria.fechaTerminacion.textContent = fechaTerminacion || 'Sin terminación';
        if (auditoria.motivoTerminacion) auditoria.motivoTerminacion.textContent = motivoTerminacion || 'Sin motivo de terminación';
    };

    const mostrarAuditoriaTerminacion = (mostrar) => {
        auditoria.terminacion.forEach((campo) => {
            campo.hidden = !mostrar;
        });
    };

    const mostrarAuditoriaAnulacion = (mostrar) => {
        auditoria.anulacion.forEach((campo) => {
            campo.hidden = !mostrar;
        });
    };

    const limpiarAuditoriaRenovaciones = () => {
        renovacionesBody?.replaceChildren();
        if (renovacionesAuditoria) renovacionesAuditoria.hidden = true;
    };

    const cargarAuditoriaRenovaciones = async (idPoliza) => {
        limpiarAuditoriaRenovaciones();
        try {
            const response = await fetch(`/polizas/${idPoliza}/renovaciones`, {
                headers: { Accept: 'application/json' }
            });
            if (!response.ok) throw new Error('No fue posible consultar las renovaciones.');
            const historial = await response.json();
            if (!renovacionesBody) return;
            const periodoInicial = historial.periodoInicial || {};
            const filas = [{
                periodo: 'Inicial',
                fechaInicio: periodoInicial.fechaInicio,
                fechaFin: periodoInicial.fechaFin,
                fechaRegistro: '',
                usuario: periodoInicial.usuario || 'Sin información',
                archivos: periodoInicial.archivos || []
            }, ...(historial.renovaciones || []).map((renovacion) => ({
                ...renovacion,
                periodo: `Renovación ${renovacion.numero}`
            }))];

            filas.forEach((renovacion) => {
                const fila = document.createElement('tr');
                [renovacion.periodo,
                    renovacion.fechaInicio,
                    renovacion.fechaFin,
                    renovacion.fechaRegistro ? new Date(renovacion.fechaRegistro).toLocaleDateString('es-CO') : '',
                    renovacion.usuario || 'Sin información'].forEach((valor) => {
                    const celda = document.createElement('td');
                    celda.textContent = valor || '';
                    fila.appendChild(celda);
                });
                const documentos = document.createElement('td');
                (renovacion.archivos || []).forEach((archivo, indice) => {
                    if (indice > 0) documentos.appendChild(document.createTextNode(', '));
                    const enlace = document.createElement('a');
                    enlace.href = archivo.url;
                    enlace.textContent = archivo.nombre;
                    enlace.setAttribute('download', '');
                    documentos.appendChild(enlace);
                });
                fila.appendChild(documentos);
                renovacionesBody.appendChild(fila);
            });
            renovacionesAuditoria.hidden = false;
        } catch (error) {
            mostrarToast(error.message || 'No fue posible consultar las renovaciones.', 'error');
        }
    };

    document.querySelectorAll('.poliza-money').forEach((celda) => {
        celda.textContent = formatearPesos(celda.textContent.trim());
    });
    document.querySelectorAll('.poliza-money-input').forEach(configurarCampoMoneda);
    reiniciarDetallesPrima();
    agregarDetalleButton?.addEventListener('click', () => agregarDetallePrima());
    detallesPrimaBody?.addEventListener('click', (event) => {
        const botonEliminar = event.target.closest('.prima-detalles-table .delete');
        if (!botonEliminar) return;
        const filas = detallesPrimaBody.querySelectorAll('tr');
        if (filas.length <= 1) {
            mostrarToast('Debe conservar al menos un detalle de prima.', 'info');
            return;
        }
        botonEliminar.closest('tr')?.remove();
        renumerarDetallesPrima();
        actualizarTotalDetallesPrima();
    });

    estadoSelect?.addEventListener('change', () => {
        estadoSelect.closest('form')?.requestSubmit();
    });

    fechaDesde?.addEventListener('change', () => {
        if (fechaHasta) fechaHasta.min = fechaDesde.value;
    });
    fechaHasta?.addEventListener('change', () => {
        if (fechaDesde) fechaDesde.max = fechaHasta.value;
    });

    const actualizarEstadosCorreo = async () => {
        const filas = [...document.querySelectorAll('tr[data-poliza-id]')];
        if (!filas.length) return;
        const parametros = filas.map((fila) => `ids=${encodeURIComponent(fila.dataset.polizaId)}`).join('&');
        try {
            const response = await fetch(`/polizas/correo-estados?${parametros}`, {
                headers: { Accept: 'application/json' }
            });
            if (!response.ok) return;
            const estados = await response.json();
            filas.forEach((fila) => {
                const celda = fila.querySelector('.correo-cell');
                if (celda) pintarEstadoCorreo(celda, estados[fila.dataset.polizaId] || null);
            });
        } catch (error) {
            // Mantener el último estado mostrado si el sondeo no está disponible.
        }
    };
    actualizarEstadosCorreo();
    window.setInterval(actualizarEstadosCorreo, 10000);

    document.querySelectorAll('.alerta-exito').forEach((alerta) => {
        mostrarToast(alerta.textContent.trim(), 'success');
        alerta.remove();
    });
    document.querySelectorAll('.alerta-error').forEach((alerta) => {
        mostrarToast(alerta.textContent.trim(), 'error');
        alerta.remove();
    });

    document.querySelectorAll('input[name="idsSedes"]').forEach((checkbox) => {
        checkbox.addEventListener('change', actualizarResumenSedes);
    });
    actualizarResumenSedes();

    const configurarSedesParaEdicion = (puedeEditar) => {
        const checkboxes = form?.querySelectorAll('input[name="idsSedes"]') || [];
        checkboxes.forEach((checkbox) => {
            const esSedePropia = String(checkbox.value) === String(sedeActualId);
            checkbox.disabled = !puedeEditar || (!esSedeNacional && !esSedePropia);
        });
    };

    const configurarSedesParaNuevaPoliza = () => {
        const checkboxes = form?.querySelectorAll('input[name="idsSedes"]') || [];
        checkboxes.forEach((checkbox) => {
            const esSedePropia = String(checkbox.value) === String(sedeActualId);
            checkbox.checked = esSedeNacional
                ? checkbox.dataset.sedeNacional === 'true'
                : esSedePropia;
        });
        configurarSedesParaEdicion(true);
        actualizarResumenSedes();
    };

    const bloquearScroll = () => {
        if (scrollOriginal) return;
        const compensacion = window.innerWidth - document.documentElement.clientWidth;
        scrollOriginal = {
            overflow: document.body.style.overflow,
            paddingRight: document.body.style.paddingRight
        };
        document.body.style.overflow = 'hidden';
        if (compensacion > 0) document.body.style.paddingRight = `${compensacion}px`;
    };

    const restaurarScroll = () => {
        if (!scrollOriginal) return;
        document.body.style.overflow = scrollOriginal.overflow;
        document.body.style.paddingRight = scrollOriginal.paddingRight;
        scrollOriginal = null;
    };

    const abrir = (modal) => {
        if (!modal) return;
        if (typeof modal.showModal === 'function') modal.showModal();
        else modal.setAttribute('open', 'open');
        bloquearScroll();
    };

    const cerrar = (modal) => {
        if (!modal) return;
        if (typeof modal.close === 'function' && modal.open) modal.close();
        else modal.removeAttribute('open');
        if (!dialog?.open && !detailDialog?.open && !activarDialog?.open && !terminarDialog?.open
            && !renovarDialog?.open) restaurarScroll();
    };

    document.querySelectorAll('.js-activar-poliza').forEach((button) => {
        button.addEventListener('click', () => {
            activarForm?.setAttribute('action', button.dataset.url || '');
            activarArchivo?.form?.reset();
            if (activarNumero) activarNumero.value = '';
            abrir(activarDialog);
        });
    });

    document.querySelectorAll('.js-cerrar-activar').forEach((button) => {
        button.addEventListener('click', () => cerrar(activarDialog));
    });

    activarArchivo?.addEventListener('change', () => {
        if (activarArchivo.files.length > 5) {
            activarArchivo.value = '';
            mostrarToast('Puedes adjuntar máximo 5 archivos PDF.', 'error');
        }
    });

    document.querySelectorAll('.js-terminar-poliza').forEach((button) => {
        button.addEventListener('click', () => {
            terminarForm?.setAttribute('action', button.dataset.url || '');
            if (terminarMotivo) terminarMotivo.value = '';
            abrir(terminarDialog);
            terminarMotivo?.focus();
        });
    });

    document.querySelectorAll('.js-cerrar-terminar').forEach((button) => {
        button.addEventListener('click', () => cerrar(terminarDialog));
    });

    document.querySelectorAll('.js-renovar-poliza').forEach((button) => {
        button.addEventListener('click', () => {
            renovarForm?.reset();
            renovarForm?.setAttribute('action', button.dataset.url || '');
            if (renovarInicio) renovarInicio.min = '';
            if (renovarFin) renovarFin.min = '';
            const vencimiento = button.dataset.fechaVencimiento;
            if (vencimiento && renovarInicio) {
                const siguienteDia = new Date(`${vencimiento}T00:00:00Z`);
                siguienteDia.setUTCDate(siguienteDia.getUTCDate() + 1);
                renovarInicio.min = siguienteDia.toISOString().slice(0, 10);
            }
            abrir(renovarDialog);
            renovarInicio?.focus();
        });
    });

    document.querySelectorAll('.js-cerrar-renovar').forEach((button) => {
        button.addEventListener('click', () => cerrar(renovarDialog));
    });

    renovarInicio?.addEventListener('change', () => {
        if (!renovarInicio.value || !renovarFin) return;
        const siguienteDia = new Date(`${renovarInicio.value}T00:00:00Z`);
        siguienteDia.setUTCDate(siguienteDia.getUTCDate() + 1);
        renovarFin.min = siguienteDia.toISOString().slice(0, 10);
        if (renovarFin.value && renovarFin.value < renovarFin.min) renovarFin.value = '';
    });

    renovarArchivos?.addEventListener('change', () => {
        if (renovarArchivos.files.length > 5) {
            renovarArchivos.value = '';
            mostrarToast('Puedes adjuntar máximo 5 archivos PDF.', 'error');
        }
    });

    renovarForm?.addEventListener('submit', async (event) => {
        event.preventDefault();
        if (!renovarSubmit) return;
        if (!renovarForm.checkValidity()) {
            renovarForm.reportValidity();
            return;
        }

        renovarSubmit.disabled = true;
        renovarSubmit.textContent = 'Procesando...';
        try {
            const response = await fetch(renovarForm.action, {
                method: 'POST',
                headers: {
                    Accept: 'application/json',
                    'X-Requested-With': 'XMLHttpRequest',
                    [document.querySelector('meta[name="_csrf_header"]')?.content || 'X-CSRF-TOKEN']:
                        document.querySelector('meta[name="_csrf"]')?.content || ''
                },
                body: new FormData(renovarForm)
            });
            if (!response.ok) throw new Error(await obtenerMensajeError(response, 'No fue posible renovar la póliza.'));
            cerrar(renovarDialog);
            mostrarToast('Póliza renovada correctamente.', 'success');
            setTimeout(() => location.reload(), 900);
        } catch (error) {
            mostrarToast(error.message || 'No fue posible renovar la póliza.', 'error');
        } finally {
            renovarSubmit.disabled = false;
            renovarSubmit.textContent = 'Guardar renovación';
        }
    });

    const prepararNueva = () => {
        limpiarAuditoriaRenovaciones();
        form?.reset();
        restablecerProveedorPoliza();
        reiniciarDetallesPrima();
        configurarEdicionDetallesPrima(true);
        if (form && createAction) form.setAttribute('action', createAction);
        if (formTitle) formTitle.textContent = 'Nueva Póliza';
        if (formDescription) formDescription.textContent = 'Registra la información principal del contrato.';
        form?.querySelectorAll('input, select, textarea').forEach((control) => {
            control.disabled = false;
        });
        if (eliminarArchivoInput) eliminarArchivoInput.value = 'false';
        if (archivoActual) archivoActual.style.display = 'none';
        if (enlaceArchivoActual) {
            enlaceArchivoActual.hidden = true;
            enlaceArchivoActual.href = '#';
        }
        if (archivoInput) {
            archivoInput.disabled = false;
            archivoInput.required = true;
            archivoInput.style.display = '';
        }
        configurarSedesParaNuevaPoliza();
        actualizarAuditoria({
            creador: form?.dataset.creadorActual,
            aprobador: 'Sin aprobación',
            sede: form?.dataset.sedeActual,
            fechaAprobacion: 'Sin aprobación',
            terminadoPor: 'Sin terminación',
            fechaTerminacion: 'Sin terminación',
            motivoTerminacion: 'Sin motivo de terminación'
        });
        mostrarAuditoriaTerminacion(false);
        if (auditoria.activador) auditoria.activador.textContent = 'N/A';
        if (auditoria.anulador) auditoria.anulador.textContent = 'N/A';
        if (auditoria.fechaAnulacion) auditoria.fechaAnulacion.textContent = 'N/A';
        mostrarAuditoriaAnulacion(false);
        if (saveButton) {
            saveButton.textContent = 'Guardar Póliza';
            saveButton.disabled = false;
            saveButton.style.display = '';
        }
    };

    const prepararExistente = async (button) => {
        const esBorrador = button.dataset.estado === 'BORRADOR';
        const esSuperAdministrador = rolUsuarioActual === 'SUPERADMINISTRADOR';
        const esCreador = button.dataset.creadorId === usuarioActualId;
        const sedePolizaId = button.dataset.sedeId || button.closest('tr[data-poliza-id]')?.dataset.sedeId || '';
        const esSedePrincipal = sedePolizaId === sedeActualId;
        const puedeEditar = esBorrador && (esSuperAdministrador || esCreador || esSedePrincipal);
        const campos = form?.elements;
        if (!form || !campos) return;

        form.setAttribute('action', button.dataset.url || createAction);
        campos.fechaCreacion.value = button.dataset.fecha || '';
        campos.fechaVencimiento.value = button.dataset.fechaVencimiento || '';
        campos.numeroContrato.value = button.dataset.contrato || '';
        campos.idProveedor.value = button.dataset.proveedor || '';
        detalle.numeroPoliza.textContent = button.dataset.numeroPoliza || 'Sin número';
        mostrarTarjetaProveedor({
            idProv: button.dataset.proveedor || '',
            nombre: button.dataset.proveedorNombre || '',
            nit: button.dataset.proveedorNit || '',
            ciudad: button.dataset.proveedorCiudad || '',
            direccion: button.dataset.proveedorDireccion || '',
            telefono: button.dataset.proveedorTelefono || '',
            correo: button.dataset.proveedorCorreo || ''
        });
        campos.cliente.value = button.dataset.cliente || '';
        campos.valorPrima.value = button.dataset.prima || '';
        campos.valorContrato.value = button.dataset.valorContrato || '';
        const campoContratoVisible = form.querySelector('[data-money-field="valorContrato"]');
        if (campoContratoVisible) campoContratoVisible.value = formatearPesos(campos.valorContrato.value);
        try {
            const detallesUrl = button.dataset.url.replace(/\/editar$/, '/detalles-prima');
            const response = await fetch(detallesUrl, { headers: { Accept: 'application/json' } });
            const detalles = response.ok ? await response.json() : [];
            reiniciarDetallesPrima(detalles.length ? detalles : [{ valor: button.dataset.prima || '', descripcion: 'Prima' }]);
        } catch (error) {
            reiniciarDetallesPrima([{ valor: button.dataset.prima || '', descripcion: 'Prima' }]);
        }
        configurarEdicionDetallesPrima(puedeEditar);
        campos.descripcion.value = button.dataset.descripcion || '';
        campos.contratoAdjunto.value = '';
        if (eliminarArchivoInput) eliminarArchivoInput.value = 'false';
        actualizarAuditoria({
            creador: button.dataset.creador,
            aprobador: button.dataset.aprobador,
            sede: button.dataset.sede,
            fechaAprobacion: button.dataset.fechaAprobacion,
            terminadoPor: button.dataset.usuarioTerminacion,
            fechaTerminacion: button.dataset.fechaTerminacion,
            motivoTerminacion: button.dataset.motivoTerminacion
        });
        mostrarAuditoriaTerminacion(button.dataset.estado === 'TERMINADA');
        if (auditoria.activador) auditoria.activador.textContent = button.dataset.activador || 'N/A';
        if (auditoria.anulador) auditoria.anulador.textContent = button.dataset.anulador || 'N/A';
        if (auditoria.fechaAnulacion) auditoria.fechaAnulacion.textContent = button.dataset.fechaAnulacion || 'N/A';
        mostrarAuditoriaAnulacion(button.dataset.estado === 'ANULADA');

        const nombreArchivo = button.dataset.archivoNombre || '';
        if (archivoActual) archivoActual.style.display = nombreArchivo ? '' : 'none';
        if (archivoInput) archivoInput.style.display = nombreArchivo ? 'none' : '';
        if (nombreArchivoActual) nombreArchivoActual.textContent = nombreArchivo || 'Sin archivo adjunto';
        if (enlaceArchivoActual) {
            enlaceArchivoActual.href = button.dataset.urlContrato || '#';
            enlaceArchivoActual.hidden = !nombreArchivo;
        }

        form.querySelectorAll('input, select, textarea').forEach((control) => {
            control.disabled = !puedeEditar;
        });
        if (cambiarProveedorButton) cambiarProveedorButton.disabled = !puedeEditar;

        const checkboxes = form.querySelectorAll('input[name="idsSedes"]');
        checkboxes.forEach((checkbox) => {
            checkbox.checked = false;
        });
        configurarSedesParaEdicion(puedeEditar);

        const idsSedes = (button.dataset.idsSedes || '')
            .split(',')
            .map((valor) => Number(valor.trim()))
            .filter((valor) => Number.isFinite(valor));
        idsSedes.forEach((idSede) => {
            const checkbox = form.querySelector(`input[name="idsSedes"][value="${idSede}"]`);
            if (checkbox) checkbox.checked = true;
        });
        actualizarResumenSedes();

        const filaPoliza = button.closest('tr[data-poliza-id]');
        if (filaPoliza?.dataset.polizaId) {
            cargarAuditoriaRenovaciones(filaPoliza.dataset.polizaId);
            try {
                const response = await fetch(`/polizas/${filaPoliza.dataset.polizaId}/aprobaciones-sedes`, {
                    headers: { Accept: 'application/json' }
                });
                if (!response.ok) {
                    throw new Error('No fue posible consultar las aprobaciones por sede.');
                }
                const aprobaciones = await response.json();
                checkboxes.forEach((checkbox) => {
                    checkbox.checked = aprobaciones.some((aprobacion) =>
                        Number(aprobacion.idSede) === Number(checkbox.value));
                });
                renderizarAprobacionesPorSede(aprobaciones);
            } catch (error) {
                const contenedor = document.getElementById('sede-aprobacion-resumen');
                if (contenedor) {
                    contenedor.innerHTML = '<span class="material-symbols">error</span><span>No fue posible cargar el estado de aprobación de las sedes.</span>';
                }
            }
        }

        if (archivoInput) {
            archivoInput.disabled = !puedeEditar || Boolean(nombreArchivo);
            archivoInput.required = puedeEditar && !nombreArchivo;
        }
        if (eliminarArchivoButton) eliminarArchivoButton.style.display = puedeEditar && nombreArchivo ? '' : 'none';
        if (formTitle) formTitle.textContent = puedeEditar ? 'Editar Póliza' : 'Ver Póliza';
        if (formDescription) {
            formDescription.textContent = puedeEditar
                ? 'Actualiza los datos de la póliza en borrador.'
                : 'Consulta la información registrada de la póliza.';
        }
        if (saveButton) {
            saveButton.textContent = 'Guardar cambios';
            saveButton.style.display = puedeEditar ? '' : 'none';
        }
        abrir(dialog);
        if (esBorrador && !puedeEditar) {
            mostrarToast(
                'Esta póliza no es suya. Por tanto, no puede editarla.',
                'info'
            );
        }
    };

    openButton?.addEventListener('click', () => {
        prepararNueva();
        abrir(dialog);
    });

    const cerrarFormulario = () => {
        cerrar(dialog);
        prepararNueva();
    };
    closeButton?.addEventListener('click', cerrarFormulario);
    cancelButton?.addEventListener('click', cerrarFormulario);
    eliminarArchivoButton?.addEventListener('click', () => {
        if (eliminarArchivoInput) eliminarArchivoInput.value = 'true';
        if (archivoActual) archivoActual.style.display = 'none';
        if (enlaceArchivoActual) enlaceArchivoActual.hidden = true;
        if (archivoInput) {
            archivoInput.disabled = false;
            archivoInput.required = true;
            archivoInput.style.display = '';
            archivoInput.focus();
        }
        mostrarToast('Archivo actual marcado para eliminar. Ahora puedes adjuntar otro PDF.', 'info');
    });
    form?.addEventListener('submit', async (event) => {
        event.preventDefault();
        if (!saveButton) return;
        if (!proveedorId.value) {
            mostrarToast('Selecciona un proveedor de la lista para continuar.', 'error');
            proveedorSearchContainer.hidden = false;
            proveedorCard.hidden = true;
            proveedorSearch.required = true;
            proveedorSearch.focus();
            return;
        }
        if (!form.checkValidity()) {
            form.reportValidity();
            return;
        }

        saveButton.disabled = true;
        saveButton.textContent = 'Procesando...';

        try {
            const response = await fetch(form.action, {
                method: 'POST',
                headers: {
                    'Accept': 'application/json',
                    'X-Requested-With': 'XMLHttpRequest',
                    [document.querySelector('meta[name="_csrf_header"]')?.content || 'X-CSRF-TOKEN']:
                        document.querySelector('meta[name="_csrf"]')?.content || ''
                },
                body: new FormData(form)
            });

            if (!response.ok) {
                throw new Error(await obtenerMensajeError(
                    response,
                    'No fue posible guardar la póliza.'
                ));
            }

            const esCreacion = form.action === createAction;
            cerrar(dialog);
            if (esCreacion) prepararNueva();
            mostrarToast(
                esCreacion
                    ? 'Póliza creada correctamente.'
                    : 'Póliza actualizada correctamente.',
                'success'
            );
            setTimeout(() => location.reload(), 1200);
        } catch (error) {
            mostrarToast(error.message || 'Ocurrió un error al conectar con el servidor.', 'error');
        } finally {
            saveButton.disabled = false;
            saveButton.textContent = form.action === createAction ? 'Guardar Póliza' : 'Guardar cambios';
        }
    });

    const detalle = {
        contrato: document.getElementById('detalle-contrato'),
        fecha: document.getElementById('detalle-fecha'),
        numeroPoliza: document.getElementById('detalle-numero-poliza'),
        proveedor: document.getElementById('detalle-proveedor'),
        cliente: document.getElementById('detalle-cliente'),
        creador: document.getElementById('detalle-creador'),
        prima: document.getElementById('detalle-prima'),
        valorContrato: document.getElementById('detalle-valor-contrato'),
        estado: document.getElementById('detalle-estado'),
        descripcion: document.getElementById('detalle-descripcion')
    };
    document.querySelectorAll('.js-ver-poliza').forEach((button) => {
        button.addEventListener('click', () => {
            prepararExistente(button);
        });
    });
    document.querySelectorAll('.js-cerrar-detalle').forEach((button) => {
        button.addEventListener('click', () => cerrar(detailDialog));
    });

    document.querySelectorAll('.js-confirm-action').forEach((actionForm) => {
        actionForm.addEventListener('submit', async (event) => {
            event.preventDefault();
            const esAprobacion = actionForm.action.endsWith('/aprobar');
            const confirmar = await mostrarConfirmacion(
                actionForm.dataset.confirm || '¿Confirmar esta acción?',
                esAprobacion ? '¿Aprobar póliza?' : '¿Anular póliza?',
                '',
                esAprobacion ? 'info' : 'warning'
            );
            if (confirmar) actionForm.submit();
        });
    });

    [dialog, detailDialog, activarDialog, terminarDialog, renovarDialog].forEach((modal) => modal?.addEventListener('click', (event) => {
        if (event.target === modal) cerrar(modal);
    }));
});
