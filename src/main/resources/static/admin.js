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

    // Botón refrescar página
    const btnRefrescar = document.getElementById('btn-refrescar');
    if(btnRefrescar){
        btnRefrescar.addEventListener('click',()=>{
            window.location.reload();
        });
    }

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

function resetForm(entity){
    if(entity === 'producto'){
        document.getElementById('idProducto').value = '';
        document.getElementById('codigoInventario').value = '';
        document.getElementById('nombreProducto').value = '';
        document.getElementById('presentacion').value = '';
        document.getElementById('descripcion').value = '';
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

function editEntity(entity, element){
    const row = element.closest('tr');
    if(!row) return;
    if(entity==='producto'){
        showSection('section-productos');
        document.getElementById('idProducto').value = row.dataset.id || '';
        document.getElementById('codigoInventario').value = row.dataset.codigo || '';
        document.getElementById('nombreProducto').value = row.dataset.nombre || '';
        document.getElementById('presentacion').value = row.dataset.presentacion || '';
        document.getElementById('descripcion').value = row.dataset.descripcion || '';
        setButtonLabel('productSubmitButton', 'save', 'Guardar cambios');
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
