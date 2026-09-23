import { useRef, useState } from "react";
import { DIAS_CORTOS, MESES, aFecha, decimal, pesos } from "../../lib/formato.js";
import { usePedidoAdmin } from "../sesion.jsx";
import { haceCuanto, hoyIso, porcentaje, sumarDias } from "../fechas.js";
import { Cabecera, EstadoCarga, Kpi, Segmentos } from "../componentes/ui.jsx";

const PERIODOS = [
  { valor: 7, texto: "7 días" },
  { valor: 30, texto: "30 días" },
  { valor: 90, texto: "90 días" },
];

/** RF-18 a RF-21: métricas del negocio. */
export default function Dashboard() {
  const [dias, setDias] = useState(30);
  const hasta = hoyIso();
  const rango = { desde: sumarDias(hasta, -(dias - 1)), hasta };

  const resumen = usePedidoAdmin("/dashboard/resumen", rango);
  const evolucion = usePedidoAdmin("/dashboard/evolucion", rango);
  const equipo = usePedidoAdmin("/dashboard/barberos", rango);
  const r = resumen.datos;

  return (
    <>
      <Cabecera titulo="Dashboard" bajada="Rendimiento y satisfacción del servicio.">
        <Segmentos opciones={PERIODOS} valor={dias} alCambiar={setDias} etiqueta="Período" />
      </Cabecera>

      <EstadoCarga pedido={resumen} />
      {r && (
        <section className="kpis">
          <Kpi destacado etiqueta="Turnos completados" valor={r.completados}
               delta={`${r.turnos} reservados · ${r.cancelados} cancelados`} />
          <Kpi etiqueta="Satisfacción promedio"
               valor={r.satisfaccion ? <>{decimal(r.satisfaccion)}<small> / 5</small></> : "—"}
               delta={`${r.encuestas} encuestas respondidas`} />
          <Kpi etiqueta="Tasa de ausentismo" valor={porcentaje(r.tasaAusentismo)} tono="baja"
               delta={`${r.ausentes} turnos marcados ausente`} />
          <Kpi etiqueta="Facturación" valor={pesos(r.facturacion)} delta="Sobre turnos completados" />
        </section>
      )}

      <div className="columnas">
        <section className="bloque">
          <header>
            <div><h2>Turnos completados por día</h2><p>Últimos {dias} días</p></div>
          </header>
          <div className="cuerpo envoltorio-grafico">
            <EstadoCarga pedido={evolucion} />
            {evolucion.datos && <GraficoBarras datos={evolucion.datos} />}
          </div>
        </section>

        <div>
          {r && (
            <section className="bloque">
              <header>
                <div><h2>Satisfacción</h2><p>Distribución de las {r.encuestas} encuestas</p></div>
                {r.satisfaccion && <span className="nota-grande">{decimal(r.satisfaccion)}</span>}
              </header>
              <div className="cuerpo">
                <div className="estrellas-dist">
                  {Object.entries(r.distribucionEstrellas).sort(([a], [b]) => b - a).map(([n, cant]) => (
                    <div className="linea" key={n}>
                      <span>{n} ★</span>
                      <span className="pista-barra">
                        <i style={{ width: `${r.encuestas ? (cant / Math.max(...Object.values(r.distribucionEstrellas))) * 100 : 0}%` }} />
                      </span>
                      <span className="cant">{cant}</span>
                    </div>
                  ))}
                </div>
              </div>
            </section>
          )}

          {r && (
            <section className="bloque">
              <header><div><h2>Últimos comentarios</h2></div></header>
              <div className="cuerpo">
                {r.ultimosComentarios.length === 0 && <p className="nota-maqueta">Todavía no hay comentarios en este período.</p>}
                {r.ultimosComentarios.map((c) => (
                  <div className="comentario" key={c.fecha + c.cliente}>
                    <div className="top"><b>{c.cliente}</b><span className="puntaje">{"★".repeat(c.calificacion)}</span></div>
                    <p>{c.comentario}</p>
                    <div className="top" style={{ margin: "6px 0 0" }}><span>{haceCuanto(c.fecha)}</span></div>
                  </div>
                ))}
              </div>
            </section>
          )}
        </div>
      </div>

      {equipo.datos && equipo.datos.length > 1 && (
        <section className="bloque">
          <header><div><h2>Por peluquero</h2><p>Últimos {dias} días</p></div><span className="tag-ext">Extensión</span></header>
          <table className="datos">
            <thead>
              <tr><th>Peluquero</th><th>Completados</th><th>Ausentes</th><th>Facturado</th><th className="fin">Satisfacción</th></tr>
            </thead>
            <tbody>
              {equipo.datos.map((b) => (
                <tr key={b.idBarbero}>
                  <td><b>{b.nombre}</b></td>
                  <td className="num">{b.completados}</td>
                  <td className="num">{b.ausentes}</td>
                  <td className="num">{pesos(b.facturado)}</td>
                  <td className="num fin">{b.satisfaccion ? <span className="texto-ambar">{decimal(b.satisfaccion)} ★</span> : "—"}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </section>
      )}
    </>
  );
}

/* Barra con esquinas superiores redondeadas, apoyada en la línea base. */
function barra(x, y, ancho, alto, r) {
  r = Math.min(r, ancho / 2, alto);
  return `M${x},${y + alto}V${y + r}a${r},${r} 0 0 1 ${r},${-r}h${ancho - 2 * r}a${r},${r} 0 0 1 ${r},${r}V${y + alto}Z`;
}

/** Una sola serie: grilla recesiva, barras con 2px de aire, fechas salteadas y tooltip. */
function GraficoBarras({ datos }) {
  const [tip, setTip] = useState(null);
  const caja = useRef(null);
  const W = 640, H = 232;
  const m = { top: 16, right: 6, bottom: 30, left: 30 };
  const anchoPlot = W - m.left - m.right;
  const altoPlot = H - m.top - m.bottom;
  const max = Math.max(0, ...datos.map((d) => d.completados));
  const tope = Math.ceil(max / 3) * 3 || 3;
  const paso = anchoPlot / datos.length;
  const anchoBarra = Math.max(2, paso - 2);
  const cada = datos.length > 45 ? 10 : datos.length > 14 ? 5 : 1;
  const y = (v) => m.top + altoPlot - (v / tope) * altoPlot;

  function mostrar(e, d) {
    const b = e.currentTarget.getBoundingClientRect();
    const padre = caja.current.getBoundingClientRect();
    setTip({ d, left: b.left - padre.left + b.width / 2, top: b.top - padre.top - 8 });
  }

  return (
    <div ref={caja} style={{ position: "relative" }}>
      <svg className="grafico" viewBox={`0 0 ${W} ${H}`} role="img"
           aria-label={`Turnos completados por día: ${datos.reduce((a, d) => a + d.completados, 0)} en total`}>
        {[0, tope / 2, tope].map((v) => (
          <g key={v}>
            <line className="eje" x1={m.left} x2={W - m.right} y1={y(v)} y2={y(v)} />
            <text x={m.left - 8} y={y(v) + 4} textAnchor="end">{v}</text>
          </g>
        ))}
        {datos.map((d, i) => d.completados > 0 && (
          <path key={d.fecha} className="barra" d={barra(m.left + i * paso + (paso - anchoBarra) / 2, y(d.completados), anchoBarra, (d.completados / tope) * altoPlot, 4)}
                onMouseEnter={(e) => mostrar(e, d)} onMouseLeave={() => setTip(null)} />
        ))}
        {datos.map((d, i) => (i % cada === 0 || i === datos.length - 1) && (
          <text key={`t${d.fecha}`} x={m.left + i * paso + paso / 2} y={H - 10} textAnchor="middle">
            {aFecha(d.fecha).getDate()} {MESES[aFecha(d.fecha).getMonth()].slice(0, 3)}
          </text>
        ))}
      </svg>
      <div className="tip" style={tip ? { opacity: 1, left: tip.left, top: tip.top } : undefined}>
        {tip && (
          <>
            <b>{tip.d.completados} completados</b>
            <span>{DIAS_CORTOS[aFecha(tip.d.fecha).getDay()]} {aFecha(tip.d.fecha).getDate()} · {tip.d.turnos} reservados</span>
          </>
        )}
      </div>
    </div>
  );
}
