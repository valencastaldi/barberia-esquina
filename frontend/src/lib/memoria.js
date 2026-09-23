/*
 * Comodidades guardadas en el navegador del cliente. Todo con try/catch:
 * en modo incógnito o con el almacenamiento bloqueado simplemente no se recuerda.
 */

const CLAVE_CLIENTE = "esquina.cliente";
const CLAVE_RESERVA = "esquina.ultimaReserva";

function leer(almacen, clave) {
  try {
    return JSON.parse(almacen.getItem(clave)) ?? null;
  } catch {
    return null;
  }
}

function guardar(almacen, clave, valor) {
  try {
    almacen.setItem(clave, JSON.stringify(valor));
  } catch {
    /* sin almacenamiento: no pasa nada */
  }
}

/** Nombre, email y teléfono de la última reserva, para no tipearlos otra vez. */
export const clienteRecordado = () => leer(localStorage, CLAVE_CLIENTE) ?? {};
export const recordarCliente = (cliente) => guardar(localStorage, CLAVE_CLIENTE, cliente);

/** La reserva recién hecha, para que la pantalla de confirmación sobreviva a un refresco. */
export const ultimaReserva = () => leer(sessionStorage, CLAVE_RESERVA);
export const recordarReserva = (reserva) => guardar(sessionStorage, CLAVE_RESERVA, reserva);
