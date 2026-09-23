import { useState } from "react";
import { Link, useParams, useSearchParams } from "react-router-dom";
import { api, ErrorApi } from "../../api/api.js";
import { usePedido } from "../../api/usePedido.js";
import { fechaLarga } from "../../lib/formato.js";
import { BarraCta, EstadoPedido, LayoutCliente, Topbar } from "../../componentes/cliente/LayoutCliente.jsx";
import { IconoEstrella, IconoOk } from "../../componentes/Iconos.jsx";

const LEYENDAS = {
  1: "Muy malo — contanos qué pasó",
  2: "Podría estar mejor",
  3: "Estuvo bien",
  4: "Muy bueno",
  5: "Excelente, volvería",
};

/** RF-15 a RF-17: encuesta de satisfacción desde el link del email. */
export default function Encuesta() {
  const { idTurno } = useParams();
  const [params] = useSearchParams();
  const token = params.get("token") ?? "";
  const ruta = `/encuestas/${encodeURIComponent(idTurno)}`;

  const info = usePedido(() => api(ruta, { params: { token } }), [idTurno, token]);
  const [calificacion, setCalificacion] = useState(0);
  const [comentario, setComentario] = useState("");
  const [enviando, setEnviando] = useState(false);
  const [falla, setFalla] = useState(null);
  const [respondida, setRespondida] = useState(false);

  async function enviar() {
    setEnviando(true);
    setFalla(null);
    try {
      await api(ruta, { metodo: "POST", params: { token }, cuerpo: { calificacion, comentario: comentario.trim() || null } });
      setRespondida(true);
      window.scrollTo({ top: 0, behavior: "smooth" });
    } catch (error) {
      if (error instanceof ErrorApi && error.status === 409) setRespondida(true);
      else setFalla(error.message);
    } finally {
      setEnviando(false);
    }
  }

  if (info.cargando || info.error) {
    return (
      <LayoutCliente titulo="Encuesta">
        <Topbar />
        <div style={{ marginTop: 32 }}>
          <EstadoPedido cargando={info.cargando}
                        error={info.error?.status === 404 ? new Error("Este link no es válido o la encuesta ya no está disponible.") : info.error}
                        alReintentar={info.error?.status === 404 ? null : info.recargar} />
        </div>
      </LayoutCliente>
    );
  }

  if (respondida || info.datos.respondida) {
    return (
      <LayoutCliente titulo="¡Gracias!">
        <Topbar />
        <section className="resultado">
          <div className="icono"><IconoOk tamano={36} /></div>
          <h1>¡Gracias!</h1>
          <p>Tu opinión nos ayuda a atenderte cada vez mejor.</p>
        </section>
        <BarraCta>
          <Link className="btn btn-primario btn-block" to="/reservar">Reservar el próximo</Link>
        </BarraCta>
      </LayoutCliente>
    );
  }

  const t = info.datos;
  return (
    <LayoutCliente titulo="¿Cómo estuvo?">
      <Topbar />
      <section className="resultado" style={{ paddingBottom: 0 }}>
        <h1 style={{ marginTop: 12 }}>¿Cómo estuvo, {t.cliente}?</h1>
        <p>{t.servicio} con {t.barbero}, el {fechaLarga(t.fecha)}.</p>
      </section>

      <div className="estrellas" role="radiogroup" aria-label="Calificación de 1 a 5">
        {[1, 2, 3, 4, 5].map((n) => (
          <button type="button" key={n} role="radio" aria-checked={n === calificacion} aria-label={`${n} de 5`}
                  className={`estrella${n <= calificacion ? " marcada" : ""}`} onClick={() => setCalificacion(n)}>
            <IconoEstrella />
          </button>
        ))}
      </div>
      <p className="leyenda-estrellas" aria-live="polite">
        {LEYENDAS[calificacion] ?? "Tocá las estrellas para calificar"}
      </p>

      <label className="campo" style={{ marginTop: 26 }}>
        <span>Comentario <em className="opcional">(opcional)</em></span>
        <textarea maxLength={500} placeholder="¿Qué salió bien? ¿Qué mejorarías?"
                  value={comentario} onChange={(e) => setComentario(e.target.value)} />
      </label>
      <p className="nota-maqueta">Lo lee solo la barbería. Máximo 500 caracteres ({500 - comentario.length} disponibles).</p>

      {falla && <div className="aviso-error" role="alert">{falla}</div>}

      <BarraCta>
        <button className="btn btn-primario btn-block" disabled={!calificacion || enviando} onClick={enviar}>
          {enviando ? "Enviando…" : "Enviar"}
        </button>
      </BarraCta>
    </LayoutCliente>
  );
}
