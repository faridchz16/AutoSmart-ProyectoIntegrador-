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
    to: "/cliente/vehiculos",
    texto: "Mis vehículos",
    descripcion: "Registra tus autos con su placa, año y kilometraje.",
    icono: (
      <Icono>
        <path d="M5 16v-4.5l1.8-4.2A2 2 0 0 1 8.6 6h6.8a2 2 0 0 1 1.8 1.3L19 11.5V16" />
        <path d="M4 16h16v2.5a.5.5 0 0 1-.5.5H18a1 1 0 0 1-1-1v-.5H7v.5a1 1 0 0 1-1 1H4.5a.5.5 0 0 1-.5-.5Z" />
        <path d="M5 11.5h14" />
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
