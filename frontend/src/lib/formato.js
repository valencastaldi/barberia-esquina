export const DIAS_CORTOS = ["Dom", "Lun", "Mar", "Mié", "Jue", "Vie", "Sáb"];
export const DIAS = ["domingo", "lunes", "martes", "miércoles", "jueves", "viernes", "sábado"];
export const MESES = ["enero", "febrero", "marzo", "abril", "mayo", "junio", "julio", "agosto",
  "septiembre", "octubre", "noviembre", "diciembre"];

export function pesos(n) {
  return "$" + Math.round(Number(n)).toLocaleString("es-AR");
}

/** "2026-09-24" → Date local (sin corrimiento por zona horaria). */
export function aFecha(iso) {
  const [a, m, d] = iso.split("-").map(Number);
  return new Date(a, m - 1, d);
}

/** Date → "2026-09-24" en hora local. */
export function aIso(fecha) {
  const m = String(fecha.getMonth() + 1).padStart(2, "0");
  const d = String(fecha.getDate()).padStart(2, "0");
  return `${fecha.getFullYear()}-${m}-${d}`;
}

/** "jue 24 de septiembre" */
export function fechaLarga(iso) {
  const f = aFecha(iso);
  return `${DIAS_CORTOS[f.getDay()].toLowerCase()} ${f.getDate()} de ${MESES[f.getMonth()]}`;
}

/** "10:00" → "10", "10:30" → "10:30" */
export function horaCorta(hhmm) {
  return hhmm.endsWith(":00") ? String(Number(hhmm.slice(0, 2))) : hhmm;
}

export function minutosDelDia(hhmm) {
  const [h, m] = hhmm.split(":").map(Number);
  return h * 60 + m;
}

export function sumarMinutos(hhmm, minutos) {
  const t = minutosDelDia(hhmm) + minutos;
  return `${String(Math.floor(t / 60) % 24).padStart(2, "0")}:${String(t % 60).padStart(2, "0")}`;
}

/** 4.47 → "4,5" */
export function decimal(n) {
  return n.toFixed(1).replace(".", ",");
}
