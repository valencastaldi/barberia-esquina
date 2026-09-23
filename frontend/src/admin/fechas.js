import { DIAS_CORTOS, MESES, aFecha, aIso } from "../lib/formato.js";

export const hoyIso = () => aIso(new Date());

export function sumarDias(iso, n) {
  const f = aFecha(iso);
  f.setDate(f.getDate() + n);
  return aIso(f);
}

/** Rango [desde, hasta] del período que contiene a la fecha. La semana va de lunes a domingo. */
export function rango(iso, periodo) {
  const f = aFecha(iso);
  if (periodo === "semana") {
    const desdeLunes = (f.getDay() + 6) % 7;
    const desde = sumarDias(iso, -desdeLunes);
    return [desde, sumarDias(desde, 6)];
  }
  if (periodo === "mes") {
    return [aIso(new Date(f.getFullYear(), f.getMonth(), 1)), aIso(new Date(f.getFullYear(), f.getMonth() + 1, 0))];
  }
  return [iso, iso];
}

/** Mueve la fecha un período para adelante o para atrás. */
export function moverPeriodo(iso, periodo, sentido) {
  if (periodo === "mes") {
    const f = aFecha(iso);
    return aIso(new Date(f.getFullYear(), f.getMonth() + sentido, 1));
  }
  return sumarDias(iso, sentido * (periodo === "semana" ? 7 : 1));
}

/** "Mar 23 de septiembre", "22 al 28 de septiembre", "Septiembre 2026" */
export function tituloPeriodo(iso, periodo) {
  const [desde, hasta] = rango(iso, periodo);
  const d = aFecha(desde), h = aFecha(hasta);
  if (periodo === "mes") return `${MESES[d.getMonth()][0].toUpperCase()}${MESES[d.getMonth()].slice(1)} ${d.getFullYear()}`;
  if (periodo === "semana") {
    return d.getMonth() === h.getMonth()
      ? `${d.getDate()} al ${h.getDate()} de ${MESES[h.getMonth()]}`
      : `${d.getDate()} de ${MESES[d.getMonth()]} al ${h.getDate()} de ${MESES[h.getMonth()]}`;
  }
  return `${DIAS_CORTOS[d.getDay()]} ${d.getDate()} de ${MESES[d.getMonth()]}`;
}

/** "hace 5 min", "hace 3 h", "ayer", "hace 4 días", "12/09" */
export function haceCuanto(fechaHora) {
  const min = Math.round((Date.now() - new Date(fechaHora).getTime()) / 60000);
  if (min < 60) return `hace ${Math.max(min, 1)} min`;
  if (min < 24 * 60) return `hace ${Math.round(min / 60)} h`;
  const dias = Math.round(min / 1440);
  if (dias === 1) return "ayer";
  if (dias < 7) return `hace ${dias} días`;
  const f = new Date(fechaHora);
  return `${String(f.getDate()).padStart(2, "0")}/${String(f.getMonth() + 1).padStart(2, "0")}`;
}

export const porcentaje = (x) => `${(x * 100).toFixed(1).replace(".", ",").replace(",0", "")}%`;
