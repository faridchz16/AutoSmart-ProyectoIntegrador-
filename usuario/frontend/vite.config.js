import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";

export default defineConfig({
  plugins: [react()],
  server: {
    proxy: {
      // Todo lo que empiece con /auth se reenvía al backend Spring Boot
      "/auth": "http://localhost:8081",
    },
  },
});