import PanelLayout, { Icono } from "./PanelLayout";

/* Panel del administrador: usa la estructura común de PanelLayout. */

const ENLACES = [
  {
    to: "/admin",
    fin: true,
    texto: "Resumen",
    icono: (
      <Icono>
        <rect x="3" y="3" width="7" height="9" rx="1.5" />
        <rect x="14" y="3" width="7" height="5" rx="1.5" />
        <rect x="14" y="12" width="7" height="9" rx="1.5" />
        <rect x="3" y="16" width="7" height="5" rx="1.5" />
      </Icono>
    ),
  },
  {
    to: "/admin/reservas",
    texto: "Reservas",
    icono: (
      <Icono>
        <rect x="3.5" y="5" width="17" height="15" rx="2" />
        <path d="M3.5 9.5h17M8 3v4M16 3v4" />
        <path d="m9 14.5 2 2 4-4" />
      </Icono>
    ),
  },
  {
    to: "/admin/usuarios",
    texto: "Usuarios",
    icono: (
      <Icono>
        <circle cx="9" cy="8" r="3.2" />
        <path d="M3 20c.6-3.3 3-5 6-5s5.4 1.7 6 5" />
        <circle cx="17" cy="9" r="2.6" />
        <path d="M16.5 14.2c2.6.2 4.1 1.8 4.5 4.8" />
      </Icono>
    ),
  },
  {
    to: "/admin/servicios",
    texto: "Servicios",
    icono: (
      <Icono>
        <path d="M14.7 6.3a4 4 0 0 0-5.4 5.1L3.5 17.2a1.8 1.8 0 0 0 2.6 2.6l5.8-5.8a4 4 0 0 0 5.1-5.4l-2.5 2.5-2.3-.5-.5-2.3 2.5-2.5Z" />
      </Icono>
    ),
  },
];

function AdminLayout() {
  return <PanelLayout enlaces={ENLACES} seccion="Administración" inicio="/admin" avisoPrueba />;
}

export default AdminLayout;
