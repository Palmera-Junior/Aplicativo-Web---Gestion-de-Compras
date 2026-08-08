// Maneja la visibilidad de secciones de administrador
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
});

function showCreate(kind){
    if(kind==='usuario') showSection('section-usuarios');
    if(kind==='proveedor') showSection('section-proveedores');
    if(kind==='producto') showSection('section-productos');
    if(kind==='sede') showSection('section-sedes');
    if(kind==='centro') showSection('section-centros');
}

function setButtonLabel(buttonId, iconName, label){
    const button = document.getElementById(buttonId);
    if(!button) return;
    button.innerHTML = `<span class="material-symbols">${iconName}</span> ${label}`;
}

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

function limpiarTablaPresentaciones(){
    const tbody = document.getElementById('presentacionesTbody');
    if(!tbody) return;
    tbody.innerHTML = '';
    crearFilaPresentacion();
}

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
