import { useState } from "react";
import { pesos } from "../../lib/formato.js";
import { useSesion, usePedidoAdmin } from "../sesion.jsx";
import { AvisoError, Cabecera, EstadoCarga, Interruptor, Modal } from "../componentes/ui.jsx";

const DURACIONES = [15, 30, 45, 60, 75, 90, 120];

/** RF-22, RF-23, RF-28, RF-29: catálogo de servicios. Editar es solo del dueño. */
export default function Servicios() {
  const { pedir, esDueno } = useSesion();
  const servicios = usePedidoAdmin("/servicios", { todos: true });
  const [editando, setEditando] = useState(null);   // {} = nuevo
  const [aviso, setAviso] = useState(null);

  async function accion(fn) {
    setAviso(null);
    try { await fn(); } catch (e) { setAviso(e.message); } finally { servicios.recargar(); }
  }

  const lista = servicios.datos ?? [];
  return (
    <>
      <Cabecera titulo="Servicios" bajada="Lo que ve el cliente al reservar. La duración define los horarios que se ofrecen.">
        {esDueno && <button type="button" className="btn btn-primario" onClick={() => setEditando({})}>+ Nuevo servicio</button>}
      </Cabecera>

      <AvisoError mensaje={aviso} alCerrar={() => setAviso(null)} />

      <section className="bloque">
        <header>
          <div><h2>Catálogo</h2></div>
          <span className="eyebrow">{lista.filter((s) => s.activo).length} de {lista.length} visibles</span>
        </header>
        <EstadoCarga pedido={servicios} />
        {lista.length > 0 && (
          <table className="datos">
            <thead>
              <tr><th>Servicio</th><th>Duración</th><th>Precio</th><th>Visible al cliente</th>{esDueno && <th className="fin">Acciones</th>}</tr>
            </thead>
            <tbody>
              {lista.map((s) => (
                <tr key={s.id}>
                  <td><b>{s.nombre}</b><span className="sub">{s.descripcion}</span></td>
                  <td className="num">{s.duracionMinutos} min</td>
                  <td className="num">{pesos(s.precio)}</td>
                  <td>
                    <Interruptor activo={s.activo} textoSi="Activo" textoNo="Oculto" deshabilitado={!esDueno}
                                 alCambiar={(activo) => accion(() => pedir(`/servicios/${s.id}/estado`, { metodo: "PATCH", cuerpo: { activo } }))} />
                  </td>
                  {esDueno && (
                    <td className="fin">
                      <div className="acciones-turno" style={{ justifyContent: "flex-end" }}>
                        <button type="button" className="mini" onClick={() => setEditando(s)}>Editar</button>
                        <button type="button" className="mini no" onClick={() => {
                          if (window.confirm(`¿Borrar "${s.nombre}"? Si ya tiene turnos no se va a poder: en ese caso ocultalo.`)) {
                            accion(() => pedir(`/servicios/${s.id}`, { metodo: "DELETE" }));
                          }
                        }}>Borrar</button>
                      </div>
                    </td>
                  )}
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </section>

      <p className="nota-maqueta">
        Un servicio con turnos registrados no se puede borrar: se oculta, así el historial y las métricas no pierden la referencia.
        Un servicio nuevo lo hacen todos los peluqueros; después se ajusta en la ficha de cada uno.
      </p>

      <ModalServicio servicio={editando} alCerrar={() => setEditando(null)} alGuardar={async (cuerpo) => {
        await (editando.id
          ? pedir(`/servicios/${editando.id}`, { metodo: "PUT", cuerpo })
          : pedir("/servicios", { metodo: "POST", cuerpo }));
        setEditando(null);
        servicios.recargar();
      }} />
    </>
  );
}

function ModalServicio({ servicio, alCerrar, alGuardar }) {
  const [form, setForm] = useState(null);
  const [error, setError] = useState(null);

  if (servicio && !form) {
    setForm({ nombre: servicio.nombre ?? "", descripcion: servicio.descripcion ?? "",
              duracionMinutos: servicio.duracionMinutos ?? 30, precio: servicio.precio != null ? String(Math.round(servicio.precio)) : "" });
    setError(null);
  }
  if (!servicio && form) setForm(null);

  const cambiar = (campo) => (e) => setForm((f) => ({ ...f, [campo]: e.target.value }));

  async function guardar(e) {
    e.preventDefault();
    try {
      await alGuardar({ ...form, nombre: form.nombre.trim(), descripcion: form.descripcion.trim() || null,
                        duracionMinutos: Number(form.duracionMinutos), precio: Number(form.precio) });
    } catch (err) {
      setError(err.message);
    }
  }

  return (
    <Modal abierto={!!servicio} alCerrar={alCerrar} titulo={servicio?.id ? `Editar ${servicio.nombre}` : "Nuevo servicio"}
           bajada="Así lo ve el cliente cuando elige qué hacerse.">
      {form && (
        <form onSubmit={guardar}>
          <label className="campo"><span>Nombre</span><input value={form.nombre} onChange={cambiar("nombre")} required maxLength={60} /></label>
          <label className="campo"><span>Descripción corta</span><input value={form.descripcion} onChange={cambiar("descripcion")} maxLength={160} /></label>
          <div className="fila-campos">
            <label className="campo"><span>Duración</span>
              <select value={form.duracionMinutos} onChange={cambiar("duracionMinutos")}>
                {DURACIONES.map((d) => <option key={d} value={d}>{d} min</option>)}
              </select>
            </label>
            <label className="campo"><span>Precio ($)</span>
              <input type="number" min="0" step="500" value={form.precio} onChange={cambiar("precio")} required />
            </label>
          </div>
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
