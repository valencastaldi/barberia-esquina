package com.barberiaesquina.turnos.modelo;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalTime;

/** RF-12: franja en la que un peluquero no toma turnos. */
@Entity
@Table(name = "bloqueo")
@Getter
@Setter
@NoArgsConstructor
public class Bloqueo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_bloqueo")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_barbero")
    private Barbero barbero;

    private LocalDate fecha;
    private LocalTime horaInicio;
    private LocalTime horaFin;
    private String motivo;

    public boolean ocupa(LocalTime inicio, LocalTime fin) {
        return horaInicio.isBefore(fin) && inicio.isBefore(horaFin);
    }
}
