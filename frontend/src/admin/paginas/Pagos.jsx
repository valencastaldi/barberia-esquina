import { useState } from "react";
import { fechaLarga, pesos } from "../../lib/formato.js";
import { useSesion, usePedidoAdmin } from "../sesion.jsx";
import { hoyIso, sumarDias } from "../fechas.js";
import { AvisoError, Cabecera, ChipMedio, EstadoCarga, Kpi, Modal, NOMBRE_MEDIO, Segmentos } from "../componentes/ui.jsx";

const PERIODOS = [
  { valor: 0, texto: "Hoy" },
  { valor: 6, texto: "7 días" },
  { valor: 29, texto: "30 días" },
];
const COLOR_MEDIO = { efectivo: "var(--verde)", transferencia: "var(--azul-claro)", mercadopago: "var(--celeste)" };

/** [Extensión] Cobros, reparto por medio de pago y liquidación por peluquero. Solo dueño. */
export default function Pagos() {
  const { pedir } = useSesion();
  const [dias, setDias] = useState(0);
  const [limite, setLimite] = useState(30);
  const [cobrando, setCobrando] = useState(null);
  const [aviso, setAviso] = useState(null);

  const hoy = hoyIso();
  const reporte = usePedidoAdmin("/pagos", { desde: sumarDias(hoy, -dias), hasta: hoy });
  const r = reporte.datos;

  async function cobrar(cuerpo) {
    await pedir("/pagos", { metodo: "POST", cuerpo });
    setCobrando(null);
    reporte.recargar();
  }

  return (
    <>
      <Cabecera titulo="Pagos" extension bajada="Lo cobrado por cada turno completado, cómo se pagó y cuánto le toca a cada peluquero.">
        <Segmentos opciones={PERIODOS} valor={dias} alCambiar={(d) => { setDias(d); setLimite(30); }} etiqueta="Período" />
      </Cabecera>

      <AvisoError mensaje={aviso} alCerrar={() => setAviso(null)} />
      <EstadoCarga pedido={reporte} />

      {r && (
        <>
          <section className="kpis">
            <Kpi destacado etiqueta="Cobrado" valor={pesos(r.resumen.cobrado)} delta={`${r.resumen.cobros} cobros`} />
            <Kpi etiqueta="Ticket promedio" valor={r.resumen.cobros ? pesos(r.resumen.ticketPromedio) : "—"} delta="Por turno cobrado" />
            <Kpi etiqueta="Para la casa" valor={pesos(r.resumen.paraLaCasa)} delta="Después de comisiones" />
            <Kpi etiqueta="Sin cobrar" valor={pesos(r.resumen.sinCobrarMonto)}
                 tono={r.resumen.sinCobrarCantidad ? "falta" : "sube"}
                 delta={r.resumen.sinCobrarCantidad
                   ? `${r.resumen.sinCobrarCantidad} ${r.resumen.sinCobrarCantidad === 1 ? "turno" : "turnos"} por cobrar`
                   : "Todo cobrado"} />
          </section>

          <div className="columnas pares">
            <section className="bloque">
              <header><div><h2>Por medio de pago</h2></div></header>
              <div className="cuerpo">
                <div className="reparto">
                  {r.porMedio.map((m) => (
                    <div className="linea" key={m.medio}>
                      <ChipMedio medio={m.medio} />
                      <b>{pesos(m.monto)}<small>{m.porcentaje}%</small></b>
                      <div className="pista-barra"><i style={{ width: `${m.porcentaje}%`, background: COLOR_MEDIO[m.medio] }} /></div>
                    </div>
                  ))}
                </div>
              </div>
            </section>

            <section className="bloque">
              <header><div><h2>Liquidación</h2><p>Lo que le corresponde a cada peluquero según su comisión.</p></div></header>
              <table className="datos">
                <thead><tr><th>Peluquero</th><th>Facturó</th><th className="fin">Le toca</th></tr></thead>
                <tbody>
                  {r.liquidacion.map((l) => (
                    <tr key={l.idBarbero}>
                      <td><b>{l.nombre}</b><span className="sub">{l.rol === "dueno" ? "Dueño" : `Comisión ${l.comisionPct}%`}</span></td>
                      <td className="num">{pesos(l.facturado)}</td>
                      <td className="num fin">{l.rol === "dueno" ? "—" : <b>{pesos(l.leToca)}</b>}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </section>
          </div>

          <section className="bloque">
            <header>
              <div><h2>Movimientos</h2><p>Los turnos completados sin cobrar aparecen primero.</p></div>
              <span className="eyebrow">{r.movimientos.length} movimientos</span>
            </header>
            {r.movimientos.length === 0 ? (
              <div className="vacio">Todavía no hay turnos completados en este período.</div>
            ) : (
              <table className="datos">
                <thead>
                  <tr><th>Cuándo</th><th>Cliente</th><th>Servicio</th><th>Peluquero</th><th>Medio</th><th className="fin">Monto</th></tr>
                </thead>
                <tbody>
                  {r.movimientos.slice(0, limite).map((t) => (
                    <tr key={t.id}>
                      <td className="num">{fechaLarga(t.fecha)} · {t.horaInicio}</td>
                      <td><b>{t.cliente.nombre} {t.cliente.apellido}</b></td>
                      <td>{t.servicio.nombre}</td>
                      <td>{t.barbero.nombre}</td>
                      <td><ChipMedio medio={t.pago?.medio} /></td>
                      <td className="num fin">
                        {t.pago ? pesos(t.pago.monto)
                                : <button type="button" className="mini cobrar" onClick={() => setCobrando(t)}>Cobrar {pesos(t.precio)}</button>}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            )}
            {r.movimientos.length > limite && (
              <div className="vacio"><button type="button" className="mini" onClick={() => setLimite((l) => l + 30)}>Ver más movimientos</button></div>
            )}
          </section>
        </>
      )}

      <p className="nota-maqueta">
        Tabla nueva Pago (id_pago, id_turno, monto, medio, fecha), agregada al modelo de la documentación.
      </p>

      <ModalCobrar turno={cobrando} alCerrar={() => setCobrando(null)} alCobrar={cobrar} alError={setAviso} />
    </>
  );
}

function ModalCobrar({ turno, alCerrar, alCobrar, alError }) {
  const [medio, setMedio] = useState("efectivo");
  const [monto, setMonto] = useState("");
  const [error, setError] = useState(null);
  const [ultimo, setUltimo] = useState(null);

  // Al abrir con otro turno, el monto arranca en el precio.
  if (turno && turno.id !== ultimo) {
    setUltimo(turno.id);
    setMonto(String(Math.round(turno.precio)));
    setMedio("efectivo");
    setError(null);
  }

  async function confirmar(e) {
    e.preventDefault();
    try {
      await alCobrar({ idTurno: turno.id, monto: Number(monto), medio });
    } catch (err) {
      setError(err.message);
      alError?.(null);
    }
  }

  return (
    <Modal abierto={!!turno} alCerrar={alCerrar} titulo="Registrar cobro"
           bajada={turno && `${turno.cliente.nombre} ${turno.cliente.apellido} · ${turno.servicio.nombre} con ${turno.barbero.nombre} · ${fechaLarga(turno.fecha)} ${turno.horaInicio}`}>
      {turno && (
        <form onSubmit={confirmar}>
          <div className="campo">
            <span>¿Cómo pagó?</span>
            <div className="pastillas grandes" role="radiogroup">
              {Object.entries(NOMBRE_MEDIO).map(([valor, texto]) => (
                <label className="pastilla" key={valor}>
                  <input type="radio" name="medio-pago" checked={medio === valor} onChange={() => setMedio(valor)} />
                  <span>{texto}</span>
                </label>
              ))}
            </div>
          </div>
          <label className="campo"><span>Monto cobrado</span>
            <input type="number" min="0" step="500" required value={monto} onChange={(e) => setMonto(e.target.value)} />
          </label>
          <AvisoError mensaje={error} />
          <div className="modal-pie">
            <button type="button" className="btn btn-secundario" onClick={alCerrar}>Cancelar</button>
            <button type="submit" className="btn btn-primario">Registrar cobro</button>
          </div>
        </form>
      )}
    </Modal>
  );
}
