import { Link } from "react-router-dom";
import { BarraCta, LayoutCliente, Topbar } from "../../componentes/cliente/LayoutCliente.jsx";
import { IconoPregunta } from "../../componentes/Iconos.jsx";

export default function NoEncontrada() {
  return (
    <LayoutCliente titulo="Página no encontrada">
      <Topbar />
      <section className="resultado">
        <div className="icono neutro"><IconoPregunta /></div>
        <h1>Esta página no existe</h1>
        <p>Puede que el link esté incompleto.</p>
      </section>
      <BarraCta>
        <Link className="btn btn-primario btn-block" to="/">Ir al inicio</Link>
      </BarraCta>
    </LayoutCliente>
  );
}
