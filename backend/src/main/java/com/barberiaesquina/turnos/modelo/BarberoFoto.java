package com.barberiaesquina.turnos.modelo;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** [Extensión] Foto de perfil de un peluquero (una por peluquero). */
@Entity
@Table(name = "barbero_foto")
@Getter
@Setter
@NoArgsConstructor
public class BarberoFoto {

    @Id
    @Column(name = "id_barbero")
    private Long idBarbero;

    @Lob
    private byte[] contenido;

    private String tipo;
}
