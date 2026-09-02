# Manual de Usuario

**Sistema:** Gestión de Compras  
**Versión:** 1.2.0  
**Audiencia:** solicitantes, aprobadores y administradores

## 1. Acceso

1. Abra la dirección proporcionada por la organización.
2. Ingrese usuario y contraseña.
3. Pulse **Iniciar Sesión**.
4. Si Microsoft OAuth2 está habilitado, puede usar el acceso federado. El correo debe estar registrado previamente.
5. Los administradores ingresan al panel administrativo; los demás roles ingresan al dashboard.

Use **Salir** para cerrar sesión. No comparta sus credenciales.

## 2. Crear una orden

1. Abra **Nueva orden de compra**.
2. Seleccione fecha, centro de costo y proveedor.
3. Agregue una fila de producto.
4. Indique cantidad y código de inventario.
5. Al salir del campo de código, el sistema consulta el catálogo exacto.
6. Si el código no existe, verá el mensaje: `El código de producto "XXX" no existe.` El aviso desaparece aproximadamente en tres segundos.
7. También puede escribir una descripción y escoger una sugerencia.
8. Seleccione la presentación; el sistema cargará el precio asociado cuando exista.
9. Revise cantidades, valor unitario, IVA, descuento y flete.
10. Pulse **Guardar**. La orden quedará en estado `BORRADOR`.

## 3. Editar y aprobar

Una orden `BORRADOR` puede abrirse para corregir sus datos. Use los filtros por texto, fechas, estado u órdenes modificadas para localizarla.

Un usuario con permiso puede seleccionar la aprobación y confirmar. La aprobación genera el número oficial, registra usuario y fecha, bloquea la edición y crea el envío automático al proveedor.

## 4. Registrar recepción

1. En una orden `APROBADA` o `FACTURADA`, seleccione **Recibir**.
2. Revise los productos y ajuste la cantidad recibida de cada línea.
3. Ingrese receptor, observaciones y valor de flete cuando corresponda.
4. Adjunte la evidencia requerida por el procedimiento interno.
5. Confirme la recepción.

El sistema registra las cantidades y muestra diferencias frente a lo solicitado.

## 5. Registrar factura

1. Seleccione **Facturar** en una orden cuyo estado lo permita.
2. Ingrese el número de factura del proveedor.
3. Adjunte el soporte digital.
4. Confirme la operación.

La orden pasará a `FACTURADA` o `COMPLETADA` según si la recepción ya había sido registrada. Se generará una notificación automática al correo de facturación configurado.

## 6. Estados de la orden

| Estado | Significado |
|---|---|
| `BORRADOR` | En edición y todavía no aprobada. |
| `APROBADA` | Autorizada y enviada al proveedor. |
| `RECIBIDA` | Se registró recepción de mercancía. |
| `FACTURADA` | Se registró la factura antes de completar la recepción. |
| `COMPLETADA` | Recepción y facturación completadas. |
| `ANULADA` | Cancelada lógicamente. |

## 7. Indicadores de correo

El dashboard separa el correo al proveedor del correo de facturación:

- Reloj: envío pendiente o en proceso.
- Marca de verificación: envío correcto.
- Cruz: fallo definitivo.

Al pulsar la cruz se abre un formulario. Escriba la descripción del fallo y confirme que el correo fue enviado por otro canal. La acción cambia únicamente el correo seleccionado a `ENVIADO`.

## 8. Administración

El administrador puede gestionar usuarios, proveedores, productos, sedes y centros de costo. Mantenga códigos de producto únicos, correos válidos, asociaciones de sede correctas y roles acordes con las responsabilidades.

## 9. Problemas frecuentes

| Problema | Acción |
|---|---|
| Código no encontrado | Revise el código exacto. Si es válido, solicite crear o corregir el producto en administración. |
| No permite aprobar | Verifique rol, estado `BORRADOR` y datos obligatorios. |
| Correo con cruz | Revise el error y use confirmación manual solo después de enviar el correo externamente. |
| No carga una pantalla | Recargue y reporte al soporte la hora, usuario, orden y acción realizada si persiste. |
