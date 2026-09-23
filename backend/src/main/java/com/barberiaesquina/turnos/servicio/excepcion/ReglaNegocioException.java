package com.barberiaesquina.turnos.servicio.excepcion;

/** 422: el pedido está bien formado pero viola una regla del negocio. */
public class ReglaNegocioException extends RuntimeException {

    public ReglaNegocioException(String mensaje) {
        super(mensaje);
    }
}
