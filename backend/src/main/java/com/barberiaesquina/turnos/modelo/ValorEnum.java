package com.barberiaesquina.turnos.modelo;

import jakarta.persistence.AttributeConverter;

/**
 * Enum que se guarda en la base y viaja en el JSON con un valor en minúscula
 * ("pendiente", "mercadopago"), igual que los ENUM del esquema SQL.
 */
public interface ValorEnum {

    String valor();

    static <E extends Enum<E> & ValorEnum> E desde(Class<E> tipo, String valor) {
        if (valor == null) return null;
        for (E e : tipo.getEnumConstants()) {
            if (e.valor().equalsIgnoreCase(valor.trim())) return e;
        }
        throw new IllegalArgumentException("Valor inválido para " + tipo.getSimpleName() + ": " + valor);
    }

    /** Conversor JPA genérico: cada enum declara una subclase con su tipo. */
    abstract class Conversor<E extends Enum<E> & ValorEnum> implements AttributeConverter<E, String> {

        private final Class<E> tipo;

        protected Conversor(Class<E> tipo) {
            this.tipo = tipo;
        }

        @Override
        public String convertToDatabaseColumn(E atributo) {
            return atributo == null ? null : atributo.valor();
        }

        @Override
        public E convertToEntityAttribute(String columna) {
            return desde(tipo, columna);
        }
    }
}
