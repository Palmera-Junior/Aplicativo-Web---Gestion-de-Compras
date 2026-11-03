package com.palmera_junior.gestion_compras.rrhh.entity;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "tipo_pago", schema = "rrhh")
@AttributeOverride(name = "id", column = @Column(name = "id_tipo_pago"))
public class TipoPago extends CatalogoRrhh {
}
