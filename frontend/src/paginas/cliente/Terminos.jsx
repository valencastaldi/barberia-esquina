import { Link, useNavigate } from "react-router-dom";
import { LayoutCliente, Topbar } from "../../componentes/cliente/LayoutCliente.jsx";
import { BARBERIA, LEGAL } from "../../config.js";

/** Términos y condiciones de uso del sitio y de las reservas (Ley 24.240 de Defensa del Consumidor). */
export default function Terminos() {
  const navegar = useNavigate();
  return (
    <LayoutCliente titulo="Términos y condiciones">
      <Topbar titulo="Términos" alVolver={() => (window.history.state?.idx > 0 ? navegar(-1) : navegar("/"))} />
      <article className="legal">
        <h1>Términos y condiciones</h1>
        <p className="actualizado">Última actualización: {LEGAL.actualizado}</p>

        <p>
          Estos términos explican cómo funcionan las reservas en {BARBERIA.nombre}. Al reservar un turno, aceptás estas
          condiciones y nuestra <Link to="/privacidad">Política de privacidad</Link>.
        </p>

        <h2>Quiénes somos</h2>
        <ul className="datos">
          <li><span>Titular</span>{LEGAL.razonSocial}</li>
          {LEGAL.cuit && <li><span>CUIT</span>{LEGAL.cuit}</li>}
          <li><span>Domicilio</span>{LEGAL.domicilio}</li>
          <li><span>Local</span>{BARBERIA.direccion}</li>
          <li><span>Teléfono</span>{LEGAL.telefono}</li>
          <li><span>Email</span>{LEGAL.emailDatos}</li>
        </ul>

        <h2>Reservas</h2>
        <ul>
          <li>Podés reservar desde el día de hoy hasta {LEGAL.diasAnticipacion} días adelante, en los horarios que el
            sitio muestra como libres.</li>
          <li>El turno queda confirmado cuando ves la pantalla de confirmación. Si dejaste tu email, también te llega
            un mensaje con el detalle y el link para cancelar.</li>
          <li>Si no elegís peluquero, te asignamos uno de los que tengan el horario libre.</li>
          <li>Los datos que cargues tienen que ser reales: los usamos para identificarte y avisarte si hay un cambio.</li>
          <li>Cada turno es para una persona y un servicio. Para varias personas, reservá un turno para cada una.</li>
        </ul>

        <h2>Precios y pago</h2>
        <ul>
          <li>Los precios están en pesos argentinos y son finales, con impuestos incluidos.</li>
          <li>Se respeta el precio que figuraba al momento de reservar, aunque después cambie.</li>
          <li><strong>No se cobra seña ni anticipo.</strong> El servicio se paga en el local, después de la atención,
            en efectivo, por transferencia o con Mercado Pago.</li>
        </ul>

        <h2>Cancelaciones</h2>
        <ul>
          <li>Cancelar no tiene costo.</li>
          <li>Podés cancelar con el link del email de confirmación hasta el horario del turno, siempre que no hayan
            pasado más de {LEGAL.horasCancelacion} horas desde que reservaste. Pasado ese plazo, avisanos por teléfono o
            WhatsApp al {BARBERIA.telefono}.</li>
          <li>Si por algún motivo la barbería tiene que cancelar tu turno, te avisamos por email o teléfono lo antes
            posible para ofrecerte otro horario.</li>
        </ul>

        <h2>Demoras y ausencias</h2>
        <ul>
          <li>Te pedimos llegar a horario. {LEGAL.tolerancia}.</li>
          <li>Si no venís y no cancelaste, el turno queda registrado como ausente.</li>
        </ul>

        <h2>Menores de edad</h2>
        <p>
          Los menores de 18 años pueden atenderse con la autorización de su madre, padre o adulto responsable, que es
          quien debería hacer la reserva con sus propios datos de contacto.
        </p>

        <h2>Encuesta y opiniones</h2>
        <p>
          Después de atenderte te enviamos una encuesta opcional. Los comentarios recientes con 4 o 5 estrellas pueden
          mostrarse en el sitio con tu nombre abreviado, como explica la <Link to="/privacidad">Política de
          privacidad</Link>. Si no querés que el tuyo aparezca, escribinos y lo sacamos.
        </p>

        <h2>Uso del sitio</h2>
        <p>
          No está permitido hacer reservas falsas ni en nombre de otra persona sin su consentimiento. Para evitar abusos,
          el sistema limita la cantidad de reservas que se pueden hacer desde una misma conexión, y la barbería puede
          cancelar turnos que sean claramente falsos.
        </p>
        <p>
          Hacemos lo posible para que el sitio funcione siempre, pero puede haber interrupciones por mantenimiento o
          problemas técnicos. Si no podés reservar, comunicate por teléfono o WhatsApp.
        </p>

        <h2>Reclamos</h2>
        <p>
          Si tenés un problema con un turno o con la atención, escribinos o acercate al local y lo resolvemos. También
          podés hacer un reclamo ante Defensa del Consumidor (Ley 24.240):
          {" "}<a href="https://www.argentina.gob.ar/produccion/defensadelconsumidor" target="_blank" rel="noreferrer">
            www.argentina.gob.ar/produccion/defensadelconsumidor</a>.
        </p>

        <h2>Cambios en estos términos</h2>
        <p>
          Podemos actualizar estos términos. Los cambios valen para las reservas que se hagan después de publicarlos; un
          turno ya reservado mantiene las condiciones con las que se reservó.
        </p>
      </article>
    </LayoutCliente>
  );
}
