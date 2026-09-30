/*
 * Respuestas recientes de la API, en memoria (se pierden al recargar la página).
 * Sirven para que una pantalla use lo que otra acaba de traer (la home trae los
 * servicios y "Reservar" los muestra sin volver a pedirlos) y para arrancar un
 * pedido antes de que exista la pantalla que lo muestra. Si dos pantallas piden
 * lo mismo a la vez, sale un solo pedido.
 */

/** Servicios, peluqueros y horario del local: cambian muy de vez en cuando. */
export const VIGENCIA_CATALOGO = 5 * 60_000;

/** Horarios libres: cambian con cada reserva, así que lo guardado vale poco. */
export const VIGENCIA_DISPONIBILIDAD = 30_000;

const guardadas = new Map();   // clave → { promesa, datos?, hora }

/** Los datos de esa clave si llegaron hace menos de `vigencia` ms; si no, undefined. */
export function leerGuardado(clave, vigencia) {
  const g = guardadas.get(clave);
  return g && "datos" in g && Date.now() - g.hora < vigencia ? g.datos : undefined;
}

/** La respuesta guardada (o el pedido en curso) si es reciente; si no, la pide de nuevo. */
export function pedirGuardado(clave, pedir, vigencia) {
  const g = guardadas.get(clave);
  if (g && Date.now() - g.hora < vigencia) return g.promesa;

  const entrada = { hora: Date.now() };
  entrada.promesa = Promise.resolve(pedir()).then(
    (datos) => {
      entrada.datos = datos;
      return datos;
    },
    (error) => {
      if (guardadas.get(clave) === entrada) guardadas.delete(clave);   // un error no se guarda
      throw error;
    }
  );
  guardadas.set(clave, entrada);
  return entrada.promesa;
}

/** Guarda algo que llegó por otro lado (los horarios del primer día que trae /disponibilidad/dias). */
export function guardar(clave, datos) {
  guardadas.set(clave, { promesa: Promise.resolve(datos), datos, hora: Date.now() });
}

/** Olvida todo lo que tenga claves que empiezan así (por ejemplo, la disponibilidad después de reservar). */
export function olvidar(prefijo) {
  for (const clave of [...guardadas.keys()]) {
    if (clave.startsWith(prefijo)) guardadas.delete(clave);
  }
}
