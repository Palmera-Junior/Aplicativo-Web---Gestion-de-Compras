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
if (-not (Test-Path .env)) { Copy-Item .env.example .env }
```

Si `.env` ya existe, consérvelo y agregue las variables faltantes; no lo sobrescriba.

Complete, como mínimo, según el escenario:

| Variable | Uso |
|---|---|
| `DB_NAME` | Nombre de la base PostgreSQL en Docker. |
| `DB_USER` | Usuario PostgreSQL compartido por el contenedor de base de datos y la aplicación. |
| `DB_PASSWORD` | Contraseña PostgreSQL obligatoria; para volúmenes existentes debe coincidir con la contraseña ya guardada en PostgreSQL. |
| `SPRING_DATASOURCE_URL` | URL JDBC opcional para conectarse a una base externa desde Compose. Por defecto apunta al servicio `db`. |
| `MICROSOFT_OAUTH2_ENABLED` | Habilita o deshabilita Microsoft OAuth2. |
| `MICROSOFT_CLIENT_ID` | Identificador de la aplicación Microsoft. |
| `MICROSOFT_CLIENT_SECRET` | Secreto de la aplicación Microsoft. |
| `MICROSOFT_TENANT_ID` | Tenant de Microsoft Entra ID. |
| `MICROSOFT_REDIRECT_URI` | URI de retorno registrado en Microsoft Entra ID. En local con el Compose OAuth2: `http://localhost:8080/login/oauth2/code/microsoft`. |
| `AWS_ACCESS_KEY_ID_SES` / `AWS_SECRET_ACCESS_KEY_SES` | Credenciales AWS opcionales; alternativamente use un rol IAM. |
| `AWS_DEFAULT_REGION_SES` | Región de Amazon SES. |
| `MAIL_FROM_ADDRESS` / `MAIL_FROM_NAME` | Remitente verificado y nombre visible. |
| `FACTURACION_NOTIFICACION_EMAIL` | Destinatario de notificaciones de facturación. |
| `CONTABILIDAD_NOTIFICACION_EMAIL` | Destinatario de avisos de pólizas. |
| `APP_BASE_URL` | URL pública incluida en los correos. |
| `SESSION_COOKIE_SECURE` | `false` para HTTP local; `true` detrás de HTTPS. |

El envío usa SES API v2, no SMTP. No comparta `.env`.

## 3. Instalación con Docker y OAuth2

```powershell
docker compose up --build
```

La aplicación queda disponible en `http://localhost:8080`. PostgreSQL no publica un puerto en el host; solo es accesible desde la red interna de Docker.

El servicio espera que estén definidos `MICROSOFT_CLIENT_ID`, `MICROSOFT_CLIENT_SECRET` y `MICROSOFT_TENANT_ID`.

En **Microsoft Entra ID > Registros de aplicaciones > Autenticación**, agregue como plataforma **Web** el valor de `MICROSOFT_REDIRECT_URI`. Para un despliegue HTTPS, use la URL pública completa, por ejemplo `https://compras.example.com/login/oauth2/code/microsoft`, y defina el mismo valor en `.env`.

## 4. Instalación sin OAuth2

Para un entorno local sin credenciales Microsoft:

```powershell
docker compose -f docker-compose.no-oauth2.yml up --build
```

Verifique que `.env` defina `DB_NAME`, `DB_USER` y `DB_PASSWORD`. En una base nueva, use una contraseña propia antes del primer arranque. El acceso inicial es `admin` / `admin123`; cambie esa clave después del primer inicio. Si `admin` ya existía, el seed conserva su contraseña y solo garantiza el rol `SUPERADMINISTRADOR`. Para HTTP local, mantenga `SESSION_COOKIE_SECURE=false`; detrás de HTTPS, active `true` y configure `APP_BASE_URL` con la URL pública.

Con `.env.example` copiado a `.env`, cambie `DB_PASSWORD` antes del primer arranque. El acceso inicial es `admin` / `admin123`; cambie esa clave después del primer inicio. Para HTTP local, mantenga `SESSION_COOKIE_SECURE=false`; detrás de HTTPS, active `true` y configure `APP_BASE_URL` con la URL pública.

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

Configure PostgreSQL con la base `palmera_junior` y el esquema `dep_compras`. En ejecución local, la URL usa `localhost`; el nombre del servicio Docker `db` solo resuelve dentro de Compose. Luego ejecute:

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

`DDL.sql` crea la sede técnica mínima. Al iniciar la aplicación, `dml.sql` también garantiza que exista la sede con prefijo `NAC` antes de crear el usuario `admin` con rol `SUPERADMINISTRADOR`. Así funciona tanto con bases inicializadas por Docker como con tablas creadas por Hibernate; no carga proveedores ni datos de demostración.

Las bases existentes deben migrarse antes de desplegar esta versión: haga un respaldo y ejecute una vez `docker/init-db/migrations/V1__catalogos_roles_y_permisos.sql` sobre la base de datos. El script conserva los valores de categoría y rol y crea las tablas para permisos directos; no es idempotente y no debe ejecutarse en una base nueva. En bases nuevas, `DDL.sql` crea el esquema normalizado.

La configuración actual usa `spring.jpa.hibernate.ddl-auto=update`; los scripts de migración se deben ejecutar explícitamente y en orden en bases existentes antes de iniciar la nueva versión. Si su instalación requiere el cambio de presentaciones opcionales, aplique también su migración V2 después de V1. Para convertir los permisos directos a excepciones individuales y habilitar permisos predeterminados por rol, ejecute `docker/init-db/migrations/V3__permisos_por_rol_y_usuario.sql` después de V1. La V3 preserva los permisos directos existentes como concesiones explícitas. En bases nuevas, el DDL y el DML crean las relaciones de permisos por rol.

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
- Proteja el volumen `pgdata` con respaldos; los adjuntos PDF se almacenan en la base de datos.
- Revise los logs de la aplicación y el estado del outbox.
- No use `docker compose down -v` en producción salvo que se pretenda eliminar los datos.
