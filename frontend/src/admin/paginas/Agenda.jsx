import { useMemo, useState } from "react";
import { fechaLarga, pesos } from "../../lib/formato.js";
import { useSesion, usePedidoAdmin } from "../sesion.jsx";
import { hoyIso, moverPeriodo, porcentaje, rango, tituloPeriodo } from "../fechas.js";
import { AvisoError, Cabecera, ChipEstado, ChipMedio, EstadoCarga, Kpi, Modal, NOMBRE_MEDIO, Segmentos } from "../componentes/ui.jsx";

const PERIODOS = [
  { valor: "dia", texto: "Día" },
  { valor: "semana", texto: "Semana" },
  { valor: "mes", texto: "Mes" },
];

/** RF-09, RF-10, RF-11, RF-12, RF-14: agenda, cambio de estado, cobro y bloqueos. */
export default function Agenda() {
  const { pedir, usuario, esDueno } = useSesion();
  const [fecha, setFecha] = useState(hoyIso);
  const [periodo, setPeriodo] = useState("dia");
  const [idBarbero, setIdBarbero] = useState(null);
  const [aviso, setAviso] = useState(null);
  const [cobrando, setCobrando] = useState(null);     // turno a completar/cobrar
  const [cancelando, setCancelando] = useState(null);  // turno a cancelar
  const [bloqueando, setBloqueando] = useState(false);

  const [desde, hasta] = rango(fecha, periodo);
  const filtros = { desde, hasta, barbero: idBarbero };
  const turnos = usePedidoAdmin("/turnos", filtros);
  const bloqueos = usePedidoAdmin("/bloqueos", filtros);
  const equipo = usePedidoAdmin("/barberos");   // lista pública: nombres de los activos

  const recargar = () => { turnos.recargar(); bloqueos.recargar(); };

  /** Ejecuta una acción; si falla, muestra el motivo que da la API. */
  async function accion(fn) {
    setAviso(null);
    try {
      await fn();
    } catch (e) {
      setAviso(e.message);
    } finally {
      recargar();
    }
  }

  const cambiarEstado = (t, estado) =>
    pedir(`/turnos/${t.id}/estado`, { metodo: "PATCH", cuerpo: { estado } });

  const lista = turnos.datos ?? [];
  // El barbero ve la agenda de todos, pero los números son solo de sus turnos.
  const kpis = useMemo(() => {
    const suyos = esDueno ? lista : lista.filter((t) => t.barbero.id === usuario.id);
    const cuenta = (e) => suyos.filter((t) => t.estado === e).length;
    const atendibles = cuenta("completado") + cuenta("ausente");
    return {
      total: suyos.filter((t) => t.estado !== "cancelado").length,
      pendientes: cuenta("pendiente"),
      completados: cuenta("completado"),
      ausentes: cuenta("ausente"),
      ausentismo: atendibles ? cuenta("ausente") / atendibles : 0,
      cobrado: suyos.reduce((a, t) => a + (t.pago ? Number(t.pago.monto) : 0), 0),
      estimado: suyos.filter((t) => t.estado === "pendiente" || t.estado === "completado")
                     .reduce((a, t) => a + Number(t.precio), 0),
    };
  }, [lista, esDueno, usuario.id]);

  // Turnos y bloqueos juntos, agrupados por día y en orden de hora.
  const porDia = useMemo(() => {
    const filas = [
      ...lista.map((t) => ({ tipo: "turno", fecha: t.fecha, hora: t.horaInicio, t })),
      ...(bloqueos.datos ?? []).map((b) => ({ tipo: "bloqueo", fecha: b.fecha, hora: b.horaInicio, b })),
    ].sort((a, b) => (a.fecha + a.hora).localeCompare(b.fecha + b.hora));
    const grupos = new Map();
    filas.forEach((f) => grupos.set(f.fecha, [...(grupos.get(f.fecha) ?? []), f]));
    return [...grupos.entries()];
  }, [lista, bloqueos.datos]);

  const activos = equipo.datos ?? [];
  const esHoy = fecha === hoyIso() && periodo === "dia";

  return (
    <>
      <Cabecera titulo="Agenda" bajada={tituloPeriodo(fecha, periodo)}>
        <div className="navegador" role="group" aria-label="Cambiar fecha">
          <button type="button" className="mini" onClick={() => setFecha(moverPeriodo(fecha, periodo, -1))} aria-label="Anterior">‹</button>
          <button type="button" className="mini" onClick={() => setFecha(hoyIso())} disabled={esHoy}>Hoy</button>
          <button type="button" className="mini" onClick={() => setFecha(moverPeriodo(fecha, periodo, 1))} aria-label="Siguiente">›</button>
        </div>
        <Segmentos opciones={PERIODOS} valor={periodo} alCambiar={setPeriodo} etiqueta="Período" />
        <button type="button" className="btn btn-secundario" onClick={() => setBloqueando(true)}>Bloquear franja</button>
      </Cabecera>

      {activos.length > 1 && (
        <div className="filtro-equipo">
          <Segmentos chico etiqueta="Peluquero" valor={idBarbero} alCambiar={setIdBarbero}
                     opciones={[{ valor: null, texto: "Todo el equipo" }, ...activos.map((b) => ({ valor: b.id, texto: b.nombre }))]} />
        </div>
      )}

      <AvisoError mensaje={aviso} alCerrar={() => setAviso(null)} />

      <section className="kpis">
        <Kpi destacado etiqueta={`${esDueno ? "Turnos" : "Tus turnos"} ${periodo === "dia" ? "del día" : "del período"}`} valor={kpis.total}
             delta={`${kpis.pendientes} por atender`} />
        <Kpi etiqueta="Completados" valor={kpis.completados} delta="Encuesta enviada a cada uno" tono="sube" />
        <Kpi etiqueta="Ausentes" valor={kpis.ausentes} delta={`${porcentaje(kpis.ausentismo)} de los atendibles`} tono="baja" />
        <Kpi etiqueta={esDueno ? "Cobrado" : "Cobraste"} valor={pesos(kpis.cobrado)} delta={`De ${pesos(kpis.estimado)} estimados`} />
      </section>

      <section className="bloque">
        <header>
          <div>
            <h2>{periodo === "dia" ? "Turnos del día" : "Turnos"}</h2>
            <p>Marcar un turno como completado le envía al cliente el email con la encuesta.</p>
          </div>
          <span className="eyebrow">{lista.length} turnos</span>
        </header>
        <EstadoCarga pedido={turnos} texto="Cargando la agenda…" />
        {turnos.datos && !porDia.length && <div className="vacio">No hay turnos en este período.</div>}
        {porDia.map(([dia, filas]) => (
          <div key={dia}>
            {periodo !== "dia" && <h3 className="separador-dia">{fechaLarga(dia)}</h3>}
            {filas.map((f) => f.tipo === "bloqueo"
              ? <FilaBloqueo key={`b${f.b.id}`} b={f.b} puedeQuitar={esDueno || f.b.idBarbero === usuario.id}
                             alQuitar={() => accion(() => pedir(`/bloqueos/${f.b.id}`, { metodo: "DELETE" }))} />
              : <FilaTurno key={f.t.id} t={f.t} puedeTocar={esDueno || f.t.barbero.id === usuario.id}
                           alCompletar={() => setCobrando({ turno: f.t, completar: true })}
                           alCobrar={() => setCobrando({ turno: f.t, completar: false })}
                           alAusente={() => accion(() => cambiarEstado(f.t, "ausente"))}
                           alCancelar={() => setCancelando(f.t)} />)}
          </div>
        ))}
      </section>

      <ModalCobro datos={cobrando} alCerrar={() => setCobrando(null)}
                  alConfirmar={(medio) => accion(async () => {
                    const { turno, completar } = cobrando;
                    setCobrando(null);
                    if (completar) await cambiarEstado(turno, "completado");
                    if (medio) await pedir("/pagos", { metodo: "POST", cuerpo: { idTurno: turno.id, monto: turno.precio, medio } });
                  })} />

      <Modal abierto={!!cancelando} alCerrar={() => setCancelando(null)} titulo="¿Cancelar este turno?"
             bajada="Al cliente le llega un email avisándole. El horario vuelve a quedar libre.">
        {cancelando && (
          <>
            <div className="cobro-resumen">
              <b>{cancelando.cliente.nombre} {cancelando.cliente.apellido}</b> · {cancelando.servicio.nombre} ·{" "}
              {fechaLarga(cancelando.fecha)} {cancelando.horaInicio}
            </div>
            <div className="modal-pie">
              <button type="button" className="btn btn-secundario" onClick={() => setCancelando(null)}>Volver</button>
              <button type="button" className="btn btn-peligro" onClick={() => {
                const t = cancelando;
                setCancelando(null);
                accion(() => cambiarEstado(t, "cancelado"));
              }}>Sí, cancelar</button>
            </div>
          </>
        )}
      </Modal>

      <ModalBloqueo abierto={bloqueando} alCerrar={() => setBloqueando(false)} fechaInicial={fecha}
                    equipo={esDueno ? activos : activos.filter((b) => b.id === usuario.id)}
                    idPropio={usuario.id} alGuardar={async (cuerpo) => {
                      await pedir("/bloqueos", { metodo: "POST", cuerpo });
                      setBloqueando(false);
                      recargar();
                    }} />
    </>
  );
}

function FilaTurno({ t, puedeTocar, alCompletar, alCobrar, alAusente, alCancelar }) {
  const cerrado = t.estado !== "pendiente";
  let acciones;
  if (!cerrado && puedeTocar) {
    acciones = (
      <div className="acciones-turno">
        <button type="button" className="mini ok" onClick={alCompletar}>Completado</button>
        <button type="button" className="mini" onClick={alAusente}>Ausente</button>
        <button type="button" className="mini no" onClick={alCancelar}>Cancelar</button>
      </div>
    );
  } else if (t.estado === "completado" && puedeTocar) {
    acciones = (
      <>
        {t.pago ? <ChipMedio medio={t.pago.medio} />
                : puedeTocar ? <button type="button" className="mini cobrar" onClick={alCobrar}>Registrar cobro</button>
                             : <ChipMedio medio={null} />}
        <ChipEstado estado="completado" />
      </>
    );
  } else {
    acciones = <ChipEstado estado={t.estado} />;
  }

  return (
    <article className={`turno${cerrado ? " cerrado" : ""}`}>
      <div className="hora">{t.horaInicio}<small>a {t.horaFin}</small></div>
      <div className="quien">
        <b>{t.cliente.nombre} {t.cliente.apellido}</b>
        <p>
          {t.servicio.nombre} · con {t.barbero.nombre}{t.cliente.telefono ? ` · ${t.cliente.telefono}` : ""}
          {t.calificacion && <span className="calif"> · {"★".repeat(t.calificacion)}</span>}
        </p>
      </div>
      <div className="derecha">
        {puedeTocar && <span className="monto">{pesos(t.precio)}</span>}
        {acciones}
      </div>
    </article>
  );
}

function FilaBloqueo({ b, puedeQuitar, alQuitar }) {
  return (
    <article className="turno bloqueo">
      <div className="hora">{b.horaInicio}<small>a {b.horaFin}</small></div>
      <div className="quien">
        <b>Franja bloqueada</b>
        <p>{b.motivo ? `${b.motivo} · ` : ""}{b.barbero}</p>
      </div>
      <div className="derecha">
        {puedeQuitar && <button type="button" className="mini" onClick={alQuitar}>Quitar bloqueo</button>}
      </div>
    </article>
  );
}

/** Al completar: cómo pagó (o cobrar después). También sirve para cobrar un completado pendiente. */
function ModalCobro({ datos, alCerrar, alConfirmar }) {
  const [medio, setMedio] = useState("efectivo");
  const t = datos?.turno;
  return (
    <Modal abierto={!!datos} alCerrar={alCerrar}
           titulo={datos?.completar ? "Turno completado" : "Registrar cobro"}
           bajada={datos?.completar ? "Se le envía la encuesta de satisfacción por email." : "El turno ya está completado; falta registrar el pago."}>
      {t && (
        <>
          <div className="cobro-resumen">
            <b>{t.cliente.nombre} {t.cliente.apellido}</b> · {t.servicio.nombre} con {t.barbero.nombre} · <b>{pesos(t.precio)}</b>
          </div>
          <div className="campo">
            <span>¿Cómo pagó?</span>
            <div className="pastillas grandes" role="radiogroup">
              {Object.entries(NOMBRE_MEDIO).map(([valor, texto]) => (
                <label className="pastilla" key={valor}>
                  <input type="radio" name="medio" value={valor} checked={medio === valor} onChange={() => setMedio(valor)} />
                  <span>{texto}</span>
                </label>
              ))}
            </div>
          </div>
          <div className="modal-pie">
            {datos.completar
              ? <button type="button" className="btn btn-secundario" onClick={() => alConfirmar(null)}>Cobrar después</button>
              : <button type="button" className="btn btn-secundario" onClick={alCerrar}>Cancelar</button>}
            <button type="button" className="btn btn-primario" onClick={() => alConfirmar(medio)}>Registrar cobro</button>
          </div>
        </>
      )}
    </Modal>
  );
}

/** RF-12: bloquear una franja (trámite, almuerzo…). */
function ModalBloqueo({ abierto, alCerrar, fechaInicial, equipo, idPropio, alGuardar }) {
  const [form, setForm] = useState(null);
  const [error, setError] = useState(null);
  const [guardando, setGuardando] = useState(false);

  // Cada vez que se abre, arranca con la fecha que se está mirando.
  if (abierto && !form) {
    setForm({ idBarbero: equipo.some((b) => b.id === idPropio) ? idPropio : equipo[0]?.id, fecha: fechaInicial,
              horaInicio: "13:00", horaFin: "14:00", motivo: "" });
  }
  if (!abierto && form) setForm(null);

  const cambiar = (campo) => (e) => setForm((f) => ({ ...f, [campo]: e.target.value }));

  async function guardar(e) {
    e.preventDefault();
    setGuardando(true);
    setError(null);
    try {
      await alGuardar({ ...form, idBarbero: Number(form.idBarbero), motivo: form.motivo.trim() || null });
    } catch (err) {
      setError(err.message);
    } finally {
      setGuardando(false);
    }
  }

  return (
    <Modal abierto={abierto} alCerrar={alCerrar} titulo="Bloquear franja"
           bajada="En ese rango no se ofrecen turnos. Si ya hay turnos reservados, primero hay que cancelarlos.">
      {form && (
        <form onSubmit={guardar}>
          <div className="fila-campos">
            <label className="campo"><span>Peluquero</span>
              <select value={form.idBarbero ?? ""} onChange={cambiar("idBarbero")} disabled={equipo.length < 2}>
                {equipo.map((b) => <option key={b.id} value={b.id}>{b.nombre} {b.apellido}</option>)}
              </select>
            </label>
            <label className="campo"><span>Día</span>
              <input type="date" value={form.fecha} onChange={cambiar("fecha")} required />
            </label>
          </div>
          <div className="fila-campos">
            <label className="campo"><span>Desde</span><input type="time" step="900" value={form.horaInicio} onChange={cambiar("horaInicio")} required /></label>
            <label className="campo"><span>Hasta</span><input type="time" step="900" value={form.horaFin} onChange={cambiar("horaFin")} required /></label>
          </div>
          <label className="campo"><span>Motivo (opcional)</span>
            <input value={form.motivo} onChange={cambiar("motivo")} maxLength={120} placeholder="Trámite, almuerzo…" />
          </label>
          <AvisoError mensaje={error} />
          <div className="modal-pie">
            <button type="button" className="btn btn-secundario" onClick={alCerrar}>Cancelar</button>
            <button type="submit" className="btn btn-primario" disabled={guardando}>{guardando ? "Guardando…" : "Bloquear"}</button>
          </div>
        </form>
      )}
    </Modal>
  );
}
