// codigo acciones del MODAL ##########################################################333

document.addEventListener("DOMContentLoaded", () => {

    const modal = document.getElementById("modal-orden");
    const btnCerrar = document.getElementById("btn-cerrar-modal");
    const btnAbrir = document.getElementById("btn-nueva-orden");

    // Abrir modal
    if (btnAbrir) {
        btnAbrir.addEventListener("click", () => {
            modal.classList.add("active");
            document.body.style.overflow = "hidden";
        });
    }

    // Cerrar con la X
    if (btnCerrar) {
        btnCerrar.addEventListener("click", cerrarModal);
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
        document.body.style.overflow = "auto";
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
            <input type="text" class="iva-total input-control td-input" disabled >
            </td>

        <td>
            <input type="text" class="valor-total input-control td-input" disabled >
        </td>

        <td>
            <button type="button" class="btn-icon delete">
                <i class="fas fa-trash"></i>
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

    const valorIva = valorTotal * (ivaPorcentaje/100);

 
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
    const numero = parseFloat(texto.replace(/[^0-9.-]+/g, ""));
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
    const ordenDTO = {
        fecha: fechaInput,
        nitProv: document.getElementById('prov-nit')?.value || 'S/N',
        nombreProv: document.getElementById('prov-nombre')?.value || 'Sin Proveedor',
        ciudadProv: document.getElementById('prov-ciudad')?.value || 'N/A',
        direccionProv: document.getElementById('prov-direccion')?.value || 'N/A',
        telefonoProv: document.getElementById('prov-telefono')?.value || 'N/A',
        correoProv: document.getElementById('prov-email')?.value || 'N/A',
        // ⚠️ CORRECCIÓN 2: Acotar el textarea al modal
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
            const vTotal = (vUnitario * cantidad) + vIva;

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
        const response = await fetch('/api/ordenes/guardar', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(ordenDTO)
        });

        if (response.ok) {
            const blob = await response.blob();
            const url = window.URL.createObjectURL(blob);
            const a = document.createElement('a');
            a.href = url;

            // Extraer nombre del archivo del header o asignar uno por defecto
            const disposition = response.headers.get('Content-Disposition');
            let filename = 'Orden_Compra.pdf';
            if (disposition && disposition.includes('filename=')) {
                filename = disposition.split('filename=')[1].replace(/["']/g, '');
            }
            a.download = filename;

            document.body.appendChild(a);
            a.click();
            a.remove();
            
            // ⚠️ CORRECCIÓN 4: Liberar memoria del Blob
            window.URL.revokeObjectURL(url);
            
            alert('Orden guardada y PDF generado exitosamente.');

            if (typeof cerrarModal === 'function') {
                cerrarModal();
            } else {
                const modal = document.getElementById('modal-orden');
                if (modal) modal.style.display = 'none';
            }

            // Actualizar la vista para reflejar la nueva orden en la tabla
            location.reload();

        } else {
            const errText = await response.text();
            console.error("Error servidor:", errText);
            alert('Error al guardar la orden de compra.');
        }
    } catch (error) {
        console.error('Error de red/servidor:', error);
        alert('Ocurrió un error al conectar con el servidor.');
    } finally {
        // Reactivar el botón al finalizar la operación
        if (btnGuardar) {
            btnGuardar.disabled = false;
            btnGuardar.innerHTML = '<i class="fas fa-save"></i> Guardar y Generar PDF';
        }
    }
}