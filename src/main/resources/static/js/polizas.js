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

document.addEventListener('DOMContentLoaded', () => {
    const dialog = document.getElementById('poliza-modal');
    const detailDialog = document.getElementById('poliza-detalle-modal');
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
    let scrollOriginal = null;

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
        if (!dialog?.open && !detailDialog?.open) restaurarScroll();
    };

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
        if (archivoInput) archivoInput.disabled = false;
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
        campos.descripcion.value = button.dataset.descripcion || '';
        campos.contratoAdjunto.value = '';
        if (eliminarArchivoInput) eliminarArchivoInput.value = 'false';

        const nombreArchivo = button.dataset.archivoNombre || '';
        if (archivoActual && nombreArchivo) archivoActual.style.display = '';
        if (nombreArchivoActual) nombreArchivoActual.textContent = nombreArchivo || 'Sin archivo adjunto';

        form.querySelectorAll('input, select, textarea').forEach((control) => {
            control.disabled = !esBorrador;
        });
        if (archivoInput) archivoInput.disabled = !esBorrador || Boolean(nombreArchivo);
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
            archivoInput.focus();
        }
        mostrarToast('Archivo actual marcado para eliminar. Ahora puedes adjuntar otro PDF.', 'info');
    });
    form?.addEventListener('submit', async (event) => {
        event.preventDefault();
        if (!saveButton) return;

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

            cerrar(dialog);
            mostrarToast(
                form.action === createAction
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

    [dialog, detailDialog].forEach((modal) => modal?.addEventListener('click', (event) => {
        if (event.target === modal) cerrar(modal);
    }));
});
