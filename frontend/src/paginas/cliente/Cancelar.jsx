import { useState } from "react";
import { Link, useParams } from "react-router-dom";
import { api } from "../../api/api.js";
import { usePedido } from "../../api/usePedido.js";
import { BARBERIA } from "../../config.js";
import { fechaLarga, pesos } from "../../lib/formato.js";
import { BarraCta, EstadoPedido, LayoutCliente, Resumen, Topbar } from "../../componentes/cliente/LayoutCliente.jsx";
import { IconoCruz, IconoPregunta } from "../../componentes/Iconos.jsx";

/** RF-06: cancelación con el link único del email, sin iniciar sesión. */
export default function Cancelar() {
  const { token } = useParams();
  const turno = usePedido(() => api(`/turnos/cancelar/${encodeURIComponent(token)}`), [token]);
  const [cancelado, setCancelado] = useState(false);
  const [enviando, setEnviando] = useState(false);
  const [falla, setFalla] = useState(null);

  async function cancelar() {
    setEnviando(true);
    setFalla(null);
    try {
      await api(`/turnos/cancelar/${encodeURIComponent(token)}`, { metodo: "PATCH" });
      setCancelado(true);
      window.scrollTo({ top: 0, behavior: "smooth" });
    } catch (error) {
      setFalla(error.message);
      turno.recargar();
    } finally {
      setEnviando(false);
    }
  }

  if (turno.cargando || turno.error) {
    return (
      <LayoutCliente titulo="Cancelar turno">
        <Topbar />
        <div style={{ marginTop: 32 }}>
          <EstadoPedido cargando={turno.cargando}
                        error={turno.error?.status === 404 ? new Error("Este link no es válido. Revisá que esté completo.") : turno.error}
                        alReintentar={turno.error?.status === 404 ? null : turno.recargar} />
        </div>
      </LayoutCliente>
    );
  }

  const t = turno.datos;
  const resumen = (
    <Resumen filas={[
      ["Servicio", t.servicio],
      ["Día", fechaLarga(t.fecha)],
      ["Horario", `${t.horaInicio} a ${t.horaFin}`],
      ["Peluquero", t.barbero],
      ["Precio", pesos(t.precio)],
    ]} />
  );

  // Ya cancelado (ahora o antes)
  if (cancelado || t.estado === "cancelado") {
    return (
      <LayoutCliente titulo="Turno cancelado">
        <Topbar />
        <section className="resultado">
          <div className="icono alerta"><IconoCruz /></div>
          <h1>Turno cancelado</h1>
          <p>{cancelado ? "Listo. El horario quedó libre para otro cliente." : "Este turno ya estaba cancelado."}</p>
        </section>
        <div style={{ marginTop: 28 }}>{resumen}</div>
        <BarraCta>
          <Link className="btn btn-primario btn-block" to="/reservar">Reservar otro turno</Link>
        </BarraCta>
      </LayoutCliente>
    );
  }

  // Ya no se puede cancelar: pasó, o venció el link de 48 h
  if (!t.cancelable) {
    const yaPaso = t.estado !== "pendiente";
    return (
      <LayoutCliente titulo="Cancelar turno">
        <Topbar />
        <section className="resultado">
          <div className="icono neutro"><IconoPregunta /></div>
          <h1>{yaPaso ? "Este turno ya pasó" : "El link venció"}</h1>
          <p>
            {yaPaso
              ? "No hay nada que cancelar."
              : "Para cancelar ahora, avisanos por WhatsApp así liberamos el horario."}
          </p>
        </section>
        <div style={{ marginTop: 28 }}>{resumen}</div>
        <BarraCta>
          {yaPaso
            ? <Link className="btn btn-primario btn-block" to="/reservar">Reservar un turno</Link>
            : <a className="btn btn-primario btn-block" href={`https://wa.me/${BARBERIA.whatsapp}`}>Escribir por WhatsApp</a>}
        </BarraCta>
      </LayoutCliente>
    );
  }

  return (
    <LayoutCliente titulo="Cancelar turno">
      <Topbar />
      <section className="resultado">
        <div className="icono neutro"><IconoPregunta /></div>
        <h1>¿Cancelás este turno?</h1>
        <p>{t.cliente}, el horario vuelve a quedar libre para otro cliente.</p>
      </section>
      <div style={{ marginTop: 28 }}>{resumen}</div>
      {falla && <div className="aviso-error" role="alert">{falla}</div>}
      <BarraCta>
        <Link className="btn btn-secundario" style={{ flex: 1 }} to="/">Mantenerlo</Link>
        <button className="btn btn-peligro" style={{ flex: 1 }} onClick={cancelar} disabled={enviando}>
          {enviando ? "Cancelando…" : "Sí, cancelar"}
        </button>
      </BarraCta>
    </LayoutCliente>
  );
}
