// codigo acciones del MODAL ##########################################################333

document.addEventListener("DOMContentLoaded", () => {

    const modal = document.getElementById("modal-orden");
    const btnCerrar = document.getElementById("btn-cerrar-modal");
    const btnAbrir = document.getElementById("btn-nueva-orden");

    // Abrir modal
    if (btnAbrir) {
        btnAbrir.addEventListener("click", () => {
            ordenIdActual = null;
            resetFormularioOrden();
            modal.classList.add("active");
            modal.style.display = 'flex';
            document.body.style.overflow = "hidden";
        });
    }

    // Cerrar con la X
    if (btnCerrar) {
        btnCerrar.addEventListener("click", cerrarModal);
    }

    // Botón cancelar dentro del modal
    const btnCancelar = document.getElementById('btn-cancelar');
    if (btnCancelar) {
        btnCancelar.addEventListener('click', cerrarModal);
    }

    // Cerrar al hacer click sobre el fondo oscuro
    modal.addEventListener("click", (e) => {
        if (e.target === modal) {
            cerrarModal();
        }
    });

    // Cerrar con ESC
    document.addEventListener("keydown", (e) => {
        if (e.key === "Escape") {
            cerrarModal();
        }
    });

    function cerrarModal() {
        modal.classList.remove("active");
        modal.style.display = "none";
        document.body.style.overflow = "auto";
        ordenIdActual = null;
    }
});

let ordenIdActual = null;

function resetFormularioOrden() {
    const fechaInput = document.querySelector('#modal-orden input[type="date"]');
    if (fechaInput) {
        fechaInput.value = '';
        fechaInput.disabled = false;
    }

    const selectCentroCosto = document.getElementById('select-centro-costo');
    if (selectCentroCosto) {
        selectCentroCosto.value = '';
        selectCentroCosto.disabled = false;
    }

    const camposProveedor = [
        document.getElementById('prov-nit'),
        document.getElementById('prov-nombre'),
        document.getElementById('prov-ciudad'),
        document.getElementById('prov-direccion'),
        document.getElementById('prov-telefono'),
        document.getElementById('prov-email')
    ];

    camposProveedor.forEach(campo => {
        if (campo) {
            campo.value = '';
            campo.disabled = false;
        }
    });

    const observaciones = document.querySelector('#modal-orden textarea');
    if (observaciones) {
        observaciones.value = '';
        observaciones.disabled = false;
    }

    const tbody = document.getElementById('tbody-productos');
    if (tbody) {
        tbody.innerHTML = '';
        if (ordenIdActual === null) {
            agregarFila();
        }
    }

    document.getElementById('subtotal-general').textContent = '$ 0.00';
    document.getElementById('iva-general').textContent = '$ 0.00';
    document.getElementById('descuento-general').textContent = '$ 0.00';
    document.getElementById('total-general').textContent = '$ 0.00';

    const chkDescuento = document.getElementById('activar-descuento');
    const inputDescuento = document.getElementById('input-descuento');
    if (chkDescuento) {
        chkDescuento.checked = false;
    }
    if (inputDescuento) {
        inputDescuento.value = 0;
        inputDescuento.style.display = 'none';
    }
    const descuentoGeneral = document.getElementById('descuento-general');
    if (descuentoGeneral) {
        descuentoGeneral.style.display = 'inline';
    }

    const ordenNumero = document.getElementById('orden-numero');
    const ordenEstado = document.getElementById('orden-estado');
    const ordenAprobadoPor = document.getElementById('orden-aprobado-por');
    const ordenFechaAprobacion = document.getElementById('orden-fecha-aprobacion');
    const ordenRecibidoPor = document.getElementById('orden-recibido-por');
    const ordenFechaRecibido = document.getElementById('orden-fecha-recibido');
    const modalOrdenTitle = document.getElementById('modal-orden-title');

    if (ordenNumero) { ordenNumero.value = ''; }
    if (ordenEstado) { ordenEstado.value = ''; }
    if (ordenAprobadoPor) { ordenAprobadoPor.textContent = ''; }
    if (ordenFechaAprobacion) { ordenFechaAprobacion.textContent = ''; }
    if (ordenRecibidoPor) { ordenRecibidoPor.textContent = ''; }
    if (ordenFechaRecibido) { ordenFechaRecibido.textContent = ''; }
    if (modalOrdenTitle) { modalOrdenTitle.textContent = 'Crear Nueva Orden de Compra'; }

    setModalOrdenModoVista(false);
}

function setModalOrdenModoVista(esVista) {
    const controles = document.querySelectorAll('#modal-orden input, #modal-orden textarea, #modal-orden select');
    controles.forEach(control => {
        if (control.id === 'btn-cerrar-modal') {
            return;
        }
        if (control.type === 'button' || control.type === 'submit' || control.type === 'reset') {
            return;
        }
        control.disabled = esVista;
    });

    const btnAgregarFila = document.getElementById('btn-agregar-fila');
    const btnGuardar = document.getElementById('btn-guardar-pdf');
    const botonesEliminar = document.querySelectorAll('#tbody-productos .btn-icon.delete');

    if (btnAgregarFila) {
        btnAgregarFila.disabled = esVista;
        btnAgregarFila.style.display = esVista ? 'none' : 'inline-block';
    }
    if (btnGuardar) {
        btnGuardar.disabled = esVista;
        btnGuardar.style.display = esVista ? 'none' : 'inline-block';
    }
    botonesEliminar.forEach(boton => {
        boton.disabled = esVista;
        boton.style.display = esVista ? 'none' : 'inline-block';
    });
}

    //Codigo de los campos del proveedor ##################################################################33

    const selectProveedor = document.getElementById("select-proveedor");

    const nit = document.getElementById("prov-nit");
    const nombre = document.getElementById("prov-nombre");
    const ciudad = document.getElementById("prov-ciudad");
    const direccion = document.getElementById("prov-direccion");
    const telefono = document.getElementById("prov-telefono");
    const email = document.getElementById("prov-email");

    const campos = [
        nit,
        nombre,
        ciudad,
        direccion,
        telefono,
        email
    ];

    selectProveedor.addEventListener("change", function () {

        const opcion = this.options[this.selectedIndex];

        // Opción "Otro"
        if (this.value === "otro") {

            campos.forEach(campo => {
                campo.disabled = false;
                campo.value = "";
            });

            return;
        }

        // Ningún proveedor seleccionado
        if (this.value === "") {

            campos.forEach(campo => {
                campo.disabled = true;
                campo.value = "";
            });

            return;
        }

        // Proveedor existente
        nit.value = opcion.dataset.nit || "";
        nombre.value = opcion.dataset.nombre || "";
        ciudad.value = opcion.dataset.ciudad || "";
        direccion.value = opcion.dataset.direccion || "";
        telefono.value = opcion.dataset.telefono || "";
        email.value = opcion.dataset.email || "";

        campos.forEach(campo => {
            campo.disabled = true;
        });

    });



// Codigo de los campos del producto ##################################################################

document.addEventListener("change", async function (e) {

    if (!e.target.classList.contains("codigo-producto")) {
        return;
    }

    const codigo = e.target.value.trim();

    const fila = e.target.closest("tr");

    const campoDescripcion = fila.querySelector(".descripcion-producto");
    const campoPresentacion = fila.querySelector(".presentacion-producto");

    if (!codigo) {
        campoDescripcion.value = "";
        campoPresentacion.value = "";
        return;
    }

    try {

        const response = await fetch(
            `/dashboard/producto?codigo=${encodeURIComponent(codigo)}`
        );

        // Si el servidor devuelve error o no encontró el producto
        if (!response.ok) {
            campoDescripcion.value = "";
            campoPresentacion.value = "";
            return;
        }

        const producto = await response.json();

        if (producto) {

            campoDescripcion.value =
                producto.descripcion || "";

            campoPresentacion.value =
                producto.presentacion || "";

        } else {

            campoDescripcion.value = "";
            campoPresentacion.value = "";

        }

    } catch (error) {

        console.error(error);

        campoDescripcion.value = "";
        campoPresentacion.value = "";

    }

});

// Codigo para boton de agregar fila ###############################################33333
const btnAgregarFila = document.getElementById("btn-agregar-fila");

btnAgregarFila.addEventListener("click", agregarFila);

function agregarFila() {

    const tbody = document.getElementById("tbody-productos");

    const nuevaFila = document.createElement("tr");

    nuevaFila.innerHTML = `
        <td>
            <input type="number" class="cantidad input-control td-input" value="1" min="0">
        </td>

        <td>
            <input type="text" class="codigo-producto input-control td-input" placeholder="PROD-01">
        </td>

        <td>
            <input type="text" class="descripcion-producto input-control td-input readonly">
        </td>
 
        <td>
            <input type="text" class="presentacion-producto input-control td-input readonly">
        </td>

        <td>
            <input type="number" class="valor-unitario input-control td-input" min="0" >
        </td>

        <td>
            <input type="number" class="iva-producto input-control td-input" min="0">
        </td>

        <td>
            <input type="text" class="iva-total input-control td-input" disabled readonly>
            </td>

        <td>
            <input type="text" class="valor-total input-control td-input" disabled readonly>
        </td>

        <td>
            <button type="button" class="btn-icon delete" aria-label="Eliminar fila">
                <span class="material-symbols">delete</span>
            </button>
        </td>
    `;

    tbody.appendChild(nuevaFila);
    calcularTotalFila(nuevaFila);
}

// funcion para eliminar fila #################################################3
document.addEventListener("click", function (e) {

    const botonEliminar = e.target.closest(".delete");

    if (!botonEliminar) {
        return;
    }

    const fila = botonEliminar.closest("tr");

    const tbody = document.getElementById("tbody-productos");

    if (tbody.rows.length > 1) {
        fila.remove();
    }
});

// codigo operaciones aritmeticas de cada fila #######################################
function formatearPesos(valor) {
    return new Intl.NumberFormat("es-CO", {
        style: "currency",
        currency: "COP",
        minimumFractionDigits: 0,
        maximumFractionDigits: 0
    }).format(valor);
}

function calcularTotalFila(fila) {

    const cantidad = parseFloat(
        fila.querySelector(".cantidad").value
    ) || 0;

    const valorUnitario = parseFloat(
        fila.querySelector(".valor-unitario").value
    ) || 0;

    const ivaPorcentaje = parseFloat(
        fila.querySelector(".iva-producto").value
    ) || 0;

    const valorTotal = cantidad * valorUnitario;

    const valorIva = valorTotal * (ivaPorcentaje / 100);


    const campoValorIva =
        fila.querySelector(".iva-total");

    const campoValorTotal =
        fila.querySelector(".valor-total");

    campoValorIva.value =
        formatearPesos(valorIva);

    campoValorTotal.value =
        formatearPesos(valorTotal);

    // Guardar valor numérico oculto
    campoValorIva.dataset.valor = valorIva;
    campoValorTotal.dataset.valor = valorTotal;

    recalcularTotalesGenerales();
}
// cambios de valores en vivo y en directo

document.addEventListener("input", function (e) {

    if (
        e.target.classList.contains("cantidad") ||
        e.target.classList.contains("valor-unitario") ||
        e.target.classList.contains("iva-producto")
    ) {

        const fila = e.target.closest("tr");

        calcularTotalFila(fila);
    }

});

// Funciones para calculos generales de la orden de compra 
function recalcularTotalesGenerales() {

    let subtotal = 0;
    let totalIva = 0;

    document.querySelectorAll("#tbody-productos tr").forEach(fila => {

        const valorTotal =
            parseFloat(
                fila.querySelector(".valor-total")
                    ?.dataset.valor || 0
            );

        const valorIva =
            parseFloat(
                fila.querySelector(".iva-total")
                    ?.dataset.valor || 0
            );

        subtotal += valorTotal;
        totalIva += valorIva;
    });

    let descuento = 0;

    if (chkDescuento.checked) {
        descuento = parseFloat(inputDescuento.value) || 0;
    }

    const totalGeneral =
        subtotal + totalIva - descuento;


    document.getElementById("subtotal-general").textContent =
        formatearPesos(subtotal);

    document.getElementById("iva-general").textContent =
        formatearPesos(totalIva);

    document.getElementById("descuento-general").textContent =
        formatearPesos(descuento);

    document.getElementById("total-general").textContent =
        formatearPesos(totalGeneral);

    lblDescuento.textContent =
        formatearPesos(descuento);
}

// codigo para mostrar/ocultar descuento 
const chkDescuento = document.getElementById("activar-descuento");
const inputDescuento = document.getElementById("input-descuento");
const lblDescuento = document.getElementById("descuento-general");

chkDescuento.addEventListener("change", function () {

    if (this.checked) {

        lblDescuento.style.display = "none";
        inputDescuento.style.display = "inline-block";

    } else {

        inputDescuento.value = 0;
        inputDescuento.style.display = "none";
        lblDescuento.style.display = "inline";

        recalcularTotalesGenerales();
    }

});

inputDescuento.addEventListener("input", function () {
    recalcularTotalesGenerales();
});

// =========================================================================
// SECCIÓN: GUARDAR Y GENERAR PDF
// =========================================================================

// Escuchador de eventos (Asegurarse de envolverlo o verificar que el DOM esté listo)
document.addEventListener('DOMContentLoaded', () => {
    const btnGuardar = document.getElementById('btn-guardar-pdf');
    if (btnGuardar) {
        btnGuardar.addEventListener('click', (e) => {
            e.preventDefault();
            guardarYGenerarPdf();
        });
    }
});

function parsearMoneda(elementId) {
    const el = document.getElementById(elementId);
    if (!el) return 0;

    const texto = el.innerText || el.value || "0";

    const limpio = texto
        .replace(/\$/g, '')
        .replace(/\./g, '')
        .replace(/,/g, '.')
        .trim();

    const numero = parseFloat(limpio);

    return isNaN(numero) ? 0 : numero;
}

async function guardarYGenerarPdf() {
    // ⚠️ CORRECCIÓN 1: Seleccionar la fecha ESPECÍFICAMENTE dentro del modal
    const fechaInput = document.querySelector('#modal-orden input[type="date"]')?.value;

    if (!fechaInput) {
        alert("Por favor selecciona una fecha válida dentro del formulario.");
        return;
    }

    // 1. DTO de la Cabecera
    const selectCentroCosto = document.getElementById('select-centro-costo');
    const idCentroCosto = selectCentroCosto ? selectCentroCosto.value : '';

    // Validación: centro de costo obligatorio
    if (!idCentroCosto) {
        alert('El campo "Centro de Costo" es obligatorio. Por favor selecciona uno.');
        return;
    }

    const camposProveedor = [
        { id: 'prov-nit', label: 'NIT' },
        { id: 'prov-nombre', label: 'Nombre' },
        { id: 'prov-ciudad', label: 'Ciudad' },
        { id: 'prov-direccion', label: 'Dirección' },
        { id: 'prov-telefono', label: 'Teléfono' },
        { id: 'prov-email', label: 'Correo' }
    ];

    const proveedorIncompleto = camposProveedor.find(campo => {
        const valor = document.getElementById(campo.id)?.value?.trim() || '';
        return !valor;
    });

    if (proveedorIncompleto) {
        alert(`El campo "${proveedorIncompleto.label}" del proveedor es obligatorio.`);
        return;
    }

    const ordenDTO = {
        fecha: fechaInput,
        idCentroCosto: idCentroCosto ? parseInt(idCentroCosto) : null,
        nitProv: document.getElementById('prov-nit')?.value?.trim() || '',
        nombreProv: document.getElementById('prov-nombre')?.value?.trim() || '',
        ciudadProv: document.getElementById('prov-ciudad')?.value?.trim() || '',
        direccionProv: document.getElementById('prov-direccion')?.value?.trim() || '',
        telefonoProv: document.getElementById('prov-telefono')?.value?.trim() || '',
        correoProv: document.getElementById('prov-email')?.value?.trim() || '',
        //  CORRECCIÓN 2: Acotar el textarea al modal
        observaciones: document.querySelector('#modal-orden textarea')?.value || '',

        subTotal: parsearMoneda('subtotal-general'),
        ivaTotal: parsearMoneda('iva-general'),
        descuento: parsearMoneda('descuento-general'),
        total: parsearMoneda('total-general'),

        detalles: []
    };

    // 2. DTO de las Líneas de Producto
    const filas = document.querySelectorAll('#tbody-productos tr');
    filas.forEach(fila => {
        const inputs = fila.querySelectorAll('input');
        const cantidad = parseInt(inputs[0].value) || 0;
        const codigo = inputs[1].value.trim();

        if (codigo !== '' && cantidad > 0) {
            const vUnitario = parseFloat(inputs[4].value) || 0;
            const pctIva = parseFloat(inputs[5].value) || 0;
            const vIva = (vUnitario * cantidad) * (pctIva / 100);
            const vTotal = (vUnitario * cantidad);

            ordenDTO.detalles.push({
                cantidad: cantidad,
                codigoInventario: codigo,
                descripcion: inputs[2].value || 'Sin descripción',
                presentacion: inputs[3].value || 'Unidad',
                valorUnitario: vUnitario,
                ivaProducto: pctIva,
                valorIva: vIva,
                valorTotalLinea: vTotal
            });
        }
    });

    if (ordenDTO.detalles.length === 0) {
        alert("Debes agregar al menos un producto con código y cantidad válida.");
        return;
    }

    // ⚠️ CORRECCIÓN 3: Deshabilitar el botón para evitar duplicados por múltiple clic
    const btnGuardar = document.getElementById('btn-guardar-pdf');
    if (btnGuardar) {
        btnGuardar.disabled = true;
        btnGuardar.innerHTML = '<i class="fas fa-spinner fa-spin"></i> Guardando...';
    }

    // 3. Envío al Backend
    try {
        const url = ordenIdActual ? `/orden-compra/${ordenIdActual}` : '/orden-compra';
        const response = await fetch(url, {
            method: ordenIdActual ? 'PUT' : 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(ordenDTO)
        });

        if (response.ok) {
            alert('Orden guardada exitosamente.');
            if (typeof cerrarModal === 'function') {
                cerrarModal();
            } else {
                const modal = document.getElementById('modal-orden');
                if (modal) modal.style.display = 'none';
            }
            location.reload();
        } else {
            const errText = await response.text();
            console.error("Error servidor:", errText);
            alert(errText || 'Error al guardar la orden de compra.');
        }
    } catch (error) {
        console.error('Error de red/servidor:', error);
        alert('Ocurrió un error al conectar con el servidor.');
    } finally {
        // Reactivar el botón al finalizar la operación
        if (btnGuardar) {
            btnGuardar.disabled = false;
            btnGuardar.innerHTML = '<i class="fas fa-save"></i> Guardar';
        }
    }
}

// CONFIRMACION PARA APROBAR DOCUMENTO

document.addEventListener("click", async function (e) {

    const boton = e.target.closest(".approve");

    if (!boton) {
        return;
    }

    const idOrden = boton.dataset.id;

    const confirmar = confirm(
        "¿Está seguro de aprobar esta Orden de Compra?\n\n" +
        "Después de aprobarla no podrá modificar productos ni valores."
    );

    if (!confirmar) {
        return;
    }

    try {

        const response = await fetch(
            `/orden-compra/${idOrden}/aprobar`,
            {
                method: "PUT"
            }
        );

        if (!response.ok) {

            const mensaje = await response.text();


            throw new Error(mensaje);
        }

        alert("Orden aprobada correctamente");

        location.reload();

    } catch (error) {

        console.error(error);

        alert(error.message);
    }
});

// DESCARGAR PDF (solo para APROBADA/RECIBIDA)
document.addEventListener("click", async function (e) {
    const boton = e.target.closest(".pdf");
    if (!boton) return;

    const idOrden = boton.dataset.id;
    if (!idOrden) {
        alert('ID de orden no disponible para la descarga.');
        return;
    }

    try {
        const response = await fetch(`/orden-compra/${idOrden}/pdf`);
        if (!response.ok) {
            const txt = await response.text();
            throw new Error(txt || 'No se pudo descargar el PDF.');
        }

        const blob = await response.blob();
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        const disposition = response.headers.get('Content-Disposition');
        let filename = `Orden_Compra_${idOrden}.pdf`;
        if (disposition && disposition.includes('filename=')) {
            filename = disposition.split('filename=')[1].replace(/['"]/g, '');
        }
        a.download = filename;
        document.body.appendChild(a);
        a.click();
        a.remove();
        window.URL.revokeObjectURL(url);

    } catch (err) {
        console.error(err);
        alert(err.message || 'Error descargando PDF');
    }
});

// ==========================================
// RECEPCIÓN DE ORDEN DE COMPRA
// ==========================================

let idOrdenSeleccionada = null;


// ABRIR MODAL
document.addEventListener("click", function (e) {

    const boton = e.target.closest(".receive");

    if (!boton) {
        return;
    }

    idOrdenSeleccionada = boton.dataset.id;

    document.getElementById(
        "recepcion-numero-orden"
    ).textContent = boton.dataset.numeroOrden;

    document.getElementById(
        "recepcion-proveedor"
    ).textContent = boton.dataset.proveedor;

    document.getElementById(
        "recepcion-observacion"
    ).textContent = boton.dataset.observacionRecepcion;

    document.getElementById(
        "modal-recibir-oc"
    ).style.display = "flex";

});


// CANCELAR RECEPCIÓN
document
    .getElementById("btn-cancelar-recepcion")
    ?.addEventListener("click", function () {

        document.getElementById(
            "modal-recibir-oc"
        ).style.display = "none";

        limpiarFormularioRecepcion();

    });


// CONFIRMAR RECEPCIÓN
document
    .getElementById("btn-confirmar-recepcion")
    ?.addEventListener("click", async function () {

        const numeroFactura = document
            .getElementById("recepcion-numero-factura")
            .value
            .trim();

        const recibidoPor = document
            .getElementById("recepcion-recibido-por")
            .value
            .trim();

        const observacion = document
            .getElementById("recepcion-observacion")
            .value
            .trim();

        // ========================
        // VALIDACIONES
        // ========================

        if (!numeroFactura) {

            alert(
                "Debes ingresar el número de factura."
            );

            return;
        }

        if (numeroFactura.length < 3) {

            alert(
                "El número de factura parece inválido."
            );

            return;
        }

        if (!recibidoPor) {

            alert(
                "Debes indicar quién recibió el pedido."
            );

            return;
        }

        if (recibidoPor.length < 3) {

            alert(
                "El nombre del receptor es demasiado corto."
            );

            return;
        }

        // ========================
        // CONFIRMACIÓN
        // ========================

        const confirmar = confirm(

`Confirme los datos de recepción:

Factura:
${numeroFactura}

Recibido por:
${recibidoPor}

${observacion ? "Observación:\n" + observacion : ""}

La orden cambiará al estado RECIBIDA.

¿Desea continuar?`

        );

        if (!confirmar) {
            return;
        }

        const btn = document.getElementById(
            "btn-confirmar-recepcion"
        );

        try {

            btn.disabled = true;

            btn.innerHTML =
                '<i class="fas fa-spinner fa-spin"></i> Procesando...';

            const response = await fetch(
                `/orden-compra/${idOrdenSeleccionada}/recibir`,
                {
                    method: "PUT",
                    headers: {
                        "Content-Type": "application/json"
                    },
                    body: JSON.stringify({
                        numeroFactura,
                        recibidoPor,
                        observacionRecepcion: observacion
                    })
                }
            );

            if (!response.ok) {

                const mensaje =
                    await response.text();

                throw new Error(mensaje);
            }

            alert(
                "Recepción registrada correctamente."
            );

            limpiarFormularioRecepcion();

            document.getElementById(
                "modal-recibir-oc"
            ).style.display = "none";

            location.reload();

        } catch (error) {

            console.error(error);

            alert(
                error.message ||
                "No fue posible registrar la recepción."
            );

        } finally {

            btn.disabled = false;

            btn.innerHTML =
                "Registrar Recepción";
        }

    });


// LIMPIAR FORMULARIO
function limpiarFormularioRecepcion() {

    document.getElementById(
        "recepcion-numero-factura"
    ).value = "";

    document.getElementById(
        "recepcion-recibido-por"
    ).value = "";

    document.getElementById(
        "recepcion-observacion"
    ).value = "";

    document.getElementById(
        "recepcion-numero-orden"
    ).textContent = "";

    document.getElementById(
        "recepcion-proveedor"
    ).textContent = "";

    idOrdenSeleccionada = null;
}

// LISTENER BOTN VIEW 
document.addEventListener(
    "click",
    async function (e) {

        const boton =
            e.target.closest(".view");

        if (!boton) {
            return;
        }

        const idOrden =
            boton.dataset.id;

        try {

            const response =
                await fetch(
                    `/orden-compra/${idOrden}`
                );

            if (!response.ok) {

                throw new Error(
                    "No fue posible cargar la orden."
                );

            }

            const orden =
                await response.json();

            ordenIdActual = parseInt(idOrden, 10);
            cargarOrdenEnModal(
                orden
            );

            abrirModalOrden();

        } catch (error) {

            console.error(error);

            alert(error.message);

        }
    }
);

function abrirModalOrden() {

    document.getElementById(
        "modal-orden"
    ).style.display = "flex";

}

function cargarOrdenEnModal(
    orden
) {
    resetFormularioOrden();

    const esVista = orden.estado !== 'BORRADOR';
    const fechaInput = document.querySelector('#modal-orden input[type="date"]');
    if (fechaInput) {
        fechaInput.value = orden.fecha || '';
        fechaInput.disabled = esVista;
    }

    const selectCentroCosto = document.getElementById('select-centro-costo');
    if (selectCentroCosto) {
        selectCentroCosto.value = orden.idCentroCosto || '';
        selectCentroCosto.disabled = esVista;
    }

    document.getElementById(
        "prov-nit"
    ).value =
        orden.nitProv || "";

    document.getElementById(
        "prov-nombre"
    ).value =
        orden.nombreProv || "";

    document.getElementById(
        "prov-ciudad"
    ).value =
        orden.ciudadProv || "";

    document.getElementById(
        "prov-direccion"
    ).value =
        orden.direccionProv || "";

    document.getElementById(
        "prov-telefono"
    ).value =
        orden.telefonoProv || "";

    document.getElementById(
        "prov-email"
    ).value =
        orden.correoProv || "";

    const observaciones = document.querySelector('#modal-orden textarea');
    if (observaciones) {
        observaciones.value = orden.observaciones || "";
        observaciones.disabled = esVista;
    }

    const tbody = document.getElementById('tbody-productos');
    if (tbody) {
        tbody.innerHTML = '';

        if (orden.detalles && orden.detalles.length > 0) {
            orden.detalles.forEach(detalle => {
                const fila = document.createElement('tr');
                fila.innerHTML = `
                    <td>
                        <input type="number" class="cantidad input-control td-input" value="${detalle.cantidad || 0}" ${esVista ? 'disabled' : ''}>
                    </td>
                    <td>
                        <input type="text" class="codigo-producto input-control td-input" value="${detalle.codigoInventario || ''}" ${esVista ? 'disabled' : ''}>
                    </td>
                    <td>
                        <input type="text" class="descripcion-producto input-control td-input readonly" value="${detalle.descripcion || ''}" ${esVista ? 'disabled' : ''}>
                    </td>
                    <td>
                        <input type="text" class="presentacion-producto input-control td-input readonly" value="${detalle.presentacion || ''}" ${esVista ? 'disabled' : ''}>
                    </td>
                    <td>
                        <input type="number" class="valor-unitario input-control td-input" value="${detalle.valorUnitario || 0}" ${esVista ? 'disabled' : ''}>
                    </td>
                    <td>
                        <input type="number" class="iva-producto input-control td-input" value="${detalle.ivaProducto || 0}" ${esVista ? 'disabled' : ''}>
                    </td>
                    <td>
                        <input type="text" class="iva-total input-control td-input" value="${formatearPesos(Number(detalle.valorIva || 0))}" disabled readonly>
                    </td>
                    <td>
                        <input type="text" class="valor-total input-control td-input" value="${formatearPesos(Number(detalle.valorTotalLinea || 0))}" disabled readonly>
                    </td>
                    <td>
                        <button type="button" class="btn-icon delete" ${esVista ? 'disabled' : ''}>
                            <i class="fas fa-trash"></i>
                        </button>
                    </td>
                `;

                const campoIvaTotal = fila.querySelector('.iva-total');
                const campoValorTotal = fila.querySelector('.valor-total');
                if (campoIvaTotal) {
                    campoIvaTotal.dataset.valor = Number(detalle.valorIva || 0);
                }
                if (campoValorTotal) {
                    campoValorTotal.dataset.valor = Number(detalle.valorTotalLinea || 0);
                }

                tbody.appendChild(fila);
            });
        } else {
            const fila = document.createElement('tr');
            fila.innerHTML = `
                <td colspan="9" style="text-align: center; padding: 1rem;">No hay detalles registrados para esta orden.</td>
            `;
            tbody.appendChild(fila);
        }
    }

    document.getElementById('subtotal-general').textContent = formatearPesos(Number(orden.subTotal || 0));
    document.getElementById('iva-general').textContent = formatearPesos(Number(orden.ivaTotal || 0));
    document.getElementById('descuento-general').textContent = formatearPesos(Number(orden.descuento || 0));
    document.getElementById('total-general').textContent = formatearPesos(Number(orden.total || 0));

    const chkDescuento = document.getElementById('activar-descuento');
    const inputDescuento = document.getElementById('input-descuento');
    if (chkDescuento && inputDescuento) {
        if (Number(orden.descuento || 0) > 0) {
            chkDescuento.checked = true;
            inputDescuento.style.display = 'inline-block';
            inputDescuento.value = Number(orden.descuento || 0);
        } else {
            chkDescuento.checked = false;
            inputDescuento.style.display = 'none';
            inputDescuento.value = 0;
        }
        chkDescuento.disabled = esVista;
    }

    document.getElementById('orden-numero').value = orden.numeroOrden || '';
    document.getElementById('orden-estado').value = orden.estado || '';
    document.getElementById('orden-aprobado-por').textContent = orden.aprobadoPor || 'N/A';
    document.getElementById('orden-fecha-aprobacion').textContent = orden.fechaAprobacion || 'N/A';
    document.getElementById('orden-recibido-por').textContent = orden.recibidoPor || 'N/A';
    document.getElementById('orden-fecha-recibido').textContent = orden.fechaRecepcion || 'N/A';
    document.getElementById('modal-orden-title').textContent = esVista ? 'Ver Orden de Compra' : 'Editar Orden de Compra';

    setModalOrdenModoVista(esVista);
}