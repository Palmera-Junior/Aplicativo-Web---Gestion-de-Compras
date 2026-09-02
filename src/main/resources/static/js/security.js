/**
 * =============================================================================
 * MÓDULO DE SEGURIDAD CLIENTE (CSRF)
 * =============================================================================
 * 
 * csrfFetch(input, init)
 * - Qué hace:
 *   Envuelve la función estándar de JavaScript `fetch()` para inyectar automáticamente
 *   el token y encabezado CSRF (Cross-Site Request Forgery) en peticiones HTTP mutantes
 *   (POST, PUT, DELETE, PATCH). Lee las etiquetas <meta name="_csrf"> y <meta name="_csrf_header">
 *   renderizadas por Spring Security en el HTML. Si la petición es segura (GET, HEAD, OPTIONS, TRACE),
 *   ejecuta el fetch estándar sin alterar los encabezados.
 * 
 * - A dónde apunta:
 *   - Lee las etiquetas meta en el DOM: `meta[name="_csrf"]` y `meta[name="_csrf_header"]`.
 *   - Apunta a cualquier endpoint del Backend Spring Boot que requiera autenticación y validación CSRF
 *     (por ejemplo: `/orden-compra`, `/orden-compra/{id}/recibir`, `/admin/productos/delete/{id}`, etc.).
 * 
 * @param {RequestInfo|string} input - URL o recurso al que se realiza la solicitud.
 * @param {RequestInit} [init={}] - Opciones de configuración de la petición HTTP (método, body, headers, etc.).
 * @returns {Promise<Response>} - Promesa con la respuesta de la petición HTTP fetch.
 * @throws {Error} - Si el método es mutante y no se encuentran las etiquetas meta del token CSRF en la página.
 */
function csrfFetch(input, init = {}) {
    const method = (init.method || 'GET').toUpperCase();
    const safeMethods = ['GET', 'HEAD', 'OPTIONS', 'TRACE'];

    if (safeMethods.includes(method)) {
        return fetch(input, init);
    }

    const token = document.querySelector('meta[name="_csrf"]')?.getAttribute('content');
    const headerName = document.querySelector('meta[name="_csrf_header"]')?.getAttribute('content');
    const headers = new Headers(init.headers || {});

    if (!token || !headerName) {
        throw new Error('No se encontró el token CSRF de la sesión. Recargue la página e intente de nuevo.');
    }

    headers.set(headerName, token);
    return fetch(input, { ...init, headers });
}

function mostrarConfirmacion(mensaje, titulo = '¿Está seguro?', textoDestacado = '', variante = 'warning') {
    return new Promise((resolve) => {
        const modal = document.createElement('div');
        modal.className = 'confirmacion-overlay' + (variante === 'info' ? ' confirmacion-info' : '');
        modal.setAttribute('role', 'presentation');
        modal.innerHTML = `
            <div class="confirmacion-modal" role="dialog" aria-modal="true" aria-labelledby="confirmacion-titulo">
                <div class="confirmacion-header">
                    <div class="confirmacion-icono confirmacion" aria-hidden="true">
                        <span class="material-symbols">${variante === 'info' ? 'info' : 'warning'}</span>
                    </div>
                    <h2 id="confirmacion-titulo"></h2>
                </div>
                <div class="confirmacion-body">
                    <p class="confirmacion-mensaje"></p>
                </div>
                <div class="confirmacion-actions">
                    <button type="button" class="confirmacion-cancelar">Cancelar</button>
                    <button type="button" class="confirmacion-confirmar">Confirmar</button>
                </div>
            </div>`;

        const cerrar = (resultado) => {
            document.removeEventListener('keydown', manejarTecla);
            modal.remove();
            resolve(resultado);
        };
        const manejarTecla = (event) => {
            if (event.key === 'Escape') cerrar(false);
        };

        modal.querySelector('#confirmacion-titulo').textContent = titulo;
        const mensajeElemento = modal.querySelector('.confirmacion-mensaje');
        mensajeElemento.textContent = mensaje;
        const marcador = '[[RECIBIDO_POR]]';
        const posicionMarcador = mensaje.indexOf(marcador);
        if (posicionMarcador >= 0) {
            const textoAntes = mensaje.slice(0, posicionMarcador);
            const textoDespues = mensaje.slice(posicionMarcador + marcador.length);
            mensajeElemento.replaceChildren(document.createTextNode(textoAntes));
            const recibidoPorElemento = document.createElement('strong');
            recibidoPorElemento.textContent = textoDestacado;
            mensajeElemento.append(recibidoPorElemento, document.createTextNode(textoDespues));
        }
        if (textoDestacado) {
            const nombreElemento = document.createElement('strong');
            nombreElemento.textContent = textoDestacado;
            if (posicionMarcador < 0) {
                mensajeElemento.append(document.createTextNode(' '), nombreElemento, document.createTextNode('?'));
            }
        }
        modal.querySelector('.confirmacion-cancelar').addEventListener('click', () => cerrar(false));
        modal.querySelector('.confirmacion-confirmar').addEventListener('click', () => cerrar(true));
        modal.addEventListener('click', (event) => {
            if (event.target === modal) cerrar(false);
        });
        document.addEventListener('keydown', manejarTecla);
        document.body.appendChild(modal);
        modal.querySelector('.confirmacion-confirmar').focus();
    });
}

