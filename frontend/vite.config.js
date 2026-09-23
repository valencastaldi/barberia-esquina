import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";

// En desarrollo, todo lo que empieza con /api se reenvía a la API Java (puerto 8080):
// el navegador ve un solo origen y no hace falta CORS.
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      "/api": "http://localhost:8080",
    },
  },
});
