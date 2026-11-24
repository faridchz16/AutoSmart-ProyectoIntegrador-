import { Icono } from "./PanelLayout";

/* Secciones del panel del cliente: las usa el menú lateral y la pantalla de inicio. */

// Pantallas que no están en el menú, pero necesitan su título en el encabezado
export const TITULOS_CLIENTE = [
  { patron: /^\/cliente\/citas\/[^/]+\/?$/, texto: "Seguimiento de la cita" },
  { patron: /^\/cliente\/citas\/[^/]+\/pago\/?$/, texto: "Pago del servicio" },
];

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
    to: "/cliente/reservar",
    texto: "Reservar cita",
    descripcion: "Elige servicio, mecánico y horario en cuatro pasos.",
    icono: (
      <Icono>
        <rect x="4" y="5.5" width="16" height="14" rx="2.5" />
        <path d="M8 3.5v4M16 3.5v4M4 10h16" />
        <path d="M12 12.5v4M10 14.5h4" />
      </Icono>
    ),
  },
  {
    to: "/cliente/citas",
    texto: "Mis citas",
    descripcion: "Estado de tus reservas; cámbialas de horario o cancélalas.",
    icono: (
      <Icono>
        <rect x="4" y="5.5" width="16" height="14" rx="2.5" />
        <path d="M8 3.5v4M16 3.5v4M4 10h16" />
        <path d="m9 14.5 2 2 4-4" />
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
