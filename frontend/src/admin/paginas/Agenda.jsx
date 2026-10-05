import { useEffect, useMemo, useState } from "react";
import { ErrorApi } from "../../api/api.js";
import { DIAS_CORTOS, aFecha, fechaLarga, pesos, sumarMinutos } from "../../lib/formato.js";
import { useSesion, usePedidoAdmin } from "../sesion.jsx";
import { hoyIso, moverPeriodo, porcentaje, rango, sumarDias, tituloPeriodo } from "../fechas.js";
import { AvisoError, Cabecera, ChipEstado, ChipMedio, EstadoCarga, Kpi, Modal, NOMBRE_MEDIO, Segmentos } from "../componentes/ui.jsx";
import { Cronograma } from "../componentes/Cronograma.jsx";

const VISTAS = [
  { valor: "lista", texto: "Lista" },
  { valor: "cronograma", texto: "Cronograma" },
];

/** La vista elegida se recuerda en este navegador (si se puede). */
const CLAVE_VISTA = "esquina.agenda.vista";
function vistaGuardada() {
  try {
    return localStorage.getItem(CLAVE_VISTA) === "cronograma" ? "cronograma" : "lista";
  } catch {
    return "lista";
  }
}

const PERIODOS = [
  { valor: "dia", texto: "Día" },
  { valor: "semana", texto: "Semana" },
  { valor: "mes", texto: "Mes" },
  { valor: "proximos", texto: "Próximos" },
];

/** RF-09, RF-10, RF-11, RF-12, RF-14: agenda, cambio de estado, cobro y bloqueos. */
export default function Agenda() {
  const { pedir, usuario, esDueno } = useSesion();
  const [fecha, setFecha] = useState(hoyIso);
  const [idBarbero, setIdBarbero] = useState(null);
  const [aviso, setAviso] = useState(null);
  const [cobrando, setCobrando] = useState(null);     // turno a completar/cobrar
  const [cancelando, setCancelando] = useState(null);  // turno a cancelar
  const [editando, setEditando] = useState(null);      // turno completado a corregir
  const [borrando, setBorrando] = useState(null);      // turno completado a borrar
  const [bloqueando, setBloqueando] = useState(false);
  const [cargandoTurno, setCargandoTurno] = useState(false);
  const [detalle, setDetalle] = useState(null);        // turno abierto desde el cronograma
  const [vista, setVista] = useState(vistaGuardada);
  // El cronograma es de un día: con esa vista el período siempre es "dia".
  const [periodoLista, setPeriodo] = useState("dia");
  const periodo = vista === "cronograma" ? "dia" : periodoLista;

  function cambiarVista(v) {
    setVista(v);
    try { localStorage.setItem(CLAVE_VISTA, v); } catch { /* sin almacenamiento: no se recuerda */ }
  }

  const proximos = periodo === "proximos";
  const [desde, hasta] = rango(fecha, periodo);
  const filtros = { desde, hasta, barbero: idBarbero };
  // "Próximos": solo lo que falta atender (los reservados de hoy en adelante).
  const turnos = usePedidoAdmin("/turnos", { ...filtros, estado: proximos ? "pendiente" : undefined });
  const bloqueos = usePedidoAdmin("/bloqueos", filtros);
  // Tira de días del cronograma: cuántos turnos hay en cada uno de los próximos 14 días.
  const tira = usePedidoAdmin(vista === "cronograma" ? "/turnos" : null,
                              { desde: hoyIso(), hasta: sumarDias(hoyIso(), 13), barbero: idBarbero });
  const equipo = usePedidoAdmin("/barberos");   // lista pública: nombres de los activos
  const horarios = usePedidoAdmin("/horarios"); // horario de la barbería: rango de horas del cronograma

  const recargar = () => { turnos.recargar(); bloqueos.recargar(); tira.recargar(); };

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
      // Para "Próximos"
      hoy: suyos.filter((t) => t.fecha === hoyIso()).length,
      semana: suyos.filter((t) => t.fecha <= sumarDias(hoyIso(), 6)).length,
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
  const puedeTocar = (t) => esDueno || t.barbero.id === usuario.id;
  const horarioDelDia = horarios.datos?.find((d) => d.diaSemana === aFecha(fecha).getDay());

  return (
    <>
      <Cabecera titulo="Agenda" bajada={tituloPeriodo(fecha, periodo)}>
        {!proximos && (
          <div className="navegador" role="group" aria-label="Cambiar fecha">
            <button type="button" className="mini" onClick={() => setFecha(moverPeriodo(fecha, periodo, -1))} aria-label="Anterior">‹</button>
            <button type="button" className="mini" onClick={() => setFecha(hoyIso())} disabled={esHoy}>Hoy</button>
            <button type="button" className="mini" onClick={() => setFecha(moverPeriodo(fecha, periodo, 1))} aria-label="Siguiente">›</button>
          </div>
        )}
        {vista === "lista" && <Segmentos opciones={PERIODOS} valor={periodo} alCambiar={setPeriodo} etiqueta="Período" />}
        <Segmentos opciones={VISTAS} valor={vista} alCambiar={cambiarVista} etiqueta="Vista" />
        {esDueno && <button type="button" className="btn btn-secundario" onClick={() => setBloqueando(true)}>Bloquear franja</button>}
        <button type="button" className="btn btn-primario" onClick={() => setCargandoTurno(true)}>Nuevo turno</button>
      </Cabecera>

      {activos.length > 1 && (
        <div className="filtro-equipo">
          <Segmentos chico etiqueta="Peluquero" valor={idBarbero} alCambiar={setIdBarbero}
                     opciones={[{ valor: null, texto: "Todo el equipo" }, ...activos.map((b) => ({ valor: b.id, texto: b.nombre }))]} />
        </div>
      )}

      <AvisoError mensaje={aviso} alCerrar={() => setAviso(null)} />

      {proximos ? (
        <section className="kpis">
          <Kpi destacado etiqueta={esDueno ? "Turnos reservados" : "Tus turnos reservados"} valor={kpis.total}
               delta={`Hasta el ${fechaLarga(hasta)}`} />
          <Kpi etiqueta="Hoy" valor={kpis.hoy} delta="Pendientes de hoy" />
          <Kpi etiqueta="Próximos 7 días" valor={kpis.semana} delta="Incluye hoy" />
          <Kpi etiqueta="Facturación estimada" valor={pesos(kpis.estimado)} delta="Si vienen todos" />
        </section>
      ) : (
      <section className="kpis">
        <Kpi destacado etiqueta={`${esDueno ? "Turnos" : "Tus turnos"} ${periodo === "dia" ? "del día" : "del período"}`} valor={kpis.total}
             delta={`${kpis.pendientes} por atender`} />
        <Kpi etiqueta="Completados" valor={kpis.completados} delta="Encuesta enviada a cada uno" tono="sube" />
        <Kpi etiqueta="Ausentes" valor={kpis.ausentes} delta={`${porcentaje(kpis.ausentismo)} de los atendibles`} tono="baja" />
        <Kpi etiqueta={esDueno ? "Cobrado" : "Cobraste"} valor={pesos(kpis.cobrado)} delta={`De ${pesos(kpis.estimado)} estimados`} />
      </section>
      )}

      {vista === "cronograma" && (
        <section className="bloque">
          <header>
            <div>
              <h2>Cronograma del día</h2>
              <p>Tocá un turno para marcarlo, cobrarlo, cancelarlo o corregirlo.</p>
            </div>
            <Leyenda />
          </header>
          <TiraDias turnos={tira.datos} fecha={fecha} alElegir={setFecha} />
          <EstadoCarga pedido={turnos} texto="Cargando la agenda…" />
          {turnos.datos && (
            <Cronograma turnos={lista} bloqueos={bloqueos.datos ?? []} horarioDelDia={horarioDelDia} esHoy={esHoy}
                        barberos={idBarbero ? activos.filter((b) => b.id === idBarbero) : activos}
                        puedeTocar={puedeTocar} esDueno={esDueno} alElegirTurno={setDetalle}
                        alQuitarBloqueo={(b) => accion(() => pedir(`/bloqueos/${b.id}`, { metodo: "DELETE" }))} />
          )}
        </section>
      )}

      {vista === "lista" && (
      <section className="bloque">
        <header>
          <div>
            <h2>{periodo === "dia" ? "Turnos del día" : proximos ? "Próximos turnos" : "Turnos"}</h2>
            <p>{proximos
              ? "Todo lo que falta atender, de hoy en adelante, agrupado por día."
              : "Marcar un turno como completado le envía al cliente el email con la encuesta."}</p>
          </div>
          <span className="eyebrow">{lista.length} turnos</span>
        </header>
        <EstadoCarga pedido={turnos} texto="Cargando la agenda…" />
        {turnos.datos && !porDia.length && <div className="vacio">No hay turnos en este período.</div>}
        {porDia.map(([dia, filas]) => (
          <div key={dia}>
            {periodo !== "dia" && (
              <h3 className="separador-dia">
                {dia === hoyIso() ? "Hoy · " : dia === sumarDias(hoyIso(), 1) ? "Mañana · " : ""}{fechaLarga(dia)}
                <span>{filas.filter((f) => f.tipo === "turno").length} turnos</span>
              </h3>
            )}
            {filas.map((f) => f.tipo === "bloqueo"
              ? <FilaBloqueo key={`b${f.b.id}`} b={f.b} puedeQuitar={esDueno}
                             alQuitar={() => accion(() => pedir(`/bloqueos/${f.b.id}`, { metodo: "DELETE" }))} />
              : <FilaTurno key={f.t.id} t={f.t} puedeTocar={esDueno || f.t.barbero.id === usuario.id}
                           alCompletar={() => setCobrando({ turno: f.t, completar: true })}
                           alCobrar={() => setCobrando({ turno: f.t, completar: false })}
                           alAusente={() => accion(() => cambiarEstado(f.t, "ausente"))}
                           alCancelar={() => setCancelando(f.t)}
                           alEditar={() => setEditando(f.t)}
                           alBorrar={() => setBorrando(f.t)} />)}
          </div>
        ))}
      </section>
      )}

      <ModalDetalle turno={detalle} puedeTocar={detalle ? puedeTocar(detalle) : false} alCerrar={() => setDetalle(null)}
                    alCompletar={(t) => { setDetalle(null); setCobrando({ turno: t, completar: true }); }}
                    alCobrar={(t) => { setDetalle(null); setCobrando({ turno: t, completar: false }); }}
                    alAusente={(t) => { setDetalle(null); accion(() => cambiarEstado(t, "ausente")); }}
                    alCancelar={(t) => { setDetalle(null); setCancelando(t); }}
                    alEditar={(t) => { setDetalle(null); setEditando(t); }}
                    alBorrar={(t) => { setDetalle(null); setBorrando(t); }} />

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

      <ModalEditar turno={editando} equipo={activos} esDueno={esDueno} alCerrar={() => setEditando(null)}
                   alGuardar={async (cuerpo) => {
                     await pedir(`/turnos/${editando.id}`, { metodo: "PUT", cuerpo });
                     setEditando(null);
                     recargar();
                   }} />

      <Modal abierto={!!borrando} alCerrar={() => setBorrando(null)} titulo="¿Borrar este turno?"
             bajada="Es para turnos cargados por error: se borra junto con su cobro y deja de contar en Pagos. No se puede deshacer.">
        {borrando && (
          <>
            <div className="cobro-resumen">
              <b>{borrando.cliente.nombre} {borrando.cliente.apellido}</b> · {borrando.servicio.nombre} con {borrando.barbero.nombre} ·{" "}
              {fechaLarga(borrando.fecha)} {borrando.horaInicio}
              {borrando.pago && <> · cobrado <b>{pesos(borrando.pago.monto)}</b> en {NOMBRE_MEDIO[borrando.pago.medio]}</>}
            </div>
            <div className="modal-pie">
              <button type="button" className="btn btn-secundario" onClick={() => setBorrando(null)}>Volver</button>
              <button type="button" className="btn btn-peligro" onClick={() => {
                const t = borrando;
                setBorrando(null);
                accion(() => pedir(`/turnos/${t.id}`, { metodo: "DELETE" }));
              }}>Sí, borrar</button>
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

      <ModalTurno abierto={cargandoTurno} alCerrar={() => setCargandoTurno(false)} fechaInicial={fecha}
                  equipo={activos} esDueno={esDueno} idPropio={usuario.id} alGuardar={async (cuerpo) => {
                    await pedir("/turnos/panel", { metodo: "POST", cuerpo });
                    setCargandoTurno(false);
                    recargar();
                  }} />
    </>
  );
}

function FilaTurno({ t, puedeTocar, alCompletar, alCobrar, alAusente, alCancelar, alEditar, alBorrar }) {
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
                : <button type="button" className="mini cobrar" onClick={alCobrar}>Registrar cobro</button>}
        <ChipEstado estado="completado" />
        <div className="acciones-turno">
          <button type="button" className="mini" onClick={alEditar}>Editar</button>
          <button type="button" className="mini no" onClick={alBorrar}>Borrar</button>
        </div>
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

/**
 * Los próximos 14 días con cuántos turnos tiene cada uno: para ver de un vistazo
 * lo que viene y saltar a ese día en el cronograma.
 */
function TiraDias({ turnos, fecha, alElegir }) {
  const dias = Array.from({ length: 14 }, (_, i) => sumarDias(hoyIso(), i));
  const cuenta = (d) => (turnos ?? []).filter((t) => t.fecha === d && t.estado !== "cancelado").length;
  return (
    <div className="tira-dias" role="group" aria-label="Próximos días">
      {dias.map((d, i) => {
        const f = aFecha(d);
        const n = cuenta(d);
        return (
          <button type="button" key={d} className="dia-tira" aria-pressed={d === fecha} onClick={() => alElegir(d)}
                  aria-label={`${fechaLarga(d)}: ${n} ${n === 1 ? "turno" : "turnos"}`}>
            <small>{i === 0 ? "Hoy" : DIAS_CORTOS[f.getDay()]}</small>
            <b>{f.getDate()}</b>
            <i className={n ? "" : "vacio-dia"}>{turnos ? `${n} ${n === 1 ? "turno" : "turnos"}` : "…"}</i>
          </button>
        );
      })}
    </div>
  );
}

/** Qué significa cada color del cronograma. */
function Leyenda() {
  return (
    <ul className="crono-leyenda" aria-label="Referencias">
      <li><i className="estado-pendiente" />Pendiente</li>
      <li><i className="estado-completado" />Completado</li>
      <li><i className="sin-cobrar" />Sin cobrar</li>
      <li><i className="estado-ausente" />Ausente</li>
      <li><i className="bloqueado" />Bloqueado</li>
    </ul>
  );
}

/** Detalle de un turno tocado en el cronograma, con las mismas acciones que la lista. */
function ModalDetalle({ turno: t, puedeTocar, alCerrar, alCompletar, alCobrar, alAusente, alCancelar, alEditar, alBorrar }) {
  const pendiente = t?.estado === "pendiente";
  const completado = t?.estado === "completado";
  const faltaCobrar = t?.estado === "completado" && !t?.pago;
  return (
    <Modal abierto={!!t} alCerrar={alCerrar} titulo={t ? `${t.cliente.nombre} ${t.cliente.apellido}` : ""}
           bajada={t ? `${fechaLarga(t.fecha)} · ${t.horaInicio} a ${t.horaFin}` : ""}>
      {t && (
        <>
          <div className="detalle-turno">
            <div><span>Servicio</span><b>{t.servicio.nombre}</b></div>
            <div><span>Peluquero</span><b>{t.barbero.nombre}</b></div>
            {puedeTocar && <div><span>Precio</span><b>{pesos(t.precio)}</b></div>}
            <div><span>Estado</span><ChipEstado estado={t.estado} /></div>
            {t.estado === "completado" && puedeTocar && (
              <div>
                <span>Cobro</span>
                {t.pago ? <b>{pesos(t.pago.monto)} · {NOMBRE_MEDIO[t.pago.medio]}</b> : <ChipMedio medio={null} />}
              </div>
            )}
            {t.cliente.telefono && (
              <div><span>Contacto</span><b>{t.cliente.telefono}{t.cliente.email ? ` · ${t.cliente.email}` : ""}</b></div>
            )}
            {t.calificacion && <div><span>Encuesta</span><b className="texto-ambar">{"★".repeat(t.calificacion)}</b></div>}
          </div>

          <div className="modal-pie">
            {!puedeTocar && <p className="texto-ayuda detalle-ajeno">Es un turno de otro peluquero.</p>}
            {puedeTocar && pendiente && (
              <>
                <button type="button" className="btn btn-peligro" onClick={() => alCancelar(t)}>Cancelar turno</button>
                <button type="button" className="btn btn-secundario" onClick={() => alAusente(t)}>Ausente</button>
                <button type="button" className="btn btn-primario" onClick={() => alCompletar(t)}>Completado</button>
              </>
            )}
            {puedeTocar && completado && (
              <>
                <button type="button" className="btn btn-peligro" onClick={() => alBorrar(t)}>Borrar</button>
                <button type="button" className="btn btn-secundario" onClick={() => alEditar(t)}>Editar</button>
              </>
            )}
            {puedeTocar && faltaCobrar && (
              <button type="button" className="btn btn-primario" onClick={() => alCobrar(t)}>Registrar cobro</button>
            )}
            {!(puedeTocar && (pendiente || completado)) && (
              <button type="button" className="btn btn-secundario" onClick={alCerrar}>Cerrar</button>
            )}
          </div>
        </>
      )}
    </Modal>
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

/**
 * Extensión: corregir un turno completado que se cargó mal. Se puede cambiar el servicio,
 * el importe y, si ya se cobró, el medio de pago. El peluquero solo lo cambia el dueño.
 */
function ModalEditar({ turno: t, equipo, esDueno, alCerrar, alGuardar }) {
  const [form, setForm] = useState(null);
  const [error, setError] = useState(null);
  const [guardando, setGuardando] = useState(false);

  // Cada vez que se abre, arranca con los datos del turno.
  if (t && form?.id !== t.id) {
    setForm({ id: t.id, idServicio: String(t.servicio.id), idBarbero: String(t.barbero.id),
              precio: String(Number(t.precio)), medio: t.pago?.medio ?? null });
    setError(null);
  }
  if (!t && form) setForm(null);

  const servicios = usePedidoAdmin(t ? "/servicios" : null);
  const opciones = servicios.datos ?? [];
  // El servicio o el peluquero del turno pueden estar dados de baja: igual tienen que aparecer.
  const conElServicio = t && !opciones.some((s) => s.id === t.servicio.id)
    ? [{ id: t.servicio.id, nombre: t.servicio.nombre }, ...opciones] : opciones;
  const conElPeluquero = t && !equipo.some((b) => b.id === t.barbero.id)
    ? [{ id: t.barbero.id, nombre: t.barbero.nombre, apellido: "" }, ...equipo] : equipo;

  const cambiar = (campo) => (e) => {
    const valor = e.target.value;
    setForm((f) => {
      const nuevo = { ...f, [campo]: valor };
      // Al cambiar el servicio, el importe pasa a ser el precio de lista (se puede ajustar).
      const elegido = opciones.find((s) => s.id === Number(valor));
      if (campo === "idServicio" && elegido) nuevo.precio = String(Number(elegido.precio));
      return nuevo;
    });
  };

  const precio = Number(form?.precio);
  const precioValido = form?.precio !== "" && Number.isFinite(precio) && precio >= 0;

  async function guardar(e) {
    e.preventDefault();
    if (!precioValido) return;
    setGuardando(true);
    setError(null);
    try {
      await alGuardar({
        idServicio: Number(form.idServicio),
        idBarbero: esDueno ? Number(form.idBarbero) : null,
        precio,
        medio: form.medio,
      });
    } catch (err) {
      setError(err.message);
    } finally {
      setGuardando(false);
    }
  }

  return (
    <Modal abierto={!!t} alCerrar={alCerrar} titulo="Editar turno"
           bajada={t ? `${t.cliente.nombre} ${t.cliente.apellido} · ${fechaLarga(t.fecha)} ${t.horaInicio}` : ""}>
      {form && (
        <form onSubmit={guardar} noValidate>
          <div className="fila-campos">
            <label className="campo"><span>Servicio</span>
              <select value={form.idServicio} onChange={cambiar("idServicio")}>
                {conElServicio.map((s) => <option key={s.id} value={s.id}>{s.nombre}</option>)}
              </select>
            </label>
            {esDueno && (
              <label className="campo"><span>Peluquero</span>
                <select value={form.idBarbero} onChange={cambiar("idBarbero")}>
                  {conElPeluquero.map((b) => <option key={b.id} value={b.id}>{b.nombre} {b.apellido}</option>)}
                </select>
              </label>
            )}
          </div>
          <label className="campo"><span>{t.pago ? "Importe cobrado" : "Importe"}</span>
            <input type="number" inputMode="numeric" min="0" step="1" value={form.precio} onChange={cambiar("precio")}
                   aria-invalid={!precioValido} />
            {!precioValido && <small className="error-campo">Poné un importe válido</small>}
          </label>
          {t.pago && (
            <div className="campo">
              <span>¿Cómo pagó?</span>
              <div className="pastillas grandes" role="radiogroup">
                {Object.entries(NOMBRE_MEDIO).map(([valor, texto]) => (
                  <label className="pastilla" key={valor}>
                    <input type="radio" name="medio-editar" value={valor} checked={form.medio === valor}
                           onChange={() => setForm((f) => ({ ...f, medio: valor }))} />
                    <span>{texto}</span>
                  </label>
                ))}
              </div>
            </div>
          )}
          <p className="texto-ayuda">Los cambios se reflejan en Pagos y en la liquidación de cada peluquero.</p>
          <AvisoError mensaje={error} />
          <div className="modal-pie">
            <button type="button" className="btn btn-secundario" onClick={alCerrar}>Cancelar</button>
            <button type="submit" className="btn btn-primario" disabled={guardando || !precioValido}>
              {guardando ? "Guardando…" : "Guardar cambios"}
            </button>
          </div>
        </form>
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

const CLIENTE_VACIO = { nombre: "", apellido: "", email: "", telefono: "" };

/** Como en la reserva del cliente, pero el email es opcional. */
function validarCliente(c) {
  const errores = {};
  if (!c.nombre.trim()) errores.nombre = "Falta el nombre";
  if (!c.apellido.trim()) errores.apellido = "Falta el apellido";
  if (c.email.trim() && !/^\S+@\S+\.\S+$/.test(c.email.trim())) errores.email = "Revisá el email o dejalo vacío";
  if (!/^[0-9 +()-]{8,30}$/.test(c.telefono.trim())) errores.telefono = "Con característica, ej. 351 555-1234";
  return errores;
}

/**
 * Extensión: cargar un turno desde el panel (cliente que llama por teléfono o viene sin reservar).
 * El dueño elige cualquier peluquero; el barbero se carga solo a sí mismo.
 */
function ModalTurno({ abierto, alCerrar, fechaInicial, equipo, esDueno, idPropio, alGuardar }) {
  const [form, setForm] = useState(null);
  const [errores, setErrores] = useState({});
  const [error, setError] = useState(null);
  const [guardando, setGuardando] = useState(false);

  // Cada vez que se abre, arranca con la fecha que se está mirando.
  if (abierto && !form) {
    setForm({ idServicio: "", idBarbero: "", fecha: fechaInicial, hora: "", ...CLIENTE_VACIO });
    setErrores({});
    setError(null);
  }
  if (!abierto && form) setForm(null);

  const servicios = usePedidoAdmin(abierto ? "/servicios" : null);
  const propio = equipo.find((b) => b.id === idPropio);
  // El barbero solo ve los servicios que hace; el dueño, todos.
  const opcionesServicio = (servicios.datos ?? []).filter((s) => esDueno || !propio || propio.servicios?.includes(s.id));
  const servicio = opcionesServicio.find((s) => s.id === Number(form?.idServicio));
  const queLoHacen = servicio ? equipo.filter((b) => b.servicios?.includes(servicio.id)) : equipo;

  const barbero = esDueno ? (form?.idBarbero ? Number(form.idBarbero) : undefined) : idPropio;
  const dias = usePedidoAdmin(servicio ? "/disponibilidad/dias" : null,
                              { servicio: servicio?.id, barbero, cantidad: 14 });
  const slots = usePedidoAdmin(servicio && form?.fecha ? "/disponibilidad" : null,
                               { servicio: servicio?.id, fecha: form?.fecha, barbero });
  const libres = (slots.datos?.slots ?? []).filter((s) => s.libre);

  // Si el día elegido no tiene lugar (o quedó fuera de las dos semanas), pasa al primero que tenga.
  useEffect(() => {
    if (!dias.datos) return;
    setForm((f) => f && !dias.datos.some((d) => d.fecha === f.fecha && d.libres > 0)
      ? { ...f, fecha: dias.datos.find((d) => d.libres > 0)?.fecha ?? "", hora: "" }
      : f);
  }, [dias.datos]);

  // Si el horario elegido dejó de estar libre, se deselecciona.
  useEffect(() => {
    if (!slots.datos) return;
    setForm((f) => f && f.hora && !slots.datos.slots.some((s) => s.hora === f.hora && s.libre) ? { ...f, hora: "" } : f);
  }, [slots.datos]);

  const cambiar = (campo) => (e) => {
    const valor = e.target.value;
    setForm((f) => {
      const nuevo = { ...f, [campo]: valor };
      if (campo === "idServicio" || campo === "idBarbero" || campo === "fecha") nuevo.hora = "";
      // Si el peluquero elegido no hace el servicio nuevo, vuelve a "Cualquiera".
      if (campo === "idServicio" && f.idBarbero &&
          !equipo.find((b) => b.id === Number(f.idBarbero))?.servicios?.includes(Number(valor))) nuevo.idBarbero = "";
      return nuevo;
    });
    if (errores[campo]) setErrores((er) => ({ ...er, [campo]: undefined }));
  };

  async function guardar(e) {
    e.preventDefault();
    const encontrados = validarCliente(form);
    setErrores(encontrados);
    if (Object.keys(encontrados).length) return;

    setGuardando(true);
    setError(null);
    try {
      await alGuardar({
        idServicio: servicio.id,
        idBarbero: barbero ?? null,
        fecha: form.fecha,
        hora: form.hora,
        cliente: {
          nombre: form.nombre.trim(),
          apellido: form.apellido.trim(),
          email: form.email.trim() || null,
          telefono: form.telefono.trim(),
        },
      });
    } catch (err) {
      setError(err.message);
      if (err instanceof ErrorApi && err.status === 400) {
        // "cliente.email" → "email"
        setErrores(Object.fromEntries(Object.entries(err.errores).map(([k, v]) => [k.replace("cliente.", ""), v])));
      } else if (err instanceof ErrorApi && err.status === 409) {
        // Se ocupó mientras se cargaba: se vuelven a pedir los horarios.
        slots.recargar();
        dias.recargar();
      }
    } finally {
      setGuardando(false);
    }
  }

  const campoCliente = (campo, etiqueta, props) => (
    <label className="campo"><span>{etiqueta}</span>
      <input value={form[campo]} onChange={cambiar(campo)} aria-invalid={!!errores[campo]}
             aria-describedby={errores[campo] ? `error-turno-${campo}` : undefined} {...props} />
      {errores[campo] && <small className="error-campo" id={`error-turno-${campo}`}>{errores[campo]}</small>}
    </label>
  );

  const selectorServicio = (
    <label className="campo"><span>Servicio</span>
      <select value={form?.idServicio ?? ""} onChange={cambiar("idServicio")} required>
        <option value="" disabled>{servicios.cargando ? "Cargando…" : "Elegí un servicio"}</option>
        {opcionesServicio.map((s) => <option key={s.id} value={s.id}>{s.nombre} · {s.duracionMinutos} min</option>)}
      </select>
    </label>
  );

  return (
    <Modal abierto={abierto} alCerrar={alCerrar} titulo="Nuevo turno"
           bajada="Para clientes que llaman por teléfono o vienen sin reservar.">
      {form && (
        <form onSubmit={guardar} noValidate>
          {esDueno ? (
            <div className="fila-campos">
              {selectorServicio}
              <label className="campo"><span>Peluquero</span>
                <select value={form.idBarbero} onChange={cambiar("idBarbero")}>
                  <option value="">Cualquiera</option>
                  {queLoHacen.map((b) => <option key={b.id} value={b.id}>{b.nombre} {b.apellido}</option>)}
                </select>
              </label>
            </div>
          ) : selectorServicio}

          {servicio && (
            <label className="campo"><span>Día</span>
              <select value={form.fecha} onChange={cambiar("fecha")} disabled={!dias.datos}>
                {!dias.datos && <option value={form.fecha}>Buscando días con lugar…</option>}
                {dias.datos && !form.fecha && <option value="">Sin lugar en las próximas dos semanas</option>}
                {dias.datos?.map((d) => (
                  <option key={d.fecha} value={d.fecha} disabled={d.libres === 0}>
                    {fechaLarga(d.fecha)} · {d.libres ? `${d.libres} libres` : d.atiende ? "completo" : "no se atiende"}
                  </option>
                ))}
              </select>
            </label>
          )}

          {servicio && form.fecha && (
            <div className="campo">
              <span>Horario{form.hora ? ` · ${form.hora} a ${sumarMinutos(form.hora, servicio.duracionMinutos)}` : ""}</span>
              {slots.cargando && !slots.datos ? <p className="texto-ayuda">Buscando horarios…</p>
                : slots.error ? <AvisoError mensaje={slots.error.message} />
                : !libres.length ? <p className="texto-ayuda">No quedan horarios libres este día.</p>
                : (
                  <div className="pastillas horas-turno" role="radiogroup" aria-label="Horarios libres">
                    {libres.map((s) => (
                      <label className="pastilla" key={s.hora}>
                        <input type="radio" name="hora-turno" value={s.hora} checked={form.hora === s.hora}
                               onChange={cambiar("hora")} />
                        <span>{s.hora}</span>
                      </label>
                    ))}
                  </div>
                )}
              {esDueno && !form.idBarbero && form.hora && (
                <p className="texto-ayuda nota-turno">Se asigna el peluquero libre con menos turnos ese día.</p>
              )}
            </div>
          )}

          <h3 className="titulo-seccion-modal">Cliente</h3>
          <div className="fila-campos">
            {campoCliente("nombre", "Nombre", { maxLength: 60, autoComplete: "off" })}
            {campoCliente("apellido", "Apellido", { maxLength: 60, autoComplete: "off" })}
          </div>
          <div className="fila-campos">
            {campoCliente("telefono", "Teléfono", { type: "tel", inputMode: "tel", maxLength: 30, placeholder: "351 555-1234", autoComplete: "off" })}
            {campoCliente("email", "Email (opcional)", { type: "email", inputMode: "email", placeholder: "cliente@gmail.com", autoComplete: "off" })}
          </div>
          <p className="texto-ayuda">Sin email no le llega la confirmación, el aviso si se cancela ni la encuesta.</p>

          <AvisoError mensaje={error} />
          <div className="modal-pie">
            <button type="button" className="btn btn-secundario" onClick={alCerrar}>Cancelar</button>
            <button type="submit" className="btn btn-primario" disabled={guardando || !servicio || !form.fecha || !form.hora}>
              {guardando ? "Guardando…" : "Cargar turno"}
            </button>
          </div>
        </form>
      )}
    </Modal>
  );
}
