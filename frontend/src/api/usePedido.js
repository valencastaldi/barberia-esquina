import { useCallback, useEffect, useState } from "react";
import { leerGuardado, olvidar, pedirGuardado } from "./cache.js";

/**
 * Pide datos a la API cuando cambian las dependencias.
 * Devuelve { datos, error, cargando, recargar }. Mientras llega lo nuevo quedan los
 * datos anteriores (con cargando = true), así la pantalla no se vacía. Si llega una
 * respuesta vieja (el usuario ya cambió de opción), se descarta.
 *
 * Con `clave`, la respuesta se guarda en memoria (cache.js): si ya se trajo hace menos
 * de `vigencia` ms, se usa esa sin pedir de nuevo, desde el primer dibujo.
 */
export function usePedido(pedir, dependencias, { clave, vigencia = 0 } = {}) {
  const [estado, setEstado] = useState(() => {
    const guardado = clave ? leerGuardado(clave, vigencia) : undefined;
    return guardado === undefined
      ? { datos: null, error: null, cargando: true }
      : { datos: guardado, error: null, cargando: false };
  });
  const [vuelta, setVuelta] = useState(0);

  useEffect(() => {
    const guardado = clave ? leerGuardado(clave, vigencia) : undefined;
    if (guardado !== undefined) {
      setEstado((e) => (e.datos === guardado && !e.cargando ? e : { datos: guardado, error: null, cargando: false }));
      return undefined;
    }

    let vigente = true;
    setEstado((e) => ({ ...e, error: null, cargando: true }));
    (clave ? pedirGuardado(clave, pedir, vigencia) : Promise.resolve(pedir()))
      .then((datos) => vigente && setEstado({ datos, error: null, cargando: false }))
      .catch((error) => vigente && setEstado({ datos: null, error, cargando: false }));
    return () => {
      vigente = false;
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [...dependencias, vuelta]);

  const recargar = useCallback(() => {
    if (clave) olvidar(clave);
    setVuelta((v) => v + 1);
  }, [clave]);
  return { ...estado, recargar };
}
