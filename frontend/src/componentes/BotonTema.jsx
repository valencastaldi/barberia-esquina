import { useTema } from "../lib/tema.js";
import { IconoLuna, IconoSol } from "./Iconos.jsx";

/**
 * Pasa de claro a oscuro y al revés. Muestra el ícono del tema al que se va a pasar.
 * conTexto: además del ícono dice "Modo oscuro" / "Modo claro" (en el menú del panel).
 */
export function BotonTema({ className = "boton-tema", conTexto = false }) {
  const { tema, alternar } = useTema();
  const aOscuro = tema !== "oscuro";
  const texto = aOscuro ? "Modo oscuro" : "Modo claro";
  return (
    <button type="button" className={className} onClick={alternar}
            aria-label={conTexto ? undefined : (aOscuro ? "Usar modo oscuro" : "Usar modo claro")}
            title={conTexto ? undefined : texto}>
      {aOscuro ? <IconoLuna /> : <IconoSol />}
      {conTexto && texto}
    </button>
  );
}
