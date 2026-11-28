import { useEffect, useState } from "react";
import { useLocation } from "react-router-dom";
import PanelLayout, { Icono } from "./PanelLayout";
import { contarAlertasStock, EVENTO_STOCK } from "../services/inventarioService";
import { contarDerivacionesPendientes, EVENTO_DERIVACIONES } from "../services/chatbotAdminService";

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
    to: "/admin/indicadores",
    texto: "Indicadores",
    icono: (
      <Icono>
        <path d="M4 20V10M10 20V4M16 20v-7M21 20H3" />
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
    to: "/admin/pagos",
    texto: "Pagos",
    icono: (
      <Icono>
        <rect x="2.5" y="5.5" width="19" height="13" rx="2" />
        <path d="M2.5 10h19M6.5 14.5h4" />
      </Icono>
    ),
  },
  {
    to: "/admin/historial-placa",
    texto: "Historial por placa",
    icono: (
      <Icono>
        <rect x="3" y="7" width="18" height="10" rx="2" />
        <path d="M7 12h3M13 12h4" />
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
  {
    to: "/admin/calificaciones",
    texto: "Calificaciones",
    icono: (
      <Icono>
        <path d="m12 3.5 2.6 5.3 5.8.8-4.2 4.1 1 5.8-5.2-2.7-5.2 2.7 1-5.8-4.2-4.1 5.8-.8z" />
      </Icono>
    ),
  },
  {
    to: "/admin/inventario",
    texto: "Inventario",
    icono: (
      <Icono>
        <path d="M3.5 8 12 3.5 20.5 8v8L12 20.5 3.5 16Z" />
        <path d="M3.5 8 12 12.5 20.5 8M12 12.5v8" />
      </Icono>
    ),
  },
  {
    to: "/admin/solicitudes",
    texto: "Solicitudes",
    icono: (
      <Icono>
        <path d="M5 4.5h14v15l-3-2-2 2-2-2-2 2-2-2-3 2Z" />
        <path d="M9 9h6M9 12.5h4" />
      </Icono>
    ),
  },
  {
    to: "/admin/derivaciones",
    texto: "Derivaciones",
    icono: (
      <Icono>
        <path d="M4.5 5.5h15v10h-9l-4 3.5v-3.5h-2Z" />
        <circle cx="12" cy="10.5" r="0.6" />
        <path d="M9 10.5h.01M15 10.5h.01" />
      </Icono>
    ),
  },
  {
    to: "/admin/preguntas-chatbot",
    texto: "Preguntas del chatbot",
    icono: (
      <Icono>
        <circle cx="12" cy="12" r="8.5" />
        <path d="M9.6 9.4a2.5 2.5 0 1 1 3.4 2.3c-.6.3-1 .8-1 1.5v.6M12 16.6h.01" />
      </Icono>
    ),
  },
  {
    to: "/admin/taller",
    texto: "Horario del taller",
    icono: (
      <Icono>
        <circle cx="12" cy="12" r="8.5" />
        <path d="M12 7.5V12l3 2" />
      </Icono>
    ),
  },
];

function AdminLayout() {
  const { pathname } = useLocation();
  const [alertas, setAlertas] = useState(0);
  const [derivaciones, setDerivaciones] = useState(0);
  const [cambios, setCambios] = useState(0);

  // T60.1: materiales en stock mínimo; se recuentan al cambiar de pantalla y cuando cambia el stock
  useEffect(() => {
    const alCambiar = () => setCambios((n) => n + 1);
    window.addEventListener(EVENTO_STOCK, alCambiar);
    window.addEventListener(EVENTO_DERIVACIONES, alCambiar);
    return () => {
      window.removeEventListener(EVENTO_STOCK, alCambiar);
      window.removeEventListener(EVENTO_DERIVACIONES, alCambiar);
    };
  }, []);

  useEffect(() => {
    let vigente = true;
    contarAlertasStock()
      .then((n) => vigente && setAlertas(n))
      .catch(() => vigente && setAlertas(0));
    // T79.3: derivaciones del chatbot que esperan a un asesor
    contarDerivacionesPendientes()
      .then((n) => vigente && setDerivaciones(n))
      .catch(() => vigente && setDerivaciones(0));
    return () => {
      vigente = false;
    };
  }, [pathname, cambios]);

  const enlaces = ENLACES.map((e) => {
    if (e.to === "/admin/inventario") {
      return { ...e, contador: alertas, etiquetaContador: `${alertas} ${alertas === 1 ? "material" : "materiales"} en stock mínimo` };
    }
    if (e.to === "/admin/derivaciones") {
      return { ...e, contador: derivaciones, etiquetaContador: `${derivaciones} ${derivaciones === 1 ? "derivación pendiente" : "derivaciones pendientes"}` };
    }
    return e;
  });

  return <PanelLayout enlaces={enlaces} seccion="Administración" inicio="/admin" avisoPrueba />;
}

export default AdminLayout;
