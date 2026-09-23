package com.barberiaesquina.turnos.servicio.excepcion;

/** 409: el pedido choca con el estado actual (horario tomado, email repetido, etc.). */
public class ConflictoException extends RuntimeException {

    public ConflictoException(String mensaje) {
        super(mensaje);
    }
}
