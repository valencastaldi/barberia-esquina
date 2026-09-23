import { useEffect, useState } from "react";
import { semanaResumida } from "../../lib/horarios.js";
import { useSesion, usePedidoAdmin } from "../sesion.jsx";
import { AvisoError, Cabecera, EstadoCarga, Interruptor, Segmentos } from "../componentes/ui.jsx";

const NOMBRES_DIA = ["Domingo", "Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado"];
const ORDEN_DIAS = [1, 2, 3, 4, 5, 6, 0];
const SLOTS = [15, 30, 45, 60].map((m) => ({ valor: m, texto: `${m} min` }));

/** RF-24, RF-25, RF-26: horario semanal de cada peluquero y slot base. */
export default function Horarios() {
  const { pedir, usuario, esDueno } = useSesion();
  const equipo = usePedidoAdmin("/barberos");   // lista pública: nombres de los activos
  const [idBarbero, setIdBarbero] = useState(usuario.id);
  const guardado = usePedidoAdmin("/horarios", { barbero: idBarbero });
  const [semana, setSemana] = useState(null);
  const [estado, setEstado] = useState({ guardando: false, error: null, ok: false });

  // Copia editable de lo que vino de la API.
  useEffect(() => { if (guardado.datos) setSemana(guardado.datos); }, [guardado.datos]);

  const puedeEditar = esDueno || idBarbero === usuario.id;
  const cambios = semana && guardado.datos && JSON.stringify(semana) !== JSON.stringify(guardado.datos);
  const slot = semana?.find((d) => d.activo)?.duracionSlotMin ?? 30;

  const cambiarDia = (dia, campos) => setSemana((s) => s.map((d) => (d.diaSemana === dia ? { ...d, ...campos } : d)));

  function alternar(d, activo) {
    // Al abrir un día que nunca tuvo horario, se propone 10 a 20.
    cambiarDia(d.diaSemana, { activo, horaInicio: d.horaInicio ?? "10:00", horaFin: d.horaFin ?? "20:00" });
  }

  async function guardar() {
    setEstado({ guardando: true, error: null, ok: false });
    try {
      await pedir("/horarios", { metodo: "PUT", params: { barbero: idBarbero }, cuerpo: { dias: semana } });
      setEstado({ guardando: false, error: null, ok: true });
      guardado.recargar();
    } catch (e) {
      setEstado({ guardando: false, error: e.message, ok: false });
    }
  }

  const activos = equipo.datos ?? [];
  const elegible = esDueno ? activos : activos.filter((b) => b.id === usuario.id);

  return (
    <>
      <Cabecera titulo={esDueno ? "Horarios de atención" : "Mis horarios"} bajada="De acá sale la disponibilidad que ve el cliente al reservar.">
        {puedeEditar && (
          <>
            <button type="button" className="btn btn-secundario" disabled={!cambios} onClick={() => setSemana(guardado.datos)}>Deshacer</button>
            <button type="button" className="btn btn-primario" disabled={!cambios || estado.guardando} onClick={guardar}>
              {estado.guardando ? "Guardando…" : "Guardar cambios"}
            </button>
          </>
        )}
      </Cabecera>

      {elegible.length > 1 && (
        <div className="filtro-equipo">
          <Segmentos chico etiqueta="Peluquero" valor={idBarbero}
                     alCambiar={(id) => { setIdBarbero(id); setSemana(null); setEstado({ guardando: false, error: null, ok: false }); }}
                     opciones={elegible.map((b) => ({ valor: b.id, texto: b.nombre }))} />
        </div>
      )}

      <AvisoError mensaje={estado.error} alCerrar={() => setEstado((e) => ({ ...e, error: null }))} />
      {estado.ok && !cambios && <div className="aviso-panel ok" role="status">Horario guardado. Ya se ofrece así al reservar.</div>}

      <div className="columnas horarios">
        <section className="bloque">
          <header><div><h2>Semana</h2><p>Apagá un día para cerrarlo sin perder el horario cargado.</p></div></header>
          <EstadoCarga pedido={guardado} />
          {semana && ORDEN_DIAS.map((n) => {
            const d = semana.find((x) => x.diaSemana === n);
            return (
              <div key={n} className={`dia-fila${d.activo ? "" : " apagado"}`}>
                <Interruptor activo={d.activo} textoSi={NOMBRES_DIA[n]} textoNo={NOMBRES_DIA[n]}
                             deshabilitado={!puedeEditar} alCambiar={(v) => alternar(d, v)} />
                <div className="rangos">
                  <input type="time" step="1800" aria-label={`${NOMBRES_DIA[n]}: desde`} value={d.horaInicio ?? ""}
                         disabled={!d.activo || !puedeEditar} onChange={(e) => cambiarDia(n, { horaInicio: e.target.value })} />
                  <span className="a">a</span>
                  <input type="time" step="1800" aria-label={`${NOMBRES_DIA[n]}: hasta`} value={d.horaFin ?? ""}
                         disabled={!d.activo || !puedeEditar} onChange={(e) => cambiarDia(n, { horaFin: e.target.value })} />
                  {!d.activo && <span className="a">Cerrado</span>}
                </div>
              </div>
            );
          })}
        </section>

        <div>
          <section className="bloque">
            <header><div><h2>Duración del slot base</h2></div></header>
            <div className="cuerpo">
              <p className="texto-ayuda">
                Cada cuánto arranca un turno posible. Con 30 min, un corte de 45 empieza a las 10:00, 10:30, 11:00…
              </p>
              <Segmentos opciones={SLOTS} valor={slot} etiqueta="Slot base"
                         alCambiar={(m) => puedeEditar && setSemana((s) => s.map((d) => ({ ...d, duracionSlotMin: m })))} />
            </div>
          </section>

          {semana && (
            <section className="bloque">
              <header><div><h2>Así lo ve el cliente</h2></div></header>
              <div className="cuerpo">
                <div className="estrellas-dist" style={{ gap: 9 }}>
                  {semanaResumida(semana).map((l) => <div key={l} className="texto-ayuda" style={{ margin: 0 }}>{l}</div>)}
                </div>
              </div>
            </section>
          )}

          <section className="bloque">
            <header><div><h2>Al guardar</h2></div></header>
            <div className="cuerpo">
              <p className="texto-ayuda">
                Los turnos ya reservados no se tocan: el cambio solo afecta los horarios que se ofrecen de ahora en más.
                Para un día puntual (trámite, feriado) usá "Bloquear franja" en la Agenda.
              </p>
            </div>
          </section>
        </div>
      </div>
    </>
  );
}
