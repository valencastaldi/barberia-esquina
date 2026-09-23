package com.barberiaesquina.turnos.modelo;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/** Se crea sola al reservar: el cliente no tiene cuenta ni contraseña. */
@Entity
@Table(name = "cliente")
@Getter
@Setter
@NoArgsConstructor
public class Cliente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_cliente")
    private Long id;

    private String nombre;
    private String apellido;
    private String email;
    private String telefono;
    private LocalDateTime fechaAlta;

    public String nombreCompleto() {
        return nombre + " " + apellido;
    }
}
