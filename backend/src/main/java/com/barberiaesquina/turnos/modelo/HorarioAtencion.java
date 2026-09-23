package com.barberiaesquina.turnos.modelo;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalTime;

/** Horario de un peluquero para un día de la semana (0 = domingo ... 6 = sábado). */
@Entity
@Table(name = "horario_atencion")
@Getter
@Setter
@NoArgsConstructor
public class HorarioAtencion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_horario")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_barbero")
    private Barbero barbero;

    private Integer diaSemana;
    private LocalTime horaInicio;
    private LocalTime horaFin;
    private Integer duracionSlotMin = 30;
    private boolean activo = true;
}
