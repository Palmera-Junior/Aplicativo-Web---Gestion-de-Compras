package com.palmera_junior.gestion_compras.entity;

import jakarta.persistence.*;
import lombok.*;
import java.util.Set;
import java.util.HashSet;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Entity
@Table(name = "proveedor")
public class Proveedor {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_prov")
    private Integer idProv;
    
    @Column(nullable = false, length = 50)
    private String nit;

    @Column(nullable = false, length = 150)
    private String nombre;

   @Column(length = 150)
   private String correo; 

   @Column(length = 255)
   private String direccion; 

   @Column(length = 50)
   private String telefono; 

   @Column(length = 150)
   private String ciudad; 

   @ManyToMany
   @JoinTable(
       name = "proveedor_sede",
       joinColumns = @JoinColumn(name = "id_prov"),
       inverseJoinColumns = @JoinColumn(name = "id_sede")
   )
   @ToString.Exclude
   private Set<Sede> sedes = new HashSet<>();

   @Override
   public boolean equals(Object o) {
      if(this== o) return true;
      if(!(o instanceof Proveedor)) return false;
      Proveedor other = (Proveedor) o;
      return idProv != null && idProv.equals(other.getIdProv());
   }
   @Override
   public int hashCode() {
      return getClass().hashCode();
   }


}

