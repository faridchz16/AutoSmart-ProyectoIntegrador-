import { Link } from "react-router-dom";
import "../pages/Login.css";

/* Estructura compartida por el inicio de sesión y el registro:
   panel visual a la izquierda y formulario a la derecha. */

const Check = () => (
  <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" strokeWidth="2.4" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
    <path d="m5 12.5 4.5 4.5L19 7.5" />
  </svg>
);

const Volver = () => (
  <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
    <path d="m15 6-6 6 6 6" />
  </svg>
);

function AuthLayout({ titulo, subtitulo, pie, children }) {
  return (
    <div className="auth">
      {/* ---------- Panel visual ---------- */}
      <aside className="auth-visual" aria-hidden="true">
        <div className="auth-bg" />
        <div className="auth-shade" />
        <span className="auth-scan" />

        <div className="auth-visual-in">
          <div className="auth-marca">
            <img src="/logo-white.png" alt="" width="46" height="46" />
            <span>AutoSmart</span>
          </div>

          <ul className="auth-chips">
            <li className="auth-chip">
              <i><Check /></i>
              <div><strong>Cambio de aceite</strong><span>Al día</span></div>
            </li>
            <li className="auth-chip">
              <i><Check /></i>
              <div><strong>Revisión de frenos</strong><span>Al día</span></div>
            </li>
            <li className="auth-chip auth-chip-aviso">
              <i>!</i>
              <div><strong>Próximo servicio</strong><span>En 1 800 km</span></div>
            </li>
          </ul>

          <div className="auth-texto">
            <h2>Todo el mantenimiento de tu auto, en un solo lugar.</h2>
            <p>Mecánicos verificados, control de kilometraje y un asistente que responde tus dudas.</p>
          </div>
        </div>

        <svg className="auth-pulso" viewBox="0 0 1200 120" preserveAspectRatio="none">
          <path className="pulso-base" d="M0 70 H330 l22 -48 l34 92 l28 -70 l16 26 H760 l22 -48 l34 92 l28 -70 l16 26 H1200" />
          <path className="pulso-mov" d="M0 70 H330 l22 -48 l34 92 l28 -70 l16 26 H760 l22 -48 l34 92 l28 -70 l16 26 H1200" />
        </svg>
      </aside>

      {/* ---------- Formulario ---------- */}
      <main className="auth-side">
        <div className="auth-topbar">
          <Link to="/" className="auth-volver"><Volver /> Volver al inicio</Link>
        </div>

        <div className="auth-card">
          <Link to="/" className="login-brand" aria-label="AutoSmart, inicio">
            <img src="/logo.png" alt="" width="44" height="44" />
            <span>AutoSmart</span>
          </Link>

          <h1 className="login-logo">{titulo}</h1>
          <p className="login-subtitle">{subtitulo}</p>

          {children}

          {pie && <p className="login-footer">{pie}</p>}
        </div>

        <p className="login-copy">© 2026 AutoSmart. Todos los derechos reservados.</p>
      </main>
    </div>
  );
}

export default AuthLayout;
