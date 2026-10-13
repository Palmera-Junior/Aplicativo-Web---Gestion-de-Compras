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

Sin OAuth2, la aplicación queda disponible en `http://localhost`; con OAuth2, en `http://localhost:8080`. Para detener los servicios:

```powershell
docker compose down
```

Para eliminar también los datos persistidos, use `docker compose down -v`. Esta operación es destructiva.

## Ejecución local

Requiere Java 17, PostgreSQL configurado con el esquema `dep_compras` y las credenciales de conexión correspondientes.

```powershell
.\mvnw.cmd clean compile
.\mvnw.cmd test
.\mvnw.cmd clean package
java -jar target\gestion_compras-0.0.1-SNAPSHOT.jar
```

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
- `docker/init-db`: creación del esquema y DDL inicial de PostgreSQL.
- `nginx`: configuración del proxy inverso.

## Estado de pruebas y consideraciones

La suite actual valida la carga del contexto Spring mediante `contextLoads()`. Antes de producción se recomienda ampliar las pruebas para autorización por rol y sede, transiciones de estado, contratos HTTP, cargas de evidencia y envío/reintento de correos.

La aplicación usa `spring.jpa.hibernate.ddl-auto=update`, que no convierte de forma fiable columnas existentes. `docker/init-db/DDL.sql` solo se ejecuta en una base nueva; para bases existentes, respalde y prepare una migración versionada antes de cambiar tipos como `proveedor.nit` de texto a `BIGINT`. El repositorio no incluye actualmente esa migración. En bases nuevas, el DDL crea el esquema actualizado.
