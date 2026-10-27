# Documentación Técnica

**Sistema:** Gestión de Compras  
**Versión:** 1.2.0  
**Fecha:** 2 de septiembre de 2026

## 1. Arquitectura

La solución es un monolito web Spring Boot con MVC y Thymeleaf.

```text
Navegador: Thymeleaf + JavaScript + CSS
                  |
       Spring MVC Controllers
                  |
          Application Services
                  |
      Spring Data JPA / Hibernate
                  |
       PostgreSQL: schemas core, compras y polizas
```

OpenPDF genera documentos en memoria. Nginx funciona como proxy inverso hacia el puerto 8080 de la aplicación. Docker Compose separa aplicación, PostgreSQL y redes interna/externa.

## 2. Organización del código

| Área | Responsabilidad |
|---|---|
| `controller` | Vistas, endpoints del dashboard, órdenes y administración. |
| `service` | Reglas de órdenes, catálogo, usuarios, organización, PDF, correo y dashboard. |
| `repository` | Consultas JPA, filtros, paginación y actualización atómica del outbox. |
| `entity` | Modelo persistente, estados y tipos de envío. |
| `dto` | Contratos de entrada y salida para órdenes, recepción, facturación y correo. |
| `events` / `Listeners` | Procesamiento posterior al commit para aprobación y facturación. |
| `templates` | Login, dashboard, administración y plantilla HTML del correo de aprobación. |
| `static/css` / `static/js` | Presentación, CSRF, transición de páginas, dashboard y administración. |

## 3. Endpoints principales

| Método | Ruta | Función | Control |
|---|---|---|---|
| `GET` | `/login` | Vista de login | Público |
| `POST` | `/login` | Autenticación local | Spring Security |
| `POST` | `/logout` | Cierre de sesión | CSRF |
| `GET` | `/dashboard` | Dashboard filtrado y paginado | Roles de negocio |
| `GET` | `/dashboard/producto?codigo=` | Producto exacto; `404` si no existe | Autenticado |
| `GET` | `/dashboard/productos/buscar?query=` | Autocompletado | Autenticado |
| `POST` | `/orden-compra` | Crear borrador | CSRF + rol |
| `PUT` | `/orden-compra/{id}` | Editar borrador | CSRF + rol |
| `PUT` | `/orden-compra/{id}/aprobar` | Aprobar | Regla de aprobación |
| `PUT` | `/orden-compra/{id}/recibir` | Registrar recepción | CSRF + rol |
| `PUT` | `/orden-compra/{id}/facturar` | Registrar factura | CSRF + rol |
| `PUT` | `/orden-compra/{id}/anular` | Anulación lógica | CSRF + rol |
| `PUT` | `/orden-compra/{id}/correo/marcar-enviado` | Confirmación manual | CSRF + rol autorizado |
| `GET` | `/orden-compra/{id}/pdf` | Descargar PDF | Estado permitido |
| `GET/POST` | `/admin/**` | Administración de catálogos | `ADMINISTRADOR` |

## 4. Persistencia

La persistencia se divide por dominio:

| Schema | Tablas |
|---|---|
| `core` | `sede`, `centro_costo`, `proveedor`, `proveedor_sede`, `usuario`, `usuario_permiso`, `rol`, `rol_permiso`, `permiso`, `password_reset_token`, `password_reset_rate_limit`. |
| `compras` | `orden_compra`, `detalle_compra`, `auditoria_envio_correo`, `categoria`, `producto`, `presentacion_producto`. |
| `polizas` | `poliza`, `poliza_sede_aprobacion`, `poliza_renovacion`, `auditoria_envio_correo_poliza`, `detalle_prima`, `poliza_adjunto`. |

Las claves foráneas entre módulos apuntan a las tablas compartidas de `core`; las relaciones de permisos por rol y proveedor por sede también residen en `core`. `usuario_permiso` guarda excepciones individuales de concesión o denegación respecto a los permisos heredados del rol.

Hay restricciones de unicidad para identificadores de negocio y relaciones con cascada para detalles y presentaciones. Las autoridades `PERM_<CODIGO>` se calculan con permisos del rol y excepciones individuales, que prevalecen sobre los valores del rol. La auditoría de correo conserva destinatario, estado, tipo, intentos, fechas y último error.

> `spring.jpa.hibernate.ddl-auto=validate` verifica que los tres schemas y sus tablas estén creados. La aplicación no modifica ni migra bases existentes.

## 5. Flujo Transactional Outbox

1. La transacción de aprobación o facturación crea un registro pendiente.
2. El listener posterior al commit inicia el procesamiento asíncrono.
3. El scheduler consulta hasta 50 registros listos cada 30 segundos.
4. El repositorio reclama atómicamente el registro y lo pasa a `PROCESANDO`.
5. El servicio carga la orden, genera el PDF, prepara el cuerpo y envía por SMTP.
6. El registro pasa a `ENVIADO` o se programa un reintento con backoff.
7. Después de cuatro intentos queda `FALLIDO` y puede confirmarse manualmente.
8. Los bloqueos `PROCESANDO` con más de 15 minutos se liberan.

El pool asíncrono utiliza cuatro hilos base, ocho máximos y una cola de 50 tareas.

## 6. Seguridad

- BCrypt para contraseñas.
- Recuperación de contraseña: token aleatorio de 256 bits, hash SHA-256 persistido, uso único y expiración de 20 minutos.
- Rate limiting compartido por PostgreSQL: máximo cinco solicitudes por correo y 20 por IP cada 15 minutos; los identificadores se persisten únicamente como hashes.
- El restablecimiento requiere al menos 12 caracteres y limita la contraseña a 72 bytes UTF-8 para evitar truncamiento de BCrypt.
- Las respuestas de solicitud no confirman si un correo corresponde a una cuenta. Los enlaces no se almacenan en claro ni se escriben en los logs de acceso de Nginx.
- CSRF mediante cookie accesible por JavaScript.
- Cambio del identificador de sesión, cookie `HttpOnly`, `SameSite=Lax` y timeout de 30 minutos.
- OAuth2 Microsoft opcional y restringido a usuarios previamente registrados.
- Actuator expone únicamente `health` e `info`.
- Recursos estáticos organizados en `/css/**`, `/js/**` e `/imgs/**`.

### Riesgos técnicos pendientes

1. Varias operaciones por ID de orden deben reforzar la validación de sede o propietario.
2. Las evidencias Base64 requieren límites de tamaño, validación MIME y validación de contenido.
3. Los parámetros de paginación deben tener límites máximos.
4. Algunos errores podrían exponer detalles internos de base de datos.
5. Faltan migraciones versionadas y una estrategia de retención de archivos/auditoría.
6. Faltan pruebas de autorización, contratos HTTP, transiciones de estado y correo.

## 7. Pruebas actuales

La suite incluye una prueba unitaria de la política de longitud de las contraseñas nuevas. Aún se requiere ampliar la cobertura con pruebas de autorización, contratos HTTP, transiciones de estado, expiración y consumo único de tokens, límites de solicitudes e integración con el proveedor de correo.
