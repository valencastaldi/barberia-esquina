import { useEffect } from "react";
import { Navigate, Outlet, Route, Routes } from "react-router-dom";
import "../estilos/admin.css";
import "../estilos/panel.css";
import { RequiereSesion, SesionProvider, SoloDueno } from "./sesion.jsx";
import { Lateral } from "./componentes/Lateral.jsx";
import Login from "./paginas/Login.jsx";
import Agenda from "./paginas/Agenda.jsx";
import Pagos from "./paginas/Pagos.jsx";
import Clientes from "./paginas/Clientes.jsx";
import Dashboard from "./paginas/Dashboard.jsx";
import Peluqueros from "./paginas/Peluqueros.jsx";
import Servicios from "./paginas/Servicios.jsx";
import Horarios from "./paginas/Horarios.jsx";

/**
 * Panel de gestión (escritorio, desde 1024px — RNF-10). Se carga aparte del
 * sitio del cliente: quien reserva desde el celular no descarga este código.
 */
export default function AdminApp() {
  return (
    <SesionProvider>
      <Routes>
        <Route path="login" element={<Login />} />
        <Route element={<RequiereSesion><Layout /></RequiereSesion>}>
          <Route index element={<Navigate to="agenda" replace />} />
          <Route path="agenda" element={<Agenda />} />
          <Route path="pagos" element={<SoloDueno><Pagos /></SoloDueno>} />
          <Route path="clientes" element={<SoloDueno><Clientes /></SoloDueno>} />
          <Route path="dashboard" element={<SoloDueno><Dashboard /></SoloDueno>} />
          <Route path="peluqueros" element={<SoloDueno><Peluqueros /></SoloDueno>} />
          <Route path="servicios" element={<SoloDueno><Servicios /></SoloDueno>} />
          <Route path="horarios" element={<Horarios />} />
          <Route path="*" element={<Navigate to="agenda" replace />} />
        </Route>
      </Routes>
    </SesionProvider>
  );
}

function Layout() {
  useEffect(() => {
    document.body.className = "admin";
    document.title = "Panel — Barbería Esquina";
  }, []);
  return (
    <div className="panel">
      <Lateral />
      <main className="contenido">
        <Outlet />
      </main>
    </div>
  );
}
