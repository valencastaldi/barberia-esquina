import { DIAS, DIAS_CORTOS, horaCorta, minutosDelDia } from "./formato.js";

/**
 * A partir del horario de la barbería (GET /horarios, 7 días) arma el texto
 * del estado: "Abierto hoy hasta las 20:00", "Abre hoy a las 10:00",
 * "Cerrado · abrimos el lunes a las 10:00".
 */
export function estadoDelLocal(semana, ahora = new Date()) {
  const porDia = Object.fromEntries(semana.map((d) => [d.diaSemana, d]));
  const hoy = porDia[ahora.getDay()];
  const minutos = ahora.getHours() * 60 + ahora.getMinutes();

  if (hoy?.activo) {
    if (minutos < minutosDelDia(hoy.horaInicio)) {
      return { abierto: false, texto: `Abre hoy a las ${hoy.horaInicio}` };
    }
    if (minutos < minutosDelDia(hoy.horaFin)) {
      return { abierto: true, texto: `Abierto hoy hasta las ${hoy.horaFin}` };
    }
  }
  for (let i = 1; i <= 7; i++) {
    const dia = porDia[(ahora.getDay() + i) % 7];
    if (dia?.activo) {
      const cuando = i === 1 ? "mañana" : `el ${DIAS[dia.diaSemana]}`;
      return { abierto: false, texto: `Cerrado · abrimos ${cuando} a las ${dia.horaInicio}` };
    }
  }
  return { abierto: false, texto: "Cerrado" };
}

/**
 * Agrupa días seguidos con el mismo horario:
 * ["Lun a Mié 10 a 20", "Jue y Vie 10 a 21", "Sáb 9 a 18", "Dom cerrado"]
 */
export function semanaResumida(semana) {
  const orden = [1, 2, 3, 4, 5, 6, 0].map((d) => semana.find((x) => x.diaSemana === d));
  const clave = (d) => (d?.activo ? `${horaCorta(d.horaInicio)} a ${horaCorta(d.horaFin)}` : "cerrado");

  const grupos = [];
  for (const dia of orden) {
    const ultimo = grupos[grupos.length - 1];
    if (ultimo && ultimo.clave === clave(dia)) ultimo.dias.push(dia.diaSemana);
    else grupos.push({ clave: clave(dia), dias: [dia.diaSemana] });
  }
  return grupos.map(({ clave, dias }) => {
    const primero = DIAS_CORTOS[dias[0]];
    const ultimo = DIAS_CORTOS[dias[dias.length - 1]];
    const nombre = dias.length === 1 ? primero : dias.length === 2 ? `${primero} y ${ultimo}` : `${primero} a ${ultimo}`;
    return `${nombre} ${clave}`;
  });
}
