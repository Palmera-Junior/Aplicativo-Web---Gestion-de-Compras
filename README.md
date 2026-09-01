## VERSION 1.1.0

## Ejecución con Docker

1. Copia [.env.example](.env.example) a `.env` y ajusta los valores si necesitas.
2. Ejecuta:
   - `docker compose up --build`
3. La aplicación quedará disponible en `http://localhost` y la base de datos en `localhost:5432`.
4. Para detener los servicios usa `docker compose down`.
5. Si no necesitas login con Microsoft, usa `docker compose -f docker-compose.no-oauth2.yml up --build` (no exige `MICROSOFT_CLIENT_ID`, `MICROSOFT_CLIENT_SECRET` ni `MICROSOFT_TENANT_ID`).

## Configuración de correo con Amazon SES

El envío automático usa Amazon SES mediante SMTP. Copia `.env.example` a `.env` y completa:

- `SES_SMTP_HOST`: endpoint SMTP de la región SES, por ejemplo `email-smtp.us-east-1.amazonaws.com`.
- `SES_SMTP_PORT`: `587` para SMTP con STARTTLS.
- `SES_SMTP_USERNAME`: usuario SMTP generado desde Amazon SES.
- `SES_SMTP_PASSWORD`: contraseña SMTP generada desde Amazon SES.
- `SES_FROM_EMAIL`: remitente verificado; por defecto `notificaciones@palmerajunior.com`.

Las credenciales SMTP de SES no son las mismas credenciales generales de AWS. El archivo `.env` está excluido de Git y no debe compartirse.
