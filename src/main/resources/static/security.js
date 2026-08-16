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
