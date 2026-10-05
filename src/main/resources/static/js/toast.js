(() => {
    let toastRoot;

    const elevarRaiz = () => {
        if (!toastRoot) return;

        toastRoot.classList.add('toast-root-visible');
        if (typeof toastRoot.showPopover !== 'function') return;

        try {
            if (toastRoot.matches(':popover-open')) toastRoot.hidePopover();
            toastRoot.showPopover();
        } catch (error) {
            // El z-index mantiene el fallback para navegadores sin soporte completo de popover.
        }
    };

    const obtenerRaiz = () => {
        if (toastRoot) return toastRoot;

        toastRoot = document.createElement('div');
        toastRoot.id = 'toast-root';
        toastRoot.setAttribute('popover', 'manual');
        toastRoot.setAttribute('role', 'status');
        toastRoot.setAttribute('aria-live', 'polite');
        document.body.appendChild(toastRoot);

        const dialogObserver = new MutationObserver((mutations) => {
            const seAbrioDialogo = mutations.some(({ target, attributeName }) =>
                attributeName === 'open' && window.HTMLDialogElement
                    && target instanceof window.HTMLDialogElement && target.open);
            if (seAbrioDialogo && toastRoot.childElementCount) elevarRaiz();
        });
        dialogObserver.observe(document.documentElement, {
            attributes: true,
            attributeFilter: ['open'],
            subtree: true
        });

        return toastRoot;
    };

    window.mostrarAppToast = (mensaje, tipo = 'success') => {
        if (!mensaje) return;

        const root = obtenerRaiz();
        const item = document.createElement('div');
        item.className = `toast-item toast-${tipo}`;
        item.textContent = mensaje;
        root.appendChild(item);
        elevarRaiz();

        window.setTimeout(() => {
            item.classList.add('toast-hide');
            window.setTimeout(() => {
                item.remove();
                if (!root.childElementCount) {
                    root.classList.remove('toast-root-visible');
                    if (typeof root.hidePopover === 'function' && root.matches(':popover-open')) {
                        root.hidePopover();
                    }
                }
            }, 400);
        }, 3000);
    };
})();