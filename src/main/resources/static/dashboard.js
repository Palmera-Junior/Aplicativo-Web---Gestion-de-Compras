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
            <input type="number" class="cantidad input-control td-input" value="1">
        </td>

        <td>
            <input type="text" class="codigo-producto input-control td-input" placeholder="Ej. PROD-01">
        </td>

        <td>
            <input type="text" class="descripcion-producto input-control td-input readonly">
        </td>

        <td>
            <input type="text" class="presentacion-producto input-control td-input readonly">
        </td>

        <td>
            <input type="number" class="valor-unitario input-control td-input" value="0">
        </td>

        <td>
            <input type="number" class="iva-producto input-control td-input" value="0">
        </td>

        <td>
            <input type="text" class="iva-total input-control td-input" disabled value="0">
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

    const subtotal = cantidad * valorUnitario;

    const valorIva = subtotal * (ivaPorcentaje / 100);

    const valorTotal = subtotal + valorIva;

    fila.querySelector(".iva-total").value =
        formatearPesos(valorTotal);
}

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