import { useState } from "react";
import { Link } from "react-router-dom";
import { login } from "../services/authService";
import "./Login.css";

const FORMATO_CORREO = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

// Valida un campo y devuelve el mensaje de error ("" si está bien)
function validar(campo, valor) {
  if (campo === "correo") {
    if (!valor.trim()) return "Ingresa tu correo electrónico.";
    if (!FORMATO_CORREO.test(valor)) return "Ingresa un correo válido.";
  }
  if (campo === "password" && !valor) return "Ingresa tu contraseña.";
  return "";
}

/* ---------- Iconos (SVG en línea, sin dependencias) ---------- */
const Icono = ({ children }) => (
  <svg
    className="input-icon"
    viewBox="0 0 24 24"
    width="18"
    height="18"
    fill="none"
    stroke="currentColor"
    strokeWidth="1.8"
    strokeLinecap="round"
    strokeLinejoin="round"
    aria-hidden="true"
  >
    {children}
  </svg>
);

const IconoCorreo = () => (
  <Icono>
    <rect x="3" y="5" width="18" height="14" rx="2" />
    <path d="m3 7 9 6 9-6" />
  </Icono>
);

const IconoCandado = () => (
  <Icono>
    <rect x="4" y="11" width="16" height="10" rx="2" />
    <path d="M8 11V7a4 4 0 0 1 8 0v4" />
  </Icono>
);

const IconoOjo = ({ tachado }) => (
  <svg
    viewBox="0 0 24 24"
    width="18"
    height="18"
    fill="none"
    stroke="currentColor"
    strokeWidth="1.8"
    strokeLinecap="round"
    strokeLinejoin="round"
    aria-hidden="true"
  >
    <path d="M2 12s3.5-7 10-7 10 7 10 7-3.5 7-10 7S2 12 2 12Z" />
    <circle cx="12" cy="12" r="3" />
    {tachado && <path d="m3 3 18 18" />}
  </svg>
);

const LogoAuto = () => (
  <svg viewBox="0 0 24 24" width="26" height="26" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
    <path d="M5 16V11l2-5h10l2 5v5" />
    <path d="M3 16h18v3H3z" />
    <circle cx="7.5" cy="16" r="0.6" fill="currentColor" />
    <circle cx="16.5" cy="16" r="0.6" fill="currentColor" />
  </svg>
);

function Login() {
  const [correo, setCorreo] = useState("");
  const [password, setPassword] = useState("");
  const [errores, setErrores] = useState({ correo: "", password: "" });
  const [errorGeneral, setErrorGeneral] = useState("");
  const [verPassword, setVerPassword] = useState(false);
  const [mayusculas, setMayusculas] = useState(false);
  const [cargando, setCargando] = useState(false); // ¿estamos esperando al backend?
  const [usuario, setUsuario] = useState(null);    // datos del usuario si el login fue exitoso

  // Valida un campo al salir de él (onBlur)
  const alSalir = (campo, valor) => {
    setErrores((prev) => ({ ...prev, [campo]: validar(campo, valor) }));
  };

  // Si el campo ya tenía error, lo limpiamos en cuanto el usuario vuelve a escribir
  const alEscribir = (campo, valor, setter) => {
    setter(valor);
    setErrorGeneral("");
    if (errores[campo]) {
      setErrores((prev) => ({ ...prev, [campo]: validar(campo, valor) }));
    }
  };

  const handleSubmit = async (e) => {
    e.preventDefault();

    const nuevosErrores = {
      correo: validar("correo", correo),
      password: validar("password", password),
    };
    setErrores(nuevosErrores);
    if (nuevosErrores.correo || nuevosErrores.password) return;

    setErrorGeneral("");
    setCargando(true);

    try {
      const datos = await login(correo.trim(), password);

      // Guardamos el token para usarlo en las siguientes pantallas
      localStorage.setItem("token", datos.token);
      localStorage.setItem("usuario", JSON.stringify({ correo: datos.correo, rol: datos.rol }));

      setUsuario(datos);
    } catch (err) {
      setErrorGeneral(err.message);
    } finally {
      setCargando(false);
    }
  };

  const cerrarSesion = () => {
    localStorage.removeItem("token");
    localStorage.removeItem("usuario");
    setUsuario(null);
    setPassword("");
  };

  // Si el login fue exitoso, mostramos un mensaje de bienvenida
  if (usuario) {
    return (
      <div className="login-page">
        <div className="login-card">
          <div className="login-brand">
            <span className="login-brand-icon"><LogoAuto /></span>
            <h1 className="login-logo">AutoSmart</h1>
          </div>
          <p className="login-success" role="status">
            ¡Bienvenido! Iniciaste sesión como <strong>{usuario.correo}</strong> (rol: {usuario.rol}).
          </p>
          <button type="button" className="login-btn-secondary" onClick={cerrarSesion}>
            Cerrar sesión
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="login-page">
      <main className="login-card">
        <div className="login-brand">
          <span className="login-brand-icon"><LogoAuto /></span>
          <h1 className="login-logo">AutoSmart</h1>
        </div>
        <p className="login-subtitle">Inicia sesión para gestionar tus vehículos</p>

        <form onSubmit={handleSubmit} noValidate>
          {/* Error general (backend / conexión) */}
          {errorGeneral && (
            <p className="login-error" role="alert">{errorGeneral}</p>
          )}

          {/* Correo */}
          <div className="field">
            <label htmlFor="correo">Correo electrónico</label>
            <div className={`input-wrap ${errores.correo ? "has-error" : ""}`}>
              <IconoCorreo />
              <input
                id="correo"
                type="email"
                inputMode="email"
                autoComplete="email"
                autoFocus
                placeholder="tucorreo@ejemplo.com"
                value={correo}
                onChange={(e) => alEscribir("correo", e.target.value, setCorreo)}
                onBlur={() => alSalir("correo", correo)}
                aria-invalid={!!errores.correo}
                aria-describedby={errores.correo ? "correo-error" : undefined}
              />
            </div>
            {errores.correo && (
              <p id="correo-error" className="field-error" role="alert">{errores.correo}</p>
            )}
          </div>

          {/* Contraseña */}
          <div className="field">
            <div className="label-row">
              <label htmlFor="password">Contraseña</label>
              <Link to="/recuperar" className="link-olvido">¿Olvidaste tu contraseña?</Link>
            </div>
            <div className={`input-wrap ${errores.password ? "has-error" : ""}`}>
              <IconoCandado />
              <input
                id="password"
                type={verPassword ? "text" : "password"}
                autoComplete="current-password"
                placeholder="••••••••"
                value={password}
                onChange={(e) => alEscribir("password", e.target.value, setPassword)}
                onBlur={() => {
                  alSalir("password", password);
                  setMayusculas(false);
                }}
                onKeyUp={(e) => setMayusculas(e.getModifierState("CapsLock"))}
                aria-invalid={!!errores.password}
                aria-describedby={errores.password ? "password-error" : undefined}
              />
              <button
                type="button"
                className="toggle-password"
                onClick={() => setVerPassword((v) => !v)}
                aria-label={verPassword ? "Ocultar contraseña" : "Mostrar contraseña"}
                aria-pressed={verPassword}
              >
                <IconoOjo tachado={verPassword} />
              </button>
            </div>
            {errores.password && (
              <p id="password-error" className="field-error" role="alert">{errores.password}</p>
            )}
            {mayusculas && !errores.password && (
              <p className="field-aviso">Las mayúsculas están activadas.</p>
            )}
          </div>

          <button type="submit" disabled={cargando}>
            {cargando && <span className="spinner" aria-hidden="true" />}
            {cargando ? "Ingresando..." : "Iniciar sesión"}
          </button>
        </form>

        <p className="login-footer">
          ¿No tienes cuenta? <Link to="/registro">Regístrate</Link>
        </p>
      </main>

      <p className="login-copy">© 2026 AutoSmart · Todos los derechos reservados</p>
    </div>
  );
}

export default Login;
