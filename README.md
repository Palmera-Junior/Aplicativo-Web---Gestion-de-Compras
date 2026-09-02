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

La aplicación queda disponible en `http://localhost`. Para detener los servicios:

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

## Correo SMTP con Amazon SES

El envío automático usa SMTP de Amazon SES. Configure en `.env`:

- `SES_SMTP_HOST`, por ejemplo `email-smtp.us-east-1.amazonaws.com`.
- `SES_SMTP_PORT`, normalmente `587` con STARTTLS.
- `SES_SMTP_USERNAME` y `SES_SMTP_PASSWORD`, generados para SMTP en SES.
- `SES_FROM_EMAIL`, remitente verificado; por defecto `notificaciones@palmerajunior.com`.
- `FACTURACION_NOTIFICACION_EMAIL`, destinatario de las notificaciones de facturación.

Las credenciales SMTP de SES no son las credenciales generales de AWS. No comparta `.env`; está excluido del control de versiones.

## Estructura relevante

- `src/main/java`: controladores, servicios, entidades, repositorios, seguridad, eventos y listeners.
- `src/main/resources/templates`: vistas Thymeleaf y plantilla de correo.
- `src/main/resources/static/css`: hojas de estilos.
- `src/main/resources/static/js`: lógica del navegador.
- `docker/init-db`: creación del esquema y DDL inicial de PostgreSQL.
- `nginx`: configuración del proxy inverso.

## Estado de pruebas y consideraciones

La suite actual valida la carga del contexto Spring mediante `contextLoads()`. Antes de producción se recomienda ampliar las pruebas para autorización por rol y sede, transiciones de estado, contratos HTTP, cargas de evidencia y envío/reintento de correos.

La carpeta `src/main/resources/db/migration` está vacía y la aplicación usa `spring.jpa.hibernate.ddl-auto=update`. Para instalaciones productivas se recomienda adoptar migraciones versionadas, respaldos y una política de retención para PDFs, evidencias y auditorías.
