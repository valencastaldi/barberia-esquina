package com.barberiaesquina.turnos.modelo;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "barbero")
@Getter
@Setter
@NoArgsConstructor
public class Barbero {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_barbero")
    private Long id;

    private String nombre;
    private String apellido;
    private String dni;
    private LocalDate fechaNacimiento;
    private String email;
    private String passwordHash;
    private String telefono;
    private LocalDateTime fechaAlta;

    @Convert(converter = Rol.Conversor.class)
    private Rol rol = Rol.BARBERO;

    private Integer comisionPct = 0;
    private boolean activo = true;

    @ManyToMany
    @JoinTable(name = "barbero_servicio",
            joinColumns = @JoinColumn(name = "id_barbero"),
            inverseJoinColumns = @JoinColumn(name = "id_servicio"))
    private Set<Servicio> servicios = new HashSet<>();

    public String nombreCompleto() {
        return nombre + " " + apellido;
    }

    public boolean haceServicio(Long idServicio) {
        return servicios.stream().anyMatch(s -> s.getId().equals(idServicio));
    }
}
