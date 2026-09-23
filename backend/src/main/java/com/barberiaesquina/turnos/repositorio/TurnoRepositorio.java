package com.barberiaesquina.turnos.repositorio;

import com.barberiaesquina.turnos.modelo.EstadoTurno;
import com.barberiaesquina.turnos.modelo.Turno;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface TurnoRepositorio extends JpaRepository<Turno, Long> {

    /** Turnos que ocupan agenda (todo menos cancelados) de un peluquero en un día. */
    @Query("""
            select t from Turno t
            where t.barbero.id = :idBarbero and t.fecha = :fecha and t.estado <> :excluido
            """)
    List<Turno> delDia(Long idBarbero, LocalDate fecha, EstadoTurno excluido);

    @Query("""
            select t from Turno t
            join fetch t.cliente join fetch t.servicio join fetch t.barbero
            where t.fecha between :desde and :hasta
              and (:idBarbero is null or t.barbero.id = :idBarbero)
            order by t.fecha, t.horaInicio
            """)
    List<Turno> entre(LocalDate desde, LocalDate hasta, Long idBarbero);

    @Query("""
            select t from Turno t
            join fetch t.cliente join fetch t.servicio join fetch t.barbero
            where t.cliente.id = :idCliente
            order by t.fecha desc, t.horaInicio desc
            """)
    List<Turno> deCliente(Long idCliente);

    @Query("""
            select t from Turno t
            join fetch t.cliente join fetch t.servicio join fetch t.barbero
            where t.cliente.id in :idsClientes
            """)
    List<Turno> deClientes(Collection<Long> idsClientes);

    @Query("select t from Turno t join fetch t.cliente join fetch t.servicio join fetch t.barbero where t.id = :id")
    Optional<Turno> conDetalle(Long id);

    @Query("select t from Turno t join fetch t.cliente join fetch t.servicio join fetch t.barbero where t.tokenCancelacion = :token")
    Optional<Turno> porTokenCancelacion(String token);

    boolean existsByServicioId(Long idServicio);

    // ---------- Agregados para la pantalla de Clientes ----------

    /** Por cliente y estado: [idCliente, estado, cantidad, suma de precios, última fecha, primera fecha]. */
    @Query("""
            select t.cliente.id, t.estado, count(t), sum(t.precio), max(t.fecha), min(t.fecha)
            from Turno t group by t.cliente.id, t.estado
            """)
    List<Object[]> agregadoPorClienteYEstado();

    /** [idCliente, idBarbero, nombreBarbero, cantidad] de turnos en ese estado. */
    @Query("""
            select t.cliente.id, t.barbero.id, t.barbero.nombre, count(t)
            from Turno t where t.estado = :estado
            group by t.cliente.id, t.barbero.id, t.barbero.nombre
            """)
    List<Object[]> atencionesPorClienteYBarbero(EstadoTurno estado);

    @Query("select distinct t.cliente.id from Turno t where t.estado = :estado and t.fecha >= :desde")
    List<Long> clientesConTurnoDesde(EstadoTurno estado, LocalDate desde);
}
