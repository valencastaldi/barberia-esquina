/* =========================================================
   Cliente de la API REST (/api/v1).
   En desarrollo Vite reenvía /api al backend (vite.config.js).
   En producción se puede apuntar a otro dominio con VITE_API_URL.
   ========================================================= */

const BASE = import.meta.env.VITE_API_URL ?? "/api/v1";

/** Error de la API con el mensaje listo para mostrar (campo "detail" del Problem Details). */
export class ErrorApi extends Error {
  constructor(mensaje, status, errores = {}) {
    super(mensaje);
    this.status = status;
    this.errores = errores; // validación: { "cliente.email": "..." }
  }
}

const MENSAJE_POR_STATUS = {
  401: "Tenés que iniciar sesión.",
  403: "No tenés permiso para hacer esto.",
  404: "No encontramos lo que buscabas.",
  429: "Hiciste demasiados pedidos seguidos. Probá de nuevo en un rato.",
};

/**
 * Las rutas que devuelve la API (por ejemplo la foto: "/api/v1/barberos/2/foto?v=…")
 * pasan a apuntar a donde está la API, aunque esté en otro dominio (VITE_API_URL).
 */
export function urlDeApi(ruta) {
  return ruta ? ruta.replace(/^\/api\/v1/, BASE) : null;
}

export async function api(ruta, { metodo = "GET", cuerpo, params, token } = {}) {
  const url = new URL(BASE + ruta, window.location.origin);
  Object.entries(params ?? {}).forEach(([k, v]) => {
    if (v !== undefined && v !== null && v !== "") url.searchParams.set(k, v);
  });

  // Un FormData (subir una foto) va tal cual: el navegador arma el multipart y su Content-Type.
  const esArchivo = cuerpo instanceof FormData;
  const headers = { Accept: "application/json" };
  if (cuerpo !== undefined && !esArchivo) headers["Content-Type"] = "application/json";
  if (token) headers.Authorization = `Bearer ${token}`;

  let respuesta;
  try {
    respuesta = await fetch(url, {
      method: metodo,
      headers,
      body: cuerpo === undefined ? undefined : esArchivo ? cuerpo : JSON.stringify(cuerpo),
    });
  } catch {
    throw new ErrorApi("No pudimos conectarnos. Revisá tu conexión e intentá de nuevo.", 0);
  }

  if (respuesta.status === 204) return null;
  const datos = await respuesta.json().catch(() => null);

  if (!respuesta.ok) {
    const mensaje = datos?.detail ?? MENSAJE_POR_STATUS[respuesta.status] ?? "Algo salió mal. Intentá de nuevo.";
    throw new ErrorApi(mensaje, respuesta.status, datos?.errores ?? {});
  }
  return datos;
}
