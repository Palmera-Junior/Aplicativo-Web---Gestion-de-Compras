package com.palmera_junior.gestion_compras.rrhh.entity;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "cargo", schema = "rrhh")
@AttributeOverride(name = "id", column = @Column(name = "id_cargo"))
public class Cargo extends CatalogoRrhh {
}
