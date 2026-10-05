/*
 * Comportamiento exclusivo del dashboard administrativo.
 * Las interacciones CRUD viven en catalogos.js.
 */

// Inicializa el tooltip del gráfico después de que el dashboard esté disponible.
document.addEventListener('DOMContentLoaded', () => {
    const donut = document.querySelector('.sede-donut');
    const tooltip = document.getElementById('sede-donut-tooltip');
    if (!donut || !tooltip) return;

    // Oculta el detalle emergente cuando el puntero o el foco salen del segmento.
    const hideTooltip = () => {
        tooltip.hidden = true;
    };

    // Presenta el valor y la participación del segmento activo del gráfico.
    const showTooltip = (segment, event) => {
        const bounds = donut.getBoundingClientRect();
        const x = event?.clientX ?? bounds.left + bounds.width / 2;
        const y = event?.clientY ?? bounds.top + bounds.height / 2;
        const name = document.createElement('strong');
        const amount = document.createElement('span');

        name.textContent = segment.dataset.sede;
        amount.textContent = `${segment.dataset.total} · ${segment.dataset.porcentaje}`;
        tooltip.replaceChildren(name, amount);
        tooltip.style.left = `${x - bounds.left}px`;
        tooltip.style.top = `${y - bounds.top}px`;
        tooltip.hidden = false;
    };

    // Habilita el tooltip del gráfico tanto con puntero como con teclado.
    donut.querySelectorAll('.sede-donut-segment').forEach(segment => {
        segment.addEventListener('pointerenter', event => showTooltip(segment, event));
        segment.addEventListener('pointermove', event => showTooltip(segment, event));
        segment.addEventListener('pointerleave', hideTooltip);
        segment.addEventListener('focus', () => showTooltip(segment));
        segment.addEventListener('blur', hideTooltip);
    });
});
