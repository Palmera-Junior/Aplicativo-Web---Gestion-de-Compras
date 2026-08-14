// Maneja la visibilidad de secciones de administrador
// showSection(id)
// - Qué hace: Muestra la sección administrativa cuyo id se pasa (p.ej. 'section-productos'),
//   oculta las demás secciones listadas, marca la tarjeta de módulo correspondiente como
//   'selected' y desplaza la vista a la sección visible (scroll suave).
// - Interacciones DOM: elementos con ids 'section-usuarios','section-proveedores',
//   'section-productos','section-sedes','section-centros', y elementos '.module-card'.
// - Endpoints: Ninguno (solo manipulación del DOM).
function showSection(id){
    const sections = ['section-usuarios','section-proveedores','section-productos','section-sedes','section-centros'];
    sections.forEach(s => {
        const el = document.getElementById(s);
        if(!el) return;
        el.style.display = (s===id) ? 'block' : 'none';
    });
    document.querySelectorAll('.module-card').forEach(c=>c.classList.remove('selected'));
    const card = document.querySelector('.module-card[data-target="'+id+'"]');
    if(card) card.classList.add('selected');
    const visible = document.getElementById(id);
    if(visible) visible.scrollIntoView({behavior:'smooth',block:'start'});
}

document.addEventListener('DOMContentLoaded',()=>{
    document.querySelectorAll('.module-card').forEach(card=>{
        card.addEventListener('click',()=>{
            const target = card.getAttribute('data-target');
            showSection(target);
        });
    });

    // Paginación AJAX de productos (sin recargar la página)
    document.addEventListener('click', async (e) => {
        const link = e.target.closest('.js-pagina-productos');
        if(!link || link.classList.contains('disabled')) return;
        e.preventDefault();
        const target = document.getElementById('productosTablaCard');
        if(!target) return;
        try{
            const res = await fetch(link.href);
            if(!res.ok) return;
            const htmlTexto = await res.text();
            const doc = new DOMParser().parseFromString(htmlTexto, 'text/html');
            const nuevoFragmento = doc.getElementById('productosTablaCard');
            if(nuevoFragmento){
                target.innerHTML = nuevoFragmento.innerHTML;
            }
            // Mantener la sección de productos abierta y visible
            showSection('section-productos');
        }catch(err){
            console.error('Error cargando página de productos:', err);
        }
    });

    // Paginación AJAX de proveedores (sin recargar la página)
    document.addEventListener('click', async (e) => {
        const link = e.target.closest('.js-pagina-proveedores');
        if(!link || link.classList.contains('disabled')) return;
        e.preventDefault();
        const target = document.getElementById('proveedoresTablaCard');
        if(!target) return;
        try{
            const res = await fetch(link.href);
            if(!res.ok) return;
            const htmlTexto = await res.text();
            const doc = new DOMParser().parseFromString(htmlTexto, 'text/html');
            const nuevoFragmento = doc.getElementById('proveedoresTablaCard');
            if(nuevoFragmento){
                target.innerHTML = nuevoFragmento.innerHTML;
            }
            // Mantener la sección de proveedores abierta y visible
            showSection('section-proveedores');
        }catch(err){
            console.error('Error cargando página de proveedores:', err);
        }
    });

    // Paginación AJAX de usuarios (sin recargar la página)
    document.addEventListener('click', async (e) => {
        const link = e.target.closest('.js-pagina-usuarios');
        if(!link || link.classList.contains('disabled')) return;
        e.preventDefault();
        const target = document.getElementById('usuariosTablaCard');
        if(!target) return;
        try{
            const res = await fetch(link.href);
            if(!res.ok) return;
            const htmlTexto = await res.text();
            const doc = new DOMParser().parseFromString(htmlTexto, 'text/html');
            const nuevoFragmento = doc.getElementById('usuariosTablaCard');
            if(nuevoFragmento){
                target.innerHTML = nuevoFragmento.innerHTML;
            }
            // Mantener la sección de usuarios abierta y visible
            showSection('section-usuarios');
        }catch(err){
            console.error('Error cargando página de usuarios:', err);
        }
    });



    // Botón refrescar página
    const btnRefrescar = document.getElementById('btn-refrescar');
    if(btnRefrescar){
        btnRefrescar.addEventListener('click',()=>{
            window.location.reload();
        });
    }

    // Agregar presentación
    const btnAgregarPres = document.getElementById('btn-agregar-presentacion');
    if(btnAgregarPres){
        btnAgregarPres.addEventListener('click',()=>{
            crearFilaPresentacion();
        });
    }

    // Eliminar presentación (delegación)
    document.addEventListener('click',(e)=>{
        const btn = e.target.closest('.pres-delete');
        if(!btn) return;
        const tbody = document.getElementById('presentacionesTbody');
        if(!tbody) return;
        const fila = btn.closest('tr');
        if(tbody.rows.length > 1){
            fila.remove();
        } else {
            crearFilaPresentacion();
            limpiarTablaPresentaciones();
        }
    });

    // Auto-ocultar alertas de éxito/error a los 3 segundos
    const alertas = document.querySelectorAll('.alert-success, .alert-error');
    alertas.forEach(alerta => {
        setTimeout(() => {
            alerta.classList.add('alert-fade');
            setTimeout(() => {
                if (alerta.parentNode) {
                    alerta.parentNode.removeChild(alerta);
                }
            }, 500);
        }, 3000);
    });

    // -----------------------
    // Filtros dinámicos (búsqueda y selects)
    // -----------------------
    // debounce(fn, wait)
// - Qué hace: Devuelve una versión con debounce de la función `fn`, retrasando su ejecución
//   hasta que hayan pasado `wait` ms sin nuevas invocaciones. Utilizada para evitar llamadas
//   frecuentes mientras el usuario escribe en inputs de filtro.
// - Endpoints: Ninguno (utilidad cliente).
function debounce(fn, wait){
        let t;
        return function(...args){
            clearTimeout(t);
            t = setTimeout(()=> fn.apply(this, args), wait);
        };
    }

    // collectFilters()
// - Qué hace: Recolecta los valores actuales de los filtros visibles en las secciones de
//   proveedores, usuarios y productos (inputs y selects) y devuelve un objeto con los
//   parámetros listos para incluir en la query string cuando se aplican filtros.
// - Uso: Llamada por applyFilters(entity).
// - Endpoints: Ninguno (prepara parámetros para peticiones GET hacia /admin).
function collectFilters(){
        const data = {};
        const provSection = document.getElementById('filtros-proveedores');
        if(provSection){
            const s = provSection.querySelector('input[name="search"]');
            if(s && s.value.trim()) data.searchProveedores = s.value.trim();
        }
        const usuariosSection = document.getElementById('filtros-usuarios');
        if(usuariosSection){
            const s = usuariosSection.querySelector('input[name="search"]');
            if(s && s.value.trim()) data.searchUsuarios = s.value.trim();
            const rol = usuariosSection.querySelector('select[name="rol"]');
            if(rol && rol.value) data.rolUsuario = rol.value;
            const sede = usuariosSection.querySelector('select[name="sedeId"]');
            if(sede && sede.value) data.sedeIdUsuario = sede.value;
        }
        const prodSection = document.getElementById('filtros-productos');
        if(prodSection){
            const s = prodSection.querySelector('input[name="search"]');
            if(s && s.value.trim()) data.searchProductos = s.value.trim();
            const cat = prodSection.querySelector('select[name="categoria"]');
            if(cat && cat.value) data.categoriaProducto = cat.value;
        }
        return data;
    }

    const pageParamMap = { proveedor: 'pageProveedores', usuario: 'pageUsuarios', producto: 'pageProductos' };
    const fragmentMap = { proveedor: 'proveedoresTablaCard', usuario: 'usuariosTablaCard', producto: 'productosTablaCard' };

    // applyFilters(entity)
// - Qué hace: Construye una URL a /admin con los parámetros devueltos por collectFilters(),
//   fuerza la página a la página 0 para la entidad indicada y realiza fetch() (GET) para
//   obtener el fragmento HTML actualizado. Reemplaza el fragmento correspondiente en DOM y
//   mantiene visible la sección relacionada.
// - Parámetro: entity ∈ { 'proveedor', 'usuario', 'producto' } — determina qué fragmento
//   actualizar (proveedoresTablaCard, usuariosTablaCard, productosTablaCard).
// - Endpoints: GET /admin?{params}
async function applyFilters(entity){
        const params = collectFilters();
        // reset the page for this entity to 0 when applying filters
        params[pageParamMap[entity]] = 0;
        const url = new URL('/admin', window.location.origin);
        Object.entries(params).forEach(([k,v]) => url.searchParams.set(k, v));
        try{
            const res = await fetch(url.href);
            if(!res.ok) return;
            const htmlTexto = await res.text();
            const doc = new DOMParser().parseFromString(htmlTexto, 'text/html');
            const fragId = fragmentMap[entity];
            const nuevoFragmento = doc.getElementById(fragId);
            if(nuevoFragmento){
                const target = document.getElementById(fragId);
                if(target) target.innerHTML = nuevoFragmento.innerHTML;
            }
            // Mantener la sección visible
            const sectionId = 'section-' + (entity === 'proveedor' ? 'proveedores' : entity === 'usuario' ? 'usuarios' : 'productos');
            showSection(sectionId);
        }catch(err){
            console.error('Error aplicando filtros:', err);
        }
    }

    // debounce para inputs de búsqueda
    document.querySelectorAll('.filtro-busqueda').forEach(input => {
        const section = input.closest('.filtros-section');
        const entity = section ? section.dataset.entity : null;
        if(!entity) return;
        input.addEventListener('input', debounce(()=> applyFilters(entity), 400));
    });

    // cambio en selects de filtro
    document.querySelectorAll('.filtro-select').forEach(sel => {
        const section = sel.closest('.filtros-section');
        const entity = section ? section.dataset.entity : null;
        if(!entity) return;
        sel.addEventListener('change', ()=> applyFilters(entity));
    });

    // limpiar filtros por sección
    document.querySelectorAll('.btn-limpiar-filtro').forEach(btn => {
        btn.addEventListener('click', ()=>{
            const entity = btn.getAttribute('data-entity');
            const sectionId = 'filtros-' + (entity === 'proveedor' ? 'proveedores' : entity + 's');
            const section = document.getElementById(sectionId);
            if(!section) return;
            section.querySelectorAll('input, select').forEach(el => {
                if(el.tagName === 'INPUT'){
                    if(el.type === 'checkbox' || el.type === 'radio') el.checked = false;
                    else el.value = '';
                } else if(el.tagName === 'SELECT'){
                    el.value = '';
                }
            });
            applyFilters(entity);
        });
    });

});

// showCreate(kind)
// - Qué hace: Atajo para abrir la sección de creación correspondiente según el `kind`:
//   'usuario'|'proveedor'|'producto'|'sede'|'centro'. Internamente llama a showSection().
// - Uso: enlazado desde botones/acciones que ponen al usuario en el formulario de creación.
// - Endpoints: Ninguno (manipulación del DOM).
function showCreate(kind){
    if(kind==='usuario') showSection('section-usuarios');
    if(kind==='proveedor') showSection('section-proveedores');
    if(kind==='producto') showSection('section-productos');
    if(kind==='sede') showSection('section-sedes');
    if(kind==='centro') showSection('section-centros');
}

// setButtonLabel(buttonId, iconName, label)
// - Qué hace: Actualiza el contenido HTML de un botón (por id) con un icono y una etiqueta.
// - Uso: usado para cambiar la etiqueta de botones submit entre 'Crear ...' y 'Guardar cambios'.
// - Endpoints: Ninguno (manipulación del DOM).
function setButtonLabel(buttonId, iconName, label){
    const button = document.getElementById(buttonId);
    if(!button) return;
    button.innerHTML = `<span class="material-symbols">${iconName}</span> ${label}`;
}

// crearFilaPresentacion(nombre, cantidad, unidad, precio)
// - Qué hace: Crea y añade una fila en el tbody 'presentacionesTbody' con inputs para
//   presentacion (nombre), cantidad, unidad y precio. Si se pasan valores los inserta.
// - Uso: utilizado en el formulario de producto para gestionar varias presentaciones.
// - Endpoints: Ninguno (manipulación del DOM).
function crearFilaPresentacion(nombre, cantidad, unidad, precio){
    const tbody = document.getElementById('presentacionesTbody');
    if(!tbody) return;
    const tr = document.createElement('tr');
    tr.className = 'presentacion-row';
    tr.innerHTML = `
        <td><input type="text" name="presentacionNombres" class="input-control pres-nombre" placeholder="Ej: Caja" value="${nombre || ''}"></td>
        <td><input type="number" name="presentacionCantidades" class="input-control pres-cant" min="0" value="${cantidad != null ? cantidad : ''}"></td>
        <td><input type="text" name="presentacionUnidades" class="input-control pres-unidad" placeholder="Ej: g" value="${unidad || ''}"></td>
        <td><input type="number" step="0.01" name="presentacionPrecios" class="input-control pres-precio" min="0" value="${precio != null ? precio : ''}"></td>
        <td>
            <button type="button" class="btn-icon delete pres-delete" title="Eliminar presentación">
                <span class="material-symbols">delete</span>
            </button>
        </td>
    `;
    tbody.appendChild(tr);
}

// limpiarTablaPresentaciones()
// - Qué hace: Vacía el tbody 'presentacionesTbody' y agrega una fila vacía (crearFilaPresentacion).
// - Uso: limpiar/reiniciar el conjunto de presentaciones en el formulario de producto.
// - Endpoints: Ninguno (manipulación del DOM).
function limpiarTablaPresentaciones(){
    const tbody = document.getElementById('presentacionesTbody');
    if(!tbody) return;
    tbody.innerHTML = '';
    crearFilaPresentacion();
}

// resetForm(entity)
// - Qué hace: Resetea los campos del formulario correspondiente a la entidad indicada
//   ('producto','centro','usuario','proveedor','sede'), restablece valores por defecto y
//   actualiza la etiqueta del botón de envío. También ajusta 'required' en campos como
//   la contraseña cuando corresponde.
// - Uso: se llama al crear o limpiar formularios antes de usar el editor.
// - Endpoints: Ninguno (manipulación del DOM).
function resetForm(entity){
if(entity === 'producto'){
        document.getElementById('idProducto').value = '';
        document.getElementById('codigoInventario').value = '';
        document.getElementById('nombreProducto').value = '';
        document.getElementById('categoria').value = '';
        limpiarTablaPresentaciones();
        setButtonLabel('productSubmitButton', 'add', 'Crear producto');
    }
if(entity === 'centro'){
        document.getElementById('idCentroCosto').value = '';
        document.getElementById('codigoCentro').value = '';
        document.getElementById('nombreCentro').value = '';
        document.getElementById('sedeCentro').value = '';
        document.getElementById('direccionCentro').value = '';
        setButtonLabel('centroSubmitButton', 'add', 'Crear centro de costo');
    }
    if(entity === 'usuario'){
        document.getElementById('idUsuario').value = '';
        document.getElementById('cedula').value = '';
        document.getElementById('nombreUsuarioField').value = '';
        document.getElementById('apellido').value = '';
        document.getElementById('cargo').value = '';
        document.getElementById('nombreUsuario').value = '';
        document.getElementById('contrasena').value = '';
        document.getElementById('rol').value = '';
        document.getElementById('sedeUsuario').value = '';
        setButtonLabel('userSubmitButton', 'add', 'Crear usuario');
        document.getElementById('contrasena').required = true;
    }
    if(entity === 'proveedor'){
        document.getElementById('idProv').value = '';
        document.getElementById('nit').value = '';
        document.getElementById('nombreProveedor').value = '';
        document.getElementById('ciudad').value = '';
        document.getElementById('direccionProveedor').value = '';
        document.getElementById('telefono').value = '';
        document.getElementById('correo').value = '';
        setButtonLabel('providerSubmitButton', 'add', 'Crear Proveedor');
        document.querySelectorAll('#proveedorForm input[name="sedeIds"]').forEach(input => input.checked = false);
    }
    if(entity === 'sede'){
        document.getElementById('idSede').value = '';
        document.getElementById('nombreSede').value = '';
        document.getElementById('prefijoCiudad').value = '';
        document.getElementById('direccionSede').value = '';
        setButtonLabel('sedeSubmitButton', 'add', 'Crear sede');
    }
}

// cargarPresentacionesEnFormulario(idProducto)
// - Qué hace: Limpia la tabla de presentaciones y, si se proporciona idProducto,
//   realiza un GET a '/admin/producto/{idProducto}/presentaciones' para recibir JSON
//   con las presentaciones del producto y las inserta en el formulario.
// - Endpoint: GET /admin/producto/{idProducto}/presentaciones  (retorna JSON: array de presentaciones)
async function cargarPresentacionesEnFormulario(idProducto){
    limpiarTablaPresentaciones();
    if(!idProducto) return;
    try{
        const res = await fetch('/admin/producto/' + idProducto + '/presentaciones');
        if(!res.ok) return;
        const presentaciones = await res.json();
        if(!presentaciones || presentaciones.length === 0){
            return;
        }
        const tbody = document.getElementById('presentacionesTbody');
        if(tbody) tbody.innerHTML = '';
        presentaciones.forEach(pres => {
            crearFilaPresentacion(pres.presentacion, pres.cantidad, pres.unidad, pres.precio);
        });
    }catch(e){
        console.error('Error cargando presentaciones:', e);
    }
}

// editEntity(entity, element)
// - Qué hace: Carga los datos de la fila (element.closest('tr')) en el formulario de edición
//   según la entidad (producto, centro, usuario, proveedor, sede). Actualiza la vista para
//   mostrar el formulario correspondiente y cambia la etiqueta del botón a 'Guardar cambios'.
// - Uso: invocado al clicar el botón de editar en tablas de listado.
// - Endpoints: llama a cargarPresentacionesEnFormulario() para productos (que a su vez hace
//   una petición GET a /admin/producto/{id}/presentaciones).
function editEntity(entity, element){
    const row = element.closest('tr');
    if(!row) return;
    if(entity === 'producto'){
        showSection('section-productos');
        document.getElementById('idProducto').value = row.dataset.id || '';
        document.getElementById('codigoInventario').value = row.dataset.codigo || '';
        document.getElementById('nombreProducto').value = row.dataset.nombre || '';
        document.getElementById('categoria').value = row.dataset.categoria || '';
        setButtonLabel('productSubmitButton', 'save', 'Guardar cambios');
        cargarPresentacionesEnFormulario(row.dataset.id);
        return;
    }
if(entity==='centro'){
        showSection('section-centros');
        document.getElementById('idCentroCosto').value = row.dataset.id || '';
        document.getElementById('codigoCentro').value = row.dataset.codigo || '';
        document.getElementById('nombreCentro').value = row.dataset.nombre || '';
        document.getElementById('sedeCentro').value = row.dataset.sedeId || '';
        document.getElementById('direccionCentro').value = row.dataset.direccion || '';
        setButtonLabel('centroSubmitButton', 'save', 'Guardar cambios');
        return;
    }
    if(entity==='usuario'){
        showSection('section-usuarios');
        document.getElementById('idUsuario').value = row.dataset.id || '';
        document.getElementById('cedula').value = row.dataset.cedula || '';
        document.getElementById('nombreUsuarioField').value = row.dataset.nombre || '';
        document.getElementById('apellido').value = row.dataset.apellido || '';
        document.getElementById('cargo').value = row.dataset.cargo || '';
        document.getElementById('nombreUsuario').value = row.dataset.nombreUsuario || '';
        document.getElementById('rol').value = row.dataset.rol || '';
        document.getElementById('sedeUsuario').value = row.dataset.sedeId || '';
        setButtonLabel('userSubmitButton', 'save', 'Guardar cambios');
        document.getElementById('contrasena').required = false;
        return;
    }
    if(entity==='proveedor'){
        showSection('section-proveedores');
        document.getElementById('idProv').value = row.dataset.id || '';
        document.getElementById('nit').value = row.dataset.nit || '';
        document.getElementById('nombreProveedor').value = row.dataset.nombre || '';
        document.getElementById('ciudad').value = row.dataset.ciudad || '';
        document.getElementById('direccionProveedor').value = row.dataset.direccion || '';
        document.getElementById('telefono').value = row.dataset.telefono || '';
        document.getElementById('correo').value = row.dataset.correo || '';
        setButtonLabel('providerSubmitButton', 'save', 'Guardar cambios');
        document.querySelectorAll('#proveedorForm input[name="sedeIds"]').forEach(input => {
            input.checked = false;
        });
        const sedeIds = row.dataset.sedeIds ? row.dataset.sedeIds.split(',').filter(Boolean) : [];
        sedeIds.forEach(id => {
            const checkbox = document.querySelector('#proveedorForm input[name="sedeIds"][value="' + id + '"]');
            if(checkbox) checkbox.checked = true;
        });
        return;
    }
    if(entity==='sede'){
        showSection('section-sedes');
        document.getElementById('idSede').value = row.dataset.id || '';
        document.getElementById('nombreSede').value = row.dataset.nombre || '';
        document.getElementById('prefijoCiudad').value = row.dataset.prefijo || '';
        document.getElementById('direccionSede').value = row.dataset.direccion || '';
        setButtonLabel('sedeSubmitButton', 'save', 'Guardar cambios');
        return;
    }
}

// deleteEntity(entity, id)
// - Qué hace: Pide confirmación al usuario y realiza la petición adecuada para eliminar
//   la entidad indicada. Mapea entidades a rutas de eliminación y ejecuta POST a la ruta
//   correspondiente (según convención del backend). Muestra alertas según la respuesta
//   y recarga la página al completar.
// - Endpoints:
//     producto -> POST /admin/productos/delete/{id}
//     centro   -> POST /admin/centros-costo/delete/{id}
//     usuario  -> POST /admin/usuarios/delete/{id}
//     proveedor-> POST /admin/proveedores/delete/{id}
//     sede     -> POST /admin/sedes/delete/{id}
async function deleteEntity(entity, id){
    if(!id){ alert('ID inválido'); return; }
    if(!confirm('¿Eliminar ' + entity + ' con id=' + id + '? Esta acción no se puede revertir.')) return;
    const routes = {
        producto: '/admin/productos/delete/',
        centro: '/admin/centros-costo/delete/',
        usuario: '/admin/usuarios/delete/',
        proveedor: '/admin/proveedores/delete/',
        sede: '/admin/sedes/delete/'
    };
    const url = routes[entity];
    if(!url){ alert('Entidad no soportada: ' + entity); return; }
    try{
        const res = await fetch(url + id, { method: 'POST' });
        const json = await res.json().catch(()=>({error:'Respuesta no válida'}));
        if(!res.ok){ alert(json.error || 'Error al eliminar'); return; }
        alert(json.success || 'Eliminado');
        location.reload();
    }catch(e){ alert('Error: '+e.message); }
}
