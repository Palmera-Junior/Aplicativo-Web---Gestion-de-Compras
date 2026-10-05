/*
 * Módulo de catálogos: formularios, filtros, paginación y acciones CRUD.
 * Cargado solo por catalogos.html; security.js debe cargarse antes.
 */

// Informa de manera silenciosa errores recuperables de las acciones del catálogo.
function reportCatalogError(message, error) {
    void message;
    void error;
}

function mostrarCatalogToast(mensaje) {
    window.mostrarAppToast(mensaje, 'error');
}

// Muestra únicamente el catálogo solicitado y lo desplaza al área visible.
function showSection(sectionId) {
    const sectionIds = [
        'section-usuarios',
        'section-proveedores',
        'section-productos',
        'section-sedes',
        'section-centros'
    ];

    sectionIds.forEach(id => {
        const section = document.getElementById(id);
        if (section) section.style.display = id === sectionId ? 'block' : 'none';
    });

    document.getElementById(sectionId)?.scrollIntoView({ behavior: 'auto', block: 'start' });
}

const catalogDrawerDetails = {
    proveedor: { title: 'Nuevo proveedor', editTitle: 'Editar proveedor', subtitle: 'Completa los datos para registrar al proveedor.', editSubtitle: 'Actualiza los datos del proveedor.', icon: 'local_shipping', accent: '#2f7d5b' },
    usuario: { title: 'Nuevo usuario', editTitle: 'Editar usuario', subtitle: 'Completa los datos para crear la cuenta.', editSubtitle: 'Actualiza los datos de la cuenta.', icon: 'group_add', accent: '#1a7cd8' },
    producto: { title: 'Nuevo producto', editTitle: 'Editar producto', subtitle: 'Completa los datos para registrar el producto.', editSubtitle: 'Actualiza los datos del producto.', icon: 'inventory_2', accent: '#b8791f' },
    sede: { title: 'Nueva sede', editTitle: 'Editar sede', subtitle: 'Completa los datos para registrar la sede.', editSubtitle: 'Actualiza los datos de la sede.', icon: 'store', accent: '#2b7a86' },
    centro: { title: 'Nuevo centro de costo', editTitle: 'Editar centro de costo', subtitle: 'Completa los datos para registrar el centro.', editSubtitle: 'Actualiza los datos del centro de costo.', icon: 'account_tree', accent: '#6b4fa0' }
};
let activeCatalogDrawer = null;
let catalogDrawerReturnFocus = null;

function initializeCatalogDrawers() {
    const backdrop = document.getElementById('catalog-drawer-backdrop');
    document.querySelectorAll('.catalog-drawer[data-entity]').forEach(drawer => {
        const entity = drawer.dataset.entity;
        const details = catalogDrawerDetails[entity];
        if (!details) return;

        drawer.hidden = true;
        drawer.setAttribute('role', 'dialog');
        drawer.setAttribute('aria-modal', 'true');
        drawer.setAttribute('aria-hidden', 'true');
        drawer.setAttribute('aria-label', details.title);
        drawer.style.setProperty('--drawer-accent', details.accent);

        const header = document.createElement('div');
        header.className = 'catalog-drawer-header';
        header.innerHTML = `
            <span class="catalog-drawer-icon material-symbols" aria-hidden="true">${details.icon}</span>
            <div class="catalog-drawer-heading">
                <h2>${details.title}</h2>
                <p>${details.subtitle}</p>
            </div>
            <button type="button" class="catalog-drawer-close" aria-label="Cerrar panel" title="Cerrar">
                <span class="material-symbols" aria-hidden="true">close</span>
            </button>`;
        drawer.prepend(header);
        document.body.appendChild(drawer);

        header.querySelector('.catalog-drawer-close').addEventListener('click', () => {
            controlarFormulario(entity, false);
        });
    });

    backdrop?.addEventListener('click', () => {
        if (activeCatalogDrawer) controlarFormulario(activeCatalogDrawer.dataset.entity, false);
    });

    document.addEventListener('keydown', event => {
        if (!activeCatalogDrawer) return;
        if (event.key === 'Escape') {
            event.preventDefault();
            controlarFormulario(activeCatalogDrawer.dataset.entity, false);
            return;
        }
        if (event.key !== 'Tab') return;

        const focusable = [...activeCatalogDrawer.querySelectorAll(
            'button:not(:disabled), input:not(:disabled), select:not(:disabled), textarea:not(:disabled), [href], [tabindex]:not([tabindex="-1"])'
        )].filter(element => element.getClientRects().length > 0);
        if (!focusable.length) return;

        const first = focusable[0];
        const last = focusable[focusable.length - 1];
        if (event.shiftKey && document.activeElement === first) {
            event.preventDefault();
            last.focus();
        } else if (!event.shiftKey && document.activeElement === last) {
            event.preventDefault();
            first.focus();
        }
    });
}

// Abre o cierra el panel lateral que contiene el formulario de la entidad.
function controlarFormulario(entity, open) {
    const formIds = {
        producto: 'productForm',
        usuario: 'usuarioForm',
        proveedor: 'proveedorForm',
        sede: 'sedeForm',
        centro: 'centroForm'
    };
    const form = document.getElementById(formIds[entity]);
    const drawer = form?.closest('.catalog-drawer');
    if (!drawer) return;

    const backdrop = document.getElementById('catalog-drawer-backdrop');
    if (open) {
        if (activeCatalogDrawer && activeCatalogDrawer !== drawer) {
            activeCatalogDrawer.hidden = true;
            activeCatalogDrawer.setAttribute('aria-hidden', 'true');
        }
        if (!activeCatalogDrawer) catalogDrawerReturnFocus = document.activeElement;
        activeCatalogDrawer = drawer;
        drawer.hidden = false;
        drawer.setAttribute('aria-hidden', 'false');
        document.body.classList.add('catalog-drawer-open');
        if (backdrop) backdrop.hidden = false;
        requestAnimationFrame(() => {
            drawer.querySelector('form input:not([type="hidden"]), form select, form textarea')?.focus();
        });
        return;
    }

    drawer.hidden = true;
    drawer.setAttribute('aria-hidden', 'true');
    if (activeCatalogDrawer === drawer) activeCatalogDrawer = null;
    if (!activeCatalogDrawer) {
        document.body.classList.remove('catalog-drawer-open');
        if (backdrop) backdrop.hidden = true;
        if (catalogDrawerReturnFocus instanceof HTMLElement && catalogDrawerReturnFocus.isConnected) {
            catalogDrawerReturnFocus.focus();
        }
        catalogDrawerReturnFocus = null;
    }
}

// Abre el formulario de creación de una entidad.
function abrirFormulario(entity) {
    controlarFormulario(entity, true);
}

function setCatalogDrawerMode(entity, editing) {
    const drawer = document.querySelector(`.catalog-drawer[data-entity="${entity}"]`);
    const details = catalogDrawerDetails[entity];
    if (!drawer || !details) return;
    drawer.querySelector('.catalog-drawer-heading h2').textContent = editing ? details.editTitle : details.title;
    drawer.querySelector('.catalog-drawer-heading p').textContent = editing ? details.editSubtitle : details.subtitle;
    drawer.setAttribute('aria-label', editing ? details.editTitle : details.title);
}

// Abre el formulario indicado por los accesos directos antiguos del panel.
function showCreate(kind) {
    const sectionByKind = {
        usuario: 'section-usuarios',
        proveedor: 'section-proveedores',
        producto: 'section-productos',
        sede: 'section-sedes',
        centro: 'section-centros'
    };
    const formKind = kind === 'producto' ? 'producto' : kind;
    if (!sectionByKind[kind]) return;
    showSection(sectionByKind[kind]);
    abrirFormulario(formKind);
}

// Actualiza el icono y texto del botón de creación o edición.
function setButtonLabel(buttonId, iconName, label) {
    const button = document.getElementById(buttonId);
    if (button) button.innerHTML = `<span class="material-symbols">${iconName}</span> ${label}`;
}

// Agrega una fila de presentación al formulario de productos.
function crearFilaPresentacion(nombre, cantidad, unidad, precio) {
    const body = document.getElementById('presentacionesTbody');
    if (!body) return;

    const row = document.createElement('tr');
    row.className = 'presentacion-row';
    row.innerHTML = `
        <td><input type="text" name="presentacionNombres" class="input-control pres-nombre" placeholder="Ej: Caja" aria-label="Presentación" value="${nombre || ''}" required></td>
        <td><input type="number" name="presentacionCantidades" class="input-control pres-cant" placeholder="Ej: 12" aria-label="Cantidad" min="0.01" step="any" value="${cantidad != null ? cantidad : ''}" required></td>
        <td><input type="text" name="presentacionUnidades" class="input-control pres-unidad" placeholder="Ej: g" aria-label="Unidad" value="${unidad || ''}" required></td>
        <td><input type="number" step="0.01" name="presentacionPrecios" class="input-control pres-precio" placeholder="Ej: 25000" aria-label="Precio" min="0.01" value="${precio != null ? precio : ''}" required></td>
        <td><button type="button" class="btn-icon delete pres-delete" title="Eliminar presentación"><span class="material-symbols">delete</span></button></td>
    `;
    body.appendChild(row);
}

// Reinicia la tabla de presentaciones dejando una fila vacía lista para editar.
function limpiarTablaPresentaciones() {
    const body = document.getElementById('presentacionesTbody');
    if (!body) return;
    body.replaceChildren();
    crearFilaPresentacion();
}

// Restablece campos, sedes seleccionadas y etiquetas del formulario indicado.
function resetForm(entity) {
    const valuesByEntity = {
        producto: [
            ['idProducto', ''], ['codigoInventario', ''], ['nombreProducto', ''], ['categoria', '']
        ],
        centro: [
            ['idCentroCosto', ''], ['codigoCentro', ''], ['nombreCentro', ''], ['sedeCentro', ''], ['direccionCentro', '']
        ],
        usuario: [
            ['idUsuario', ''], ['cedula', ''], ['nombreUsuarioField', ''], ['apellido', ''], ['cargo', ''],
            ['nombreUsuario', ''], ['contrasena', ''], ['rol', ''], ['sedeUsuario', '']
        ],
        proveedor: [
            ['idProv', ''], ['nit', ''], ['nombreProveedor', ''], ['ciudad', ''], ['direccionProveedor', ''],
            ['telefono', ''], ['correo', '']
        ],
        sede: [
            ['idSede', ''], ['nombreSede', ''], ['prefijoCiudad', ''], ['direccionSede', '']
        ]
    };
    const buttonByEntity = {
        producto: ['productSubmitButton', 'add', 'Crear producto'],
        centro: ['centroSubmitButton', 'add', 'Crear centro de costo'],
        usuario: ['userSubmitButton', 'add', 'Crear usuario'],
        proveedor: ['providerSubmitButton', 'add', 'Crear Proveedor'],
        sede: ['sedeSubmitButton', 'add', 'Crear sede']
    };

    (valuesByEntity[entity] || []).forEach(([id, value]) => {
        const field = document.getElementById(id);
        if (field) field.value = value;
    });

    if (entity === 'producto') limpiarTablaPresentaciones();
    if (entity === 'usuario') {
        const password = document.getElementById('contrasena');
        if (password) password.required = true;
    }
    if (entity === 'proveedor') {
        delete document.getElementById('nit')?.dataset.nitToastShown;
        document.querySelectorAll('#proveedorForm input[name="sedeIds"]').forEach(input => {
            input.checked = input.dataset.sedeNacional === 'true';
        });
    }

    const button = buttonByEntity[entity];
    if (button) setButtonLabel(...button);
    setCatalogDrawerMode(entity, false);
    controlarFormulario(entity, false);
}

// Obtiene presentaciones existentes y las carga en el formulario al editar producto.
async function cargarPresentacionesEnFormulario(productId) {
    limpiarTablaPresentaciones();
    if (!productId) return;

    try {
        const response = await fetch(`/admin/producto/${productId}/presentaciones`);
        if (!response.ok) return;
        const presentations = await response.json();
        if (!presentations?.length) return;

        const body = document.getElementById('presentacionesTbody');
        if (body) body.replaceChildren();
        presentations.forEach(item => {
            crearFilaPresentacion(item.presentacion, item.cantidad, item.unidad, item.precio);
        });
    } catch (error) {
        reportCatalogError('No se pudieron cargar las presentaciones del producto.', error);
    }
}

// Copia los datos de una fila a su formulario y lo abre en modo edición.
function editEntity(entity, element) {
    const row = element.closest('tr');
    if (!row) return;

    const sectionByEntity = {
        producto: 'section-productos',
        centro: 'section-centros',
        usuario: 'section-usuarios',
        proveedor: 'section-proveedores',
        sede: 'section-sedes'
    };
    showSection(sectionByEntity[entity]);
    abrirFormulario(entity);

    if (entity === 'producto') {
        document.getElementById('idProducto').value = row.dataset.id || '';
        document.getElementById('codigoInventario').value = row.dataset.codigo || '';
        document.getElementById('nombreProducto').value = row.dataset.nombre || '';
        document.getElementById('categoria').value = row.dataset.categoria || '';
        setButtonLabel('productSubmitButton', 'save', 'Guardar cambios');
        cargarPresentacionesEnFormulario(row.dataset.id);
    } else if (entity === 'centro') {
        document.getElementById('idCentroCosto').value = row.dataset.id || '';
        document.getElementById('codigoCentro').value = row.dataset.codigo || '';
        document.getElementById('nombreCentro').value = row.dataset.nombre || '';
        document.getElementById('sedeCentro').value = row.dataset.sedeId || '';
        document.getElementById('direccionCentro').value = row.dataset.direccion || '';
        setButtonLabel('centroSubmitButton', 'save', 'Guardar cambios');
    } else if (entity === 'usuario') {
        document.getElementById('idUsuario').value = row.dataset.id || '';
        document.getElementById('cedula').value = row.dataset.cedula || '';
        document.getElementById('nombreUsuarioField').value = row.dataset.nombre || '';
        document.getElementById('apellido').value = row.dataset.apellido || '';
        document.getElementById('cargo').value = row.dataset.cargo || '';
        document.getElementById('nombreUsuario').value = row.dataset.nombreUsuario || '';
        document.getElementById('rol').value = row.dataset.rol || '';
        document.getElementById('sedeUsuario').value = row.dataset.sedeId || '';
        document.getElementById('contrasena').required = false;
        setButtonLabel('userSubmitButton', 'save', 'Guardar cambios');
    } else if (entity === 'proveedor') {
        document.getElementById('idProv').value = row.dataset.id || '';
        document.getElementById('nit').value = row.dataset.nit || '';
        document.getElementById('nombreProveedor').value = row.dataset.nombre || '';
        document.getElementById('ciudad').value = row.dataset.ciudad || '';
        document.getElementById('direccionProveedor').value = row.dataset.direccion || '';
        document.getElementById('telefono').value = row.dataset.telefono || '';
        document.getElementById('correo').value = row.dataset.correo || '';
        setButtonLabel('providerSubmitButton', 'save', 'Guardar cambios');
        document.querySelectorAll('#proveedorForm input[name="sedeIds"]').forEach(input => {
            input.checked = input.dataset.sedeNacional === 'true'
                || (row.dataset.sedeIds || '').split(',').includes(input.value);
        });
    } else if (entity === 'sede') {
        document.getElementById('idSede').value = row.dataset.id || '';
        document.getElementById('nombreSede').value = row.dataset.nombre || '';
        document.getElementById('prefijoCiudad').value = row.dataset.prefijo || '';
        document.getElementById('direccionSede').value = row.dataset.direccion || '';
        setButtonLabel('sedeSubmitButton', 'save', 'Guardar cambios');
    }
    setCatalogDrawerMode(entity, true);
}

// Envía la solicitud de eliminación con CSRF y comunica el resultado al usuario.
async function deleteEntity(entity, id, nombre) {
    if (!id) {
        alert('ID inválido');
        return;
    }
    if (!await mostrarConfirmacion(
        'Esta acción no se puede deshacer. ¿Desea continuar con la eliminación de',
        '¿Está seguro?',
        nombre
    )) return;

    const routes = {
        producto: '/admin/productos/delete/',
        centro: '/admin/centros-costo/delete/',
        usuario: '/admin/usuarios/delete/',
        proveedor: '/admin/proveedores/delete/',
        sede: '/admin/sedes/delete/'
    };
    const route = routes[entity];
    if (!route) {
        alert(`Entidad no soportada: ${entity}`);
        return;
    }

    try {
        const response = await csrfFetch(route + id, { method: 'POST' });
        const result = await response.json().catch(() => ({ error: 'Respuesta no válida' }));
        if (!response.ok) {
            alert(result.error || 'Error al eliminar');
            return;
        }
        alert(result.success || 'Eliminado');
        location.reload();
    } catch (error) {
        alert(`Error: ${error.message}`);
    }
}

// Lee los filtros visibles y los convierte en parámetros de la consulta actual.
function collectCatalogFilters() {
    const filters = {};
    const providerSection = document.getElementById('filtros-proveedores');
    if (providerSection) {
        const search = providerSection.querySelector('input[name="search"]');
        if (search?.value.trim()) filters.searchProveedores = search.value.trim();
    }

    const userSection = document.getElementById('filtros-usuarios');
    if (userSection) {
        const search = userSection.querySelector('input[name="search"]');
        const role = userSection.querySelector('select[name="rol"]');
        const site = userSection.querySelector('select[name="sedeId"]');
        if (search?.value.trim()) filters.searchUsuarios = search.value.trim();
        if (role?.value) filters.rolUsuario = role.value;
        if (site?.value) filters.sedeIdUsuario = site.value;
    }

    const productSection = document.getElementById('filtros-productos');
    if (productSection) {
        const search = productSection.querySelector('input[name="search"]');
        const category = productSection.querySelector('select[name="categoria"]');
        if (search?.value.trim()) filters.searchProductos = search.value.trim();
        if (category?.value) filters.categoriaProducto = category.value;
    }
    return filters;
}

// Solicita la primera página filtrada desde la ruta del catálogo actualmente abierto.
async function applyCatalogFilters(entity) {
    const pageParams = {
        proveedor: 'pageProveedores',
        usuario: 'pageUsuarios',
        producto: 'pageProductos'
    };
    const fragmentIds = {
        proveedor: 'proveedoresTablaCard',
        usuario: 'usuariosTablaCard',
        producto: 'productosTablaCard'
    };
    const params = collectCatalogFilters();
    params[pageParams[entity]] = 0;
    const url = new URL(window.location.pathname, window.location.origin);
    Object.entries(params).forEach(([key, value]) => url.searchParams.set(key, value));

    try {
        const response = await fetch(url.href);
        if (!response.ok) return;
        const html = await response.text();
        const documentFromResponse = new DOMParser().parseFromString(html, 'text/html');
        const fragment = documentFromResponse.getElementById(fragmentIds[entity]);
        const target = document.getElementById(fragmentIds[entity]);
        if (fragment && target) target.innerHTML = fragment.innerHTML;
    } catch (error) {
        reportCatalogError('No se pudieron aplicar los filtros del catálogo.', error);
    }
}

// Sustituye la tabla visible por el fragmento devuelto por su endpoint de paginación.
async function cargarPaginaCatalogo(link, tableId) {
    const target = document.getElementById(tableId);
    if (!target || link.classList.contains('disabled')) return;

    try {
        const response = await fetch(link.href);
        if (!response.ok) return;
        const html = await response.text();
        const documentFromResponse = new DOMParser().parseFromString(html, 'text/html');
        const fragment = documentFromResponse.getElementById(tableId);
        if (fragment) target.innerHTML = fragment.innerHTML;
    } catch (error) {
        reportCatalogError('No se pudo cargar la página del catálogo.', error);
    }
}

// Registra filtros reactivos, paginación, filas de presentaciones y avisos temporales.
document.addEventListener('DOMContentLoaded', () => {
    initializeCatalogDrawers();

    const nitProveedor = document.getElementById('nit');
    const formularioProveedor = document.getElementById('proveedorForm');
    nitProveedor?.addEventListener('input', () => {
        const valor = nitProveedor.value.trim();
        if (valor && !/^\d+$/.test(valor)) {
            if (nitProveedor.dataset.nitToastShown !== 'true') {
                mostrarCatalogToast('El NIT del proveedor solo debe contener números.');
                nitProveedor.dataset.nitToastShown = 'true';
            }
            return;
        }
        delete nitProveedor.dataset.nitToastShown;
    });
    formularioProveedor?.addEventListener('submit', event => {
        if (nitProveedor?.value.trim() && !/^\d+$/.test(nitProveedor.value.trim())) {
            event.preventDefault();
            mostrarCatalogToast('El NIT del proveedor debe ser un número entero.');
            nitProveedor.focus();
        }
    });

    document.querySelectorAll('[data-catalog-create]').forEach(button => {
        button.addEventListener('click', () => {
            const entity = button.dataset.catalogCreate;
            resetForm(entity);
            controlarFormulario(entity, true);
        });
    });

    const pageTables = {
        '.js-pagina-productos': 'productosTablaCard',
        '.js-pagina-proveedores': 'proveedoresTablaCard',
        '.js-pagina-usuarios': 'usuariosTablaCard'
    };
    document.addEventListener('click', event => {
        const link = event.target.closest(Object.keys(pageTables).join(','));
        if (!link || link.classList.contains('disabled')) return;
        event.preventDefault();
        const selector = Object.keys(pageTables).find(item => link.matches(item));
        cargarPaginaCatalogo(link, pageTables[selector]);
    });

    document.querySelectorAll('.filtro-busqueda').forEach(input => {
        const entity = input.closest('.filtros-section')?.dataset.entity;
        if (!entity) return;
        let debounceTimer;
        input.addEventListener('input', () => {
            clearTimeout(debounceTimer);
            debounceTimer = setTimeout(() => applyCatalogFilters(entity), 400);
        });
    });

    document.querySelectorAll('.filtro-select').forEach(select => {
        const entity = select.closest('.filtros-section')?.dataset.entity;
        if (entity) select.addEventListener('change', () => applyCatalogFilters(entity));
    });

    document.querySelectorAll('.btn-limpiar-filtro').forEach(button => {
        button.addEventListener('click', () => {
            const entity = button.dataset.entity;
            const sectionId = `filtros-${entity === 'proveedor' ? 'proveedores' : `${entity}s`}`;
            const section = document.getElementById(sectionId);
            if (!section) return;
            section.querySelectorAll('input, select').forEach(field => {
                if (field.type === 'checkbox' || field.type === 'radio') field.checked = false;
                else field.value = '';
            });
            applyCatalogFilters(entity);
        });
    });

    document.getElementById('btn-agregar-presentacion')?.addEventListener('click', () => {
        crearFilaPresentacion();
    });

    document.addEventListener('click', event => {
        const button = event.target.closest('.pres-delete');
        if (!button) return;
        const body = document.getElementById('presentacionesTbody');
        const row = button.closest('tr');
        if (!body || !row) return;
        if (body.rows.length > 1) row.remove();
        else limpiarTablaPresentaciones();
    });

    document.querySelectorAll('.alert-success, .alert-error').forEach(alert => {
        setTimeout(() => {
            alert.classList.add('alert-fade');
            setTimeout(() => alert.remove(), 500);
        }, 3000);
    });
});
