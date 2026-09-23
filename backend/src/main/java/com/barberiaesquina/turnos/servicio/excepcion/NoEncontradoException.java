package com.barberiaesquina.turnos.servicio.excepcion;

/** 404: el recurso pedido no existe. */
public class NoEncontradoException extends RuntimeException {

    public NoEncontradoException(String mensaje) {
        super(mensaje);
    }

    public static NoEncontradoException de(String recurso, Object id) {
        return new NoEncontradoException(recurso + " " + id + " no existe");
    }
}
