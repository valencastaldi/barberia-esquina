import { useState } from "react";
import { Link } from "react-router-dom";
import { decimal, pesos } from "../../lib/formato.js";
import { useSesion, usePedidoAdmin } from "../sesion.jsx";
import { hoyIso, sumarDias } from "../fechas.js";
import { AvisoError, Cabecera, EstadoCarga, Interruptor, Modal } from "../componentes/ui.jsx";

const LETRAS = ["D", "L", "M", "M", "J", "V", "S"];
const NOMBRES_DIA = ["Domingo", "Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado"];
const ORDEN_DIAS = [1, 2, 3, 4, 5, 6, 0];   // la semana de trabajo se lee desde el lunes

/** [Extensión] El equipo: alta, edición, servicios que hace y rendimiento de 30 días. */
export default function Peluqueros() {
  const { pedir, esDueno } = useSesion();
  const equipo = usePedidoAdmin("/barberos/equipo");
  const servicios = usePedidoAdmin("/servicios", { todos: true });
  const hoy = hoyIso();
  const rendimiento = usePedidoAdmin("/dashboard/barberos", { desde: sumarDias(hoy, -29), hasta: hoy });
  const [editando, setEditando] = useState(null);   // {} = nuevo
  const [aviso, setAviso] = useState(null);

  const nombreServicio = (id) => servicios.datos?.find((s) => s.id === id)?.nombre ?? "";
  const numeros = (id) => rendimiento.datos?.find((r) => r.idBarbero === id);

  async function cambiarEstado(b, activo) {
    setAviso(null);
    try {
      await pedir(`/barberos/${b.id}/estado`, { metodo: "PATCH", cuerpo: { activo } });
    } catch (e) {
      setAviso(e.message);
    } finally {
      equipo.recargar();
    }
  }

  return (
    <>
      <Cabecera titulo="Peluqueros" extension bajada="El equipo, qué días atiende cada uno y cómo le fue en los últimos 30 días.">
        {esDueno && <button type="button" className="btn btn-primario" onClick={() => setEditando({})}>+ Sumar peluquero</button>}
      </Cabecera>

      <AvisoError mensaje={aviso} alCerrar={() => setAviso(null)} />
      <EstadoCarga pedido={equipo} />

      <section className="equipo">
        {equipo.datos?.map((b) => {
          const n = numeros(b.id);
          return (
            <article key={b.id} className={`tarjeta-pelu${b.activo ? "" : " inactivo"}`}>
              <div className="persona">
                <span className="avatar">{b.nombre[0]}{b.apellido[0]}</span>
                <span>
                  <b>{b.nombre} {b.apellido}</b>
                  <small>{b.rol === "dueno" ? "Dueño" : `Barbero · comisión ${b.comisionPct}%`}</small>
                </span>
              </div>
              <div className="semana-mini" aria-label="Días que atiende">
                {ORDEN_DIAS.map((d) => (
                  <span key={d} className={b.diasAtencion.includes(d) ? "si" : ""} title={NOMBRES_DIA[d]}>{LETRAS[d]}</span>
                ))}
              </div>
              <p className="servs">{b.servicios.map(nombreServicio).filter(Boolean).join(" · ") || "Sin servicios asignados"}</p>
              <div className="numeros">
                <div><b>{n?.completados ?? 0}</b>turnos</div>
                <div><b>{pesos(n?.facturado ?? 0)}</b>facturado</div>
                <div><b className="estrella">{n?.satisfaccion ? `${decimal(n.satisfaccion)} ★` : "—"}</b>satisfacción</div>
              </div>
              <div className="pie">
                <Interruptor activo={b.activo} textoSi="Toma turnos" textoNo="No toma turnos"
                             deshabilitado={!esDueno || b.rol === "dueno"} alCambiar={(v) => cambiarEstado(b, v)} />
                {esDueno && <button type="button" className="mini" onClick={() => setEditando(b)}>Editar</button>}
              </div>
            </article>
          );
        })}
      </section>

      <p className="nota-maqueta">
        Un peluquero con turnos no se borra: se desactiva. Deja de aparecer para reservar pero conserva su historial.
        Los días y horarios de cada uno se cargan en <Link to="/admin/horarios">Horarios</Link>.
      </p>

      <ModalPeluquero peluquero={editando} servicios={servicios.datos ?? []} alCerrar={() => setEditando(null)}
                      alGuardar={async (cuerpo) => {
                        await (editando.id
                          ? pedir(`/barberos/${editando.id}`, { metodo: "PUT", cuerpo })
                          : pedir("/barberos", { metodo: "POST", cuerpo }));
                        setEditando(null);
                        equipo.recargar();
                      }} />
    </>
  );
}

function ModalPeluquero({ peluquero, servicios, alCerrar, alGuardar }) {
  const [form, setForm] = useState(null);
  const [error, setError] = useState(null);
  const nuevo = !peluquero?.id;

  if (peluquero && !form) {
    setForm({
      nombre: peluquero.nombre ?? "", apellido: peluquero.apellido ?? "", email: peluquero.email ?? "",
      telefono: peluquero.telefono ?? "", dni: peluquero.dni ?? "", rol: peluquero.rol ?? "barbero",
      comisionPct: peluquero.comisionPct ?? 50,
      servicios: peluquero.servicios ?? servicios.filter((s) => s.activo).map((s) => s.id),
      password: "",
    });
    setError(null);
  }
  if (!peluquero && form) setForm(null);

  const cambiar = (campo) => (e) => setForm((f) => ({ ...f, [campo]: e.target.value }));
  const alternarServicio = (id) => setForm((f) => ({
    ...f, servicios: f.servicios.includes(id) ? f.servicios.filter((x) => x !== id) : [...f.servicios, id],
  }));

  async function guardar(e) {
    e.preventDefault();
    try {
      await alGuardar({
        nombre: form.nombre.trim(), apellido: form.apellido.trim(), email: form.email.trim(),
        telefono: form.telefono.trim() || null, dni: form.dni.trim() || null, rol: form.rol,
        comisionPct: form.rol === "dueno" ? 0 : Number(form.comisionPct), servicios: form.servicios,
        password: form.password || null,
      });
    } catch (err) {
      setError(Object.values(err.errores ?? {})[0] ? `${err.message}: ${Object.entries(err.errores).map(([k, v]) => `${k} ${v}`).join(", ")}` : err.message);
    }
  }

  return (
    <Modal abierto={!!peluquero} alCerrar={alCerrar} titulo={nuevo ? "Sumar peluquero" : `Editar a ${peluquero?.nombre}`}
           bajada="Con estos datos entra al panel y aparece como opción al reservar.">
      {form && (
        <form onSubmit={guardar}>
          <div className="fila-campos">
            <label className="campo"><span>Nombre</span><input value={form.nombre} onChange={cambiar("nombre")} required /></label>
            <label className="campo"><span>Apellido</span><input value={form.apellido} onChange={cambiar("apellido")} required /></label>
          </div>
          <div className="fila-campos">
            <label className="campo"><span>Email (para su login)</span><input type="email" value={form.email} onChange={cambiar("email")} required /></label>
            <label className="campo"><span>Teléfono</span><input value={form.telefono} onChange={cambiar("telefono")} inputMode="tel" /></label>
          </div>
          <div className="fila-campos">
            <label className="campo"><span>Rol</span>
              <select value={form.rol} onChange={cambiar("rol")}>
                <option value="barbero">Barbero</option>
                <option value="dueno">Dueño</option>
              </select>
            </label>
            <label className="campo"><span>Comisión del peluquero (%)</span>
              <input type="number" min="0" max="100" step="5" value={form.rol === "dueno" ? 0 : form.comisionPct}
                     onChange={cambiar("comisionPct")} disabled={form.rol === "dueno"} />
            </label>
          </div>
          <div className="campo">
            <span>Servicios que hace</span>
            <div className="pastillas">
              {servicios.map((s) => (
                <label className="pastilla" key={s.id}>
                  <input type="checkbox" checked={form.servicios.includes(s.id)} onChange={() => alternarServicio(s.id)} />
                  <span>{s.nombre}{s.activo ? "" : " (oculto)"}</span>
                </label>
              ))}
            </div>
          </div>
          <label className="campo">
            <span>{nuevo ? "Contraseña inicial (mínimo 8)" : "Nueva contraseña (dejar vacío para no cambiarla)"}</span>
            <input type="password" value={form.password} onChange={cambiar("password")} minLength={8} required={nuevo}
                   autoComplete="new-password" />
          </label>
          <AvisoError mensaje={error} />
          <div className="modal-pie">
            <button type="button" className="btn btn-secundario" onClick={alCerrar}>Cancelar</button>
            <button type="submit" className="btn btn-primario">Guardar</button>
          </div>
        </form>
      )}
    </Modal>
  );
}
