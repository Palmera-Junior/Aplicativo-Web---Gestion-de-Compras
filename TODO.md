# TODO - Paginación AJAX en Productos (Admin)

## Tareas
- [x] 1. Agregar endpoint `GET /admin/productos/pagina` en `AdminController.java` que devuelve el fragmento de la tabla de productos.
- [x] 2. En `admin.html`, marcar el `tabla-card` de productos con `th:fragment` e `id`.
- [x] 3. En `admin.html`, cambiar los enlaces de paginación de productos para que usen la ruta AJAX.
- [x] 4. En `admin.js`, añadir el listener que hace fetch del fragmento y reemplaza el contenido manteniendo la sección abierta.
