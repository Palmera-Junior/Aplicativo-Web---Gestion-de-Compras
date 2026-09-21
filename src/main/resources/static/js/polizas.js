function mostrarToast(mensaje, tipo = 'success') {
    if (!mensaje) return;

    let toast = document.getElementById('toast-root');
    if (!toast) {
        toast = document.createElement('div');
        toast.id = 'toast-root';
        document.body.appendChild(toast);
    }

    const item = document.createElement('div');
    item.className = `toast-item toast-${tipo}`;
    item.textContent = mensaje;
    toast.appendChild(item);

    setTimeout(() => {
        item.classList.add('toast-hide');
        setTimeout(() => item.remove(), 400);
    }, 3000);
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

document.addEventListener('DOMContentLoaded', () => {
    const dialog = document.getElementById('poliza-modal');
    const detailDialog = document.getElementById('poliza-detalle-modal');
    const activarDialog = document.getElementById('poliza-activar-modal');
    const activarForm = document.getElementById('poliza-activar-form');
    const activarFecha = document.getElementById('poliza-activar-fecha');
    const activarArchivo = document.getElementById('poliza-fisica-input');
    const openButton = document.getElementById('btn-nueva-poliza');
    const closeButton = document.querySelector('#poliza-modal .modal-close');
    const cancelButton = document.querySelector('#poliza-modal .btn-cancel');
    const saveButton = document.getElementById('btn-guardar-poliza');
    const form = document.getElementById('poliza-form');
    const archivoInput = form?.elements.contratoAdjunto;
    const archivoActual = document.getElementById('poliza-archivo-actual');
    const nombreArchivoActual = document.getElementById('poliza-archivo-nombre');
    const eliminarArchivoButton = document.getElementById('btn-eliminar-archivo-poliza');
    const eliminarArchivoInput = form?.elements.eliminarContratoAdjunto;
    const formTitle = document.getElementById('poliza-modal-title');
    const formDescription = document.getElementById('poliza-modal-description');
    const createAction = form?.getAttribute('action');
    const fechaDesde = document.getElementById('poliza-fecha-desde');
    const fechaHasta = document.getElementById('poliza-fecha-hasta');
    const estadoSelect = document.querySelector('.polizas-filters .estado-select');
    const auditoria = {
        creador: document.getElementById('poliza-creado-por'),
        aprobador: document.getElementById('poliza-aprobado-por'),
        sede: document.getElementById('poliza-sede'),
        fechaAprobacion: document.getElementById('poliza-fecha-aprobacion')
    };
    let scrollOriginal = null;

    const actualizarAuditoria = ({ creador, aprobador, sede, fechaAprobacion }) => {
        if (auditoria.creador) auditoria.creador.textContent = creador || 'Sin información';
        if (auditoria.aprobador) auditoria.aprobador.textContent = aprobador || 'Sin aprobación';
        if (auditoria.sede) auditoria.sede.textContent = sede || 'Sin información';
        if (auditoria.fechaAprobacion) auditoria.fechaAprobacion.textContent = fechaAprobacion || 'Sin aprobación';
    };

    document.querySelectorAll('.poliza-money').forEach((celda) => {
        celda.textContent = formatearPesos(celda.textContent.trim());
    });
    document.querySelectorAll('.poliza-money-input').forEach(configurarCampoMoneda);

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
        if (!dialog?.open && !detailDialog?.open && !activarDialog?.open) restaurarScroll();
    };

    document.querySelectorAll('.js-activar-poliza').forEach((button) => {
        button.addEventListener('click', () => {
            activarForm?.setAttribute('action', button.dataset.url || '');
            activarArchivo?.form?.reset();
            if (activarFecha) {
                activarFecha.value = button.dataset.fechaVencimiento || '';
                activarFecha.min = new Date().toISOString().slice(0, 10);
            }
            abrir(activarDialog);
        });
    });

    document.querySelectorAll('.js-cerrar-activar').forEach((button) => {
        button.addEventListener('click', () => cerrar(activarDialog));
    });

    const prepararNueva = () => {
        form?.reset();
        if (form && createAction) form.setAttribute('action', createAction);
        if (formTitle) formTitle.textContent = 'Nueva Póliza';
        if (formDescription) formDescription.textContent = 'Registra la información principal del contrato.';
        form?.querySelectorAll('input, select, textarea').forEach((control) => {
            control.disabled = false;
        });
        if (eliminarArchivoInput) eliminarArchivoInput.value = 'false';
        if (archivoActual) archivoActual.style.display = 'none';
        if (archivoInput) {
            archivoInput.disabled = false;
            archivoInput.required = true;
        }
        actualizarAuditoria({
            creador: form?.dataset.creadorActual,
            aprobador: 'Sin aprobación',
            sede: form?.dataset.sedeActual,
            fechaAprobacion: 'Sin aprobación'
        });
        if (saveButton) {
            saveButton.textContent = 'Guardar Póliza';
            saveButton.disabled = false;
            saveButton.style.display = '';
        }
    };

    const prepararExistente = (button) => {
        const esBorrador = button.dataset.estado === 'BORRADOR';
        const campos = form?.elements;
        if (!form || !campos) return;

        form.setAttribute('action', button.dataset.url || createAction);
        campos.fechaCreacion.value = button.dataset.fecha || '';
        campos.fechaVencimiento.value = button.dataset.fechaVencimiento || '';
        campos.numeroContrato.value = button.dataset.contrato || '';
        campos.idProveedor.value = button.dataset.proveedor || '';
        campos.cliente.value = button.dataset.cliente || '';
        campos.valorPrima.value = button.dataset.prima || '';
        campos.valorContrato.value = button.dataset.valorContrato || '';
        const campoPrimaVisible = form.querySelector('[data-money-field="valorPrima"]');
        const campoContratoVisible = form.querySelector('[data-money-field="valorContrato"]');
        if (campoPrimaVisible) campoPrimaVisible.value = formatearPesos(campos.valorPrima.value);
        if (campoContratoVisible) campoContratoVisible.value = formatearPesos(campos.valorContrato.value);
        campos.descripcion.value = button.dataset.descripcion || '';
        campos.contratoAdjunto.value = '';
        if (eliminarArchivoInput) eliminarArchivoInput.value = 'false';
        actualizarAuditoria({
            creador: button.dataset.creador,
            aprobador: button.dataset.aprobador,
            sede: button.dataset.sede,
            fechaAprobacion: button.dataset.fechaAprobacion
        });

        const nombreArchivo = button.dataset.archivoNombre || '';
        if (archivoActual && nombreArchivo) archivoActual.style.display = '';
        if (nombreArchivoActual) nombreArchivoActual.textContent = nombreArchivo || 'Sin archivo adjunto';

        form.querySelectorAll('input, select, textarea').forEach((control) => {
            control.disabled = !esBorrador;
        });
        if (archivoInput) {
            archivoInput.disabled = !esBorrador || Boolean(nombreArchivo);
            archivoInput.required = esBorrador && !nombreArchivo;
        }
        if (eliminarArchivoButton) eliminarArchivoButton.style.display = esBorrador && nombreArchivo ? '' : 'none';
        if (formTitle) formTitle.textContent = esBorrador ? 'Editar Póliza' : 'Ver Póliza';
        if (formDescription) {
            formDescription.textContent = esBorrador
                ? 'Actualiza los datos de la póliza en borrador.'
                : 'Consulta la información registrada de la póliza.';
        }
        if (saveButton) {
            saveButton.textContent = 'Guardar cambios';
            saveButton.style.display = esBorrador ? '' : 'none';
        }
        abrir(dialog);
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
        if (archivoInput) {
            archivoInput.disabled = false;
            archivoInput.required = true;
            archivoInput.focus();
        }
        mostrarToast('Archivo actual marcado para eliminar. Ahora puedes adjuntar otro PDF.', 'info');
    });
    form?.addEventListener('submit', async (event) => {
        event.preventDefault();
        if (!saveButton) return;
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

    [dialog, detailDialog, activarDialog].forEach((modal) => modal?.addEventListener('click', (event) => {
        if (event.target === modal) cerrar(modal);
    }));
});
