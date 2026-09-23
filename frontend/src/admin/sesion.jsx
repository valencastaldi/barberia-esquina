import { createContext, useCallback, useContext, useMemo, useState } from "react";
import { Navigate, useLocation, useNavigate } from "react-router-dom";
import { api, ErrorApi } from "../api/api.js";
import { usePedido } from "../api/usePedido.js";

/*
 * Sesión del panel: el JWT que devuelve POST /auth/login se guarda en el
 * navegador hasta que vence (8 h) o hasta cerrar sesión.
 */

const CLAVE = "esquina.sesion";
const Contexto = createContext(null);

function sesionGuardada() {
  try {
    const s = JSON.parse(localStorage.getItem(CLAVE));
    return s && new Date(s.vence) > new Date() ? s : null;
  } catch {
    return null;
  }
}

export function SesionProvider({ children }) {
  const [sesion, setSesion] = useState(sesionGuardada);
  const navegar = useNavigate();
  const ubicacion = useLocation();

  const iniciar = useCallback(async (email, password) => {
    const r = await api("/auth/login", { metodo: "POST", cuerpo: { email, password } });
    const nueva = { token: r.token, vence: r.vence, usuario: r.usuario };
    try { localStorage.setItem(CLAVE, JSON.stringify(nueva)); } catch { /* sin almacenamiento: dura hasta recargar */ }
    setSesion(nueva);
  }, []);

  const cerrar = useCallback(() => {
    try { localStorage.removeItem(CLAVE); } catch { /* nada */ }
    setSesion(null);
  }, []);

  /** Pedido a la API con el token. Si la sesión venció (401), vuelve al login. */
  const pedir = useCallback(async (ruta, opciones = {}) => {
    try {
      return await api(ruta, { ...opciones, token: sesion?.token });
    } catch (error) {
      if (error instanceof ErrorApi && error.status === 401) {
        cerrar();
        navegar("/admin/login", { replace: true, state: { desde: ubicacion.pathname, motivo: "Tu sesión venció. Ingresá de nuevo." } });
      }
      throw error;
    }
  }, [sesion?.token, cerrar, navegar, ubicacion.pathname]);

  const valor = useMemo(() => ({
    sesion,
    usuario: sesion?.usuario ?? null,
    esDueno: sesion?.usuario?.rol === "dueno",
    iniciar,
    cerrar,
    pedir,
  }), [sesion, iniciar, cerrar, pedir]);

  return <Contexto.Provider value={valor}>{children}</Contexto.Provider>;
}

export const useSesion = () => useContext(Contexto);

/**
 * GET con el token de la sesión; se vuelve a pedir cuando cambian la ruta o los parámetros.
 * Con ruta null no pide nada (útil para modales cerrados).
 */
export function usePedidoAdmin(ruta, params) {
  const { pedir } = useSesion();
  return usePedido(() => (ruta ? pedir(ruta, { params }) : null), [ruta, JSON.stringify(params ?? {})]);
}

/** Sin sesión, al login (recordando a dónde quería ir). */
export function RequiereSesion({ children }) {
  const { sesion } = useSesion();
  const ubicacion = useLocation();
  if (!sesion) return <Navigate to="/admin/login" replace state={{ desde: ubicacion.pathname }} />;
  return children;
}

/** Solo para el dueño (la API igual lo controla: esto evita mostrar pantallas que darían 403). */
export function SoloDueno({ children }) {
  const { esDueno } = useSesion();
  if (!esDueno) return <Navigate to="/admin/agenda" replace />;
  return children;
}
