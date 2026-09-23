import { Link, NavLink, useNavigate } from "react-router-dom";
import { useSesion } from "../sesion.jsx";

const ICONOS = {
  agenda: "M8 2v4M16 2v4M3 10h18M5 4h14a2 2 0 012 2v14a2 2 0 01-2 2H5a2 2 0 01-2-2V6a2 2 0 012-2z",
  dashboard: "M3 3v18h18M7 15v3M12 10v8M17 6v12",
  servicios: "M6 3v12M18 3v12M6 15a3 3 0 100 6 3 3 0 000-6zM18 15a3 3 0 100 6 3 3 0 000-6z",
  horarios: "M12 3a9 9 0 100 18 9 9 0 000-18zM12 7v5l3.2 1.9",
  pagos: "M4 5h16a2 2 0 012 2v10a2 2 0 01-2 2H4a2 2 0 01-2-2V7a2 2 0 012-2zM2 10h20M6 15h4",
  clientes: "M16 21v-2a4 4 0 00-4-4H6a4 4 0 00-4 4v2M9 11a4 4 0 100-8 4 4 0 000 8zM22 21v-2a4 4 0 00-3-3.87M16 3.13a4 4 0 010 7.75",
  peluqueros: "M12 3a4 4 0 100 8 4 4 0 000-8zM5.5 21a6.5 6.5 0 0113 0M9 14.5l3 3 3-3",
  salir: "M9 21H5a2 2 0 01-2-2V5a2 2 0 012-2h4M16 17l5-5-5-5M21 12H9",
};

export function Icono({ nombre }) {
  return (
    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8"
         strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
      <path d={ICONOS[nombre]} />
    </svg>
  );
}

const GRUPOS = [
  { titulo: "Operación", secciones: [
    { id: "agenda", txt: "Agenda" },
    { id: "pagos", txt: "Pagos", soloDueno: true },
    { id: "clientes", txt: "Clientes" },
  ] },
  { titulo: "Negocio", secciones: [
    { id: "dashboard", txt: "Dashboard" },
    { id: "peluqueros", txt: "Peluqueros" },
    { id: "servicios", txt: "Servicios" },
    { id: "horarios", txt: "Horarios" },
  ] },
];

export function Lateral() {
  const { usuario, esDueno, cerrar, pedir } = useSesion();
  const navegar = useNavigate();

  async function salir() {
    await pedir("/auth/logout", { metodo: "POST" }).catch(() => {});
    cerrar();
    navegar("/admin/login", { replace: true });
  }

  return (
    <aside className="lateral">
      <Link className="marca" to="/">
        <img src="/logo.jpg" alt="" />
        <span><b>Barbería Esquina</b><small>Panel de gestión</small></span>
      </Link>
      <nav className="nav">
        {GRUPOS.map((g) => (
          <div key={g.titulo} style={{ display: "contents" }}>
            <span className="nav-grupo">{g.titulo}</span>
            {g.secciones.filter((s) => !s.soloDueno || esDueno).map((s) => (
              <NavLink key={s.id} to={`/admin/${s.id}`} className={({ isActive }) => (isActive ? "activo" : undefined)}>
                <Icono nombre={s.id} />{s.txt}
              </NavLink>
            ))}
          </div>
        ))}
      </nav>
      <div className="abajo">
        <div className="usuario">
          <span className="avatar">{usuario.nombre[0]}{usuario.apellido[0]}</span>
          <span>
            <b>{usuario.nombre} {usuario.apellido}</b>
            <small>{esDueno ? "Dueño · Super admin" : "Barbero"}</small>
          </span>
        </div>
        <nav className="nav">
          <button type="button" className="nav-boton" onClick={salir}><Icono nombre="salir" />Cerrar sesión</button>
        </nav>
      </div>
    </aside>
  );
}
