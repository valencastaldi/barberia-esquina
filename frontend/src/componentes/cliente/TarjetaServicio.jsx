import { Link } from "react-router-dom";
import { pesos } from "../../lib/formato.js";

function Contenido({ servicio }) {
  return (
    <>
      <h3>{servicio.nombre}</h3>
      <div className="precio">{pesos(servicio.precio)}</div>
      <p className="detalle">{servicio.descripcion}</p>
      <div className="duracion">{servicio.duracionMinutos} min</div>
    </>
  );
}

/** En la home es un link al flujo de reserva; en el paso 1 es un botón seleccionable. */
export function TarjetaServicio({ servicio, elegido, alElegir, href }) {
  if (href) {
    return (
      <Link className="servicio" to={href}>
        <Contenido servicio={servicio} />
      </Link>
    );
  }
  return (
    <button type="button" className="servicio" aria-pressed={elegido} onClick={() => alElegir(servicio)}>
      <Contenido servicio={servicio} />
    </button>
  );
}
