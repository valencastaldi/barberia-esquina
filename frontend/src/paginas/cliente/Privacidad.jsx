import { Link, useNavigate } from "react-router-dom";
import { LayoutCliente, Topbar } from "../../componentes/cliente/LayoutCliente.jsx";
import { BARBERIA, LEGAL } from "../../config.js";

/** Política de privacidad (Ley 25.326 de Protección de Datos Personales). */
export default function Privacidad() {
  const navegar = useNavigate();
  return (
    <LayoutCliente titulo="Política de privacidad">
      <Topbar titulo="Privacidad" alVolver={() => (window.history.state?.idx > 0 ? navegar(-1) : navegar("/"))} />
      <article className="legal">
        <h1>Política de privacidad</h1>
        <p className="actualizado">Última actualización: {LEGAL.actualizado}</p>

        <p>
          En {BARBERIA.nombre} usamos tus datos solo para darte el turno y atenderte bien. Acá te contamos qué datos
          guardamos, para qué y cómo podés pedir que los corrijamos o los borremos.
        </p>

        <h2>Quién es el responsable</h2>
        <ul className="datos">
          <li><span>Titular</span>{LEGAL.razonSocial}</li>
          <li><span>CUIT</span>{LEGAL.cuit}</li>
          <li><span>Domicilio</span>{LEGAL.domicilio}</li>
          <li><span>Contacto</span>{LEGAL.emailDatos}</li>
        </ul>

        <h2>Qué datos guardamos</h2>
        <ul>
          <li><strong>Tus datos de contacto:</strong> nombre, apellido, email y teléfono. El email es opcional si el
            turno lo carga la barbería (por teléfono o en el local).</li>
          <li><strong>Tus turnos:</strong> servicio, peluquero, día, hora, precio y si asististe o cancelaste.</li>
          <li><strong>Los cobros:</strong> monto y medio de pago (efectivo, transferencia o Mercado Pago). El pago se
            hace en el local: el sitio no pide ni guarda datos de tarjetas ni cuentas.</li>
          <li><strong>La encuesta:</strong> la calificación y el comentario, si decidís responderla.</li>
        </ul>

        <h2>Para qué los usamos</h2>
        <ul>
          <li>Reservar, confirmar y cancelar tus turnos.</li>
          <li>Mandarte por email la confirmación con el link para cancelar, el aviso si un turno se cancela y, después
            de atenderte, la encuesta.</li>
          <li>Reconocerte cuando volvés a reservar con el mismo email.</li>
          <li>Llevar la agenda y la caja del local, y armar estadísticas internas (turnos, ausencias, satisfacción).</li>
        </ul>
        <p>No usamos tus datos para publicidad y no los vendemos ni los cedemos a nadie.</p>

        <h2>Tus opiniones en el sitio</h2>
        <p>
          Los comentarios recientes de la encuesta con 4 o 5 estrellas pueden mostrarse en la página de inicio, junto
          con la calificación y tu nombre abreviado (por ejemplo, "Mateo G."). Nunca se publican tu apellido completo, tu email ni tu teléfono. Si no
          querés que tu comentario aparezca, escribinos y lo sacamos.
        </p>

        <h2>¿Es obligatorio darlos?</h2>
        <p>
          Nombre, apellido y teléfono son necesarios para reservar: sin ellos no podemos darte el turno ni avisarte si
          hay un cambio. Para reservar desde el sitio también pedimos el email, porque ahí te llega la confirmación y
          el link para cancelar. Si preferís no darlo, podés sacar el turno llamando o en el local.
        </p>

        <h2>Quién más accede a tus datos</h2>
        <ul>
          <li><strong>El equipo de la barbería.</strong> Cada peluquero ve el contacto solo de sus propios clientes;
            el dueño ve todo.</li>
          <li><strong>El proveedor de email</strong> (actualmente Google, con Gmail), que envía los mensajes.</li>
          <li><strong>El proveedor de hosting</strong>, donde funciona el sistema y se guarda la base de datos.</li>
        </ul>
        <p>Estos proveedores solo procesan los datos para prestar su servicio. Fuera de eso, únicamente los
          entregaríamos si lo ordena una autoridad competente.</p>

        <h2>Lo que queda en tu navegador</h2>
        <p>
          Para que no tengas que escribirlos otra vez, tu navegador recuerda el nombre, el apellido, el email y el
          teléfono de tu última reserva. Quedan solo en tu dispositivo: no nos llegan por esa vía. Si usás una
          computadora compartida, podés borrarlos limpiando los datos del sitio en tu navegador.
        </p>
        <p>El sitio no usa cookies de publicidad ni herramientas de seguimiento o analítica.</p>

        <h2>Cuánto tiempo los guardamos</h2>
        <p>
          Mientras seas cliente, y hasta {LEGAL.conservacion} desde tu último turno. Después los borramos o los
          anonimizamos: conservamos solo los números del turno (servicio, fecha y monto), sin nada que te identifique,
          para las cuentas del local.
        </p>

        <h2>Cómo los cuidamos</h2>
        <p>
          El sitio funciona con conexión cifrada (HTTPS). El panel de la barbería requiere usuario y contraseña, las
          contraseñas se guardan cifradas y cada persona del equipo accede solo a lo que necesita para trabajar.
        </p>

        <h2>Tus derechos</h2>
        <p>
          Podés pedirnos en cualquier momento que te digamos qué datos tuyos tenemos (acceso), que los corrijamos o
          actualicemos (rectificación) o que los borremos (supresión). Escribinos a <strong>{LEGAL.emailDatos}</strong> desde
          el email con el que reservaste o acercate al local. Respondemos los pedidos de acceso dentro de los 10 días
          corridos y los de corrección o borrado dentro de los 5 días hábiles, como indica la Ley 25.326.
        </p>
        <p>
          Si borrás tus datos, los turnos que tengas pendientes se cancelan, porque ya no podríamos avisarte ni
          identificarte al llegar.
        </p>

        <h2>Cambios en esta política</h2>
        <p>
          Si cambiamos algo importante, lo vas a ver en esta página con una nueva fecha de actualización antes de tu
          próxima reserva.
        </p>

        <div className="aviso-legal">
          <p>
            El titular de los datos personales tiene la facultad de ejercer el derecho de acceso a los mismos en forma
            gratuita a intervalos no inferiores a seis meses, salvo que se acredite un interés legítimo al efecto
            conforme lo establecido en el artículo 14, inciso 3 de la Ley Nº 25.326.
          </p>
          <p>
            La AGENCIA DE ACCESO A LA INFORMACIÓN PÚBLICA, en su carácter de Órgano de Control de la Ley Nº 25.326,
            tiene la atribución de atender las denuncias y reclamos que interpongan quienes resulten afectados en sus
            derechos por incumplimiento de las normas vigentes en materia de protección de datos personales.
          </p>
          <p><a href="https://www.argentina.gob.ar/aaip" target="_blank" rel="noreferrer">www.argentina.gob.ar/aaip</a></p>
        </div>

        <p className="relacionado">Ver también los <Link to="/terminos">Términos y condiciones</Link>.</p>
      </article>
    </LayoutCliente>
  );
}
