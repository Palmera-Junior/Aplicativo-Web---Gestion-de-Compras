# Manual de Instalación

**Sistema:** Gestión de Compras  
**Versión:** 1.2.0  
**Fecha:** 2 de septiembre de 2026

## 1. Requisitos

### Docker recomendado

- Docker Engine con Docker Compose.
- Puertos disponibles `80` y `5432` si se publicará PostgreSQL localmente.
- Credenciales de Microsoft OAuth2 si se usará login federado.
- Credenciales SMTP de Amazon SES si se habilitará el envío automático.

### Ejecución local sin Docker

- Java 17.
- Maven Wrapper incluido (`mvnw.cmd` en Windows).
- PostgreSQL accesible.
- Esquema `dep_compras` creado mediante los scripts SQL.

## 2. Configurar variables

Copie el archivo de ejemplo:

```powershell
Copy-Item .env.example .env
```

Complete, como mínimo, según el escenario:

| Variable | Uso |
|---|---|
| `DB_NAME` | Nombre de la base PostgreSQL en Docker. |
| `DB_USER` / `DB_PASSWORD` | Credenciales de PostgreSQL. |
| `SPRING_DATASOURCE_URL` | URL JDBC de la aplicación. |
| `DB_SCHEMA` | Esquema Hibernate; por defecto `dep_compras`. |
| `MICROSOFT_OAUTH2_ENABLED` | Habilita o deshabilita Microsoft OAuth2. |
| `MICROSOFT_CLIENT_ID` | Identificador de la aplicación Microsoft. |
| `MICROSOFT_CLIENT_SECRET` | Secreto de la aplicación Microsoft. |
| `MICROSOFT_TENANT_ID` | Tenant de Microsoft Entra ID. |
| `SES_SMTP_HOST` | Endpoint SMTP de Amazon SES. |
| `SES_SMTP_PORT` | Puerto SMTP; normalmente `587`. |
| `SES_SMTP_USERNAME` | Usuario SMTP generado por SES. |
| `SES_SMTP_PASSWORD` | Contraseña SMTP generada por SES. |
| `SES_FROM_EMAIL` | Remitente verificado. |
| `FACTURACION_NOTIFICACION_EMAIL` | Destinatario de notificaciones de facturación. |
| `SESSION_COOKIE_SECURE` | Debe ser `true` detrás de HTTPS. |

Las credenciales SMTP de SES no son las credenciales generales de AWS. No comparta `.env`.

## 3. Instalación con Docker y OAuth2

```powershell
docker compose up --build
```

La aplicación queda disponible en `http://localhost`. PostgreSQL queda accesible en `localhost:5432` cuando la configuración del Compose lo publica.

El servicio espera que estén definidos `MICROSOFT_CLIENT_ID`, `MICROSOFT_CLIENT_SECRET` y `MICROSOFT_TENANT_ID`.

## 4. Instalación sin OAuth2

Para un entorno local sin credenciales Microsoft:

```powershell
docker compose -f docker-compose.no-oauth2.yml up --build
```

## 5. Detener servicios

```powershell
docker compose down
```

Para eliminar también los volúmenes y datos persistidos:

```powershell
docker compose down -v
```

La eliminación de volúmenes es destructiva.

## 6. Ejecución local

Configure PostgreSQL y las variables `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD` y `DB_SCHEMA`. Luego ejecute:

```powershell
.\mvnw.cmd clean compile
.\mvnw.cmd test
.\mvnw.cmd clean package
java -jar target\gestion_compras-0.0.1-SNAPSHOT.jar
```

La aplicación escucha normalmente en el puerto `8080` cuando se ejecuta directamente.

## 7. Base de datos

Los scripts de inicialización están en `docker/init-db/`:

- `01-create-schema.sql`: creación del esquema.
- `DDL.sql`: tablas, relaciones, índices y restricciones.

La configuración actual usa `spring.jpa.hibernate.ddl-auto=update`. Antes de producción, adopte migraciones versionadas y defina una política de respaldo. La carpeta `src/main/resources/db/migration` está actualmente vacía.

## 8. Verificación de instalación

1. Compruebe que PostgreSQL esté saludable.
2. Compruebe `http://localhost/actuator/health` detrás de Nginx o `http://localhost:8080/actuator/health` en ejecución local.
3. Abra `/login` y valide el acceso.
4. Cree una orden de prueba en estado `BORRADOR`.
5. Verifique que un código inexistente muestre el toast del dashboard.
6. Pruebe aprobación, generación de PDF y el registro de correo.
7. Si SMTP está configurado, confirme que el remitente esté verificado en SES y que el destinatario pueda recibir mensajes.

## 9. Despliegue y mantenimiento

- Mantenga `SESSION_COOKIE_SECURE=true` cuando Nginx publique HTTPS.
- No exponga PostgreSQL públicamente en producción sin una necesidad justificada.
- Proteja los volúmenes `pgdata` y `pdf_storage` con respaldos.
- Revise los logs de la aplicación y el estado del outbox.
- No use `docker compose down -v` en producción salvo que se pretenda eliminar los datos.
