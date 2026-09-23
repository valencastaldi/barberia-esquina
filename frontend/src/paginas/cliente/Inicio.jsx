import { Link } from "react-router-dom";
import { api } from "../../api/api.js";
import { usePedido } from "../../api/usePedido.js";
import { BARBERIA } from "../../config.js";
import { decimal } from "../../lib/formato.js";
import { estadoDelLocal, semanaResumida } from "../../lib/horarios.js";
import { BarraCta, EstadoPedido, LayoutCliente } from "../../componentes/cliente/LayoutCliente.jsx";
import { TarjetaServicio } from "../../componentes/cliente/TarjetaServicio.jsx";

export default function Inicio() {
  const servicios = usePedido(() => api("/servicios"), []);
  const horarios = usePedido(() => api("/horarios"), []);
  const resenas = usePedido(() => api("/resenas"), []);

  const local = horarios.datos ? estadoDelLocal(horarios.datos) : null;

  return (
    <LayoutCliente>
      <header className="hero">
        <img className="sello" src="/logo.jpg" alt="Escudo de Barbería Esquina 1290" />
        <h1>
          {BARBERIA.nombre}
          <em>{BARBERIA.rotulo}</em>
        </h1>
        <p>Reservá tu turno en 3 pasos. Sin llamadas, sin cuenta, sin esperar respuesta.</p>
        {local && (
          <div className="estado-local">
            <span className={local.abierto ? "punto-vivo" : "punto-apagado"} aria-hidden="true" />
            <span>{local.texto}</span>
          </div>
        )}
      </header>

      <section className="seccion">
        <div className="seccion-cab">
          <h2>Servicios</h2>
          <span className="eyebrow">Precios al día</span>
        </div>
        <EstadoPedido {...servicios} alReintentar={servicios.recargar} />
        {servicios.datos && (
          <div className="servicios">
            {servicios.datos.map((s) => (
              <TarjetaServicio key={s.id} servicio={s} href={`/reservar?servicio=${s.id}`} />
            ))}
          </div>
        )}
      </section>

      {resenas.datos?.encuestas > 0 && (
        <section className="seccion">
          <div className="seccion-cab">
            <h2>Lo que dicen</h2>
            <span className="eyebrow">{resenas.datos.encuestas} encuestas</span>
          </div>
          <div className="resumen">
            <div className="fila">
              <span>Satisfacción promedio</span>
              <strong className="texto-ambar">★ {decimal(resenas.datos.promedio)} de 5</strong>
            </div>
            <div className="fila">
              <span>Turnos atendidos este mes</span>
              <strong>{resenas.datos.atendidosMes}</strong>
            </div>
          </div>
          {resenas.datos.comentarios.map((c) => (
            <figure className="cita" key={c.fecha + c.cliente}>
              <blockquote>“{c.comentario}”</blockquote>
              <figcaption>
                <span className="texto-ambar">{"★".repeat(c.calificacion)}</span> {c.cliente}
              </figcaption>
            </figure>
          ))}
        </section>
      )}

      <section className="seccion">
        <div className="seccion-cab"><h2>Dónde estamos</h2></div>
        <div className="local">
          <div><span>Dirección</span><strong>{BARBERIA.direccion}</strong></div>
          {horarios.datos && (
            <div>
              <span>Horarios</span>
              <strong>
                {semanaResumida(horarios.datos).map((linea) => <span className="linea" key={linea}>{linea}</span>)}
              </strong>
            </div>
          )}
          <div>
            <span>Teléfono</span>
            <strong><a href={`https://wa.me/${BARBERIA.whatsapp}`}>{BARBERIA.telefono}</a></strong>
          </div>
        </div>
      </section>

      <footer className="pie">
        <p>{BARBERIA.nombre} · {BARBERIA.rotulo}</p>
      </footer>

      <BarraCta>
        <Link className="btn btn-primario btn-block" to="/reservar">Reservar turno</Link>
      </BarraCta>
    </LayoutCliente>
  );
}
