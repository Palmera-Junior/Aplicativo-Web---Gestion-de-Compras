## VERSION 1.1.0

## Ejecución con Docker

1. Copia [.env.example](.env.example) a `.env` y ajusta los valores si necesitas.
2. Ejecuta:
   - `docker compose up --build`
3. La aplicación quedará disponible en `http://localhost` y la base de datos en `localhost:5432`.
4. Para detener los servicios usa `docker compose down`.
