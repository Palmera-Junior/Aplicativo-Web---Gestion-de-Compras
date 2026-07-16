document.addEventListener("DOMContentLoaded", () => {

    const modal = document.getElementById("modal-orden");
    const btnCerrar = document.getElementById("btn-cerrar-modal");
    const btnAbrir = document.getElementById("btn-nueva-orden");

    // Abrir modal
    if (btnAbrir) {
        btnAbrir.addEventListener("click", () => {
            modal.classList.add("active");
            document.body.style.overflow = "hidden";
        });
    }

    // Cerrar con la X
    if (btnCerrar) {
        btnCerrar.addEventListener("click", cerrarModal);
    }

    // Cerrar al hacer click sobre el fondo oscuro
    modal.addEventListener("click", (e) => {
        if (e.target === modal) {
            cerrarModal();
        }
    });

    // Cerrar con ESC
    document.addEventListener("keydown", (e) => {
        if (e.key === "Escape") {
            cerrarModal();
        }
    });

    function cerrarModal() {
        modal.classList.remove("active");
        document.body.style.overflow = "auto";
    }

    //Codigo de los campos del proveedor 

    const selectProveedor = document.getElementById("select-proveedor");

    const nit = document.getElementById("prov-nit");
    const nombre = document.getElementById("prov-nombre");
    const ciudad = document.getElementById("prov-ciudad");
    const direccion = document.getElementById("prov-direccion");
    const telefono = document.getElementById("prov-telefono");
    const email = document.getElementById("prov-email");

    const campos = [
    nit,
    nombre,
    ciudad,
    direccion,
    telefono,
    email
];

    selectProveedor.addEventListener("change", function () {

        const opcion = this.options[this.selectedIndex];

        // Opción "Otro"
    if (this.value === "otro") {

        campos.forEach(campo => {
            campo.disabled = false;
            campo.value = "";
        });

        return;
    }

    // Ningún proveedor seleccionado
    if (this.value === "") {

        campos.forEach(campo => {
            campo.disabled = true;
            campo.value = "";
        });

        return;
    }

        // Proveedor existente
        nit.value = opcion.dataset.nit || "";
        nombre.value = opcion.dataset.nombre || "";
        ciudad.value = opcion.dataset.ciudad || "";
        direccion.value = opcion.dataset.direccion || "";
        telefono.value = opcion.dataset.telefono || "";
        email.value = opcion.dataset.email || "";

        campos.forEach(campo => {
        campo.disabled = true;
    });

    });
});

