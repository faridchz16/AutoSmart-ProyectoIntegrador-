import { Icono } from "./PanelLayout";

/* Secciones del panel del cliente: las usa el menú lateral y la pantalla de inicio. */

// Pantallas que no están en el menú, pero necesitan su título en el encabezado
export const TITULOS_CLIENTE = [];

export const ENLACES_CLIENTE = [
  {
    to: "/cliente",
    fin: true,
    texto: "Inicio",
    descripcion: "Tu resumen y accesos rápidos.",
    icono: (
      <Icono>
        <path d="M4 11.5 12 5l8 6.5" />
        <path d="M6 10v9h12v-9" />
        <path d="M10 19v-5h4v5" />
      </Icono>
    ),
  },
  {
    to: "/cliente/perfil",
    texto: "Mi perfil",
    descripcion: "Tu nombre y teléfono de contacto.",
    icono: (
      <Icono>
        <circle cx="12" cy="8" r="3.6" />
        <path d="M4.5 20c.8-3.6 3.7-5.5 7.5-5.5s6.7 1.9 7.5 5.5" />
      </Icono>
    ),
  },
];
