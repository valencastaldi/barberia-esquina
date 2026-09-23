import { Link, Navigate } from "react-router-dom";
import { BARBERIA } from "../../config.js";
import { fechaLarga, pesos } from "../../lib/formato.js";
import { ultimaReserva } from "../../lib/memoria.js";
import { BarraCta, LayoutCliente, Resumen, Topbar } from "../../componentes/cliente/LayoutCliente.jsx";
import { IconoOk } from "../../componentes/Iconos.jsx";

/** RF-04 / RF-05: turno confirmado. Los datos vienen de la respuesta del POST /turnos. */
export default function Confirmacion() {
  const reserva = ultimaReserva();
  if (!reserva) return <Navigate to="/" replace />;

  return (
    <LayoutCliente titulo="Turno confirmado">
      <Topbar />

      <section className="resultado">
        <div className="icono"><IconoOk /></div>
        <h1>Turno confirmado</h1>
        <p>{reserva.nombre}, te esperamos.</p>
      </section>

      <div style={{ marginTop: 28 }}>
        <Resumen
          filas={[
            ["Servicio", reserva.servicio],
            ["Día", fechaLarga(reserva.fecha)],
            ["Horario", `${reserva.horaInicio} a ${reserva.horaFin}`],
            ["Peluquero", reserva.barbero],
          ]}
          total={["A pagar en el local", pesos(reserva.precio)]}
        />
      </div>

      <div className="local" style={{ marginBottom: 22 }}>
        <div><span>Dirección</span><strong>{BARBERIA.direccion}</strong></div>
        <div><span>Turno N°</span><strong>#{reserva.idTurno}</strong></div>
      </div>

      <p className="nota-maqueta" style={{ textAlign: "center" }}>
        Te mandamos un email a <strong>{reserva.email}</strong> con estos datos
        y un link para cancelar si no podés venir.
      </p>

      <footer className="pie">
        <p>¿Necesitás cambiar algo? Cancelá y reservá de nuevo, lleva 30 segundos.</p>
      </footer>

      <BarraCta>
        <Link className="btn btn-secundario" style={{ flex: 1 }} to={`/cancelar/${reserva.tokenCancelacion}`}>
          Cancelar turno
        </Link>
        <Link className="btn btn-primario" style={{ flex: 1 }} to="/">Listo</Link>
      </BarraCta>
    </LayoutCliente>
  );
}
