package com.palmera_junior.gestion_compras.rrhh.entity;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "eps", schema = "rrhh")
@AttributeOverride(name = "id", column = @Column(name = "id_eps"))
public class Eps extends CatalogoRrhh {
}
