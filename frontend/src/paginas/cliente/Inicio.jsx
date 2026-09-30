import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { api } from "../../api/api.js";
import { pedirGuardado, VIGENCIA_CATALOGO } from "../../api/cache.js";
import { usePedido } from "../../api/usePedido.js";
import { BARBERIA } from "../../config.js";
import { decimal } from "../../lib/formato.js";
import { estadoDelLocal, semanaResumida } from "../../lib/horarios.js";
import { BarraCta, EstadoPedido, LayoutCliente } from "../../componentes/cliente/LayoutCliente.jsx";
import { TarjetaServicio } from "../../componentes/cliente/TarjetaServicio.jsx";
import { Logo } from "../../componentes/Logo.jsx";

/** Lo de la home se guarda un rato: "Reservar" usa los mismos servicios sin volver a pedirlos. */
const catalogo = (ruta) => ({ clave: ruta, vigencia: VIGENCIA_CATALOGO });

export default function Inicio() {
  const servicios = usePedido(() => api("/servicios"), [], catalogo("/servicios"));
  const horarios = usePedido(() => api("/horarios"), [], catalogo("/horarios"));
  const resenas = usePedido(() => api("/resenas"), [], catalogo("/resenas"));

  // "Reservar" necesita también a los peluqueros: se piden ya, así el paso 2 aparece completo.
  useEffect(() => {
    pedirGuardado("/barberos", () => api("/barberos"), VIGENCIA_CATALOGO).catch(() => {});
  }, []);

  // Lo que va debajo de los servicios aparece recién cuando llegó todo (y ya no se va):
  // si cada parte apareciera cuando llega su respuesta, empujaría lo que el cliente ya está mirando.
  const todoListo = [servicios, horarios, resenas].every((p) => !p.cargando);
  const [conResto, setConResto] = useState(todoListo);
  if (todoListo && !conResto) setConResto(true);

  const local = horarios.datos ? estadoDelLocal(horarios.datos) : null;

  return (
    <LayoutCliente>
      <header className="hero">
        <Logo tamano={132} className="sello" alt="Escudo de Barbería Esquina 1290" />
        <h1>
          {BARBERIA.nombre}
          <em>{BARBERIA.rotulo}</em>
        </h1>
        <p>Reservá tu turno en 3 pasos. Sin llamadas, sin cuenta, sin esperar respuesta.</p>
        {!horarios.error && (
          // Mientras llega el horario, su lugar queda reservado (invisible) para que no salte nada.
          <div className={`estado-local${local ? "" : " esperando"}`} aria-hidden={local ? undefined : true}>
            <span className={local?.abierto ? "punto-vivo" : "punto-apagado"} aria-hidden="true" />
            <span>{local?.texto ?? "Horario de hoy"}</span>
          </div>
        )}
      </header>

      <section className="seccion">
        <div className="seccion-cab">
          <h2>Servicios</h2>
          <span className="eyebrow">Precios al día</span>
        </div>
        <EstadoPedido cargando={servicios.cargando && !servicios.datos} error={servicios.error}
                      alReintentar={servicios.recargar} />
        {servicios.datos && (
          <div className="servicios">
            {servicios.datos.map((s) => (
              <TarjetaServicio key={s.id} servicio={s} href={`/reservar?servicio=${s.id}`} />
            ))}
          </div>
        )}
      </section>

      {conResto && <Resto resenas={resenas} horarios={horarios} />}

      <BarraCta>
        <Link className="btn btn-primario btn-block" to="/reservar">Reservar turno</Link>
      </BarraCta>
    </LayoutCliente>
  );
}

/** Reseñas, dónde estamos y el pie. */
function Resto({ resenas, horarios }) {
  return (
    <>
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
        <p className="pie-links">
          <Link to="/terminos">Términos y condiciones</Link>
          <Link to="/privacidad">Política de privacidad</Link>
        </p>
        <p style={{ marginTop: 8 }}><Link to="/admin">Acceso del equipo →</Link></p>
      </footer>
    </>
  );
}
