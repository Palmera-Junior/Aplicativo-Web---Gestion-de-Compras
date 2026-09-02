/**
 * =============================================================================
 * MÓDULO DE DASHBOARD PRINCIPAL (dashboard.js)
 * =============================================================================
 * Controla la lógica de interfaz de usuario para el panel principal de compras (/dashboard):
 * - Creación y edición de órdenes de compra con cálculo de subtotales, IVA, descuento y flete.
 * - Autocompletado reactivo de productos y sugerencia de presentaciones por fila.
 * - Aprobación, anulación y descarga de PDF de órdenes.
 * - Modal de recepción de pedidos con detección de diferencias de llegada vs solicitado.
 * - Modal de registro de facturación de proveedores con carga de evidencias (foto/PDF base64).
 * - Modal de auditoría para marcar correos fallidos como enviados manualmente.
 * - Endpoints consumidos: /orden-compra, /dashboard/producto, /dashboard/productos/buscar,
 *   /orden-compra/{id}/pdf, /orden-compra/{id}/recibir, /orden-compra/{id}/facturar,
 *   /orden-compra/{id}/aprobar, /orden-compra/{id}/anular, /orden-compra/{id}/detalles.
 * =============================================================================
 */

// ==========================================
// TOAST (mensaje temporal auto-ocultable)
// ==========================================
// reportClientError(mensaje, detalle)
// - Qué hace: Captura silenciosa de errores en cliente sin exponer lógica sensible en consola.
// - A dónde apunta: Ninguno (control interno).
function reportClientError(mensaje, detalle) {
    // No se exponen datos ni lógica del negocio en la consola del navegador.
    // Los errores se manejan en la UI con toasts y mensajes amigables.
    void mensaje;
    void detalle;
}


function mostrarToast(mensaje, tipo) {
    tipo = tipo || 'success';
    let toast = document.getElementById('toast-root');
    if (!toast) {
        toast = document.createElement('div');
        toast.id = 'toast-root';
        document.body.appendChild(toast);
    }
    const t = document.createElement('div');
    t.className = 'toast-item toast-' + tipo;
    t.textContent = mensaje;
    toast.appendChild(t);

    // Ocultar automáticamente a los 3 segundos
    setTimeout(() => {
        t.classList.add('toast-hide');
        setTimeout(() => t.remove(), 400);
    }, 3000);
}

document.addEventListener("DOMContentLoaded", () => {

    // Botón refrescar página
    const btnRefrescar = document.getElementById("btn-refrescar");
    if (btnRefrescar) {
        btnRefrescar.addEventListener("click", () => {
            window.location.reload();
        });
    }

    // Aplicar filtro por estado al hacer click/seleccionar una opción
    const estadoSelect = document.querySelector('.estado-select');
    if (estadoSelect) {
        estadoSelect.addEventListener('change', () => {
            const form = estadoSelect.closest('form');
            if (form) {
                // Enviar formulario para aplicar filtros (GET)
                form.submit();
            }
        });
        // También permitir aplicar si se hace click en la opción (algunos navegadores no disparan change hasta blur)
        estadoSelect.addEventListener('click', (e) => {
            // No forzar submit en cada click en el control desplegable, solo cuando cambia el valor
        });
    }

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

    // cerrarModal()
    // - Qué hace: Cierra el modal de creación/edición de ordenes, restaura el overflow del body
    //   y resetea el identificador `ordenIdActual`.
    // - Contexto: Declarada dentro del DOMContentLoaded; es usada por los handlers locales para
    //   cerrar el modal. Nota: no es global (está en el scope del listener DOMContentLoaded),
    //   por lo que llamadas externas deben comprobar su existencia.
    // - Endpoints: Ninguno (UI cliente).
    function cerrarModal() {
        modal.classList.remove("active");
        modal.style.display = "none";
        document.body.style.overflow = "auto";
        ordenIdActual = null;
    }
});

let ordenIdActual = null;

// resetFormularioOrden()
// - Qué hace: Resetea todos los campos del modal de orden (fecha, centro de costo, datos
//   de proveedor, observaciones, tabla de productos y totales). Si `ordenIdActual` es null
//   añade una fila inicial. También restablece controles de descuento y flete.
// - Uso: invocado al abrir un nuevo modal y antes de cargar una orden para edición.
// - Endpoints: Ninguno (manipulación del DOM).
function resetFormularioOrden() {
    const fechaInput = document.getElementById('fecha-orden');
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
    document.getElementById('flete-general').textContent = '$ 0.00';
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

    const chkFlete = document.getElementById('activar-flete');
    const inputFlete = document.getElementById('input-flete');
    if (chkFlete) {
        chkFlete.checked = false;
    }
    if (inputFlete) {
        inputFlete.value = 0;
        inputFlete.style.display = 'none';
    }
    const fleteGeneral = document.getElementById('flete-general');
    if (fleteGeneral) {
        fleteGeneral.style.display = 'inline';
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

// setModalOrdenModoVista(esVista)
// - Qué hace: Activa o desactiva (modo vista) los controles del modal de orden. Si
//   esVista === true, deshabilita inputs y botones de edición (oculta botones de agregar
//   fila y guardar), dejando el modal en modo lectura.
// - Uso: llamado desde cargarOrdenEnModal() para bloquear la edición cuando la orden no
//   está en estado BORRADOR.
// - Endpoints: Ninguno (interacción de UI).
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

    // Permitir edición de los datos del proveedor existente en el modal
    campos.forEach(campo => {
        campo.disabled = false;
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
    const campoValorUnitario = fila.querySelector(".valor-unitario");

    if (!codigo) {
        campoDescripcion.value = "";
        if (campoPresentacion) campoPresentacion.value = "";
        campoValorUnitario.value = "";
        return;
    }

    try {

        const response = await fetch(
            `/dashboard/producto?codigo=${encodeURIComponent(codigo)}`
        );

        // Si el servidor devuelve error o no encontró el producto
        if (!response.ok) {
            campoDescripcion.value = "";
            if (campoPresentacion) campoPresentacion.value = "";
            campoValorUnitario.value = "";
            return;
        }

        const producto = await response.json();

        if (producto) {

            // La columna "descripción" del modal ahora muestra el nombre
            // del producto (antes se usaba la columna descripcion/categoria)
            campoDescripcion.value =
                producto.nombre || "";

            poblarPresentacionEnFila(campoPresentacion, producto.presentaciones, campoValorUnitario, fila);

        } else {

            campoDescripcion.value = "";
            if (campoPresentacion) campoPresentacion.value = "";
            campoValorUnitario.value = "";

        }

    } catch (error) {

        reportClientError('Error al buscar el producto.', error);

        campoDescripcion.value = "";
        if (campoPresentacion) campoPresentacion.value = "";
        campoValorUnitario.value = "";

    }

});

// Rellena el datalist de la fila con las presentaciones del producto seleccionado.
// Cada fila tiene su propio datalist, por lo que solo se sugieren las presentaciones
// de SU producto (no las de los demás productos del modal).
// poblarDatalistPresentaciones(datalist, presentaciones)
// - Qué hace: Llena el <datalist> de una fila con las presentaciones del producto
//   (cada fila tiene su propio datalist para sugerencias de presentación).
// - Uso: llamado por poblarPresentacionEnFila() tras obtener presentaciones del backend.
// - Endpoints: Ninguno (manipulación del DOM con datos ya obtenidos).
function poblarDatalistPresentaciones(datalist, presentaciones) {
    if (!datalist || !presentaciones) return;
    datalist.innerHTML = "";
    presentaciones.forEach(function (pres) {
        const opt = document.createElement("option");
        opt.value = textoPresentacion(pres);
        datalist.appendChild(opt);
    });
}

// Formatea el texto visible de una presentación (coincide con el option del datalist).
// Si la cantidad es null (1 unidad por defecto), no se muestra "1".
// textoPresentacion(pres)
// - Qué hace: Retorna el texto legible que representa una presentación (p.ej. 'Caja (10 g)'
//   o 'Caja (Unidad)'). Esta representación se usa en opciones del datalist y para
//   comparar la entrada del usuario con presentaciones conocidas.
// - Uso: usado por poblarDatalistPresentaciones(), poblarPresentacionEnFila() y por el
//   comparador cuando el usuario selecciona una presentación.
// - Endpoints: Ninguno.
function textoPresentacion(pres) {
    if (pres.cantidad != null && pres.unidad) {
        return pres.presentacion + " (" + pres.cantidad + " " + pres.unidad + ")";
    }
    return pres.presentacion + " (" + (pres.unidad || "Und") + ")";
}

// Rellena el input editable de presentación y autocompleta el valor unitario
// si el producto tiene una única presentación. Además guarda las presentaciones
// del producto en el input para poder cargar el precio al seleccionar una.
// poblarPresentacionEnFila(campoPresentacion, presentaciones, campoValorUnitario, fila)
// - Qué hace: Guarda las presentaciones del producto en el input de presentación de la fila,
//   limpia el valor si no fue escrito manualmente, pobla el datalist asociado a la fila y,
//   si existe una única presentación, la selecciona automáticamente y coloca el precio
//   correspondiente en el campo de valor unitario. Finalmente recalcula el total de la fila.
// - Uso: llamado después de obtener datos de producto (por código o desde el autocomplete).
// - Endpoints: Ninguno (trabaja con datos ya obtenidos del servidor).
function poblarPresentacionEnFila(campoPresentacion, presentaciones, campoValorUnitario, fila) {
    if (!campoPresentacion) return;
    // Guardar las presentaciones del producto en el input para reconocer el precio
    campoPresentacion._presentaciones = presentaciones || [];
    // Limpiar solo si el usuario no escribió manualmente un valor personalizado
    if (!campoPresentacion.dataset.manual) {
        campoPresentacion.value = "";
    }
    // Poblar el datalist propio de la fila con las presentaciones del producto
    const datalistId = campoPresentacion.getAttribute("list");
    const datalist = datalistId ? document.getElementById(datalistId) : null;
    poblarDatalistPresentaciones(datalist, presentaciones);
    if (presentaciones && presentaciones.length === 1) {
        const pres = presentaciones[0];
        campoPresentacion.value = textoPresentacion(pres);
        if (campoValorUnitario) {
            campoValorUnitario.value = pres.precio;
        }
        if (fila) calcularTotalFila(fila);
    }
}

// Al cambiar la presentación seleccionada en el datalist, cargar el precio
// de esa presentación en el campo "valor unitario".
document.addEventListener("change", function (e) {
    if (e.target.classList.contains("presentacion-producto")) {
        const input = e.target;
        const fila = e.target.closest("tr");
        const campoValorUnitario = fila ? fila.querySelector(".valor-unitario") : null;
        const presentaciones = input._presentaciones || [];
        if (presentaciones.length > 0) {
            const seleccionada = presentaciones.find(function (pres) {
                return textoPresentacion(pres) === input.value;
            });
            if (seleccionada && campoValorUnitario) {
                campoValorUnitario.value = seleccionada.precio;
                if (fila) calcularTotalFila(fila);
            }
        }
    }
});

// Genera un id único para el datalist de cada fila
let contadorDatalist = 0;
// crearDatalistFila()
// - Qué hace: Genera un id único para el <datalist> de cada fila de la tabla de productos,
//   crea el datalist en <body> si no existe y devuelve el id para asignarlo al atributo
//   list del input de presentación de la fila.
// - Uso: cada vez que se agrega una nueva fila con agregarFila().
// - Endpoints: Ninguno.
function crearDatalistFila() {
    contadorDatalist++;
    const id = "datalist-presentaciones-" + contadorDatalist;
    let datalist = document.getElementById(id);
    if (!datalist) {
        datalist = document.createElement("datalist");
        datalist.id = id;
        document.body.appendChild(datalist);
    }
    return id;
}

// Al cambiar la presentación seleccionada (o escribir manualmente), recalcular
document.addEventListener("input", function (e) {
    if (e.target.classList.contains("presentacion-producto")) {
        const fila = e.target.closest("tr");
        if (fila) calcularTotalFila(fila);
    }
});

// ============================================
// AUTOCOMPLETADO - Descripción de Producto
// ============================================
// Se usan dropdowns individuales DENTRO de cada fila (.autocomplete-container)
// con posición absolute. La tabla de productos (.tabla-productos) tiene
// overflow: visible para no recortarlos. Adicionalmente, si el contenedor
// .autocomplete-dropdown no existe, se crea automáticamente en la fila.

let autocompleteTimer = null;

// Asegura que la fila tenga su dropdown
// obtenerDropdown(input)
// - Qué hace: Asegura que la fila contenga su contenedor .autocomplete-container y que
//   exista (o se cree) el elemento .autocomplete-dropdown usado para mostrar sugerencias
//   de autocompletado dentro de la fila. Devuelve el dropdown.
// - Uso: utilizado por el autocompletado de descripción de producto (input .descripcion-producto).
// - Endpoints: Ninguno.
function obtenerDropdown(input) {
    const container = input.closest(".autocomplete-container");
    if (!container) {
        return null;
    }
    let dropdown = container.querySelector(".autocomplete-dropdown");
    if (!dropdown) {
        dropdown = document.createElement("div");
        dropdown.className = "autocomplete-dropdown";
        container.appendChild(dropdown);
    }
    return dropdown;
}

document.addEventListener("input", function (e) {
    if (!e.target.classList.contains("descripcion-producto")) {
        return;
    }

    const input = e.target;
    const fila = input.closest("tr");
    const dropdown = obtenerDropdown(input);

    // Limpiar el código de inventario cuando el usuario edita el texto manualmente
    const campoCodigo = fila ? fila.querySelector(".codigo-producto") : null;
    const campoPresentacion = fila ? fila.querySelector(".presentacion-producto") : null;

    const termino = input.value.trim();

    // Limpiar el temporizador anterior
    clearTimeout(autocompleteTimer);

    if (!dropdown) {
        return;
    }

    if (termino.length < 2) {
        dropdown.innerHTML = "";
        dropdown.style.display = "none";
        return;
    }

    autocompleteTimer = setTimeout(async () => {
        try {
            const response = await fetch(
                `/dashboard/productos/buscar?query=${encodeURIComponent(termino)}`
            );

            if (!response.ok) {
                dropdown.innerHTML = "";
                dropdown.style.display = "none";
                return;
            }

            const productos = await response.json();


            if (!productos || productos.length === 0) {
                dropdown.innerHTML = "";
                dropdown.style.display = "none";
                return;
            }

            dropdown.innerHTML = "";
            dropdown.style.display = "block";

            productos.slice(0, 8).forEach(producto => {
                const item = document.createElement("div");
                item.className = "autocomplete-item";
                item.textContent = producto.nombre;

                // Mostrar el código como subtexto si existe
                if (producto.codigoInventario) {
                    const sub = document.createElement("small");
                    sub.textContent = " (" + producto.codigoInventario + ")";
                    item.appendChild(sub);
                }

                item.addEventListener("mousedown", function (ev) {
                    ev.preventDefault();

                    // Rellenar descripción, código y presentación
                    input.value = producto.nombre || "";
                    input.dataset.idProducto = producto.idProducto || "";
                    if (campoCodigo) {
                        campoCodigo.value = producto.codigoInventario || "";
                        campoCodigo.dataset.idProducto = producto.idProducto || "";
                    }
                    if (campoPresentacion) {
                        const campoVal = fila ? fila.querySelector(".valor-unitario") : null;
                        poblarPresentacionEnFila(campoPresentacion, producto.presentaciones, campoVal, fila);
                    }

                    dropdown.innerHTML = "";
                    dropdown.style.display = "none";
                });


                dropdown.appendChild(item);

            });

        } catch (error) {
            reportClientError('Error al cargar sugerencias de producto.', error);
            if (dropdown) {
                dropdown.innerHTML = "";
                dropdown.style.display = "none";
            }
        }
    }, 250);
});

// Cerrar todos los dropdowns al hacer clic fuera de un input de producto
document.addEventListener("click", function (e) {
    if (e.target.classList.contains("descripcion-producto")) {
        return;
    }
    document.querySelectorAll(".autocomplete-dropdown").forEach(function (dd) {
        if (dd.style.display === "block") {
            dd.innerHTML = "";
            dd.style.display = "none";
        }
    });
});

// Codigo para boton de agregar fila ###############################################33333
const btnAgregarFila = document.getElementById("btn-agregar-fila");

btnAgregarFila.addEventListener("click", agregarFila);

// agregarFila()
// - Qué hace: Crea y agrega una nueva fila a la tabla #tbody-productos con inputs para
//   cantidad, código, descripción (autocomplete), presentación, valor unitario, IVA y totales.
//   Asocia un datalist único a la fila y calcula el total inicial de la fila.
// - Uso: llamado por el botón 'Agregar fila' y por resetFormularioOrden() cuando la orden
//   está vacía.
// - Endpoints: Ninguno (manipulación DOM).
function agregarFila() {

    const tbody = document.getElementById("tbody-productos");

    // Cada fila tiene su propio datalist para que la presentación solo sugiera
    // las presentaciones del producto de esa fila (no las de los demás).
    const idDatalist = crearDatalistFila();

    const nuevaFila = document.createElement("tr");

    nuevaFila.innerHTML = `
        <td>
            <input type="number" class="cantidad input-control td-input" value="1" min="0">
        </td>

        <td>
            <input type="text" class="codigo-producto input-control td-input" >
        </td>

<td>
            <div class="autocomplete-container">
                <input type="text" class="descripcion-producto input-control td-input" placeholder="Escriba el producto...">
                <div class="autocomplete-dropdown"></div>
            </div>
        </td>

<td>
            <input type="text" class="presentacion-producto input-control td-input" list="${idDatalist}" placeholder="Seleccione o escriba...">
        </td>

        <td>
            <input type="number" class="valor-unitario input-control td-input" min="0" >
        </td>

        <td>
            <input type="number" class="iva-producto input-control td-input"  min="0">
        </td>

        <td>
            <input type="text" class="iva-total input-control td-input" disabled readonly>
            </td>

        <td>
            <input type="text" class="valor-total input-control td-input" disabled readonly>
        </td>

        <td>
            <button  title="Eliminar producto" type="button" class="btn-icon delete" aria-label="Eliminar fila">
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
// formatearPesos(valor)
// - Qué hace: Formatea un número en formato de moneda COP (sin decimales) usando
//   Intl.NumberFormat. Usado para mostrar subtotales, IVA y totales en la UI.
// - Uso: recalcularTotalesGenerales(), calcularTotalFila(), y al renderizar ordenes.
// - Endpoints: Ninguno.
function formatearPesos(valor) {
    return new Intl.NumberFormat("es-CO", {
        style: "currency",
        currency: "COP",
        minimumFractionDigits: 0,
        maximumFractionDigits: 0
    }).format(valor);
}

// calcularTotalFila(fila)
// - Qué hace: Calcula el valor total y el IVA de una fila concreta usando cantidad,
//   valor unitario e IVA porcentual. Actualiza los campos .iva-total y .valor-total (formateados)
//   y almacena los valores numéricos en dataset.valor para uso en cálculos generales.
// - Uso: llamado al cambiar cantidad, valor unitario o IVA en una fila, y al agregar filas.
// - Endpoints: Ninguno.
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
// recalcularTotalesGenerales()
// - Qué hace: Recorre todas las filas en #tbody-productos para sumar subtotal y total de IVA
//   (tomando los valores numéricos guardados en dataset.valor por fila), aplica descuento y
//   flete si están activados, y actualiza los elementos DOM que muestran subtotal, IVA,
//   descuento, flete y total general.
// - Uso: llamado desde calcularTotalFila() y desde los controles de descuento/flete.
// - Endpoints: Ninguno.
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

    let flete = 0;

    if (chkFlete.checked) {
        flete = parseFloat(inputFlete.value) || 0;
    }

    const totalGeneral =
        subtotal + totalIva - descuento + flete;


    document.getElementById("subtotal-general").textContent =
        formatearPesos(subtotal);

    document.getElementById("iva-general").textContent =
        formatearPesos(totalIva);

    document.getElementById("descuento-general").textContent =
        formatearPesos(descuento);

    document.getElementById("flete-general").textContent =
        formatearPesos(flete);

    document.getElementById("total-general").textContent =
        formatearPesos(totalGeneral);

    lblDescuento.textContent =
        formatearPesos(descuento);

    lblFlete.textContent =
        formatearPesos(flete);
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

// codigo para mostrar/ocultar flete
const chkFlete = document.getElementById("activar-flete");
const inputFlete = document.getElementById("input-flete");
const lblFlete = document.getElementById("flete-general");

if (chkFlete) {
    chkFlete.addEventListener("change", function () {
        if (this.checked) {
            lblFlete.style.display = "none";
            inputFlete.style.display = "inline-block";
        } else {
            inputFlete.value = 0;
            inputFlete.style.display = "none";
            lblFlete.style.display = "inline";
            recalcularTotalesGenerales();
        }
    });
}

if (inputFlete) {
    inputFlete.addEventListener("input", function () {
        recalcularTotalesGenerales();
    });
}

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

// parsearMoneda(elementId)
// - Qué hace: Parsea el texto de un elemento (innerText o value) que contiene una moneda
//   con formato (p.ej. "$ 15.000") y devuelve el número float correspondiente. Devuelve 0
//   si no puede parsear.
// - Uso: usado antes de construir el DTO que se envía al backend para convertir textos de
//   totales a valores numéricos.
// - Endpoints: Ninguno.
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

// guardarYGenerarPdf()
// - Qué hace: Valida el formulario del modal de orden, construye el DTO (cabecera + detalles)
//   con subtotales, IVA, descuento y flete, y envía la petición al backend para crear o
//   actualizar la orden. Deshabilita temporalmente el botón de guardar para evitar
//   duplicados y, tras respuesta exitosa, cierra el modal y recarga la página.
// - Endpoints:
//     POST /orden-compra          (crear nueva orden)
//     PUT  /orden-compra/{id}     (actualizar orden existente)
// - Notas: valida que la fecha y centro de costo estén presentes y que cada fila tenga
//   un código de inventario no vacío.
async function guardarYGenerarPdf() {
    // ⚠️ CORRECCIÓN 1: Seleccionar la fecha ESPECÍFICAMENTE dentro del modal
    const fechaInput = document.getElementById('fecha-orden')?.value;

    if (!fechaInput) {
        mostrarToast("Por favor selecciona una fecha válida dentro del formulario.", 'error');
        return;
    }

    // 1. DTO de la Cabecera
    const selectCentroCosto = document.getElementById('select-centro-costo');
    const idCentroCosto = selectCentroCosto ? selectCentroCosto.value : '';

    // Validación: centro de costo obligatorio
    if (!idCentroCosto) {
        mostrarToast('Debes seleccionar un Centro de Costo para continuar.', 'error');
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

    const selectProveedorElem = document.getElementById('select-proveedor');
    const proveedorSeleccion = selectProveedorElem ? selectProveedorElem.value : '';

    // Validar los datos de proveedor cuando hay un proveedor seleccionado
    if (proveedorSeleccion !== '') {
        const proveedorIncompleto = camposProveedor.find(campo => {
            const valor = document.getElementById(campo.id)?.value?.trim() || '';
            return !valor;
        });

        if (proveedorIncompleto) {
            mostrarToast(`El campo "${proveedorIncompleto.label}" del proveedor es obligatorio.`, 'error');
            return;
        }
    }

    const ordenDTO = {
        fecha: fechaInput,
        idCentroCosto: idCentroCosto ? parseInt(idCentroCosto) : null,
        // Si se seleccionó un proveedor existente, enviar su id; si fue 'otro' o vacío, enviar null
        idProv: proveedorSeleccion && proveedorSeleccion !== 'otro' ? parseInt(proveedorSeleccion) : null,
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

        pagaFlete: document.getElementById('activar-flete')?.checked || false,
        valorFlete: document.getElementById('activar-flete')?.checked
            ? (parseFloat(document.getElementById('input-flete')?.value) || null)
            : null,

        detalles: []
    };

    // 2. DTO de las Líneas de Producto
    const filasRows = document.querySelectorAll('#tbody-productos tr');
    filasRows.forEach(fila => {
        const completo = fila.querySelector('.cantidad');
        const cantidad = parseInt(completo ? completo.value : 0) || 0;
        const campoCodigoFila = fila.querySelector('.codigo-producto');
        const codigo = campoCodigoFila ? (campoCodigoFila.value || 'N/A').trim() : '';

        if (codigo !== '' && cantidad > 0) {
            const campoValorUnitario = fila.querySelector('.valor-unitario');
            const campoCampoIva = fila.querySelector('.iva-producto');
            const vUnitario = parseFloat(campoValorUnitario ? campoValorUnitario.value : 0) || 0;
            const pctIva = parseFloat(campoCampoIva ? campoCampoIva.value : 0) || 0;
            const vIva = (vUnitario * cantidad) * (pctIva / 100);
            const vTotal = (vUnitario * cantidad);

            const campoCodigo = fila.querySelector('.codigo-producto');
            const idProducto = campoCodigo && campoCodigo.dataset.idProducto
                ? parseInt(campoCodigo.dataset.idProducto)
                : null;

            const campoPresentacionSel = fila.querySelector('.presentacion-producto');
            const presentacionSel = campoPresentacionSel ? campoPresentacionSel.value || '(Und)' : '(Und)';

            const campoDescripcion = fila.querySelector('.descripcion-producto');
            const descripcion = campoDescripcion ? campoDescripcion.value : '';

            ordenDTO.detalles.push({
                idProducto: idProducto,
                cantidad: cantidad,
                codigoInventario: codigo,
                descripcion: descripcion,
                presentacion: presentacionSel,
                valorUnitario: vUnitario,
                ivaProducto: pctIva,
                valorIva: vIva,
                valorTotalLinea: vTotal
            });
        }
    });

    if (ordenDTO.detalles.length === 0) {
        mostrarToast("Debes agregar al menos un producto con código y cantidad válida.", 'error');
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
        const response = await csrfFetch(url, {
            method: ordenIdActual ? 'PUT' : 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(ordenDTO)
        });

        if (response.ok) {
            mostrarToast('Orden guardada exitosamente.', 'success');
            if (typeof cerrarModal === 'function') {
                cerrarModal();
            } else {
                const modal = document.getElementById('modal-orden');
                if (modal) modal.style.display = 'none';
            }
            setTimeout(() => location.reload(), 1200);
        } else {
            const errText = await response.text();
            reportClientError('Error al guardar la orden de compra.', errText);
            mostrarToast(errText || 'Error al guardar la orden de compra.', 'error');
        }
    } catch (error) {
        reportClientError('Error de red/servidor.', error);
        mostrarToast('Ocurrió un error al conectar con el servidor.', 'error');
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

    const confirmar = await mostrarConfirmacion(
        "¿Está seguro de aprobar esta Orden de Compra? \n\n" +
        "Después de aprobarla no podrá modificar productos ni valores.",
        '¿Está seguro?',
        '',
        'info'
    );

    if (!confirmar) {
        return;
    }

    try {

        const response = await csrfFetch(
            `/orden-compra/${idOrden}/aprobar`,
            {
                method: "PUT"
            }
        );

        if (!response.ok) {

            const mensaje = await response.text();


            throw new Error(mensaje);
        }

        mostrarToast("Orden aprobada correctamente", 'success');

        // Recargar después de que el toast se haya ocultado (3s)
        setTimeout(() => location.reload(), 1200);

    } catch (error) {

        reportClientError('Error al aprobar la orden.', error);

        mostrarToast(error.message || 'No fue posible aprobar la orden.', 'error');
    }
});

// abrirModalFactura(boton)
// - Qué hace: Extrae los atributos data-* del botón de facturar (id, numeroOrden, proveedor),
//   limpia el formulario de factura, asigna los datos en el modal y lo muestra en pantalla.
// - A dónde apunta:
//   - DOM: #modal-facturar-oc, #factura-numero-orden, #factura-proveedor, #factura-numero-factura.
function abrirModalFactura(boton) {
    idOrdenSeleccionada = boton.dataset.id;
    const numeroOrden = boton.dataset.numeroOrden || idOrdenSeleccionada;
    const proveedor = boton.dataset.proveedor || '';

    limpiarFormularioFactura();
    document.getElementById('factura-numero-orden').textContent = numeroOrden;
    document.getElementById('factura-proveedor').textContent = proveedor;
    document.getElementById('modal-facturar-oc').style.display = 'flex';
    document.getElementById('factura-numero-factura').focus();
}

// limpiarFormularioFactura()
// - Qué hace: Restablece todos los campos del modal de factura (número, vista previa de imagen/PDF,
//   input de archivo y variables base64).
// - A dónde apunta:
//   - DOM: #factura-numero-factura, #factura-file-info, #factura-preview-image, #factura-preview-container, #factura-foto-input, #btn-eliminar-factura.
function limpiarFormularioFactura() {
    const inputFactura = document.getElementById('factura-numero-factura');
    if (inputFactura) inputFactura.value = '';

    const infoFactura = document.getElementById('factura-file-info');
    if (infoFactura) {
        infoFactura.textContent = '';
        infoFactura.style.display = 'none';
    }

    const previewFactura = document.getElementById('factura-preview-image');
    if (previewFactura) previewFactura.src = '';

    const previewContainerFactura = document.getElementById('factura-preview-container');
    if (previewContainerFactura) previewContainerFactura.style.display = 'none';

    const inputFileFactura = document.getElementById('factura-foto-input');
    if (inputFileFactura) inputFileFactura.value = '';

    const btnEliminarFactura = document.getElementById('btn-eliminar-factura');
    if (btnEliminarFactura) btnEliminarFactura.style.display = 'none';

    facturaDocumentoBase64 = null;
    facturaDocumentoNombre = '';
}

// handleAdjuntoArchivo(file, options)
// - Qué hace: Valida que el archivo adjuntado sea una imagen o PDF; si es una imagen, la redimensiona
//   usando un canvas HTML5 (máx 1600px) y la comprime a JPEG (calidad 0.85); si es PDF, lee su data URL
//   directamente y actualiza la vista previa y el callback setDataUrl.
// - A dónde apunta:
//   - FileReader y Canvas API en cliente; actualiza elementos de previsualización DOM pasados en options.
function handleAdjuntoArchivo(file, { previewContainer, previewImage, infoElement, setDataUrl, label, inputFile }) {

    if (!file) return;

    const valido = file.type.startsWith('image/') || file.type === 'application/pdf' || /\.pdf$/i.test(file.name);
    if (!valido) {
        mostrarToast(label + ' debe ser una imagen o un PDF.', 'error');
        if (inputFile) inputFile.value = '';
        return;
    }

    const reader = new FileReader();
    reader.onload = function (event) {
        const result = event.target.result;

        if (file.type.startsWith('image/')) {
            const img = new Image();
            img.onload = function () {
                const maxDim = 1600;
                let width = img.width;
                let height = img.height;

                if (width > maxDim || height > maxDim) {
                    if (width > height) {
                        height = Math.round((height * maxDim) / width);
                        width = maxDim;
                    } else {
                        width = Math.round((width * maxDim) / height);
                        height = maxDim;
                    }
                }

                const canvas = document.createElement('canvas');
                canvas.width = width;
                canvas.height = height;
                const ctx = canvas.getContext('2d');
                ctx.drawImage(img, 0, 0, width, height);

                const dataUrl = canvas.toDataURL('image/jpeg', 0.85);
                if (setDataUrl) setDataUrl(dataUrl, file.name);
                if (previewContainer) previewContainer.style.display = 'flex';
                if (previewImage) previewImage.src = dataUrl;
                if (infoElement) {
                    infoElement.textContent = file.name;
                    infoElement.style.display = 'flex';
                }
            };
            img.src = result;
            return;
        }

        if (setDataUrl) setDataUrl(result, file.name);
        if (previewContainer) previewContainer.style.display = 'none';
        if (previewImage) previewImage.src = '';
        if (infoElement) {
            infoElement.textContent = file.name;
            infoElement.style.display = 'flex';
        }
    };

    reader.readAsDataURL(file);
}

document.addEventListener("click", function (e) {
    const boton = e.target.closest(".facturar");
    if (!boton) return;
    abrirModalFactura(boton);
});

document.getElementById('btn-cancelar-factura')?.addEventListener('click', function () {
    document.getElementById('modal-facturar-oc').style.display = 'none';
    limpiarFormularioFactura();
    idOrdenSeleccionada = null;
});

document.getElementById('btn-confirmar-factura')?.addEventListener('click', async function () {
    const numeroFactura = document.getElementById('factura-numero-factura')?.value.trim();
    if (!numeroFactura) {
        mostrarToast('Debes ingresar el número de factura.', 'error');
        return;
    }
    if (!facturaDocumentoBase64) {
        mostrarToast('Debes adjuntar la evidencia de la factura.', 'error');
        return;
    }

    const btn = document.getElementById('btn-confirmar-factura');
    btn.disabled = true;
    btn.textContent = 'Procesando...';

    try {
        const response = await csrfFetch(`/orden-compra/${idOrdenSeleccionada}/facturar`, {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                numeroFactura,
                fotoRecepcion: facturaDocumentoBase64
            })
        });

        if (!response.ok) {
            const mensaje = await response.text();
            throw new Error(mensaje || 'No se pudo registrar la factura.');
        }

        mostrarToast('Factura registrada correctamente.', 'success');
        document.getElementById('modal-facturar-oc').style.display = 'none';
        limpiarFormularioFactura();
        idOrdenSeleccionada = null;
        setTimeout(() => location.reload(), 1200);
    } catch (error) {
        reportClientError('Error al registrar la factura.', error);
        mostrarToast(error.message || 'No fue posible registrar la factura.', 'error');
    } finally {
        btn.disabled = false;
        btn.textContent = 'Registrar Factura';
    }
});

// ANULAR ORDEN (lógica de anulación, no eliminación física)
document.addEventListener("click", async function (e) {
    const boton = e.target.closest(".delete-orden");
    if (!boton) return;

    const idOrden = boton.dataset.id;
    const numeroOrden = boton.dataset.numeroOrden || "";

    if (!idOrden) {
        mostrarToast('ID de orden no disponible para anularla.', 'error');
        return;
    }

    const confirmar = await mostrarConfirmacion(
        "¿Está seguro de anular esta Orden de Compra?\n\n" +
        "N° Orden: " + (numeroOrden || idOrden) + "\n\n" +
        "Este proceso no se puede deshacer."
    );

    if (!confirmar) {
        return;
    }

    try {
        const response = await csrfFetch(`/orden-compra/${idOrden}/anular`, {
            method: "PUT"
        });

        if (!response.ok) {
            const mensaje = await response.text();
            throw new Error(mensaje || "No se pudo anular la orden.");
        }

        mostrarToast("Orden anulada correctamente.", 'success');
        setTimeout(() => location.reload(), 1200);
    } catch (error) {
        reportClientError('Error al anular la orden.', error);
        mostrarToast(error.message, 'error');
    }
});

// DESCARGAR PDF (solo para APROBADA)
document.addEventListener("click", async function (e) {
    const boton = e.target.closest(".pdf");
    if (!boton) return;

    const idOrden = boton.dataset.id;
    if (!idOrden) {
        mostrarToast('ID de orden no disponible para la descarga.', 'error');
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
        reportClientError('Error descargando PDF.', err);
        mostrarToast(err.message || 'Error descargando PDF', 'error');
    }
});

// ==========================================
// RECEPCIÓN DE ORDEN DE COMPRA
// ==========================================

let idOrdenSeleccionada = null;
let fotoRecepcionBase64 = null;
let facturaDocumentoBase64 = null;
let facturaDocumentoNombre = '';

// Estado temporal de modificaciones locales por orden (no persistidas hasta confirmar recepción)
// Estructura: { [ordenId]: [ { idDetalle, idProducto, descripcion, presentacion, cantidadSolicitada, cantidadRecibida, recibido } ] }
const ordenesModificadas = {};

// renderProductosRecepcion(productos, estadoOrden)
// - Qué hace: Construye e inserta dinámicamente las filas de productos en la tabla del modal de recepción (#recepcion-productos-body).
//   Configura el switch de recibido (checkbox), inputs de cantidad llegada y advertencias visuales de discrepancia.
// - A dónde apunta:
//   - DOM: #recepcion-productos-body. Interactúa con las funciones evaluarCambiosEnModal() ante cambios del usuario.
function renderProductosRecepcion(productos = [], estadoOrden = 'APROBADA') {
    const tbody = document.getElementById('recepcion-productos-body');
    if (!tbody) return;

    tbody.innerHTML = '';
    if (!Array.isArray(productos) || productos.length === 0) {
        tbody.innerHTML = '<tr><td colspan="4" class="recepcion-productos-vacio">No hay productos asociados a esta orden.</td></tr>';
        return;
    }

    const mostrarAdvertencia = ['RECIBIDA', 'FACTURADA', 'COMPLETADA'].includes(estadoOrden);

    productos.forEach((producto) => {
        const fila = document.createElement('tr');
        const cantidadSolicitada = Number(producto.cantidad ?? producto.cantidadSolicitada ?? 0) || 0;
        const idDetalle = producto.idDetalle ?? producto.id_detalle ?? null;
        const idProducto = producto.idProducto ?? producto.id_producto ?? null;
        const codigoInventario = producto.codigoInventario || producto.codigo_inventario || '';
        const presentacion = producto.presentacion || '';
        const descripcion = producto.descripcion || '';
        const nombreProducto = [descripcion, presentacion].filter(Boolean).join(' - ').trim() || codigoInventario || 'Producto';
        const checked = true;

        fila.dataset.idDetalle = idDetalle ?? '';
        fila.dataset.idProducto = idProducto ?? '';
        fila.dataset.codigoInventario = codigoInventario;
        fila.dataset.presentacion = presentacion;
        fila.dataset.descripcion = descripcion;
        fila.dataset.cantidad = String(cantidadSolicitada);
        fila.dataset.estadoOrden = estadoOrden;

        const html = `
            <td>
                <div class="recepcion-producto-nombre">${nombreProducto}</div>
            </td>
            <td class="recepcion-producto-cantidad">${cantidadSolicitada}</td>
            <td class="recepcion-producto-check">
                <label class="switch-check">
                    <input type="checkbox" class="recepcion-producto-checkbox" checked>
                    <span class="slider"></span>
                </label>
            </td>
            <td class="recepcion-producto-llegada">
                <div class="recepcion-cantidad-wrapper">
                    <input type="number" min="0" step="1" value="${cantidadSolicitada}" class="recepcion-cantidad-input" disabled>
                    <span class="recepcion-warning material-symbols" title="Cantidad modificada después de registrar la recepción." style="display: none;">warning</span>
                </div>
            </td>
        `;

        fila.innerHTML = html;

        const checkbox = fila.querySelector('.recepcion-producto-checkbox');
        const inputCantidad = fila.querySelector('.recepcion-cantidad-input');
        const warningIcon = fila.querySelector('.recepcion-warning');

        checkbox.checked = checked;
        inputCantidad.value = String(cantidadSolicitada);
        inputCantidad.disabled = true;

        const actualizarWarning = () => {
            if (!mostrarAdvertencia) {
                warningIcon.style.display = 'none';
                return;
            }

            const valorOriginal = Number(fila.dataset.cantidad || 0);
            const valorActual = Number(inputCantidad.value || 0);
            const modificado = checkbox.checked ? false : valorActual !== valorOriginal;
            warningIcon.style.display = modificado ? 'inline-flex' : 'none';
            warningIcon.title = modificado
                ? 'La cantidad fue modificada en una orden ya recibida, facturada o completada.'
                : 'Cantidad sin ajustes';
        };

        checkbox.addEventListener('change', () => {
            if (checkbox.checked) {
                inputCantidad.value = String(cantidadSolicitada);
                inputCantidad.disabled = true;
            } else {
                inputCantidad.disabled = false;
                inputCantidad.value = '';
                inputCantidad.focus();
            }
            actualizarWarning();
            // evaluar cambios en modal (actualiza estado local y el icono en la lista)
            evaluarCambiosEnModal(idOrdenSeleccionada);
        });

        inputCantidad.addEventListener('input', () => {
            if (inputCantidad.value === '') {
                warningIcon.style.display = mostrarAdvertencia ? 'inline-flex' : 'none';
                evaluarCambiosEnModal(idOrdenSeleccionada);
                return;
            }
            actualizarWarning();
            evaluarCambiosEnModal(idOrdenSeleccionada);
        });

        actualizarWarning();
        tbody.appendChild(fila);
    });
}

// obtenerProductosRecepcion()
// - Qué hace: Lee las filas actuales de la tabla de recepción (#recepcion-productos-body) y recopila
//   un arreglo de objetos con cantidades solicitadas, cantidades recibidas y flags de llegada.
// - A dónde apunta:
//   - DOM: #recepcion-productos-body tr. Usado por el botón "Confirmar Recepción" para enviar el payload a PUT /orden-compra/{id}/recibir.
function obtenerProductosRecepcion() {
    const filas = document.querySelectorAll('#recepcion-productos-body tr');
    const productos = [];

    filas.forEach((fila) => {
        if (fila.querySelector('.recepcion-productos-vacio')) {
            return;
        }

        const checkbox = fila.querySelector('.recepcion-producto-checkbox');
        const inputCantidad = fila.querySelector('.recepcion-cantidad-input');
        const cantidadSolicitada = Number(fila.dataset.cantidad || 0);
        const recibido = checkbox ? checkbox.checked : true;
        const cantidadRecibida = recibido ? cantidadSolicitada : (inputCantidad && inputCantidad.value !== '' ? Number(inputCantidad.value) : 0);
        const noLlego = !recibido && Number(cantidadRecibida) === 0;

        productos.push({
            idDetalle: fila.dataset.idDetalle ? Number(fila.dataset.idDetalle) : null,
            idProducto: fila.dataset.idProducto ? Number(fila.dataset.idProducto) : null,
            codigoInventario: fila.dataset.codigoInventario || null,
            presentacion: fila.dataset.presentacion || null,
            descripcion: fila.dataset.descripcion || null,
            cantidadSolicitada,
            cantidadRecibida,
            recibido,
            noLlego,
            uniqueKey: (fila.dataset.idDetalle || '') + '||' + (fila.dataset.presentacion || '') + '||' + (fila.dataset.idProducto || '')
        });
    });

    return productos;
}

// evaluarCambiosEnModal(ordenId)
// - Qué hace: Evalúa si existe alguna diferencia entre las cantidades recibidas y solicitadas en el modal de recepción;
//   actualiza el objeto en memoria `ordenesModificadas[ordenId]` y refleja el estado en el botón de alerta.
// - A dónde apunta:
//   - Objeto local `ordenesModificadas` y función `marcarOrdenConCambios(ordenId, anyModified)`.
function evaluarCambiosEnModal(ordenId) {
    if (!ordenId) return;
    const productos = obtenerProductosRecepcion();
    const anyModified = productos.some(p => Number(p.cantidadRecibida) !== Number(p.cantidadSolicitada));
    if (anyModified) {
        ordenesModificadas[ordenId] = productos;
    } else {
        delete ordenesModificadas[ordenId];
    }
    marcarOrdenConCambios(ordenId, anyModified);
}

// marcarOrdenConCambios(ordenId, hasChanges)
// - Qué hace: Muestra u oculta el botón de alerta/advertencia (.reception-alert) en la fila correspondiente
//   de la tabla principal de órdenes en el dashboard.
// - A dónde apunta:
//   - DOM: `.reception-alert[data-id="${ordenId}"]`.
function marcarOrdenConCambios(ordenId, hasChanges) {
    if (!ordenId) return;
    const selector = `.reception-alert[data-id="${ordenId}"]`;
    const btn = document.querySelector(selector);
    if (!btn) return;
    if (hasChanges) {
        btn.style.display = 'inline-flex';
        btn.title = 'Esta orden tiene cambios en la recepción';
        btn.dataset.changed = 'true';
    } else {
        // Si el servidor indicó que hay diferencias persistidas, mantener visible
        const serverFlag = btn.dataset.serverChanged === 'true';
        if (serverFlag) {
            btn.style.display = 'inline-flex';
            btn.title = 'Esta orden tiene cambios persistidos en la recepción';
            btn.dataset.changed = '';
            return;
        }
        btn.style.display = 'none';
        btn.dataset.changed = '';
    }
}

// checkPersistedDifferencesForAllOrders()
// - Qué hace: Consulta al backend en lote (`GET /orden-compra/has-diferencias?ids=...`) para saber cuáles órdenes
//   tienen discrepancias registradas en base de datos entre cantidad solicitada y cantidad recibida,
//   marcando los iconos correspondientes en la tabla del dashboard.
// - A dónde apunta:
//   - Backend: GET /orden-compra/has-diferencias?ids=...
//   - DOM: elementos `.reception-alert[data-id]`.
async function checkPersistedDifferencesForAllOrders() {

    const botones = document.querySelectorAll('.reception-alert[data-id]');
    if (!botones || botones.length === 0) return;

    // Construir lista de ids
    const ids = Array.from(botones).map(b => b.dataset.id).filter(Boolean);
    if (ids.length === 0) return;

    try {
        const resp = await fetch(`/orden-compra/has-diferencias?ids=${encodeURIComponent(ids.join(','))}`);
        if (!resp.ok) {
            // No bloquear la UI por errores parciales
            return;
        }
        const data = await resp.json();
        const idsWithDiff = Array.isArray(data.ids) ? data.ids.map(x => String(x)) : [];

        // Marcar botones según respuesta
        botones.forEach(btn => {
            const id = btn.dataset.id;
            const has = idsWithDiff.includes(String(id));
            btn.dataset.serverChanged = has ? 'true' : '';
            if (has) {
                btn.style.display = 'inline-flex';
                btn.title = 'Esta orden tiene cambios persistidos en la recepción';
            } else {
                // Solo ocultar si no hay cambios locales
                if (btn.dataset.changed !== 'true') {
                    btn.style.display = 'none';
                }
            }
        });

    } catch (err) {
        console.debug('Error comprobando diferencias persistidas en lote', err);
    }
}

// Ejecutar la comprobación on-load para marcar los iconos de advertencia persistidos
document.addEventListener('DOMContentLoaded', function () {
    // Intentar comprobar diferencias persistidas tras cargar la página
    try {
        checkPersistedDifferencesForAllOrders();
    } catch (err) {
        console.debug('No fue posible verificar diferencias persistidas:', err);
    }

    // Inicializar filtro "Sólo modificadas" (si el checkbox existe)
    const filtroCheckbox = document.getElementById('filtrar-modificadas');
    if (filtroCheckbox) {
        filtroCheckbox.addEventListener('change', async function () {
            // Asegurarse de tener el state server-side actualizado antes de filtrar
            try {
                await checkPersistedDifferencesForAllOrders();
            } catch (err) {
                console.debug('Error actualizando diferencias persistidas antes de filtrar', err);
            }
            applyFiltroModificadas();
        });
    }

});

// abrirModalCambios(ordenId, numeroOrden, proveedor)
// - Qué hace: Abre el modal secundario (#modal-cambios-recepcion) y muestra la lista de productos
//   cuyas cantidades de llegada discrepan de las cantidades solicitadas (o si algún producto no llegó).
//   Si los datos no están en memoria local, los obtiene mediante `GET /orden-compra/{ordenId}/detalles`.
// - A dónde apunta:
//   - Backend: GET /orden-compra/{ordenId}/detalles
//   - DOM: #modal-cambios-recepcion, #cambios-recepcion-body, #cambios-recepcion-numero-orden, #cambios-recepcion-proveedor.
async function abrirModalCambios(ordenId, numeroOrden = '', proveedor = '') {
    const modal = document.getElementById('modal-cambios-recepcion');
    const body = document.getElementById('cambios-recepcion-body');
    const numeroSpan = document.getElementById('cambios-recepcion-numero-orden');
    const proveedorSpan = document.getElementById('cambios-recepcion-proveedor');

    if (!modal || !body) return;

    body.innerHTML = '';
    numeroSpan.textContent = numeroOrden || '';
    proveedorSpan.textContent = proveedor || '';

    let productos = ordenesModificadas[ordenId];
    if (!Array.isArray(productos)) {
        // intentar obtener del servidor (persisted state)
        try {
            const resp = await fetch(`/orden-compra/${ordenId}/detalles`);
            if (resp.ok) {
                const data = await resp.json();
                productos = Array.isArray(data.detalles) ? data.detalles.map(d => {
                    const cantidadSolicitada = d.cantidad ?? d.cantidadSolicitada ?? 0;
                    const cantidadRecibida = d.cantidadRecibida ?? d.cantidad_recibida ?? d.cantidad ?? d.cantidadSolicitada ?? 0;
                    return {
                        idDetalle: d.idDetalle || d.id_detalle || null,
                        idProducto: d.idProducto || d.productoId || d.id_producto || null,
                        descripcion: d.descripcion || d.productoNombre || '',
                        presentacion: d.presentacion || d.presentacionProducto || '',
                        codigoInventario: d.codigoInventario || d.codigo_inventario || '',
                        cantidadSolicitada: Number(cantidadSolicitada),
                        cantidadRecibida: Number(cantidadRecibida),
                        // clave única para distinguir productos con mismo nombre pero diferente presentación
                        uniqueKey: (d.idDetalle || d.id_detalle || '') + '||' + (d.presentacion || d.presentacionProducto || '') + '||' + (d.idProducto || d.productoId || '')
                    };
                }) : [];
            } else {
                productos = [];
            }
        } catch (err) {
            console.debug('Error cargando detalles para modal cambios', err);
            productos = [];
        }
    }

    // Mostrar únicamente productos cuya cantidad llegada difiere de la solicitada
    const changed = Array.isArray(productos) ? productos.filter(p => Number(p.cantidadRecibida) !== Number(p.cantidadSolicitada)) : [];

    if (!changed || changed.length === 0) {
        body.innerHTML = '<tr><td colspan="3" style="padding:12px; color:#64748b; text-align:center;">No hay cambios en las cantidades de llegada para esta orden.</td></tr>';
    } else {
        changed.forEach(p => {
            const tr = document.createElement('tr');
            const nombre = `${p.descripcion || ''} ${p.presentacion ? '- ' + p.presentacion : ''}`.trim();
            const cantidadRecibida = Number(p.cantidadRecibida ?? 0);
            const cantidadSolicitada = Number(p.cantidadSolicitada ?? 0);
            const valorRecibido = cantidadRecibida === 0 && cantidadSolicitada > 0 ? '<span class="no-llego">NO LLEGÓ</span>' : (p.cantidadRecibida ?? '');

            tr.innerHTML = `
                <td>${nombre}</td>
                <td>${cantidadSolicitada ?? ''}</td>
                <td>${valorRecibido}</td>
            `;
            body.appendChild(tr);
        });
    }

    modal.style.display = 'flex';
}

// Cerrar modal cambios
document.getElementById('btn-cerrar-cambios')?.addEventListener('click', () => {
    const modal = document.getElementById('modal-cambios-recepcion');
    if (modal) modal.style.display = 'none';
});

// applyFiltroModificadas()
// - Qué hace: Aplica el filtro en cliente "Sólo modificadas" sobre la tabla de órdenes del dashboard,
//   ocultando las filas que no tengan discrepancias en la recepción y mostrando las que sí.
// - A dónde apunta:
//   - DOM: #filtrar-modificadas y filas `tr` dentro de `.data-table tbody`.
function applyFiltroModificadas() {
    const checkbox = document.getElementById('filtrar-modificadas');
    const tbody = document.querySelector('.data-table tbody');
    if (!tbody) return;
    const rows = Array.from(tbody.querySelectorAll('tr'));
    const shouldFilter = checkbox && checkbox.checked;
    rows.forEach(row => {
        // Buscar botón de alerta dentro de la fila
        const boton = row.querySelector('.reception-alert');
        let isModified = false;
        if (boton) {
            // Si el botón está visible en el DOM o tiene flags (local/server)
            const styleDisplay = window.getComputedStyle(boton).display;
            const localFlag = boton.dataset.changed === 'true';
            const serverFlag = boton.dataset.serverChanged === 'true';
            if (localFlag || serverFlag || (styleDisplay && styleDisplay !== 'none')) {
                isModified = true;
            }
        }
        // Mostrar u ocultar la fila según el filtro
        if (shouldFilter) {
            row.style.display = isModified ? '' : 'none';
        } else {
            row.style.display = '';
        }
    });
}

// También exponer una función pública para que otros flujos la llamen
window.applyFiltroModificadas = applyFiltroModificadas;

// Diagnostic: informa en consola el estado de elementos clave para depuración de la UI
document.addEventListener('DOMContentLoaded', function () {
    try {
        const countAlerts = document.querySelectorAll('.reception-alert').length;
        console.debug('[dashboard.js diagnostic] Loaded. reception-alert count =', countAlerts);
        if (countAlerts === 0) {
            console.debug('[dashboard.js diagnostic] Aviso: no se encontraron botones .reception-alert. El template debería renderizarlos (aunque estén ocultos).');
        }
        // comprobar que funciones clave existen
        console.debug('[dashboard.js diagnostic] Functions: checkPersistedDifferencesForAllOrders=', typeof checkPersistedDifferencesForAllOrders, 'applyFiltroModificadas=', typeof applyFiltroModificadas);
    } catch (err) {
        console.error('[dashboard.js diagnostic] Error inicializando diagnóstico:', err);
    }
});

// cargarProductosRecepcion(idOrden, estadoOrden)
// - Qué hace: Realiza una petición GET al endpoint `/orden-compra/{idOrden}/detalles` y procesa el JSON
//   para renderizar la tabla interactiva de productos de la orden en el modal de recepción.
// - A dónde apunta:
//   - Backend: GET /orden-compra/{idOrden}/detalles
//   - Llama a `renderProductosRecepcion(productos, estadoOrdenActual)`.
async function cargarProductosRecepcion(idOrden, estadoOrden = 'APROBADA') {
    try {
        const response = await fetch(`/orden-compra/${idOrden}/detalles`);
        if (!response.ok) {
            throw new Error('No se pudieron cargar los productos de la orden.');
        }
        const data = await response.json();
        const productos = Array.isArray(data.detalles) ? data.detalles : [];
        const estadoOrdenActual = (data.estado || estadoOrden || 'APROBADA').toUpperCase();
        renderProductosRecepcion(productos, estadoOrdenActual);
    } catch (error) {
        reportClientError('Error cargando detalles de la orden.', error);
        renderProductosRecepcion([], estadoOrden.toUpperCase());
        mostrarToast(error.message || 'No se pudieron cargar los productos de la orden.', 'error');
    }
}

// Manejo de captura / subida de foto y archivo para facturación y recepción
document.addEventListener("DOMContentLoaded", () => {
    // setupEvidenceUploader(config)
    // - Qué hace: Vincula los botones de cámara / adjuntar archivo / eliminar archivo con el input oculto
    //   y gestiona el ciclo de vida de la previsualización y callbacks base64.
    // - A dónde apunta:
    //   - Elementos DOM especificados en el objeto de configuración (botones, preview, input file).
    const setupEvidenceUploader = ({
        inputId,
        buttonTomarId,
        buttonAdjuntarId,
        buttonEliminarId,
        previewContainerId,
        previewImageId,
        infoId,
        onFileReady,
        clearOnRemove,
        typeLabel
    }) => {
        const inputFile = document.getElementById(inputId);
        const btnTomar = document.getElementById(buttonTomarId);
        const btnAdjuntar = document.getElementById(buttonAdjuntarId);
        const btnEliminar = document.getElementById(buttonEliminarId);
        const previewContainer = document.getElementById(previewContainerId);
        const imgPreview = document.getElementById(previewImageId);
        const infoElement = document.getElementById(infoId);

        if (btnTomar && inputFile) {
            btnTomar.addEventListener("click", () => inputFile.click());
        }

        if (btnAdjuntar && inputFile) {
            btnAdjuntar.addEventListener("click", () => inputFile.click());
        }

        if (inputFile) {
            inputFile.addEventListener("change", function (e) {
                const file = e.target.files && e.target.files[0];
                if (!file) return;

                handleAdjuntoArchivo(file, {
                    previewContainer,
                    previewImage: imgPreview,
                    infoElement,
                    label: typeLabel,
                    setDataUrl: onFileReady,
                    inputFile
                });
            });
        }

        if (btnEliminar) {
            btnEliminar.addEventListener("click", function () {
                if (clearOnRemove) clearOnRemove();
                if (inputFile) inputFile.value = "";
                if (imgPreview) imgPreview.src = "";
                if (previewContainer) previewContainer.style.display = "none";
                if (infoElement) {
                    infoElement.textContent = "";
                    infoElement.style.display = "none";
                }
                btnEliminar.style.display = "none";
            });
        }
    };

    setupEvidenceUploader({
        inputId: 'factura-foto-input',
        buttonTomarId: 'btn-tomar-foto-factura',
        buttonAdjuntarId: 'btn-adjuntar-factura',
        buttonEliminarId: 'btn-eliminar-factura',
        previewContainerId: 'factura-preview-container',
        previewImageId: 'factura-preview-image',
        infoId: 'factura-file-info',
        typeLabel: 'La evidencia',
        onFileReady: (dataUrl, fileName) => {
            facturaDocumentoBase64 = dataUrl;
            facturaDocumentoNombre = fileName;
            document.getElementById('btn-eliminar-factura').style.display = 'inline-flex';
        },
        clearOnRemove: () => {
            facturaDocumentoBase64 = null;
            facturaDocumentoNombre = '';
        }
    });

    setupEvidenceUploader({
        inputId: 'recepcion-foto-input',
        buttonTomarId: 'btn-tomar-foto-recepcion',
        buttonAdjuntarId: 'btn-adjuntar-foto-recepcion',
        buttonEliminarId: 'btn-eliminar-foto',
        previewContainerId: 'recepcion-foto-preview-container',
        previewImageId: 'recepcion-foto-preview',
        infoId: 'recepcion-file-info',
        typeLabel: 'La evidencia',
        onFileReady: (dataUrl, fileName) => {
            fotoRecepcionBase64 = dataUrl;
            const btnEliminar = document.getElementById('btn-eliminar-foto');
            if (btnEliminar) btnEliminar.style.display = 'inline-flex';
            const infoElement = document.getElementById('recepcion-file-info');
            if (infoElement) {
                infoElement.textContent = fileName || 'Archivo adjunto';
                infoElement.style.display = 'flex';
            }
        },
        clearOnRemove: () => {
            fotoRecepcionBase64 = null;
        }
    });
});

// limpiarFotoRecepcion()
// - Qué hace: Resetea el input de foto/archivo de recepción, la vista previa y la variable en memoria fotoRecepcionBase64.
// - A dónde apunta:
//   - DOM: #recepcion-foto-input, #recepcion-foto-preview, #recepcion-foto-preview-container, #btn-eliminar-foto, #recepcion-file-info.
function limpiarFotoRecepcion() {
    fotoRecepcionBase64 = null;
    const inputFoto = document.getElementById("recepcion-foto-input");
    const imgPreview = document.getElementById("recepcion-foto-preview");
    const previewContainer = document.getElementById("recepcion-foto-preview-container");
    const btnEliminarFoto = document.getElementById("btn-eliminar-foto");
    const infoElement = document.getElementById("recepcion-file-info");

    if (inputFoto) inputFoto.value = "";
    if (imgPreview) imgPreview.src = "";
    if (previewContainer) previewContainer.style.display = "none";
    if (btnEliminarFoto) btnEliminarFoto.style.display = "none";
    if (infoElement) {
        infoElement.textContent = "";
        infoElement.style.display = "none";
    }
}

// limpiarFotoFactura()
// - Qué hace: Resetea el input de factura adjunta, vista previa y variables en memoria facturaDocumentoBase64 y facturaDocumentoNombre.
// - A dónde apunta:
//   - DOM: #factura-foto-input, #btn-eliminar-factura, #factura-file-info.
function limpiarFotoFactura() {
    facturaDocumentoBase64 = null;
    facturaDocumentoNombre = '';
    const inputFile = document.getElementById('factura-foto-input');
    const btnEliminar = document.getElementById('btn-eliminar-factura');
    const infoElement = document.getElementById('factura-file-info');

    if (inputFile) inputFile.value = '';
    if (btnEliminar) btnEliminar.style.display = 'none';
    if (infoElement) {
        infoElement.textContent = '';
        infoElement.style.display = 'none';
    }
}



// ABRIR MODAL
document.addEventListener("click", async function (e) {

    // abrir modal de recepcion
    const boton = e.target.closest(".receive");

    if (boton) {
        idOrdenSeleccionada = boton.dataset.id;
        limpiarFormularioRecepcion();

        document.getElementById(
            "recepcion-numero-orden"
        ).textContent = boton.dataset.numeroOrden;

        document.getElementById(
            "recepcion-proveedor"
        ).textContent = boton.dataset.proveedor;

        document.getElementById(
            "recepcion-observacion"
        ).textContent = boton.dataset.observacionRecepcion;

        const estadoOrden = (boton.dataset.estado || 'APROBADA').toUpperCase();
        const tbodyProductos = document.getElementById('recepcion-productos-body');
        if (tbodyProductos) {
            tbodyProductos.innerHTML = '<tr><td colspan="4" class="recepcion-productos-vacio">Cargando productos...</td></tr>';
        }

        // Flete: mostrar el campo solo si la orden tiene pagaFlete
        const pagaFlete = boton.dataset.pagaFlete === 'true';
        const fleteGroup = document.getElementById('recepcion-flete-group');
        const fleteInput = document.getElementById('recepcion-valor-flete');
        if (fleteGroup && fleteInput) {
            if (pagaFlete) {
                fleteGroup.style.display = 'block';
                const valorFleteInicial = boton.dataset.valorFlete ? parseFloat(boton.dataset.valorFlete) : 0;
                fleteInput.value = valorFleteInicial > 0 ? formatearPesos(valorFleteInicial) : '';
            } else {
                fleteGroup.style.display = 'none';
                fleteInput.value = '';
            }
        }

        try {
            await cargarProductosRecepcion(idOrdenSeleccionada, estadoOrden);
        } catch (error) {
            reportClientError('No se pudo abrir la recepción.', error);
        }

        document.getElementById(
            "modal-recibir-oc"
        ).style.display = "flex";

        // Si existen cambios locales previos para esta orden, aplicarlos en la UI
        if (ordenesModificadas[idOrdenSeleccionada]) {
            // recorrer filas y aplicar valores
            const filas = document.querySelectorAll('#recepcion-productos-body tr');
            filas.forEach(fila => {
                const detId = fila.dataset.idDetalle ? String(fila.dataset.idDetalle) : null;
                const prodId = fila.dataset.idProducto ? String(fila.dataset.idProducto) : null;
                const match = ordenesModificadas[idOrdenSeleccionada].find(p => String(p.idDetalle) === detId || String(p.idProducto) === prodId);
                if (match) {
                    const checkbox = fila.querySelector('.recepcion-producto-checkbox');
                    const inputCantidad = fila.querySelector('.recepcion-cantidad-input');
                    if (checkbox && inputCantidad) {
                        checkbox.checked = !!match.recibido;
                        inputCantidad.value = match.cantidadRecibida != null ? String(match.cantidadRecibida) : (match.cantidadSolicitada ? String(match.cantidadSolicitada) : '');
                        inputCantidad.disabled = !!match.recibido;
                    }
                    // actualizar el warning visual por fila
                    const warningIcon = fila.querySelector('.recepcion-warning');
                    if (warningIcon) {
                        const valorOriginal = Number(fila.dataset.cantidad || 0);
                        const valorActual = Number(inputCantidad.value || 0);
                        const modificado = checkbox.checked ? false : valorActual !== valorOriginal;
                        warningIcon.style.display = modificado ? 'inline-flex' : 'none';
                    }
                }
            });
        }

        return;
    }

    // abrir modal de cambios (clic en el icono de alerta en la lista)
    const botonAlerta = e.target.closest('.reception-alert');
    if (botonAlerta) {
        const ordenId = botonAlerta.dataset.id;
        const tr = botonAlerta.closest('tr');
        let numero = '';
        let proveedor = '';
        if (tr) {
            numero = tr.querySelector('td strong') ? tr.querySelector('td strong').textContent : '';
            const provCell = tr.querySelector('td:nth-child(3)');
            proveedor = provCell ? provCell.textContent.trim() : '';
        }
        abrirModalCambios(ordenId, numero, proveedor);
        return;
    }

});


// CANCELAR RECEPCIÓN
document
    .getElementById("btn-cancelar-recepcion")
    ?.addEventListener("click", function () {

        document.getElementById(
            "modal-recibir-oc"
        ).style.display = "none";

        limpiarFormularioRecepcion();
        idOrdenSeleccionada = null;

    });


// CONFIRMAR RECEPCIÓN
document
    .getElementById("btn-confirmar-recepcion")
    ?.addEventListener("click", async function () {

        const recibidoPor = document
            .getElementById("recepcion-recibido-por")
            .value
            .trim();

        const observacion = document
            .getElementById("recepcion-observacion")
            .value
            .trim();

        const pagaFleteSegunOrden = document.getElementById('recepcion-flete-group').style.display === 'block';
        const valorFleteInput = document.getElementById('recepcion-valor-flete');
        // Limpiar el formato de moneda (p. ej. "$15.000" -> "15000") antes de parsear
        let valorFlete = null;
        if (valorFleteInput && valorFleteInput.value.trim() !== '') {
            const limpioFlete = valorFleteInput.value
                .replace(/\$/g, '')
                .replace(/\./g, '')
                .replace(/,/g, '.')
                .trim();
            const numeroFlete = parseFloat(limpioFlete);
            valorFlete = isNaN(numeroFlete) ? null : numeroFlete;
        }

        const productos = obtenerProductosRecepcion();
        for (const producto of productos) {
            if (!producto.recibido && (producto.cantidadRecibida === null || Number(producto.cantidadRecibida) < 0)) {
                mostrarToast('Cada producto sin recibir debe tener una cantidad válida.', 'error');
                return;
            }
            if (producto.recibido) {
                producto.cantidadRecibida = producto.cantidadSolicitada;
            }
            if (!producto.recibido && Number(producto.cantidadRecibida) === 0) {
                producto.noLlego = true;
            }
        }

        // ========================
        // VALIDACIONES
        // ========================

        if (!recibidoPor) {

            mostrarToast(
                "Debes indicar quién recibió el pedido.",
                'error'
            );

            return;
        }

        if (recibidoPor.length < 3) {

            mostrarToast(
                "El nombre del receptor es demasiado corto.",
                'error'
            );

            return;
        }

        // Validación: si la orden maneja flete (pagaFlete), el valor no puede ser 0 ni vacío
        if (pagaFleteSegunOrden && (valorFlete === null || valorFlete <= 0)) {

            mostrarToast(
                "El valor del flete debe ser mayor a 0.",
                'error'
            );

            return;
        }

        // Evidencia de recepción opcional: no es obligatorio adjuntar foto/PDF.
        // Si existe, se enviará en fotoRecepcion; si no, se enviará null.

        // ========================
        // CONFIRMACIÓN
        // ========================

        const confirmar = await mostrarConfirmacion(

            `Confirme los datos de recepción:

                Recibido por: [[RECIBIDO_POR]]

                ${observacion ? "Observación:\n" + observacion : "Sin observaciones."}`,
            '¿Está seguro?', recibidoPor, 'info'
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

            const response = await csrfFetch(
                `/orden-compra/${idOrdenSeleccionada}/recibir`,
                {
                    method: "PUT",
                    headers: {
                        "Content-Type": "application/json"
                    },
                    body: JSON.stringify({
                        recibidoPor,
                        observacionRecepcion: observacion,
                        valorFlete,
                        fotoRecepcion: fotoRecepcionBase64,
                        productos
                    })
                }
            );

            if (!response.ok) {

                const mensaje =
                    await response.text();

                throw new Error(mensaje);
            }

            mostrarToast(
                "Recepción registrada correctamente.",
                'success'
            );

            limpiarFormularioRecepcion();
            idOrdenSeleccionada = null;

            document.getElementById(
                "modal-recibir-oc"
            ).style.display = "none";

            setTimeout(() => location.reload(), 1200);

        } catch (error) {

            reportClientError('Error al registrar la recepción.', error);

            mostrarToast(
                error.message ||
                "No fue posible registrar la recepción.",
                'error'
            );

        } finally {

            btn.disabled = false;

            btn.innerHTML =
                "Registrar Recepción";
        }

    });


// LIMPIAR FORMULARIO
// limpiarFormularioRecepcion()
// - Qué hace: Limpia los campos del modal de recepción (número de factura, receptor,
//   observación y valor de flete) y resetea la variable idOrdenSeleccionada.
// - Uso: se llama al cancelar la recepción o tras procesarla con éxito.
// - Endpoints: Ninguno (manipulación del DOM).
function limpiarFormularioRecepcion() {

    const recibidoPor = document.getElementById("recepcion-recibido-por");
    if (recibidoPor) recibidoPor.value = "";

    const observacion = document.getElementById("recepcion-observacion");
    if (observacion) observacion.value = "";

    const numeroOrden = document.getElementById("recepcion-numero-orden");
    if (numeroOrden) numeroOrden.textContent = "";

    const proveedor = document.getElementById("recepcion-proveedor");
    if (proveedor) proveedor.textContent = "";

    const fleteInput = document.getElementById('recepcion-valor-flete');
    if (fleteInput) {
        fleteInput.value = "";
    }

    const tbodyProductos = document.getElementById('recepcion-productos-body');
    if (tbodyProductos) {
        tbodyProductos.innerHTML = '';
    }

    limpiarFotoRecepcion();
}

// ==========================================
// MARCAR CORREO FALLIDO COMO ENVIADO MANUALMENTE
// ==========================================

let idOrdenCorreoSeleccionada = null;
let tipoEnvioCorreoSeleccionado = null;

// ABRIR MODAL (clic sobre el ícono ❌ de correo FALLIDO)
document.addEventListener("click", function (e) {

    const boton = e.target.closest(".correo-marcar-enviado");

    if (!boton) {
        return;
    }

    idOrdenCorreoSeleccionada = boton.dataset.id;
    tipoEnvioCorreoSeleccionado = boton.getAttribute('data-tipo-envio')?.toUpperCase();

    if (!idOrdenCorreoSeleccionada || !['APROBACION', 'FACTURACION'].includes(tipoEnvioCorreoSeleccionado)) {
        mostrarToast('No fue posible identificar el tipo de correo.', 'error');
        return;
    }

    document.getElementById(
        "correo-fallido-numero-orden"
    ).textContent = boton.dataset.numeroOrden || idOrdenCorreoSeleccionada;

    document.getElementById(
        "modal-correo-fallido"
    ).style.display = "flex";

});

// CANCELAR
document
    .getElementById("btn-cancelar-correo-fallido")
    ?.addEventListener("click", function () {

        document.getElementById(
            "modal-correo-fallido"
        ).style.display = "none";

        limpiarFormularioCorreoFallido();

    });

// CONFIRMAR: envía la descripción del fallo y marca el correo como ENVIADO
document
    .getElementById("btn-confirmar-correo-fallido")
    ?.addEventListener("click", async function () {

        const descripcion = document
            .getElementById("correo-fallido-descripcion")
            .value
            .trim();

        if (!descripcion) {
            mostrarToast("Debes ingresar una descripción del fallo.", 'error');
            return;
        }

        if (descripcion.length < 5) {
            mostrarToast("La descripción es demasiado corta.", 'error');
            return;
        }

        const btn = document.getElementById("btn-confirmar-correo-fallido");

        try {
            if (!idOrdenCorreoSeleccionada || !tipoEnvioCorreoSeleccionado) {
                throw new Error('No fue posible identificar el correo seleccionado.');
            }

            btn.disabled = true;
            btn.innerHTML = '<i class="fas fa-spinner fa-spin"></i> Procesando...';

            const response = await csrfFetch(
                `/orden-compra/${idOrdenCorreoSeleccionada}/correo/marcar-enviado`,
                {
                    method: "PUT",
                    headers: { "Content-Type": "application/json" },
                    body: JSON.stringify({ descripcion, tipoEnvio: tipoEnvioCorreoSeleccionado })
                }
            );

            if (!response.ok) {
                const mensaje = await response.text();
                throw new Error(mensaje);
            }

            mostrarToast("Correo marcado como enviado correctamente.", 'success');

            limpiarFormularioCorreoFallido();

            document.getElementById(
                "modal-correo-fallido"
            ).style.display = "none";

            setTimeout(() => location.reload(), 1200);

        } catch (error) {

            reportClientError('Error al marcar el correo como enviado.', error);

            mostrarToast(
                error.message || "No fue posible marcar el correo como enviado.",
                'error'
            );

        } finally {

            btn.disabled = false;
            btn.innerHTML = "Marcar como Enviado";

        }

    });

// limpiarFormularioCorreoFallido()
// - Qué hace: Limpia el textarea de descripción y resetea la orden seleccionada del
//   modal de "marcar correo como enviado".
// - Uso: se llama al cancelar o tras confirmar exitosamente la acción.
// - Endpoints: Ninguno (manipulación del DOM).
function limpiarFormularioCorreoFallido() {

    const descripcionInput = document.getElementById("correo-fallido-descripcion");
    if (descripcionInput) {
        descripcionInput.value = "";
    }

    document.getElementById("correo-fallido-numero-orden").textContent = "";

    idOrdenCorreoSeleccionada = null;
    tipoEnvioCorreoSeleccionado = null;
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

            reportClientError('Error al cargar la orden.', error);

            alert(error.message);

        }
    }
);

// abrirModalOrden()
// - Qué hace: Muestra el modal de orden estableciendo display:flex al contenedor #modal-orden.
// - Uso: llamado después de cargar una orden en el modal (cargarOrdenEnModal) para visualizarla.
// - Endpoints: Ninguno (UI cliente).
function abrirModalOrden() {

    document.getElementById(
        "modal-orden"
    ).style.display = "flex";

}

// cargarOrdenEnModal(orden)
// - Qué hace: Recibe un objeto `orden` (JSON obtenido desde GET /orden-compra/{id}) y
//   rellena el formulario/modal de orden con los datos de cabecera y las líneas (detalles).
//   Determina si la orden está en modo vista (estado distinto a 'BORRADOR') para bloquear
//   controles y ajustar la interfaz. Actualiza totales y metadatos (número, estado, fechas).
// - Uso: invocado tras obtener la orden desde el servidor para ver/editar.
// - Endpoints: consume el objeto orden proporcionado (obtenido con GET /orden-compra/{id}).
function cargarOrdenEnModal(
    orden
) {
    resetFormularioOrden();

    const esVista = orden.estado !== 'BORRADOR';
    const fechaInput = document.getElementById('fecha-orden');
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
                        <div class="autocomplete-container">
                            <input type="text" class="descripcion-producto input-control td-input" value="${detalle.descripcion || ''}" ${esVista ? 'disabled' : ''}>
                            <div class="autocomplete-dropdown"></div>
                        </div>
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
                             <span  class="material-symbols">delete</span>
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

                // Preservar la FK del producto al editar una orden existente
                const campoCodigo = fila.querySelector('.codigo-producto');
                if (campoCodigo && detalle.idProducto) {
                    campoCodigo.dataset.idProducto = detalle.idProducto;
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
    document.getElementById('flete-general').textContent = formatearPesos(Number(orden.valorFlete || 0));
    document.getElementById('total-general').textContent = formatearPesos(Number(orden.total || 0));

    const chkDescuento = document.getElementById('activar-descuento');
    const inputDescuento = document.getElementById('input-descuento');
    const lblDescuento = document.getElementById('descuento-general');

    if (chkDescuento && inputDescuento && lblDescuento) {
        const valorDescuento = Number(orden.descuento || 0);
        chkDescuento.checked = valorDescuento > 0;
        chkDescuento.disabled = esVista;
        inputDescuento.value = valorDescuento;

        if (esVista) {
            inputDescuento.style.display = 'none';
            lblDescuento.style.display = 'inline';
            lblDescuento.textContent = formatearPesos(valorDescuento);

        } else {
            if (valorDescuento > 0) {
                lblDescuento.style.display = 'none';
                inputDescuento.style.display = 'inline-block';

            } else {
                inputDescuento.style.display = 'none';
                lblDescuento.style.display = 'inline';
            }
        }
    }

    const chkFlete = document.getElementById('activar-flete');
    const inputFlete = document.getElementById('input-flete');
    const lblFlete = document.getElementById('flete-general');
    if (chkFlete && inputFlete && lblFlete) {
        const pagaFlete = !!orden.pagaFlete;
        const valorFlete = Number(orden.valorFlete || 0);
        chkFlete.checked = pagaFlete;
        chkFlete.disabled = esVista;
        inputFlete.value = valorFlete;
        if (esVista) {
            inputFlete.style.display = 'none';
            lblFlete.style.display = 'inline';
            lblFlete.textContent = formatearPesos(valorFlete);
        } else {
            if (pagaFlete) {
                lblFlete.style.display = 'none';
                inputFlete.style.display = 'inline-block';
            } else {
                inputFlete.style.display = 'none';
                lblFlete.style.display = 'inline';
            }
        }
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
