import { useEffect, useState } from "react";
import { NavLink, Link, Outlet, useLocation, useNavigate } from "react-router-dom";
import { cerrarSesion, obtenerUsuario, normalizarRol } from "../services/sesion";
import "./Admin.css";

/* Estructura del panel de administración:
   barra lateral + encabezado con el usuario y el botón "Cerrar sesión".
   Las pantallas internas se dibujan dentro de <Outlet />. */

const Icono = ({ children }) => (
  <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
    {children}
  </svg>
);

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
  const [abierto, setAbierto] = useState(false); // menú lateral en móvil
  const { pathname } = useLocation();
  const navigate = useNavigate();
  const usuario = obtenerUsuario();

  const actual = ENLACES.find((e) => (e.fin ? pathname === e.to : pathname.startsWith(e.to)));

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
        <Link to="/admin" className="adm-marca" onClick={() => setAbierto(false)}>
          <img src="/logo-white.png" alt="" width="36" height="36" />
          <span>AutoSmart</span>
        </Link>

        <p className="adm-seccion">Administración</p>
        <nav className="adm-nav">
          {ENLACES.map((e) => (
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

          <div className="adm-usuario">
            <span className="adm-avatar" aria-hidden="true">
              {(usuario?.correo ?? "?").slice(0, 2).toUpperCase()}
            </span>
            <div className="adm-datos">
              <strong>{usuario?.correo}</strong>
              <span>{normalizarRol(usuario?.rol) === "ADMIN" ? "Administrador" : usuario?.rol}</span>
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

export default AdminLayout;
