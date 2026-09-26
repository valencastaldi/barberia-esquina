package com.barberiaesquina.turnos.servicio;

import com.barberiaesquina.turnos.config.AppProperties;
import com.barberiaesquina.turnos.servicio.excepcion.ReglaNegocioException;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** "Ahora" y "hoy" siempre en la hora de Córdoba, a partir del reloj inyectado. */
@Component
public class Calendario {

    private final Clock reloj;
    private final AppProperties props;

    public Calendario(Clock reloj, AppProperties props) {
        this.reloj = reloj;
        this.props = props;
    }

    public LocalDateTime ahora() {
        return LocalDateTime.now(reloj);
    }

    public LocalDate hoy() {
        return LocalDate.now(reloj);
    }

    /** Último día en que se puede reservar. */
    public LocalDate ultimoDiaReservable() {
        return hoy().plusDays(props.turnos().diasAnticipacion());
    }

    /** Los listados y reportes del panel aceptan como mucho un año por pedido. */
    public static void validarRango(LocalDate desde, LocalDate hasta) {
        if (hasta.isBefore(desde)) throw new ReglaNegocioException("'hasta' no puede ser anterior a 'desde'");
        if (desde.plusDays(366).isBefore(hasta)) throw new ReglaNegocioException("El rango máximo es de un año");
    }

    /** Día de la semana con la convención del documento: 0 = domingo ... 6 = sábado. */
    public static int diaSemana(LocalDate fecha) {
        return fecha.getDayOfWeek().getValue() % 7;
    }
}
