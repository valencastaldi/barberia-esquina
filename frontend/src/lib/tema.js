import { useEffect, useState } from "react";

/*
 * Tema claro / oscuro. El script de index.html ya dejó <html data-tema="…"> puesto antes
 * de pintar (si no, la página aparecería un instante en el tema equivocado). Acá solo se
 * cambia y se recuerda. Mientras la persona no elija, se sigue al tema del sistema.
 */

const CLAVE = "necks.tema";
const sistema = window.matchMedia("(prefers-color-scheme: dark)");

function aplicar(tema) {
  document.documentElement.dataset.tema = tema;
  document.querySelector('meta[name="theme-color"]')
    ?.setAttribute("content", tema === "oscuro" ? "#0F0E0D" : "#F6F3EE");
}

function elegido() {
  try {
    return localStorage.getItem(CLAVE);
  } catch {
    return null;
  }
}

export function useTema() {
  const [tema, setTema] = useState(() => document.documentElement.dataset.tema ?? "claro");

  // Sin elección propia, si el sistema cambia de tema (p. ej. al anochecer) la página lo acompaña.
  useEffect(() => {
    const alCambiar = (e) => {
      if (elegido()) return;
      const nuevo = e.matches ? "oscuro" : "claro";
      aplicar(nuevo);
      setTema(nuevo);
    };
    sistema.addEventListener("change", alCambiar);
    return () => sistema.removeEventListener("change", alCambiar);
  }, []);

  function alternar() {
    const nuevo = tema === "oscuro" ? "claro" : "oscuro";
    aplicar(nuevo);
    setTema(nuevo);
    try {
      localStorage.setItem(CLAVE, nuevo);
    } catch {
      /* sin almacenamiento: vale para esta visita */
    }
  }

  return { tema, alternar };
}
