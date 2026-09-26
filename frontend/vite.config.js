import { defineConfig, loadEnv } from "vite";
import react from "@vitejs/plugin-react";

/*
 * Content-Security-Policy del sitio publicado. El token del panel vive en
 * localStorage: si algún día se colara un XSS, esto impide cargar scripts de
 * otro lado y mandar datos a un dominio ajeno. Solo se agrega en el build
 * porque el servidor de desarrollo usa scripts inline para la recarga en caliente.
 * Si el hosting permite headers, conviene mandarla también como header
 * (ahí se puede sumar frame-ancestors, que en <meta> no tiene efecto).
 */
function politicaDeSeguridad(apiUrl) {
  // Si la API está en otro dominio (VITE_API_URL), el navegador tiene que poder llamarla.
  const api = /^https?:\/\//.test(apiUrl ?? "") ? " " + new URL(apiUrl).origin : "";
  const csp = [
    "default-src 'self'",
    "script-src 'self'",
    "style-src 'self' https://fonts.googleapis.com",
    "font-src https://fonts.gstatic.com",
    "img-src 'self' data:",
    `connect-src 'self'${api}`,
    "object-src 'none'",
    "base-uri 'self'",
    "form-action 'self'",
  ].join("; ");

  return {
    name: "politica-de-seguridad",
    apply: "build",
    transformIndexHtml: () => [
      { tag: "meta", attrs: { "http-equiv": "Content-Security-Policy", content: csp }, injectTo: "head-prepend" },
      { tag: "meta", attrs: { name: "referrer", content: "strict-origin-when-cross-origin" }, injectTo: "head-prepend" },
    ],
  };
}

// En desarrollo, todo lo que empieza con /api se reenvía a la API Java (puerto 8080):
// el navegador ve un solo origen y no hace falta CORS.
export default defineConfig(({ mode }) => ({
  plugins: [react(), politicaDeSeguridad(loadEnv(mode, process.cwd(), "VITE_").VITE_API_URL)],
  server: {
    port: 5173,
    proxy: {
      "/api": "http://localhost:8080",
    },
  },
}));
