import { useEffect, useMemo, useState } from "react";
import { minutosDelDia } from "../../lib/formato.js";
import { Avatar } from "../../componentes/Avatar.jsx";
import { NOMBRE_MEDIO } from "./ui.jsx";

/*
 * Cronograma del día: una columna por peluquero y cada turno como un bloque
 * del alto de su duración (tipo Google Calendar). Tocar un bloque abre el
 * detalle, donde están las mismas acciones que en la lista.
 */

const PX_POR_MINUTO = 1.8;            // 30 min = 54 px: entran hora, cliente y servicio
const HORARIO_POR_DEFECTO = { desde: 9 * 60, hasta: 21 * 60 };

const aHora = (min) => `${String(Math.floor(min / 60)).padStart(2, "0")}:${String(min % 60).padStart(2, "0")}`;

export function Cronograma({ turnos, bloqueos, barberos, horarioDelDia, esHoy, puedeTocar, esDueno,
                             alElegirTurno, alQuitarBloqueo }) {
  const ahora = useMinutoActual(esHoy);

  // Los cancelados no ocupan lugar: se listan aparte para no tapar al turno que tomó su horario.
  const visibles = turnos.filter((t) => t.estado !== "cancelado");
  const cancelados = turnos.length - visibles.length;

  // Rango de horas: el horario del local, estirado si algún turno o bloqueo queda afuera.
  const { desde, hasta } = useMemo(() => {
    let d = horarioDelDia?.activo ? minutosDelDia(horarioDelDia.horaInicio) : HORARIO_POR_DEFECTO.desde;
    let h = horarioDelDia?.activo ? minutosDelDia(horarioDelDia.horaFin) : HORARIO_POR_DEFECTO.hasta;
    [...visibles, ...bloqueos].forEach((x) => {
      d = Math.min(d, minutosDelDia(x.horaInicio));
      h = Math.max(h, minutosDelDia(x.horaFin));
    });
    return { desde: Math.floor(d / 60) * 60, hasta: Math.ceil(h / 60) * 60 };
  }, [horarioDelDia, visibles, bloqueos]);

  const alto = (hasta - desde) * PX_POR_MINUTO;
  const y = (hhmm) => (minutosDelDia(hhmm) - desde) * PX_POR_MINUTO;
  const horas = [];
  for (let m = desde; m <= hasta; m += 60) horas.push(m);

  // Columnas: los peluqueros elegidos, más cualquiera que tenga turnos ese día aunque esté dado de baja.
  const columnas = useMemo(() => {
    const lista = [...barberos];
    visibles.forEach((t) => {
      if (!lista.some((b) => b.id === t.barbero.id)) lista.push({ id: t.barbero.id, nombre: t.barbero.nombre, apellido: "" });
    });
    return lista;
  }, [barberos, visibles]);

  if (!columnas.length) return <div className="vacio">No hay peluqueros para mostrar.</div>;

  return (
    <div className="cronograma-envoltorio">
      <div className="cronograma" style={{ gridTemplateColumns: `56px repeat(${columnas.length}, minmax(190px, 1fr))` }}>
        {/* Cabecera: foto, nombre y cuántos turnos tiene */}
        <div className="crono-esquina" />
        {columnas.map((b) => {
          const cant = visibles.filter((t) => t.barbero.id === b.id).length;
          return (
            <div className="crono-cabecera" key={b.id}>
              <Avatar persona={b} className="avatar chico" />
              <span><b>{b.nombre}</b><small>{cant} {cant === 1 ? "turno" : "turnos"}</small></span>
            </div>
          );
        })}

        {/* Eje de horas */}
        <div className="crono-eje" style={{ height: alto }}>
          {horas.map((m) => (
            <span key={m} style={{ top: (m - desde) * PX_POR_MINUTO }}>{aHora(m)}</span>
          ))}
        </div>

        {/* Una columna por peluquero */}
        {columnas.map((b) => (
          <div className="crono-columna" key={b.id} style={{ height: alto, "--media-hora": `${30 * PX_POR_MINUTO}px` }}>
            {bloqueos.filter((x) => x.idBarbero === b.id).map((x) => (
              <div key={`b${x.id}`} className="crono-bloqueo"
                   style={{ top: y(x.horaInicio), height: y(x.horaFin) - y(x.horaInicio) - 3 }}>
                <span>{x.horaInicio}–{x.horaFin} · Bloqueado{x.motivo ? ` · ${x.motivo}` : ""}</span>
                {esDueno && (
                  <button type="button" className="crono-quitar" aria-label="Quitar bloqueo"
                          onClick={() => alQuitarBloqueo(x)}>&times;</button>
                )}
              </div>
            ))}

            {visibles.filter((t) => t.barbero.id === b.id).map((t) => {
              const h = y(t.horaFin) - y(t.horaInicio) - 3;
              const sinCobrar = t.estado === "completado" && !t.pago && puedeTocar(t);
              // Según el alto: 3 renglones (45 min o más), 2 (30 min) o 1 (menos).
              const tamano = h >= 70 ? "" : h >= 40 ? " corto" : " mini";
              return (
                <button type="button" key={t.id}
                        className={`crono-turno estado-${t.estado}${sinCobrar ? " sin-cobrar" : ""}${tamano}`}
                        style={{ top: y(t.horaInicio), height: h }}
                        onClick={() => alElegirTurno(t)}
                        title={`${t.horaInicio}–${t.horaFin} · ${t.cliente.nombre} ${t.cliente.apellido} · ${t.servicio.nombre}`}
                        aria-label={`${t.horaInicio} a ${t.horaFin}, ${t.cliente.nombre} ${t.cliente.apellido}, ${t.servicio.nombre}, ${t.estado}${sinCobrar ? ", sin cobrar" : ""}`}>
                  <span className="crono-hora">{t.horaInicio}–{t.horaFin}</span>
                  <span className="crono-linea">
                    <b>{t.cliente.nombre} {t.cliente.apellido}</b>
                    <span className="crono-servicio">{t.servicio.nombre}</span>
                  </span>
                  <span className="crono-marca">{marca(t, sinCobrar)}</span>
                </button>
              );
            })}

            {ahora !== null && ahora >= desde && ahora <= hasta && (
              <div className="crono-ahora" style={{ top: (ahora - desde) * PX_POR_MINUTO }} aria-hidden="true" />
            )}
          </div>
        ))}
      </div>

      {cancelados > 0 && (
        <p className="nota-maqueta crono-nota">
          {cancelados} {cancelados === 1 ? "turno cancelado no se muestra" : "turnos cancelados no se muestran"} en el
          cronograma; están en la vista Lista.
        </p>
      )}
    </div>
  );
}

/** Texto chico en la esquina del bloque: el estado o cómo se cobró. */
function marca(t, sinCobrar) {
  if (sinCobrar) return "Sin cobrar";
  if (t.estado === "completado") return t.pago ? NOMBRE_MEDIO[t.pago.medio] : "Completado";
  if (t.estado === "ausente") return "Ausente";
  return "";
}

/** Minuto del día actual (se actualiza solo), o null si no se está mirando hoy. */
function useMinutoActual(esHoy) {
  const calcular = () => {
    const d = new Date();
    return d.getHours() * 60 + d.getMinutes();
  };
  const [minuto, setMinuto] = useState(calcular);
  useEffect(() => {
    if (!esHoy) return undefined;
    const intervalo = setInterval(() => setMinuto(calcular()), 60_000);
    return () => clearInterval(intervalo);
  }, [esHoy]);
  return esHoy ? minuto : null;
}
