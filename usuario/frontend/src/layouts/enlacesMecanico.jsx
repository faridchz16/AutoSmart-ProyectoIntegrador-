import { Icono } from "./PanelLayout";

/* Secciones del panel del mecánico: las usa el menú lateral y la pantalla de inicio. */

export const ENLACES_MECANICO = [
  {
    to: "/mecanico",
    fin: true,
    texto: "Inicio",
    descripcion: "Accesos rápidos a tu agenda y configuración.",
    icono: (
      <Icono>
        <path d="M4 11.5 12 5l8 6.5" />
        <path d="M6 10v9h12v-9" />
        <path d="M10 19v-5h4v5" />
      </Icono>
    ),
  },
  {
    to: "/mecanico/agenda",
    texto: "Agenda semanal",
    descripcion: "Tus citas de la semana, con navegación entre semanas.",
    icono: (
      <Icono>
        <rect x="3.5" y="5" width="17" height="15" rx="2" />
        <path d="M3.5 9.5h17M8 3v4M16 3v4" />
      </Icono>
    ),
  },
  {
    to: "/mecanico/citas-hoy",
    texto: "Citas de hoy",
    descripcion: "Las citas del día ordenadas por hora.",
    icono: (
      <Icono>
        <path d="M9 5h11M9 12h11M9 19h11" />
        <circle cx="4.5" cy="5" r="1.3" />
        <circle cx="4.5" cy="12" r="1.3" />
        <circle cx="4.5" cy="19" r="1.3" />
      </Icono>
    ),
  },
  {
    to: "/mecanico/horarios",
    texto: "Mis horarios",
    descripcion: "Los días y horas en que atiendes en el taller.",
    icono: (
      <Icono>
        <circle cx="12" cy="12" r="8.5" />
        <path d="M12 7.5V12l3 2" />
      </Icono>
    ),
  },
  {
    to: "/mecanico/bloqueos",
    texto: "Fechas bloqueadas",
    descripcion: "Días libres en los que no recibirás reservas.",
    icono: (
      <Icono>
        <rect x="3.5" y="5" width="17" height="15" rx="2" />
        <path d="M3.5 9.5h17M8 3v4M16 3v4M9.5 12.5l5 5M14.5 12.5l-5 5" />
      </Icono>
    ),
  },
  {
    to: "/mecanico/perfil",
    texto: "Mi perfil",
    descripcion: "Tus especialidades y años de experiencia.",
    icono: (
      <Icono>
        <circle cx="12" cy="8" r="3.6" />
        <path d="M4.5 20c.8-3.6 3.7-5.5 7.5-5.5s6.7 1.9 7.5 5.5" />
      </Icono>
    ),
  },
];
