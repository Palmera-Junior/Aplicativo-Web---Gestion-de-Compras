// codigo acciones del MODAL ##########################################################333

// ==========================================
// TOAST (mensaje temporal auto-ocultable)
// ==========================================
// mostrarToast(mensaje, tipo)
// - Qué hace: Muestra un mensaje tipo toast en pantalla (temporal) con estilo según `tipo`
//   ('success' por defecto). El toast se oculta automáticamente a los ~3 segundos.
// - Uso: usado en múltiples acciones para dar feedback al usuario (éxito/error/info).
// - Endpoints: Ninguno (UI cliente).
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

    const confirmar = confirm(
        "¿Está seguro de aprobar esta Orden de Compra?\n\n" +
        "Después de aprobarla no podrá modificar productos ni valores."
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

// ELIMINAR ORDEN (solo para BORRADOR y creador)
document.addEventListener("click", async function (e) {
    const boton = e.target.closest(".delete-orden");
    if (!boton) return;

    const idOrden = boton.dataset.id;
    const numeroOrden = boton.dataset.numeroOrden || "";

    if (!idOrden) {
        mostrarToast('ID de orden no disponible para eliminarla.', 'error');
        return;
    }

    const confirmar = confirm(
        "¿Está seguro de eliminar esta Orden de Compra?\n\n" +
        "N° Orden: " + (numeroOrden || idOrden) + "\n\n" +
        "Solo podrá eliminar una orden en estado BORRADOR y únicamente si usted fue quien la creó.\n\n" +
        "Esta acción no se puede deshacer."
    );

    if (!confirmar) {
        return;
    }

    try {
        const response = await csrfFetch(`/orden-compra/${idOrden}`, {
            method: "DELETE"
        });

        if (!response.ok) {
            const mensaje = await response.text();
            throw new Error(mensaje || "No se pudo eliminar la orden.");
        }

        mostrarToast("Orden eliminada correctamente.", 'success');
        setTimeout(() => location.reload(), 1200);
    } catch (error) {
        reportClientError('Error al eliminar la orden.', error);
        mostrarToast(error.message, 'error');
    }
});

// DESCARGAR PDF (solo para APROBADA/RECIBIDA)
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

// Manejo de captura / subida de foto de recepción
document.addEventListener("DOMContentLoaded", () => {
    const inputFoto = document.getElementById("recepcion-foto-input");
    const btnTomarFoto = document.getElementById("btn-tomar-foto");
    const btnEliminarFoto = document.getElementById("btn-eliminar-foto");
    const previewContainer = document.getElementById("recepcion-foto-preview-container");
    const imgPreview = document.getElementById("recepcion-foto-preview");

    if (btnTomarFoto && inputFoto) {
        btnTomarFoto.addEventListener("click", () => {
            inputFoto.click();
        });
    }

    if (inputFoto) {
        inputFoto.addEventListener("change", function (e) {
            const file = e.target.files && e.target.files[0];
            if (!file) return;

            if (!file.type.startsWith("image/")) {
                mostrarToast("El archivo seleccionado debe ser una imagen.", "error");
                inputFoto.value = "";
                return;
            }

            const reader = new FileReader();
            reader.onload = function (readerEvent) {
                const img = new Image();
                img.onload = function () {
                    // Redimensionar para optimizar tamaño manteniendo buena calidad
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

                    const canvas = document.createElement("canvas");
                    canvas.width = width;
                    canvas.height = height;
                    const ctx = canvas.getContext("2d");
                    ctx.drawImage(img, 0, 0, width, height);

                    const dataUrl = canvas.toDataURL("image/jpeg", 0.85);
                    fotoRecepcionBase64 = dataUrl;

                    if (imgPreview) {
                        imgPreview.src = dataUrl;
                    }
                    if (previewContainer) {
                        previewContainer.style.display = "flex";
                    }
                    if (btnEliminarFoto) {
                        btnEliminarFoto.style.display = "inline-flex";
                    }
                };
                img.src = readerEvent.target.result;
            };
            reader.readAsDataURL(file);
        });
    }

    if (btnEliminarFoto) {
        btnEliminarFoto.addEventListener("click", function () {
            limpiarFotoRecepcion();
        });
    }
});

function limpiarFotoRecepcion() {
    fotoRecepcionBase64 = null;
    const inputFoto = document.getElementById("recepcion-foto-input");
    const imgPreview = document.getElementById("recepcion-foto-preview");
    const previewContainer = document.getElementById("recepcion-foto-preview-container");
    const btnEliminarFoto = document.getElementById("btn-eliminar-foto");

    if (inputFoto) inputFoto.value = "";
    if (imgPreview) imgPreview.src = "";
    if (previewContainer) previewContainer.style.display = "none";
    if (btnEliminarFoto) btnEliminarFoto.style.display = "none";
}


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

        // ========================
        // VALIDACIONES
        // ========================

        if (!numeroFactura) {

            mostrarToast(
                "Debes ingresar el número de factura.",
                'error'
            );

            return;
        }

        if (numeroFactura.length < 3) {

            mostrarToast(
                "El número de factura parece inválido.",
                'error'
            );

            return;
        }

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

            const response = await csrfFetch(
                `/orden-compra/${idOrdenSeleccionada}/recibir`,
                {
                    method: "PUT",
                    headers: {
                        "Content-Type": "application/json"
                    },
                    body: JSON.stringify({
                        numeroFactura,
                        recibidoPor,
                        observacionRecepcion: observacion,
                        valorFlete,
                        fotoRecepcion: fotoRecepcionBase64
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

    const fleteInput = document.getElementById('recepcion-valor-flete');
    if (fleteInput) {
        fleteInput.value = "";
    }

    limpiarFotoRecepcion();

    idOrdenSeleccionada = null;
}

// ==========================================
// MARCAR CORREO FALLIDO COMO ENVIADO MANUALMENTE
// ==========================================

let idOrdenCorreoSeleccionada = null;

// ABRIR MODAL (clic sobre el ícono ❌ de correo FALLIDO)
document.addEventListener("click", function (e) {

    const boton = e.target.closest(".correo-marcar-enviado");

    if (!boton) {
        return;
    }

    idOrdenCorreoSeleccionada = boton.dataset.id;

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
            btn.disabled = true;
            btn.innerHTML = '<i class="fas fa-spinner fa-spin"></i> Procesando...';

            const response = await csrfFetch(
                `/orden-compra/${idOrdenCorreoSeleccionada}/correo/marcar-enviado`,
                {
                    method: "PUT",
                    headers: { "Content-Type": "application/json" },
                    body: JSON.stringify({ descripcion })
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
