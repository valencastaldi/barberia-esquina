import { api } from "./api.js";
import { guardar, olvidar, pedirGuardado, VIGENCIA_DISPONIBILIDAD } from "./cache.js";

/*
 * Pedidos del paso 2 de la reserva. Las claves llevan el servicio y el peluquero
 * (vacío = cualquiera) para que lo guardado de una combinación no se mezcle con otra.
 */

export const claveDias = (idServicio, idBarbero) => `disponibilidad/dias/${idServicio}/${idBarbero ?? ""}`;
export const claveDia = (idServicio, idBarbero, fecha) => `disponibilidad/dia/${idServicio}/${idBarbero ?? ""}/${fecha}`;

/**
 * Días con lugar (GET /disponibilidad/dias). El primero con lugar viene con sus horarios:
 * se guardan como la respuesta de GET /disponibilidad de ese día, que así no hace falta pedir.
 */
export function pedirDias(idServicio, idBarbero) {
  return api("/disponibilidad/dias", { params: { servicio: idServicio, barbero: idBarbero, cantidad: 14 } })
    .then((dias) => {
      const primero = dias.find((d) => d.slots);
      if (primero) guardar(claveDia(idServicio, idBarbero, primero.fecha), { fecha: primero.fecha, slots: primero.slots });
      return dias;
    });
}

/** Horarios de un día (GET /disponibilidad): los ocupados vienen tachados. */
export function pedirDia(idServicio, idBarbero, fecha) {
  return api("/disponibilidad", { params: { servicio: idServicio, fecha, barbero: idBarbero } });
}

/** Arranca el pedido de días antes de mostrar el paso 2: al elegir el servicio, o si ya viene en la URL. */
export function adelantarDias(idServicio) {
  pedirGuardado(claveDias(idServicio, null), () => pedirDias(idServicio, null), VIGENCIA_DISPONIBILIDAD)
    .catch(() => {});   // si falla, el paso 2 lo pide de nuevo y muestra el error
}

/** Después de reservar o cancelar, o si un horario se ocupó: lo guardado ya no sirve. */
export const olvidarDisponibilidad = () => olvidar("disponibilidad/");
