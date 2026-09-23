import { useEffect, useMemo, useState } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import { api, ErrorApi } from "../../api/api.js";
import { usePedido } from "../../api/usePedido.js";
import { DIAS_CORTOS, aFecha, fechaLarga, pesos, sumarMinutos } from "../../lib/formato.js";
import { recordarCliente, recordarReserva, clienteRecordado } from "../../lib/memoria.js";
import { BarraCta, EstadoPedido, LayoutCliente, Resumen, Topbar } from "../../componentes/cliente/LayoutCliente.jsx";
import { TarjetaServicio } from "../../componentes/cliente/TarjetaServicio.jsx";

const NOMBRES_PASO = { 1: "Servicio", 2: "Día y horario", 3: "Tus datos" };
const FRANJAS = [
  { titulo: "Mañana", desde: 0, hasta: 13 },
  { titulo: "Tarde", desde: 13, hasta: 18 },
  { titulo: "Noche", desde: 18, hasta: 24 },
];

/** Flujo de reserva en 3 pasos (RF-01, RF-02, RF-03, RNF-11). */
export default function Reservar() {
  const navegar = useNavigate();
  const [params] = useSearchParams();

  const [paso, setPaso] = useState(1);
  const [servicio, setServicio] = useState(null);
  const [barbero, setBarbero] = useState(null);   // el que eligió el cliente; null = cualquiera
  const [asignado, setAsignado] = useState(null); // con "cualquiera": quién lo atiende en el horario elegido
  const [fecha, setFecha] = useState(null);       // "2026-09-24"
  const [hora, setHora] = useState(null);         // "15:00"
  const [aviso, setAviso] = useState(null);       // mensaje arriba del paso 2

  const servicios = usePedido(() => api("/servicios"), []);
  const barberos = usePedido(() => api("/barberos"), []);

  // Si viene de la home con un servicio elegido, se saltea el paso 1.
  useEffect(() => {
    const id = Number(params.get("servicio"));
    const elegido = servicios.datos?.find((s) => s.id === id);
    if (elegido && !servicio) {
      setServicio(elegido);
      setPaso(2);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [servicios.datos]);

  function irA(n) {
    setPaso(n);
    window.scrollTo({ top: 0, behavior: "smooth" });
  }

  function elegirServicio(s) {
    setServicio(s);
    setBarbero(null);
    setAsignado(null);
    setFecha(null);
    setHora(null);
  }

  function volver() {
    if (paso > 1) irA(paso - 1);
    else navegar("/");
  }

  /** El horario se ocupó mientras el cliente completaba sus datos: vuelve al paso 2. */
  function horarioOcupado(mensaje) {
    setHora(null);
    setAsignado(null);
    setAviso(mensaje);
    irA(2);
  }

  return (
    <LayoutCliente titulo="Reservar turno">
      <Topbar titulo="Reservar turno" alVolver={volver} />

      <div className="pasos" aria-hidden="true">
        {[1, 2, 3].map((n) => (
          <div key={n} className={`paso${n === paso ? " activo" : ""}${n < paso ? " hecho" : ""}`} />
        ))}
      </div>
      <div className="pasos-txt">
        <span>Paso <strong>{paso}</strong> de 3</span>
        <span>{NOMBRES_PASO[paso]}</span>
      </div>

      {paso === 1 && (
        <PasoServicio servicios={servicios} elegido={servicio} alElegir={elegirServicio}
                      alContinuar={() => irA(2)} />
      )}
      {paso === 2 && servicio && (
        <PasoHorario servicio={servicio} barberos={barberos.datos ?? []}
                     barbero={barbero} setBarbero={setBarbero} asignado={asignado} setAsignado={setAsignado}
                     fecha={fecha} setFecha={setFecha} hora={hora} setHora={setHora}
                     aviso={aviso} setAviso={setAviso}
                     alCambiarServicio={() => irA(1)} alContinuar={() => irA(3)} />
      )}
      {paso === 3 && servicio && fecha && hora && (barbero || asignado) && (
        <PasoDatos servicio={servicio} barbero={barbero ?? asignado} fecha={fecha} hora={hora}
                   alHorarioOcupado={horarioOcupado}
                   alConfirmar={(reserva, cliente) => {
                     recordarCliente(cliente);
                     recordarReserva({ ...reserva, email: cliente.email, nombre: cliente.nombre });
                     navegar("/turno/confirmado", { replace: true });
                   }} />
      )}
    </LayoutCliente>
  );
}

// ------------------------------------------------------------------
// Paso 1: servicio
// ------------------------------------------------------------------

function PasoServicio({ servicios, elegido, alElegir, alContinuar }) {
  return (
    <section className="vista activa" aria-labelledby="t1">
      <h2 id="t1">¿Qué te hacés hoy?</h2>
      <p className="bajada">Elegí el servicio. La duración define los horarios que te vamos a ofrecer.</p>
      <EstadoPedido {...servicios} alReintentar={servicios.recargar} />
      <div className="servicios" role="group" aria-label="Servicios disponibles">
        {servicios.datos?.map((s) => (
          <TarjetaServicio key={s.id} servicio={s} elegido={elegido?.id === s.id} alElegir={alElegir} />
        ))}
      </div>

      <BarraCta>
        {elegido && (
          <div className="info">
            <small>{elegido.duracionMinutos} min</small>
            <strong>{elegido.nombre} · {pesos(elegido.precio)}</strong>
          </div>
        )}
        <button className={`btn btn-primario${elegido ? "" : " btn-block"}`} disabled={!elegido} onClick={alContinuar}>
          Continuar
        </button>
      </BarraCta>
    </section>
  );
}

// ------------------------------------------------------------------
// Paso 2: con quién, día y horario
// ------------------------------------------------------------------

function PasoHorario({ servicio, barberos, barbero, setBarbero, asignado, setAsignado, fecha, setFecha,
                       hora, setHora, aviso, setAviso, alCambiarServicio, alContinuar }) {
  const queLoHacen = barberos.filter((b) => b.servicios.includes(servicio.id));
  const quienAtiende = barbero ?? asignado;
  const listo = fecha && hora && quienAtiende;

  function limpiarHora() {
    setHora(null);
    setAsignado(null);
    setAviso(null);
  }

  /** Con "cualquiera", al tocar un horario ya se muestra quién atiende (el menos cargado ese día). */
  function elegirHora(slot) {
    setHora(slot.hora);
    setAviso(null);
    if (!barbero) setAsignado(barberos.find((b) => b.id === slot.barberos[0]) ?? null);
  }

  // Días con lugar (GET /disponibilidad/dias). Arranca en el primero que tenga horarios libres.
  const dias = usePedido(
    () => api("/disponibilidad/dias", { params: { servicio: servicio.id, barbero: barbero?.id, cantidad: 14 } }),
    [servicio.id, barbero?.id]
  );
  useEffect(() => {
    if (!dias.datos) return;
    const sigueValido = dias.datos.some((d) => d.fecha === fecha && d.libres > 0);
    if (!sigueValido) {
      setFecha(dias.datos.find((d) => d.libres > 0)?.fecha ?? null);
      setHora(null);
      setAsignado(null);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [dias.datos]);

  // Horarios del día elegido (GET /disponibilidad): los ocupados vienen tachados.
  const slots = usePedido(
    () => (fecha ? api("/disponibilidad", { params: { servicio: servicio.id, fecha, barbero: barbero?.id } }) : null),
    [servicio.id, barbero?.id, fecha, aviso]
  );

  return (
    <section className="vista activa" aria-labelledby="t2">
      <h2 id="t2">Elegí día y horario</h2>
      <p className="bajada">
        {servicio.nombre} · {servicio.duracionMinutos} min · {pesos(servicio.precio)}{" "}
        <button className="enlace" onClick={alCambiarServicio}>Cambiar</button>
      </p>

      {aviso && <div className="aviso-error" role="alert">{aviso}</div>}

      {queLoHacen.length > 1 && (
        <div className="con-quien">
          <h3>Con quién</h3>
          <div className="chips" role="group" aria-label="Peluquero">
            <button type="button" className="chip" aria-pressed={!barbero}
                    onClick={() => { setBarbero(null); limpiarHora(); }}>
              Cualquiera
            </button>
            {queLoHacen.map((b) => (
              <button type="button" key={b.id} className="chip" aria-pressed={barbero?.id === b.id}
                      onClick={() => { setBarbero(b); limpiarHora(); }}>
                {b.nombre}
              </button>
            ))}
          </div>
        </div>
      )}

      <EstadoPedido {...dias} texto="Buscando días con lugar…" alReintentar={dias.recargar} />
      {dias.datos && (
        <div className="dias" role="group" aria-label="Días">
          {dias.datos.map((d, i) => {
            const f = aFecha(d.fecha);
            return (
              <button type="button" key={d.fecha} className="dia" aria-pressed={d.fecha === fecha}
                      disabled={d.libres === 0}
                      aria-label={`${fechaLarga(d.fecha)}: ${d.libres ? `${d.libres} horarios libres` : "sin lugar"}`}
                      onClick={() => { setFecha(d.fecha); limpiarHora(); }}>
                <small>{i === 0 ? "Hoy" : DIAS_CORTOS[f.getDay()]}</small>
                <b>{f.getDate()}</b>
                <i>{d.libres ? `${d.libres} libres` : "—"}</i>
              </button>
            );
          })}
        </div>
      )}

      {dias.datos && !fecha && (
        <div className="vacio">No hay lugar para {servicio.nombre} en las próximas dos semanas.</div>
      )}
      {fecha && (
        <Horarios slots={slots} hora={hora} alElegir={elegirHora} servicio={servicio}
                  quienAtiende={!barbero && hora ? (
                    <QuienAtiende slot={slots.datos?.slots.find((s) => s.hora === hora)} barberos={barberos}
                                  asignado={asignado} setAsignado={setAsignado} />
                  ) : null} />
      )}

      <BarraCta>
        {listo && (
          <div className="info">
            <small>{fechaLarga(fecha)}</small>
            <strong>{hora} hs · {quienAtiende.nombre}</strong>
          </div>
        )}
        <button className={`btn btn-primario${listo ? "" : " btn-block"}`} disabled={!listo}
                onClick={alContinuar}>
          Continuar
        </button>
      </BarraCta>
    </section>
  );
}

/**
 * Con "cualquiera": quién atiende en el horario elegido. Si hay más de uno libre,
 * se puede cambiar ahí mismo; viene sugerido el que tiene menos turnos ese día.
 */
function QuienAtiende({ slot, barberos, asignado, setAsignado }) {
  const libres = (slot?.barberos ?? []).map((id) => barberos.find((b) => b.id === id)).filter(Boolean);
  if (!asignado || !libres.length) return null;
  if (libres.length === 1) {
    return <p className="te-atiende">Te atiende <strong>{asignado.nombre}</strong></p>;
  }
  return (
    <div className="te-atiende">
      <p>A las {slot.hora} te atiende</p>
      <div className="chips" role="group" aria-label={`Peluqueros libres a las ${slot.hora}`}>
        {libres.map((b) => (
          <button type="button" key={b.id} className="chip" aria-pressed={asignado.id === b.id}
                  onClick={() => setAsignado(b)}>
            {b.nombre}
          </button>
        ))}
      </div>
    </div>
  );
}

function Horarios({ slots, hora, alElegir, servicio, quienAtiende }) {
  if (slots.cargando || slots.error) {
    return <EstadoPedido {...slots} texto="Buscando horarios…" alReintentar={slots.recargar} />;
  }
  const lista = slots.datos?.slots ?? [];
  if (!lista.some((s) => s.libre)) {
    return (
      <div className="vacio">
        No quedan horarios para {servicio.nombre} este día.<br />Probá con otra fecha.
      </div>
    );
  }
  return FRANJAS.map((f) => {
    const delTramo = lista.filter((s) => {
      const h = Number(s.hora.slice(0, 2));
      return h >= f.desde && h < f.hasta;
    });
    if (!delTramo.length) return null;
    const tieneLaElegida = delTramo.some((s) => s.hora === hora);
    return (
      <div className="franja" key={f.titulo}>
        <h3>{f.titulo}</h3>
        <div className="horarios">
          {delTramo.map((s) => (
            <button type="button" key={s.hora} className="hora" disabled={!s.libre} aria-pressed={s.hora === hora}
                    aria-label={s.libre ? s.hora : `${s.hora}, ocupado`} onClick={() => alElegir(s)}>
              {s.hora}
            </button>
          ))}
        </div>
        {/* Justo debajo del horario tocado, para que se vea sin scrollear */}
        {tieneLaElegida && quienAtiende}
      </div>
    );
  });
}

// ------------------------------------------------------------------
// Paso 3: datos del cliente y confirmación (POST /turnos)
// ------------------------------------------------------------------

const VACIO = { nombre: "", apellido: "", email: "", telefono: "" };

function validar(c) {
  const errores = {};
  if (!c.nombre.trim()) errores.nombre = "Contanos tu nombre";
  if (!c.apellido.trim()) errores.apellido = "Contanos tu apellido";
  if (!/^\S+@\S+\.\S+$/.test(c.email.trim())) errores.email = "Revisá el email: ahí te llega la confirmación";
  if (!/^[0-9 +()-]{8,30}$/.test(c.telefono.trim())) errores.telefono = "Poné tu celular con característica, ej. 351 555-1234";
  return errores;
}

function PasoDatos({ servicio, barbero, fecha, hora, alHorarioOcupado, alConfirmar }) {
  const [cliente, setCliente] = useState(() => ({ ...VACIO, ...clienteRecordado() }));
  const [errores, setErrores] = useState({});
  const [enviando, setEnviando] = useState(false);
  const [falla, setFalla] = useState(null);

  const filas = useMemo(() => [
    ["Servicio", servicio.nombre],
    ["Día", fechaLarga(fecha)],
    ["Horario", `${hora} a ${sumarMinutos(hora, servicio.duracionMinutos)}`],
    ["Peluquero", barbero ? barbero.nombre : "El primero libre"],
  ], [servicio, fecha, hora, barbero]);

  function cambiar(e) {
    const { name, value } = e.target;
    setCliente((c) => ({ ...c, [name]: value }));
    if (errores[name]) setErrores((er) => ({ ...er, [name]: undefined }));
  }

  async function confirmar(e) {
    e.preventDefault();
    const encontrados = validar(cliente);
    setErrores(encontrados);
    const primero = Object.keys(encontrados)[0];
    if (primero) {
      document.querySelector(`[name="${primero}"]`)?.focus();
      return;
    }

    setEnviando(true);
    setFalla(null);
    const datos = Object.fromEntries(Object.entries(cliente).map(([k, v]) => [k, v.trim()]));
    try {
      const reserva = await api("/turnos", {
        metodo: "POST",
        cuerpo: { idServicio: servicio.id, idBarbero: barbero?.id ?? null, fecha, hora, cliente: datos },
      });
      alConfirmar(reserva, datos);
    } catch (error) {
      setEnviando(false);
      if (error instanceof ErrorApi && error.status === 409) {
        alHorarioOcupado(error.message);
      } else if (error instanceof ErrorApi && error.status === 400) {
        // Errores de validación del servidor: "cliente.email" → "email"
        setErrores(Object.fromEntries(Object.entries(error.errores).map(([k, v]) => [k.replace("cliente.", ""), v])));
      } else {
        setFalla(error.message);
      }
    }
  }

  const campo = (name, etiqueta, props) => (
    <label className="campo">
      <span>{etiqueta}</span>
      <input name={name} value={cliente[name]} onChange={cambiar} aria-invalid={!!errores[name]}
             aria-describedby={errores[name] ? `error-${name}` : undefined} {...props} />
      {errores[name] && <small className="error-campo" id={`error-${name}`}>{errores[name]}</small>}
    </label>
  );

  return (
    <section className="vista activa" aria-labelledby="t3">
      <h2 id="t3">Confirmá tu turno</h2>
      <p className="bajada">Te mandamos la confirmación por email. No hace falta crear cuenta.</p>

      <Resumen filas={filas} total={["A pagar en el local", pesos(servicio.precio)]} />

      <form id="form-datos" onSubmit={confirmar} noValidate>
        {campo("nombre", "Nombre", { autoComplete: "given-name", placeholder: "Agustín" })}
        {campo("apellido", "Apellido", { autoComplete: "family-name", placeholder: "Rosas" })}
        {campo("email", "Email", { type: "email", inputMode: "email", autoComplete: "email", placeholder: "tuemail@gmail.com" })}
        {campo("telefono", "Celular", { type: "tel", inputMode: "tel", autoComplete: "tel", placeholder: "351 555-1234" })}
        <p className="nota-maqueta">Usamos tus datos solo para confirmarte el turno y enviarte la encuesta.</p>
      </form>

      {falla && <div className="aviso-error" role="alert">{falla}</div>}

      <BarraCta>
        <button className="btn btn-primario btn-block" type="submit" form="form-datos" disabled={enviando}>
          {enviando ? "Reservando…" : "Confirmar turno"}
        </button>
      </BarraCta>
    </section>
  );
}
