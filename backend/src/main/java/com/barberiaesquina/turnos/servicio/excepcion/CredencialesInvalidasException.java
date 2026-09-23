package com.barberiaesquina.turnos.servicio.excepcion;

/** 401: email o contraseña incorrectos. */
public class CredencialesInvalidasException extends RuntimeException {

    public CredencialesInvalidasException() {
        super("Email o contraseña incorrectos");
    }
}
