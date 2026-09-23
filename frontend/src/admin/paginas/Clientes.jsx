import { useEffect, useState } from "react";
import { fechaLarga, pesos, decimal } from "../../lib/formato.js";
import { useSesion, usePedidoAdmin } from "../sesion.jsx";
import { Cabecera, ChipEstado, EstadoCarga, Kpi, Modal, Segmentos } from "../componentes/ui.jsx";

const FILTROS = [
  { valor: "todos", texto: "Todos" },
  { valor: "frecuentes", texto: "Frecuentes" },
  { valor: "nuevos", texto: "Nuevos" },
  { valor: "ausencias", texto: "Con ausencias" },
  { valor: "perdidos", texto: "Dejaron de venir" },
];
const TAMANO = 25;

const iniciales = (c) => `${c.nombre[0] ?? ""}${c.apellido[0] ?? ""}`;
const fechaCorta = (iso) => (iso ? fechaLarga(iso) : "—");

/** [Extensión] Clientes: se registran solos al reservar; acá solo se consultan. */
export default function Clientes() {
  const { pedir } = useSesion();
  const [texto, setTexto] = useState("");
  const [busqueda, setBusqueda] = useState("");
  const [filtro, setFiltro] = useState("todos");
  const [pagina, setPagina] = useState(0);
  const [idFicha, setIdFicha] = useState(null);

  // Espera a que se deje de tipear para buscar.
  useEffect(() => {
    const t = setTimeout(() => { setBusqueda(texto.trim()); setPagina(0); }, 300);
    return () => clearTimeout(t);
  }, [texto]);

  const lista = usePedidoAdmin("/clientes", { q: busqueda, filtro, pagina, tamano: TAMANO });

  // Totales para las tarjetas: el total de cada filtro, pidiendo páginas de 1.
  const [totales, setTotales] = useState(null);
  useEffect(() => {
    const total = (f) => pedir("/clientes", { params: { filtro: f, tamano: 1 } }).then((p) => p.total);
    Promise.all(["todos", "frecuentes", "nuevos", "perdidos"].map(total))
      .then(([todos, frecuentes, nuevos, perdidos]) => setTotales({ todos, frecuentes, nuevos, perdidos }))
      .catch(() => setTotales(null));
  }, [pedir]);

  const p = lista.datos;
  return (
    <>
      <Cabecera titulo="Clientes" extension bajada="Se registran solos la primera vez que reservan. Nadie tiene que cargarlos a mano." />

      <section className="kpis">
        <Kpi destacado etiqueta="Clientes" valor={totales?.todos ?? "—"} delta="Con al menos un turno" />
        <Kpi etiqueta="Frecuentes" valor={totales?.frecuentes ?? "—"} delta="4 visitas o más" />
        <Kpi etiqueta="Nuevos" valor={totales?.nuevos ?? "—"} delta="Primer turno en los últimos 30 días" />
        <Kpi etiqueta="Dejaron de venir" valor={totales?.perdidos ?? "—"} tono="baja" delta="Venían seguido y no vuelven hace +3 semanas" />
      </section>

      <section className="bloque">
        <header>
          <div className="barra-filtros">
            <label className="buscador">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" aria-hidden="true">
                <circle cx="11" cy="11" r="7" /><path d="M20 20l-3.5-3.5" />
              </svg>
              <span className="sr-only">Buscar cliente</span>
              <input type="search" placeholder="Nombre, email o teléfono" value={texto} onChange={(e) => setTexto(e.target.value)} />
            </label>
            <Segmentos chico opciones={FILTROS} valor={filtro} alCambiar={(f) => { setFiltro(f); setPagina(0); }} etiqueta="Filtro" />
          </div>
          <span className="eyebrow">{p ? `${p.total} clientes` : ""}</span>
        </header>

        <EstadoCarga pedido={lista} />
        {p && p.total === 0 && <div className="vacio">No hay clientes que coincidan con la búsqueda.</div>}
        {p && p.total > 0 && (
          <table className="datos">
            <thead>
              <tr><th>Cliente</th><th>Visitas</th><th>Última visita</th><th>Gastado</th><th>Ausencias</th><th>Se atiende con</th></tr>
            </thead>
            <tbody>
              {p.contenido.map((c) => (
                <tr key={c.id} className="clic" tabIndex={0} onClick={() => setIdFicha(c.id)}
                    onKeyDown={(e) => e.key === "Enter" && setIdFicha(c.id)}>
                  <td>
                    <div className="quien-celda">
                      <span className="avatar chico gris">{iniciales(c)}</span>
                      <div><b>{c.nombre} {c.apellido}</b><span className="sub">{c.telefono}</span></div>
                    </div>
                  </td>
                  <td className="num">{c.visitas}</td>
                  <td>{fechaCorta(c.ultimaVisita)}</td>
                  <td className="num">{pesos(c.gastado)}</td>
                  <td className={`num${c.ausencias >= 2 ? " alerta" : ""}`}>{c.ausencias}</td>
                  <td>{c.habitual?.nombre ?? "—"}</td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
        {p && p.total > TAMANO && (
          <div className="paginador">
            <span>{pagina * TAMANO + 1}–{Math.min((pagina + 1) * TAMANO, p.total)} de {p.total}</span>
            <button type="button" className="mini" disabled={pagina === 0} onClick={() => setPagina(pagina - 1)}>Anterior</button>
            <button type="button" className="mini" disabled={(pagina + 1) * TAMANO >= p.total} onClick={() => setPagina(pagina + 1)}>Siguiente</button>
          </div>
        )}
      </section>

      <FichaCliente id={idFicha} alCerrar={() => setIdFicha(null)} />
    </>
  );
}

function FichaCliente({ id, alCerrar }) {
  const ficha = usePedidoAdmin(id ? `/clientes/${id}` : null);
  const f = ficha.datos;
  const c = f?.cliente;
  const wa = c ? `https://wa.me/549${c.telefono.replace(/\D/g, "")}` : "";

  return (
    <Modal abierto={!!id} alCerrar={alCerrar} lateral>
      <EstadoCarga pedido={ficha} />
      {c && c.id === id && (
        <>
          <div className="persona">
            <span className="avatar gris">{iniciales(c)}</span>
            <span><b>{c.nombre} {c.apellido}</b><small>Cliente desde {fechaCorta(c.primeraVisita)}</small></span>
          </div>
          <div className="contacto"><span>{c.telefono}</span><span>{c.email}</span></div>
          <div className="contacto-acciones">
            <a className="btn btn-secundario" href={wa} target="_blank" rel="noreferrer">WhatsApp</a>
            <a className="btn btn-secundario" href={`mailto:${c.email}`}>Email</a>
          </div>
          <div className="ficha-datos">
            <div>Visitas<b>{c.visitas}</b></div>
            <div>Gastado<b>{pesos(c.gastado)}</b></div>
            <div>Ausencias<b className={c.ausencias >= 2 ? "alerta" : undefined}>{c.ausencias}</b></div>
            <div>Satisfacción<b>{f.satisfaccion ? `${decimal(f.satisfaccion)} ★` : "Sin encuestas"}</b></div>
          </div>
          {f.proximoTurno && (
            <div className="cobro-resumen">
              Próximo turno: <b>{fechaLarga(f.proximoTurno.fecha)} a las {f.proximoTurno.horaInicio}</b> ·{" "}
              {f.proximoTurno.servicio.nombre} con {f.proximoTurno.barbero.nombre}
            </div>
          )}
          <div className="historial">
            <h3>Historial</h3>
            {f.historial.map((t) => (
              <div className="hist-item" key={t.id}>
                <span>{t.servicio.nombre} <small>con {t.barbero.nombre}</small></span>
                <ChipEstado estado={t.estado} />
                <small>{fechaLarga(t.fecha)} · {t.horaInicio} · {pesos(t.precio)}</small>
                <small>{t.calificacion ? <span className="notas">{"★".repeat(t.calificacion)}</span> : null}</small>
              </div>
            ))}
          </div>
        </>
      )}
    </Modal>
  );
}
