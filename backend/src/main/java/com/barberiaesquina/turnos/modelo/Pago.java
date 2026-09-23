package com.barberiaesquina.turnos.modelo;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** [Extensión] Cobro de un turno completado. Un turno tiene como mucho un pago. */
@Entity
@Table(name = "pago")
@Getter
@Setter
@NoArgsConstructor
public class Pago {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_pago")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_turno")
    private Turno turno;

    private BigDecimal monto;

    @Convert(converter = MedioPago.Conversor.class)
    private MedioPago medio;

    private LocalDateTime fecha;
}
