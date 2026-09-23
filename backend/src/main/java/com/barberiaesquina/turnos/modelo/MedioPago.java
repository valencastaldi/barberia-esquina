package com.barberiaesquina.turnos.modelo;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum MedioPago implements ValorEnum {
    EFECTIVO("efectivo"),
    TRANSFERENCIA("transferencia"),
    MERCADOPAGO("mercadopago");

    private final String valor;

    MedioPago(String valor) {
        this.valor = valor;
    }

    @Override
    @JsonValue
    public String valor() {
        return valor;
    }

    @JsonCreator
    public static MedioPago desde(String valor) {
        return ValorEnum.desde(MedioPago.class, valor);
    }

    public static class Conversor extends ValorEnum.Conversor<MedioPago> {
        public Conversor() {
            super(MedioPago.class);
        }
    }
}
