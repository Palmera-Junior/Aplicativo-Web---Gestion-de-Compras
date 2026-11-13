CREATE TABLE IF NOT EXISTS core.usuario_sede (
    id_usuario INT NOT NULL,
    id_sede INT NOT NULL,
    CONSTRAINT pk_usuario_sede PRIMARY KEY (id_usuario, id_sede),
    CONSTRAINT fk_usuario_sede_usuario FOREIGN KEY (id_usuario)
        REFERENCES core.usuario (id_usuario) ON DELETE CASCADE,
    CONSTRAINT fk_usuario_sede_sede FOREIGN KEY (id_sede)
        REFERENCES core.sede (id_sede) ON DELETE RESTRICT
);

CREATE INDEX IF NOT EXISTS idx_usuario_sede_sede ON core.usuario_sede (id_sede);

INSERT INTO core.usuario_sede (id_usuario, id_sede)
SELECT id_usuario, id_sede
FROM core.usuario
WHERE id_sede IS NOT NULL
ON CONFLICT (id_usuario, id_sede) DO NOTHING;
