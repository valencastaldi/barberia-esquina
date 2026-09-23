import { useCallback, useEffect, useState } from "react";

/**
 * Pide datos a la API cuando cambian las dependencias.
 * Devuelve { datos, error, cargando, recargar }. Si llega una respuesta vieja
 * (el usuario ya cambió de opción), se descarta.
 */
export function usePedido(pedir, dependencias) {
  const [estado, setEstado] = useState({ datos: null, error: null, cargando: true });
  const [vuelta, setVuelta] = useState(0);

  useEffect(() => {
    let vigente = true;
    setEstado((e) => ({ ...e, error: null, cargando: true }));
    Promise.resolve(pedir())
      .then((datos) => vigente && setEstado({ datos, error: null, cargando: false }))
      .catch((error) => vigente && setEstado({ datos: null, error, cargando: false }));
    return () => {
      vigente = false;
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [...dependencias, vuelta]);

  const recargar = useCallback(() => setVuelta((v) => v + 1), []);
  return { ...estado, recargar };
}
