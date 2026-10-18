import { useEffect, useState } from "react";
import { NavLink, Link, Outlet, useLocation, useNavigate } from "react-router-dom";
import { cerrarSesion, obtenerUsuario, normalizarRol } from "../services/sesion";
import { DATOS_PRUEBA } from "../services/datosPrueba";
import "./Admin.css";

/* Estructura común de los paneles (administrador y mecánico):
   barra lateral + encabezado con el usuario y el botón "Cerrar sesión".
   Cada panel solo define sus enlaces; las pantallas se dibujan en <Outlet />.

   Props:
     enlaces   [{ to, texto, icono, fin? }]  (fin = true solo marca activo en la ruta exacta)
     seccion   título pequeño sobre el menú ("Administración", "Mi taller")
     inicio    ruta del logo (inicio del panel)
     avisoPrueba  muestra la etiqueta "Datos de prueba" si están activos */

export const Icono = ({ children }) => (
  <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
    {children}
  </svg>
);

const NOMBRE_ROL = { ADMIN: "Administrador", MECANICO: "Mecánico", CLIENTE: "Cliente" };

function PanelLayout({ enlaces, seccion, inicio, avisoPrueba = false }) {
  const [abierto, setAbierto] = useState(false); // menú lateral en móvil
  const { pathname } = useLocation();
  const navigate = useNavigate();
  const usuario = obtenerUsuario();
  const rol = normalizarRol(usuario?.rol);

  const actual = enlaces.find((e) => (e.fin ? pathname === e.to : pathname.startsWith(e.to)));

  // El título de la pestaña cambia según la pantalla
  useEffect(() => {
    document.title = `${actual?.texto ?? "Panel"} · AutoSmart`;
    return () => {
      document.title = "AutoSmart";
    };
  }, [actual]);

  // Esc cierra el menú en móvil
  useEffect(() => {
    const alTeclear = (e) => e.key === "Escape" && setAbierto(false);
    window.addEventListener("keydown", alTeclear);
    return () => window.removeEventListener("keydown", alTeclear);
  }, []);

  const salir = () => {
    cerrarSesion();
    navigate("/login", { replace: true });
  };

  return (
    <div className="adm">
      {/* ---------- Barra lateral ---------- */}
      <aside className={abierto ? "adm-side abierto" : "adm-side"} aria-label="Menú del panel">
        <Link to={inicio} className="adm-marca" onClick={() => setAbierto(false)}>
          <img src="/logo-white.png" alt="" width="36" height="36" />
          <span>AutoSmart</span>
        </Link>

        <p className="adm-seccion">{seccion}</p>
        <nav className="adm-nav">
          {enlaces.map((e) => (
            <NavLink
              key={e.to}
              to={e.to}
              end={e.fin}
              className={({ isActive }) => (isActive ? "adm-link activo" : "adm-link")}
              onClick={() => setAbierto(false)}
            >
              {e.icono}
              {e.texto}
            </NavLink>
          ))}
        </nav>

        <Link to="/" className="adm-sitio">
          ← Ir al sitio principal
        </Link>
      </aside>

      {abierto && <div className="adm-velo" onClick={() => setAbierto(false)} aria-hidden="true" />}

      {/* ---------- Contenido ---------- */}
      <div className="adm-main">
        <header className="adm-header">
          <button
            type="button"
            className="adm-burger"
            onClick={() => setAbierto((v) => !v)}
            aria-label={abierto ? "Cerrar menú" : "Abrir menú"}
            aria-expanded={abierto}
          >
            <Icono>
              <path d="M4 7h16M4 12h16M4 17h16" />
            </Icono>
          </button>

          <h1 className="adm-titulo">{actual?.texto ?? "Panel"}</h1>

          {avisoPrueba && DATOS_PRUEBA && (
            <span className="adm-prueba" title="El panel usa datos de ejemplo. Cambia VITE_DATOS_PRUEBA a false en .env para usar el backend.">
              Datos de prueba
            </span>
          )}

          <div className="adm-usuario">
            <span className="adm-avatar" aria-hidden="true">
              {(usuario?.correo ?? "?").slice(0, 2).toUpperCase()}
            </span>
            <div className="adm-datos">
              <strong>{usuario?.correo}</strong>
              <span>{NOMBRE_ROL[rol] ?? usuario?.rol}</span>
            </div>
            <button type="button" className="adm-salir" onClick={salir}>
              <Icono>
                <path d="M15 4h3a2 2 0 0 1 2 2v12a2 2 0 0 1-2 2h-3M10 8l-4 4 4 4M6 12h10" />
              </Icono>
              <span>Cerrar sesión</span>
            </button>
          </div>
        </header>

        <main className="adm-contenido">
          <Outlet />
        </main>
      </div>
    </div>
  );
}

export default PanelLayout;
