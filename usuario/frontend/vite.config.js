import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";

// En desarrollo, las llamadas a la API se reenvían al backend Spring Boot (puerto 8081)
const BACKEND = "http://localhost:8081";

export default defineConfig({
  plugins: [react()],
  server: {
    proxy: {
      "/auth": BACKEND,
      "/services": BACKEND,
      "/mechanics": BACKEND,
      // /admin también es una página del frontend: si el navegador pide HTML
      // (abrir o recargar la página), se sirve React; si es una llamada de datos, va al backend.
      "/admin": {
        target: BACKEND,
        bypass: (req) => (req.headers.accept?.includes("text/html") ? "/index.html" : undefined),
      },
    },
  },
});
