import { useState } from "react";
import { decimal, fechaLarga } from "../../lib/formato.js";
import { useSesion, usePedidoAdmin } from "../sesion.jsx";
import { haceCuanto, hoyIso, sumarDias } from "../fechas.js";
import { Cabecera, EstadoCarga, Segmentos } from "../componentes/ui.jsx";

const PERIODOS = [
  { valor: 30, texto: "30 días" },
  { valor: 90, texto: "90 días" },
  { valor: 365, texto: "1 año" },
];
const MOSTRAR = [
  { valor: "todas", texto: "Todas" },
  { valor: "comentario", texto: "Con comentario" },
  { valor: "bajas", texto: "3 estrellas o menos" },
];

/** Lo que dicen los clientes en la encuesta (RF-15 a RF-17). La ve todo el equipo. */
export default function Opiniones() {
  const { usuario } = useSesion();
  const [dias, setDias] = useState(90);
  const [idBarbero, setIdBarbero] = useState(null);
  const [mostrar, setMostrar] = useState("todas");
  const [limite, setLimite] = useState(30);

  const hoy = hoyIso();
  const reporte = usePedidoAdmin("/opiniones", { desde: sumarDias(hoy, -(dias - 1)), hasta: hoy, barbero: idBarbero });
  const r = reporte.datos;

  const opiniones = (r?.opiniones ?? []).filter((o) =>
    mostrar === "comentario" ? !!o.comentario : mostrar === "bajas" ? o.calificacion <= 3 : true);

  return (
    <>
      <Cabecera titulo="Opiniones" bajada="Lo que responden los clientes en la encuesta después de cada turno.">
        <Segmentos opciones={PERIODOS} valor={dias} alCambiar={(d) => { setDias(d); setLimite(30); }} etiqueta="Período" />
      </Cabecera>

      <EstadoCarga pedido={reporte} />

      {r && (
        <section className="equipo opiniones-equipo" aria-label="Promedio de cada peluquero">
          <TarjetaPromedio titulo="Todo el equipo" promedio={promedioGeneral(r.porBarbero)} cantidad={total(r.porBarbero)}
                           elegida={idBarbero === null} alElegir={() => setIdBarbero(null)} />
          {r.porBarbero.map((b) => (
            <TarjetaPromedio key={b.idBarbero} titulo={b.idBarbero === usuario.id ? `${b.nombre} (vos)` : b.nombre}
                             promedio={b.promedio} cantidad={b.cantidad} distribucion={b.distribucion}
                             elegida={idBarbero === b.idBarbero} alElegir={() => setIdBarbero(b.idBarbero)} />
          ))}
        </section>
      )}

      {r && (
        <section className="bloque">
          <header>
            <div>
              <h2>{idBarbero ? `Opiniones de ${r.porBarbero.find((b) => b.idBarbero === idBarbero)?.nombre}` : "Todas las opiniones"}</h2>
              <p>Últimos {dias === 365 ? "12 meses" : `${dias} días`}. La más nueva arriba.</p>
            </div>
            <Segmentos chico opciones={MOSTRAR} valor={mostrar} alCambiar={(m) => { setMostrar(m); setLimite(30); }} etiqueta="Mostrar" />
          </header>
          {opiniones.length === 0 && <div className="vacio">No hay opiniones para mostrar con este filtro.</div>}
          <div className="cuerpo lista-opiniones">
            {opiniones.slice(0, limite).map((o) => (
              <article className="comentario" key={o.idTurno}>
                <div className="top">
                  <b>{o.cliente}</b>
                  <span className="puntaje" aria-label={`${o.calificacion} de 5`}>
                    {"★".repeat(o.calificacion)}<span className="apagadas">{"★".repeat(5 - o.calificacion)}</span>
                  </span>
                </div>
                {o.comentario ? <p>{o.comentario}</p> : <p className="sin-comentario">Calificó sin dejar comentario.</p>}
                <div className="top" style={{ margin: "6px 0 0" }}>
                  <span>{o.servicio} con {o.barbero.nombre} · {fechaLarga(o.fecha)}</span>
                  <span>{haceCuanto(o.fechaRespuesta)}</span>
                </div>
              </article>
            ))}
          </div>
          {opiniones.length > limite && (
            <div className="vacio"><button type="button" className="mini" onClick={() => setLimite((l) => l + 30)}>Ver más opiniones</button></div>
          )}
        </section>
      )}
    </>
  );
}

function TarjetaPromedio({ titulo, promedio, cantidad, distribucion, elegida, alElegir }) {
  const max = distribucion ? Math.max(1, ...Object.values(distribucion)) : 1;
  return (
    <button type="button" className="tarjeta-pelu tarjeta-opinion" aria-pressed={elegida} onClick={alElegir}>
      <span className="nombre">{titulo}</span>
      <span className="promedio">
        {promedio ? decimal(promedio) : "—"}<small> ★</small>
      </span>
      <span className="cantidad">{cantidad} {cantidad === 1 ? "encuesta" : "encuestas"}</span>
      {distribucion && (
        <span className="mini-dist" aria-hidden="true">
          {Object.entries(distribucion).sort(([a], [b]) => b - a).map(([n, c]) => (
            <span key={n}><i style={{ width: `${(c / max) * 100}%` }} /></span>
          ))}
        </span>
      )}
    </button>
  );
}

const total = (lista) => lista.reduce((a, b) => a + b.cantidad, 0);
function promedioGeneral(lista) {
  const n = total(lista);
  return n ? lista.reduce((a, b) => a + (b.promedio ?? 0) * b.cantidad, 0) / n : null;
}
