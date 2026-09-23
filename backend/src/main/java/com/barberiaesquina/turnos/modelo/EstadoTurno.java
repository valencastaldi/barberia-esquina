package com.barberiaesquina.turnos.modelo;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Ciclo de vida del turno (Etapa 3): nace pendiente y pasa una sola vez
 * a uno de los tres estados finales.
 */
public enum EstadoTurno implements ValorEnum {
    PENDIENTE("pendiente"),
    COMPLETADO("completado"),
    AUSENTE("ausente"),
    CANCELADO("cancelado");

    private final String valor;

    EstadoTurno(String valor) {
        this.valor = valor;
    }

    @Override
    @JsonValue
    public String valor() {
        return valor;
    }

    @JsonCreator
    public static EstadoTurno desde(String valor) {
        return ValorEnum.desde(EstadoTurno.class, valor);
    }

    public boolean esFinal() {
        return this != PENDIENTE;
    }

    public static class Conversor extends ValorEnum.Conversor<EstadoTurno> {
        public Conversor() {
            super(EstadoTurno.class);
        }
    }
}
