import { useEffect } from "react";
import { createPortal } from "react-dom";
import { Link } from "react-router-dom";
import { IconoVolver } from "../Iconos.jsx";
import { BARBERIA } from "../../config.js";

/** Columna mobile-first del sitio del cliente (máx. 430px, centrada en escritorio). */
export function LayoutCliente({ titulo, children }) {
  useEffect(() => {
    document.body.className = "cliente";
    document.title = titulo ? `${titulo} — ${BARBERIA.nombre}` : `${BARBERIA.nombre} — Reservá tu turno`;
  }, [titulo]);

  return <main className="app">{children}</main>;
}

/** Barra superior: con botón de volver, o con la marca que lleva al inicio. */
export function Topbar({ titulo, alVolver }) {
  if (alVolver) {
    return (
      <header className="topbar">
        <button className="volver" onClick={alVolver} aria-label="Volver">
          <IconoVolver />
        </button>
        <span className="titulo">{titulo}</span>
      </header>
    );
  }
  return (
    <header className="topbar">
      <Link className="marca" to="/">
        <img src="/logo.jpg" alt="" />
        <strong>{BARBERIA.nombre}</strong>
      </Link>
    </header>
  );
}

/**
 * Barra de acción fija abajo, como en una app. Se monta directo en <body>:
 * si quedara dentro de una sección animada, el transform de la animación
 * haría que "fixed" se ubique respecto de la sección y no de la pantalla.
 */
export function BarraCta({ children }) {
  return createPortal(
    <div className="barra-cta">
      <div>{children}</div>
    </div>,
    document.body
  );
}

/** Resumen de un turno en filas etiqueta / valor. */
export function Resumen({ filas, total }) {
  return (
    <div className="resumen">
      {filas.map(([etiqueta, valor]) => (
        <div className="fila" key={etiqueta}>
          <span>{etiqueta}</span>
          <strong>{valor}</strong>
        </div>
      ))}
      {total && (
        <div className="fila total">
          <span>{total[0]}</span>
          <strong>{total[1]}</strong>
        </div>
      )}
    </div>
  );
}

/** Estado de carga o de error de un pedido, con reintento. */
export function EstadoPedido({ cargando, error, alReintentar, texto = "Cargando…" }) {
  if (cargando) return <div className="vacio cargando" role="status">{texto}</div>;
  if (error) {
    return (
      <div className="vacio" role="alert">
        {error.message}
        {alReintentar && (
          <>
            <br />
            <button className="enlace" onClick={alReintentar}>Reintentar</button>
          </>
        )}
      </div>
    );
  }
  return null;
}
