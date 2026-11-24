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
      "/appointments": BACKEND,
      "/work-orders": BACKEND,
      "/photos": BACKEND, // fotos de evidencia de las deficiencias (T44.1)
      "/materials": BACKEND, // materiales del almacén para el presupuesto y las solicitudes (T46.2, T50.2)
      "/payments": BACKEND, // comprobante PDF de un pago (T70.2)
      "/payment-methods": BACKEND, // métodos de pago habilitados (T70.2)
      "/chatbot": BACKEND, // chatbot de la página de inicio y prueba desde el panel (T80.2, TEC4.1)
      "/users": BACKEND, // perfil del cliente: GET y PUT /users/me (panel del cliente)
      // /admin también es una página del frontend: si el navegador pide HTML
      // (abrir o recargar la página), se sirve React; si es una llamada de datos, va al backend.
      "/admin": {
        target: BACKEND,
        bypass: (req) => (req.headers.accept?.includes("text/html") ? "/index.html" : undefined),
      },
    },
  },
});
