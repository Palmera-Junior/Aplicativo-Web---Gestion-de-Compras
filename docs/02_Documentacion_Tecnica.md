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
       PostgreSQL, esquema dep_compras
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

El DDL crea el esquema `dep_compras` con tablas para `sede`, `centro_costo`, `producto`, `presentacion_producto`, `proveedor`, `proveedor_sede`, `usuario`, `orden_compra`, `detalle_compra` y `auditoria_envio_correo`.

Hay restricciones de unicidad para identificadores de negocio, checks para estados/roles/tipos y relaciones con cascada para detalles y presentaciones. La auditoría de correo conserva destinatario, estado, tipo, intentos, fechas y último error.

> `spring.jpa.hibernate.ddl-auto=update` está activo y `src/main/resources/db/migration` está vacío. El DDL inicial de Docker no sustituye migraciones versionadas para bases existentes.

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

La suite contiene `GestionComprasApplicationTests`, que actualmente verifica `contextLoads()`. La compilación y el contexto Spring se validan, pero no existe cobertura automatizada suficiente para todos los flujos críticos.
