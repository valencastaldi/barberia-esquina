package com.barberiaesquina.turnos.modelo;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "turno")
@Getter
@Setter
@NoArgsConstructor
public class Turno {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_turno")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_cliente")
    private Cliente cliente;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_servicio")
    private Servicio servicio;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_barbero")
    private Barbero barbero;

    private LocalDate fecha;
    private LocalTime horaInicio;
    private LocalTime horaFin;

    @Convert(converter = EstadoTurno.Conversor.class)
    private EstadoTurno estado = EstadoTurno.PENDIENTE;

    private BigDecimal precio;
    private String tokenCancelacion;
    private LocalDateTime tokenVencimiento;
    private String tokenEncuesta;
    private LocalDateTime fechaCreacion;

    /** ¿Se superpone con el rango [inicio, fin)? */
    public boolean ocupa(LocalTime inicio, LocalTime fin) {
        return horaInicio.isBefore(fin) && inicio.isBefore(horaFin);
    }
}
