import { useEffect } from "react";
import { Route, Routes, useLocation } from "react-router-dom";
import Inicio from "./paginas/cliente/Inicio.jsx";
import Reservar from "./paginas/cliente/Reservar.jsx";
import Confirmacion from "./paginas/cliente/Confirmacion.jsx";
import Cancelar from "./paginas/cliente/Cancelar.jsx";
import Encuesta from "./paginas/cliente/Encuesta.jsx";
import NoEncontrada from "./paginas/cliente/NoEncontrada.jsx";

/**
 * Rutas del sitio del cliente. Las de /cancelar y /encuesta son las que
 * llegan por email (el backend las arma con app.frontend-url).
 */
export default function App() {
  // Cada pantalla nueva arranca desde arriba (si no, conserva el scroll de la anterior).
  const { pathname } = useLocation();
  useEffect(() => {
    window.scrollTo(0, 0);
  }, [pathname]);

  return (
    <Routes>
      <Route path="/" element={<Inicio />} />
      <Route path="/reservar" element={<Reservar />} />
      <Route path="/turno/confirmado" element={<Confirmacion />} />
      <Route path="/cancelar/:token" element={<Cancelar />} />
      <Route path="/encuesta/:idTurno" element={<Encuesta />} />
      <Route path="*" element={<NoEncontrada />} />
    </Routes>
  );
}
