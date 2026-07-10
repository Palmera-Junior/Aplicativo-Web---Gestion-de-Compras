-- 1. Creación de tipos personalizados (ENUMS)
CREATE TYPE tipo_rol AS ENUM ('ADMIN', 'USER');

-- 2. Creación de tablas independientes (sin llaves foráneas que dependan de otras)

CREATE TABLE sede (
    id_sede SERIAL PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    prefijo_ciudad VARCHAR(10) NOT NULL UNIQUE,
    direccion VARCHAR(255)
);

CREATE TABLE producto (
    id_producto SERIAL PRIMARY KEY,
    codigo_inventario VARCHAR(50) NOT NULL UNIQUE,
    nombre VARCHAR(150) NOT NULL,
    presentacion VARCHAR(100),
    descripcion TEXT
);

-- 3. Creación de tablas con dependencias simples (Nivel 1)

CREATE TABLE usuario (
    id_usuario SERIAL PRIMARY KEY,
    cedula VARCHAR(20) NOT NULL UNIQUE,
    nombre VARCHAR(100) NOT NULL,
    apellido VARCHAR(100) NOT NULL,
    cargo VARCHAR(100),
    nombre_usuario VARCHAR(50) NOT NULL UNIQUE,
    contrasenia VARCHAR(255) NOT NULL,
    rol tipo_rol NOT NULL,
    id_sede INT NOT NULL,
    CONSTRAINT fk_usuario_sede FOREIGN KEY (id_sede) REFERENCES sede (id_sede)
);

CREATE TABLE proveedor (
    id_prov SERIAL PRIMARY KEY,
    nit VARCHAR(50) NOT NULL,
    nombre VARCHAR(150) NOT NULL,
    correo VARCHAR(150),
    direccion VARCHAR(255),
    telefono VARCHAR(50),
    ciudad VARCHAR(100),
    id_sede INT NOT NULL,
    CONSTRAINT fk_proveedor_sede FOREIGN KEY (id_sede) REFERENCES sede (id_sede)
);

-- 4. Creación de tabla principal transaccional (Nivel 2)

CREATE TABLE orden_compra (
    id_orden SERIAL PRIMARY KEY,
    id_prov INT, -- Nullable para el caso de proveedores esporádicos ("Otro")
    id_sede INT NOT NULL,
    id_usuario INT NOT NULL,
    numero_orden VARCHAR(50) NOT NULL UNIQUE,
    fecha TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    -- Snapshot del Proveedor
    nombre_prov VARCHAR(150) NOT NULL,
    nit_prov VARCHAR(50) NOT NULL,
    direccion_prov VARCHAR(255),
    ciudad_prov VARCHAR(100),
    telefono_prov VARCHAR(50),
    correo_prov VARCHAR(150),
    
    -- Totales y Observaciones
    observaciones TEXT, -- Nullable
    sub_total DECIMAL(10,2) NOT NULL,
    iva_total DECIMAL(10,2) NOT NULL,
    descuento DECIMAL(10,2) DEFAULT 0.00, -- Nullable/Default 0
    total DECIMAL(10,2) NOT NULL,
    
    CONSTRAINT fk_orden_proveedor FOREIGN KEY (id_prov) REFERENCES proveedor (id_prov) ON DELETE SET NULL,
    CONSTRAINT fk_orden_sede FOREIGN KEY (id_sede) REFERENCES sede (id_sede),
    CONSTRAINT fk_orden_usuario FOREIGN KEY (id_usuario) REFERENCES usuario (id_usuario)
);

-- 5. Creación de tabla de detalle (Nivel 3)

CREATE TABLE detalle_compra (
    id_detalle SERIAL PRIMARY KEY,
    id_orden INT NOT NULL,
    id_producto INT NOT NULL,
    
    -- Snapshot del Producto en el momento de la compra
    cantidad INT NOT NULL,
    codigo_inventario VARCHAR(50) NOT NULL,
    descripcion VARCHAR(150) NOT NULL,
    presentacion TEXT,
    valor_unitario DECIMAL(10,2) NOT NULL, -- Ingresado manualmente
    iva_producto DECIMAL(10,2) NOT NULL,
    valor_total_linea DECIMAL(10,2) NOT NULL,
    
    CONSTRAINT fk_detalle_orden FOREIGN KEY (id_orden) REFERENCES orden_compra (id_orden) ON DELETE CASCADE,
    CONSTRAINT fk_detalle_producto FOREIGN KEY (id_producto) REFERENCES producto (id_producto)
);