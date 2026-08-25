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

