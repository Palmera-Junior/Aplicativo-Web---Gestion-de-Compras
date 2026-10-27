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
- PostgreSQL local activo y una base de datos vacía ya creada.

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

En un volumen nuevo, PostgreSQL ejecuta automáticamente `docker/init-db/01-create-schema.sql` y `docker/init-db/DDL.sql`. Si necesita reinicializar un volumen anterior con los schemas nuevos, detenga los servicios y elimine el volumen antes de arrancar:

```powershell
docker compose down -v
docker compose up --build
```

La eliminación del volumen es destructiva: borra todos los datos PostgreSQL asociados a ese Compose. Este proyecto no migra tablas de `dep_compras`; utilice este reinicio solo cuando haya confirmado que los datos anteriores pueden descartarse.

El servicio espera que estén definidos `MICROSOFT_CLIENT_ID`, `MICROSOFT_CLIENT_SECRET` y `MICROSOFT_TENANT_ID`.

En **Microsoft Entra ID > Registros de aplicaciones > Autenticación**, agregue como plataforma **Web** el valor de `MICROSOFT_REDIRECT_URI`. Para un despliegue HTTPS, use la URL pública completa, por ejemplo `https://compras.example.com/login/oauth2/code/microsoft`, y defina el mismo valor en `.env`.

## 4. Instalación sin OAuth2

En Linux, instale Docker Engine y el plugin Docker Compose, clone el repositorio y entre en su directorio. En Ubuntu, por ejemplo, puede instalar Docker desde el repositorio oficial:

```bash
sudo apt-get update
sudo apt-get install -y ca-certificates curl
sudo install -m 0755 -d /etc/apt/keyrings
sudo curl -fsSL https://download.docker.com/linux/ubuntu/gpg -o /etc/apt/keyrings/docker.asc
sudo chmod a+r /etc/apt/keyrings/docker.asc
echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.asc] https://download.docker.com/linux/ubuntu $(. /etc/os-release && echo "${UBUNTU_CODENAME:-$VERSION_CODENAME}") stable" | sudo tee /etc/apt/sources.list.d/docker.list > /dev/null
sudo apt-get update
sudo apt-get install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin git
```

Clone el repositorio (use la URL SSH si es privado) y configure las variables:

```bash
git clone https://github.com/Palmera-Junior/Aplicativo-Web---Gestion-de-Compras.git
cd Aplicativo-Web---Gestion-de-Compras
cp .env.example .env
openssl rand -hex 32
nano .env
```

En `.env`, establezca una contraseña aleatoria propia en `DB_PASSWORD` y ponga `MICROSOFT_OAUTH2_ENABLED=false`. No use la contraseña de ejemplo `postgres`. Para una prueba por HTTP, configure `APP_BASE_URL=http://IP_DEL_SERVIDOR:8081` y `SESSION_COOKIE_SECURE=false`. El Compose publica el puerto `8081`; configure el firewall del proveedor y del servidor para permitirlo. Para producción, ponga HTTPS delante de la aplicación antes de habilitar acceso público y configure `APP_BASE_URL` y las cookies seguras acorde con su proxy TLS.

Guarde el archivo y restrinja sus permisos; luego valide y arranque:

```bash
chmod 600 .env
docker compose -f docker-compose.no-oauth2.yml config
sudo docker compose -f docker-compose.no-oauth2.yml up -d --build
sudo docker compose -f docker-compose.no-oauth2.yml ps
sudo docker compose -f docker-compose.no-oauth2.yml logs --tail=100 app
```

La primera compilación puede tardar varios minutos. PostgreSQL crea las tablas en el volumen nuevo; Spring aplica el esquema, valida las entidades y carga `src/main/resources/db/dml.sql`. El acceso inicial es `admin` / `admin123`; cámbielo después de iniciar sesión. Acceda en `http://IP_DEL_SERVIDOR:8081`.

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

Requiere Java 17, PostgreSQL activo en `localhost:5432` y una base de datos vacía ya creada. Configure `DB_USER` y `DB_PASSWORD` en `.env` con credenciales válidas para PostgreSQL y ejecute la aplicación normalmente:

```powershell
.\mvnw.cmd spring-boot:run
```

Al iniciar, Spring crea los schemas y tablas usando los scripts de `docker/init-db/`, carga los datos iniciales desde `src/main/resources/db/dml.sql` y luego valida el modelo JPA. La aplicación escucha normalmente en el puerto `8080`.

## 7. Base de datos

Los scripts de inicialización están en `docker/init-db/`:

- `01-create-schema.sql`: creación de los schemas `core`, `compras` y `polizas`.
- `DDL.sql`: tablas, relaciones, índices y restricciones, con claves foráneas entre schemas.

`src/main/resources/db/dml.sql` carga los catálogos, roles, permisos, sede inicial y usuario `admin` al iniciar. Sus inserciones son idempotentes, así que los reinicios no duplican esos registros.

`spring.jpa.hibernate.ddl-auto=validate` comprueba que las entidades coincidan con el DDL. Los scripts de esquema se ejecutan antes de esta validación y están preparados para repetirse en cada inicio. Para tener una base realmente vacía, use una base PostgreSQL nueva; el arranque no borra tablas ni registros existentes.

## 8. Verificación de instalación

1. Compruebe que PostgreSQL esté saludable.
2. Compruebe `http://localhost/actuator/health` detrás de Nginx o `http://localhost:8080/actuator/health` en ejecución local.
3. Abra `/login`; no podrá autenticarse hasta que agregue usuarios a la base.
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
