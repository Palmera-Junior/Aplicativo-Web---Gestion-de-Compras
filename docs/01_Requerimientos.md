# Requerimientos del Sistema

**Sistema:** Gestión de Compras  
**Versión:** 1.2.0  
**Fecha:** 2 de septiembre de 2026  
**Tipo:** Especificación funcional y no funcional

## 1. Propósito y alcance

La aplicación gestiona el ciclo de vida de órdenes de compra: creación, aprobación, recepción de mercancía, registro de factura, generación de PDF y notificaciones por correo. También administra usuarios, proveedores, productos, presentaciones, sedes y centros de costo.


## 2. Actores y permisos

| Actor | Responsabilidades |
|---|---|
| `SOLICITANTE` | Crear y editar borradores, consultar órdenes, registrar operaciones permitidas y confirmar manualmente correos fallidos. |
| `APROBADOR` | Funciones del solicitante y aprobación de órdenes en estado `BORRADOR`. |
| `ADMINISTRADOR` | Administrar usuarios, proveedores, productos, sedes y centros de costo; acceder al dashboard y ejecutar operaciones autorizadas. |

## 3. Requerimientos funcionales

1. Permitir autenticación local mediante usuario y contraseña.
2. Permitir autenticación Microsoft OAuth2 cuando esté habilitada y exigir que el correo federado exista previamente en el sistema.
3. Redirigir al administrador a `/admin` y a los demás roles a `/dashboard`.
4. Permitir crear, editar, listar, paginar y eliminar las entidades administrativas según sus controladores.
5. Permitir crear y editar órdenes `BORRADOR` con fecha, proveedor, centro de costo, líneas, cantidades, precios, IVA, descuento y flete.
6. Buscar productos por código exacto y por coincidencia parcial en nombre o código.
7. Mostrar una advertencia temporal cuando un código de producto no existe.
8. Cargar presentaciones y precios asociados al producto seleccionado.
9. Recalcular los totales en servidor sin confiar en los valores calculados por el navegador.
10. Aprobar órdenes, registrar usuario y fecha de aprobación y generar un número consecutivo por sede.
11. Registrar recepción parcial o completa, cantidades recibidas, receptor, observaciones, flete y evidencia.
12. Detectar diferencias entre cantidades solicitadas y recibidas.
13. Registrar número de factura y soporte digital del proveedor.
14. Cambiar la orden a `FACTURADA` o `COMPLETADA` según su estado previo.
15. Anular lógicamente una orden conservando sus datos.
16. Generar y descargar el PDF de una orden en estado permitido.
17. Enviar correo automático al proveedor después de la aprobación.
18. Enviar notificación automática al correo de facturación después de registrar una factura.
19. Registrar estados, intentos y errores de cada correo mediante Transactional Outbox.
20. Permitir marcar manualmente como `ENVIADO` un correo `FALLIDO`, especificando `APROBACION` o `FACTURACION` y una justificación.
21. Filtrar órdenes por texto, rango de fechas, estado y diferencias de recepción.
22. Paginar las órdenes y los catálogos administrativos.

## 4. Estados de negocio

### Orden de compra

- `BORRADOR`: orden editable aún no aprobada.
- `APROBADA`: autorizada y enviada al proveedor.
- `RECIBIDA`: se registró la recepción de mercancía.
- `FACTURADA`: se registró la factura antes de completar la recepción.
- `COMPLETADA`: recepción y facturación completadas.
- `ANULADA`: orden cancelada lógicamente.

Flujo principal: `BORRADOR -> APROBADA -> RECIBIDA` y/o `FACTURADA -> COMPLETADA`.

### Envío de correo

`PENDIENTE -> PROCESANDO -> ENVIADO`. Los errores pasan a `REINTENTAR` y, después de cuatro intentos, a `FALLIDO`.

## 5. Requerimientos no funcionales

- Usar Java 17, Spring Boot 4.1, Spring MVC, Thymeleaf, JPA/Hibernate y PostgreSQL.
- Proteger contraseñas con BCrypt.
- Proteger solicitudes mutantes mediante CSRF.
- Procesar el correo de forma asíncrona y confiable mediante Transactional Outbox.
- Mantener una interfaz web responsive con modales, validación cliente/servidor y mensajes toast.
- Externalizar credenciales y configuración sensible mediante variables de entorno.
- Ejecutar la solución mediante Maven Wrapper o Docker Compose.

## 6. Criterios de aceptación

| Área | Criterio |
|---|---|
| Orden | Una orden guardada conserva líneas y totales consistentes con el recálculo del servidor. |
| Aprobación | Solo un rol autorizado aprueba un borrador y se registra el consecutivo. |
| Producto | Un código inexistente produce HTTP 404 y el dashboard muestra un toast durante aproximadamente tres segundos. |
| Correo | El outbox registra destinatario, tipo, intentos y resultado; un correo `FALLIDO` puede confirmarse manualmente. |
| Seguridad | Las solicitudes mutantes sin CSRF son rechazadas y un usuario sin rol no accede a administración. |


