package com.palmera_junior.gestion_compras.rrhh.entity;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "nivel_educativo", schema = "rrhh")
@AttributeOverride(name = "id", column = @Column(name = "id_nivel_educativo"))
public class NivelEducativo extends CatalogoRrhh {
}
