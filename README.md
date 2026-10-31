# Gestión de Compras

**Versión:** 1.2.0
**Stack:** Java 17, Spring Boot 4.1, Thymeleaf, Spring Data JPA, PostgreSQL, Docker y Nginx.

Aplicación web para administrar órdenes de compra, proveedores, productos, sedes, centros de costo, recepción de mercancía, facturación, generación de PDF y notificaciones automáticas por correo mediante Transactional Outbox.

## Documentación

- [01. Requerimientos](docs/01_Requerimientos.md): alcance, actores, requisitos, estados y criterios de aceptación.
- [02. Documentación técnica](docs/02_Documentacion_Tecnica.md): arquitectura, endpoints, persistencia, seguridad, correo y pruebas.
- [03. Manual de usuario](docs/03_Manual_Usuario.md): operación del dashboard, órdenes, recepción, facturación y administración.
- [04. Manual de instalación](docs/04_Manual_Instalacion.md): requisitos, variables, Docker, ejecución local y verificación.

## Ejecución con Docker

1. Copie `.env.example` a `.env` y ajuste las variables necesarias.
2. Con Microsoft OAuth2 habilitado, ejecute:

   ```powershell
   docker compose up --build
   ```

3. Sin OAuth2 de Microsoft, ejecute:

   ```powershell
   docker compose -f docker-compose.no-oauth2.yml up --build
   ```

Flyway aplica las migraciones en orden al iniciar la aplicación. En una base vacía, `V1` crea los schemas, las tablas y los datos semilla. En una base existente sin historial Flyway, la configuración registra un baseline en V1 y asume que el esquema actual ya está instalado; revise esa base y haga un respaldo antes del primer arranque.

Sin OAuth2 o con OAuth2, la aplicación queda disponible en `http://localhost:8081`. Para detener los servicios sin borrar datos:

```powershell
docker compose down
```

Para eliminar también los datos persistidos, use `docker compose down -v`. Esta operación es destructiva.

## Ejecución local

Requiere Java 17 y una base PostgreSQL vacía ya creada. Configure las credenciales en `.env` y ejecute la aplicación como de costumbre:

```powershell
.\mvnw.cmd spring-boot:run
```

Al arrancar, Flyway aplica las migraciones de `src/main/resources/db/migration/` y Hibernate valida las entidades.

## Correo con Amazon SES

El envío automático usa Amazon SES API v2. Configure en `.env`:

- `AWS_ACCESS_KEY_ID_SES` y `AWS_SECRET_ACCESS_KEY_SES`, o asigne un rol IAM con permiso para enviar mediante SES.
- `AWS_DEFAULT_REGION_SES`, región de SES; por defecto `us-east-1`.
- `MAIL_FROM_ADDRESS`, remitente verificado.
- `MAIL_FROM_NAME`, nombre visible del remitente.
- `FACTURACION_NOTIFICACION_EMAIL`, destinatario de las notificaciones de facturación.
- `CONTABILIDAD_NOTIFICACION_EMAIL`, destinatario de avisos de pólizas.

No comparta `.env`; está excluido del control de versiones. La aplicación inicia sin credenciales SES, pero no enviará correos hasta configurar credenciales o un rol IAM.

## Estructura relevante

- `src/main/java`: controladores, servicios, entidades, repositorios, seguridad, eventos y listeners.
- `src/main/resources/templates`: vistas Thymeleaf y plantilla de correo.
- `src/main/resources/static/css`: hojas de estilos.
- `src/main/resources/static/js`: lógica del navegador.
- `src/main/resources/db/migration`: migraciones versionadas de PostgreSQL (por ejemplo, `V1__esquema_inicial_y_datos_semilla.sql`).
- `nginx`: configuración del proxy inverso.

## Estado de pruebas y consideraciones

La suite actual valida la carga del contexto Spring mediante `contextLoads()`. Antes de producción se recomienda ampliar las pruebas para autorización por rol y sede, transiciones de estado, contratos HTTP, cargas de evidencia y envío/reintento de correos.

La aplicación valida el esquema al iniciar (`spring.jpa.hibernate.ddl-auto=validate`). Las instalaciones existentes con los schemas actuales se registran como baseline V1; esto no transforma ni repara un esquema distinto. No hay migración automática desde el schema anterior `dep_compras`.
