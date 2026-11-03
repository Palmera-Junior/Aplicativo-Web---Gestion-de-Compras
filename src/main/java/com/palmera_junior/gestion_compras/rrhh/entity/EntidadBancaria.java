package com.palmera_junior.gestion_compras.rrhh.entity;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "entidad_bancaria", schema = "rrhh")
@AttributeOverride(name = "id", column = @Column(name = "id_entidad_bancaria"))
public class EntidadBancaria extends CatalogoRrhh {
}
