package com.barberiaesquina.turnos.web.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public final class ClienteDtos {

    private ClienteDtos() {}

    public record Resumen(Long id, String nombre, String apellido, String email, String telefono,
                          long visitas, long ausencias, BigDecimal gastado,
                          LocalDate ultimaVisita, LocalDate primeraVisita, TurnoDtos.Ref habitual) {}

    public record Pagina(List<Resumen> contenido, long total, int pagina, int tamano) {}

    public record Ficha(Resumen cliente, Double satisfaccion, TurnoDtos.Detalle proximoTurno,
                        List<TurnoDtos.Detalle> historial) {}
}
