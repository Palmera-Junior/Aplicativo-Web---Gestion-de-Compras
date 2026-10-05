(function () {
    'use strict';

    var adminSidebar = document.getElementById('main-admin-sidebar');
    var mobileSidebarToggle = document.querySelector('.sidebar-mobile-toggle');

    function setMobileSidebarExpanded(expanded) {
        if (!adminSidebar || !mobileSidebarToggle) return;
        adminSidebar.classList.toggle('is-mobile-expanded', expanded);
        mobileSidebarToggle.setAttribute('aria-expanded', String(expanded));
        mobileSidebarToggle.setAttribute('aria-label', expanded ? 'Contraer navegación' : 'Expandir navegación');
        mobileSidebarToggle.title = expanded ? 'Contraer navegación' : 'Expandir navegación';
        mobileSidebarToggle.querySelector('.material-symbols').textContent = expanded ? 'close' : 'menu';
    }

    if (mobileSidebarToggle) {
        mobileSidebarToggle.addEventListener('click', function () {
            setMobileSidebarExpanded(!adminSidebar.classList.contains('is-mobile-expanded'));
        });
    }

    window.addEventListener('load', function () {
        var section = window.location.hash.substring(1);
        if (section && typeof window.showSection === 'function') window.showSection(section);
        if (section) setActiveCatalog(section);
    });

    document.addEventListener('click', function (event) {
        var groupToggle = event.target.closest('.sidebar-group-title');
        if (groupToggle) {
            var group = groupToggle.parentElement;
            if (window.matchMedia('(max-width: 900px), (hover: none)').matches) {
                setMobileSidebarExpanded(true);
            }
            if (group.querySelector('.sidebar-submenu a.is-current')) return;
            var expanded = group.classList.toggle('is-open');
            groupToggle.setAttribute('aria-expanded', String(expanded));
            return;
        }

        var link = event.target.closest('[data-admin-section]');
        if (!link) return;
        if (typeof window.showSection === 'function') window.showSection(link.dataset.adminSection);
        setActiveCatalog(link.dataset.adminSection);
    });

    function setActiveCatalog(section) {
        document.querySelectorAll('[data-admin-section]').forEach(function (link) {
            link.classList.toggle('is-current', link.dataset.adminSection === section);
        });
        var dashboard = document.querySelector('.app-sidebar a[href$="/admin"]');
        if (dashboard) dashboard.classList.remove('is-current');
    }
})();
