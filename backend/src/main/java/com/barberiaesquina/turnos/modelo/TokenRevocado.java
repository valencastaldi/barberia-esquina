package com.barberiaesquina.turnos.modelo;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/** Un JWT cerrado con "Cerrar sesión": deja de valer aunque todavía no haya vencido. */
@Entity
@Table(name = "token_revocado")
@Getter
@Setter
@NoArgsConstructor
public class TokenRevocado {

    /** El claim jti del token. */
    @Id
    private String jti;

    /** Cuándo vence el token: pasada esa hora la fila ya no hace falta. */
    private LocalDateTime vence;
}
