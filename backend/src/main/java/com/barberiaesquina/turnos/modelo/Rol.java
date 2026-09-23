package com.barberiaesquina.turnos.modelo;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/** DUENO es el "Super Admin" de la documentación; BARBERO es la extensión multi-peluquero. */
public enum Rol implements ValorEnum {
    DUENO("dueno"),
    BARBERO("barbero");

    private final String valor;

    Rol(String valor) {
        this.valor = valor;
    }

    @Override
    @JsonValue
    public String valor() {
        return valor;
    }

    @JsonCreator
    public static Rol desde(String valor) {
        return ValorEnum.desde(Rol.class, valor);
    }

    public static class Conversor extends ValorEnum.Conversor<Rol> {
        public Conversor() {
            super(Rol.class);
        }
    }
}
