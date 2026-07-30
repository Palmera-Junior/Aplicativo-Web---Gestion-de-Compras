# Aplicativo Web - Gestión de Compras

Aplicación web desarrollada en Java con Spring Boot para digitalizar y controlar el proceso de gestión de órdenes de compra dentro de una organización. El sistema permite registrar solicitudes de compra, validar su información, aprobarlas, recibirlas y generar documentos PDF alineados con los procesos administrativos de la empresa.

## 1. Propósito del sistema

Este aplicativo busca reemplazar procesos manuales y dispersos de compra mediante una plataforma centralizada donde los usuarios pueden:

- crear y editar órdenes de compra;
- asociar productos, proveedores, centros de costo y sedes;
- controlar el estado de cada solicitud desde borrador hasta recepción;
- aprobar órdenes según permisos de negocio;
- generar documentos PDF para formalizar la operación;
- consultar el historial de compras desde un tablero principal.

## 2. Problema que resuelve

Antes de esta solución, la gestión de compras podía estar sujeta a:

- registros dispersos en hojas, correos o archivos compartidos;
- falta de trazabilidad del estado de una orden;
- errores en la captura de datos de proveedores, productos y costos;
- dificultades para auditar aprobaciones y recepciones;
- generación manual de documentos para la confirmación de compra.

Con esta herramienta, las operaciones quedan registradas, visibles y controladas dentro del mismo flujo de trabajo.

## 3. Alcance funcional

### 3.1 Gestión de órdenes de compra

El sistema permite:

- crear órdenes de compra desde un formulario estructurado;
- registrar datos del proveedor, centro de costo, sede y observaciones;
- agregar múltiples líneas de detalle por producto o concepto;
- calcular subtotales, IVA, descuentos y totales;
- asignar automáticamente un número de orden basado en la sede y la fecha.

### 3.2 Ciclo de vida de una orden

Cada orden de compra tiene un estado de negocio:

- Borrador: la orden se crea o edita antes de ser aprobada.
- Aprobada: la orden fue validada por un aprobador autorizado.
- Recibida: la orden ya fue recepcionada y se registraron datos de recepción.

El flujo está diseñado para asegurar que una orden no pueda avanzar sin cumplir las reglas de negocio asociadas.

### 3.3 Aprobaciones y recepción

- Solo usuarios con rol de aprobador pueden aprobar órdenes.
- La aprobación queda asociada al usuario que ejecuta la acción y a la fecha de aprobación.
- La recepción permite registrar número de factura, persona que recibe, observaciones y fecha de recepción.

### 3.4 Generación de PDF

El sistema integra la generación de documentos PDF para las órdenes aprobadas o recibidas. El archivo incluye:

- encabezado institucional con logos;
- datos generales de la orden;
- información del proveedor;
- tabla de detalles con cantidades, valores y total por línea;
- totales generales;
- datos de aprobación y recepción;
- información de facturación electrónica.

## 4. Arquitectura del sistema

La aplicación está construida con una arquitectura Spring Boot MVC + servicios + repositorios.

### 4.1 Capas principales

- Controllers: exponen los endpoints web y las vistas del tablero y autenticación.
- Services: contienen la lógica de negocio y validaciones.
- Repositories: encapsulan el acceso a la base de datos.
- Entities: representan los modelos principales del negocio.
- DTOs: transfieren datos entre la interfaz y la lógica de negocio.

### 4.2 Componentes principales

- Login y seguridad: autenticación y autorización del sistema.
- Dashboard: panel de visualización y filtrado de órdenes de compra.
- Orden de compra: creación, edición, aprobación y recepción.
- PDF: generación de documentos formales.

## 5. Modelo de negocio

El sistema trabaja con entidades clave como:

- Usuario
- Sede
- Centro de costo
- Proveedor
- Producto
- Orden de compra
- Detalle de compra

Cada orden se encuentra vinculada a una sede, un centro de costo, un usuario solicitante y un proveedor. Los detalles permiten desglosar los productos o servicios incluidos en la compra.

## 6. Tecnologías utilizadas

- Java 17
- Spring Boot 4.1.0
- Spring MVC
- Spring Security
- Spring Data JPA
- Thymeleaf
- PostgreSQL
- Lombok
- OpenPDF (para generación de PDF)
- Maven

## 7. Estructura del proyecto

```text
src/
  main/
    java/
      com/palmera_junior/gestion_compras/
        config/
        controller/
        dto/
        entity/
        repository/
        security/
        service/
    resources/
      application.properties
      static/
      templates/
  test/
    java/
```

## 8. Requisitos previos

Antes de ejecutar la aplicación, asegúrate de tener instalado:

- JDK 17 o superior
- Maven
- PostgreSQL
- Git

## 9. Configuración de la base de datos

La aplicación utiliza PostgreSQL. Debes configurar las variables o credenciales en el archivo de propiedades de la aplicación:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/tu_base
spring.datasource.username=tu_usuario
spring.datasource.password=tu_password
spring.datasource.driver-class-name=org.postgresql.Driver
```

Asegúrate de crear la base de datos antes de iniciar la aplicación.

## 10. Ejecución

Desde la raíz del proyecto, ejecuta:

```bash
./mvnw spring-boot:run
```

En Windows:

```powershell
mvnw.cmd spring-boot:run
```

La aplicación quedará disponible en:

- http://localhost:8080/login

## 11. Flujo de uso recomendado

1. Iniciar sesión con credenciales válidas.
2. Acceder al dashboard para ver las órdenes registradas.
3. Crear una nueva orden de compra con sus detalles.
4. Guardar la orden en estado de borrador.
5. Aprobarla si eres aprobador.
6. Recibir la orden cuando llegue físicamente o documentalmente.
7. Descargar el PDF generado para la orden.

## 12. Funcionalidades destacadas

- Gestión centralizada de órdenes de compra.
- Control de estados y trazabilidad.
- Validaciones de negocio por rol.
- Generación automática de documentos PDF.
- Panel de consulta y filtrado por fecha y texto.

## 13. Consideraciones de negocio

El sistema está orientado a procesos empresariales donde:

- una compra debe estar soportada por datos completos del proveedor;
- la aprobación debe ser realizada por un usuario con autoridad;
- la recepción de la compra debe quedar registrada con evidencia administrativa;
- el PDF funciona como respaldo formal del proceso.

## 14. Mejoras futuras

Algunas mejoras recomendadas para evolución del aplicativo:

- módulo de usuarios y permisos más granular;
- auditoría completa de cambios por orden;
- integración con ERP o contabilidad;
- notificaciones por correo electrónico;
- exportación a Excel o CSV;
- gestión de contratos y cotizaciones.

## 15. Autor y contexto

Aplicación desarrollada para apoyar la operación de compras y la trazabilidad documental de órdenes dentro de una organización.
