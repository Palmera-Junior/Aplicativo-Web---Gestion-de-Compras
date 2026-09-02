/**
 * =============================================================================
 * MÓDULO DE TRANSICIÓN DE PÁGINA (page-transition.js)
 * =============================================================================
 * Gestiona el overlay de carga que aparece durante la transición entre páginas.
 * - Activa el overlay al hacer clic en enlaces con clase .js-page-transition
 * - Desactiva el overlay automáticamente cuando la página termina de cargar
 * - Incluye fallback de tiempo máximo por seguridad
 * =============================================================================
 */

(function () {
    'use strict';

    // Configuración
    const CONFIG = {
        overlayId: 'page-transition-overlay',
        transitionClass: 'js-page-transition',
        activeClass: 'is-active',
        maxWaitTime: 10000 // 10 segundos máximo
    };

    /**
     * Obtiene el elemento overlay
     */
    function getOverlay() {
        return document.getElementById(CONFIG.overlayId);
    }

    /**
     * Muestra el overlay
     */
    function showOverlay() {
        const overlay = getOverlay();
        if (overlay) {
            overlay.classList.add(CONFIG.activeClass);
        }
    }

    /**
     * Oculta el overlay
     */
    function hideOverlay() {
        const overlay = getOverlay();
        if (overlay) {
            overlay.classList.remove(CONFIG.activeClass);
        }
    }

    /**
     * Inicializa el manejador de transición de página
     */
    function initPageTransition() {
        // Ocultar overlay cuando la página termine de cargar
        window.addEventListener('load', hideOverlay);

        // Ocultar overlay al restaurar la página desde el bfcache (navegación
        // con los botones atrás/adelante), caso en el que 'load' no se dispara.
        window.addEventListener('pageshow', hideOverlay);

        // Mostrar overlay al hacer clic en enlaces de transición
        document.addEventListener('click', function (e) {
            const link = e.target.closest('.' + CONFIG.transitionClass);
            if (!link) return;

            // Si el clic abre el enlace en una pestaña/ventana nueva (Ctrl/Cmd/Shift+click,
            // clic central o target="_blank"), la página actual no navega y por lo tanto
            // 'load'/'pageshow' nunca se disparan aquí: no mostrar el overlay en ese caso.
            const abreEnNuevaPestana = e.ctrlKey || e.metaKey || e.shiftKey || e.button === 1
                || link.target === '_blank';
            if (abreEnNuevaPestana) return;

            showOverlay();

            // Fallback: ocultar overlay después de tiempo máximo
            setTimeout(hideOverlay, CONFIG.maxWaitTime);
        });

        // Ocultar overlay si la página ya está cargada (por ejemplo, en recarga)
        if (document.readyState === 'complete') {
            hideOverlay();
        }
    }

    // Iniciar cuando el DOM esté listo
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', initPageTransition);
    } else {
        initPageTransition();
    }
})();
