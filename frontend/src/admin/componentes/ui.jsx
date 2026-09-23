import { useEffect, useRef } from "react";

/* Piezas chicas que se repiten en las pantallas del panel. */

export function Cabecera({ titulo, extension, bajada, children }) {
  return (
    <header className="cabecera">
      <div>
        <h1>{titulo}{extension && <span className="tag-ext">Extensión</span>}</h1>
        {bajada && <p>{bajada}</p>}
      </div>
      {children && <div className="acciones">{children}</div>}
    </header>
  );
}

export function Kpi({ etiqueta, valor, delta, tono, destacado }) {
  return (
    <div className={`kpi${destacado ? " destacado" : ""}`}>
      <div className="etiqueta">{etiqueta}</div>
      <div className="valor">{valor}</div>
      {delta && <div className={`delta${tono ? ` ${tono}` : ""}`}>{delta}</div>}
    </div>
  );
}

/** Botonera tipo "Día · Semana · Mes". opciones: [{ valor, texto }] */
export function Segmentos({ opciones, valor, alCambiar, chico, etiqueta }) {
  return (
    <div className={`segmentos${chico ? " chico" : ""}`} role="group" aria-label={etiqueta}>
      {opciones.map((o) => (
        <button type="button" key={String(o.valor)} aria-pressed={o.valor === valor} onClick={() => alCambiar(o.valor)}>
          {o.texto}
        </button>
      ))}
    </div>
  );
}

export function Interruptor({ activo, alCambiar, textoSi, textoNo, deshabilitado }) {
  return (
    <label className="switch">
      <input type="checkbox" checked={activo} disabled={deshabilitado} onChange={(e) => alCambiar(e.target.checked)} />
      <span className="pista" />
      <span>{activo ? textoSi : textoNo}</span>
    </label>
  );
}

const MEDIOS = { efectivo: "Efectivo", transferencia: "Transferencia", mercadopago: "Mercado Pago" };
export const NOMBRE_MEDIO = MEDIOS;

export function ChipMedio({ medio }) {
  return medio
    ? <span className={`medio medio-${medio}`}>{MEDIOS[medio]}</span>
    : <span className="medio medio-nada">Sin cobrar</span>;
}

const ESTADOS = { pendiente: "Pendiente", completado: "Completado", ausente: "Ausente", cancelado: "Cancelado" };
export function ChipEstado({ estado }) {
  return <span className={`estado estado-${estado}`}>{ESTADOS[estado]}</span>;
}

/** Mensaje de error de una acción, con botón para cerrarlo. */
export function AvisoError({ mensaje, alCerrar }) {
  if (!mensaje) return null;
  return (
    <div className="aviso-panel" role="alert">
      <span>{mensaje}</span>
      {alCerrar && <button type="button" className="cerrar" onClick={alCerrar} aria-label="Cerrar aviso">&times;</button>}
    </div>
  );
}

/** Carga o error de un pedido dentro de un bloque. */
export function EstadoCarga({ pedido, texto = "Cargando…" }) {
  if (pedido.cargando && !pedido.datos) return <div className="vacio" role="status">{texto}</div>;
  if (pedido.error) {
    return (
      <div className="vacio" role="alert">
        {pedido.error.message}{" "}
        <button type="button" className="mini" onClick={pedido.recargar}>Reintentar</button>
      </div>
    );
  }
  return null;
}

/**
 * Ventana modal sobre <dialog>. Se abre cuando `abierto` es true;
 * clic afuera o Escape llaman a alCerrar.
 */
export function Modal({ abierto, alCerrar, titulo, bajada, lateral, children }) {
  const ref = useRef(null);
  useEffect(() => {
    const d = ref.current;
    if (abierto && !d.open) d.showModal();
    if (!abierto && d.open) d.close();
  }, [abierto]);

  return (
    <dialog ref={ref} className={`modal${lateral ? " lado" : ""}`} onCancel={(e) => { e.preventDefault(); alCerrar(); }}
            onClick={(e) => { if (e.target === ref.current) alCerrar(); }}>
      {abierto && (
        <div className="modal-cuerpo">
          <button type="button" className="cerrar" aria-label="Cerrar" onClick={alCerrar}>&times;</button>
          {titulo && <h2>{titulo}</h2>}
          {bajada && <p className="bajada">{bajada}</p>}
          {children}
        </div>
      )}
    </dialog>
  );
}
