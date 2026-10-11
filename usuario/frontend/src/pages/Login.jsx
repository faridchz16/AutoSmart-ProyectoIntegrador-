import { useState } from "react";
import { Link } from "react-router-dom";
import { login } from "../services/authService";
import AuthLayout from "../components/AuthLayout";
import Campo from "../components/Campo";
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

const IconoFlecha = () => (
  <svg className="btn-flecha" viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
    <path d="M5 12h14M13 6l6 6-6 6" />
  </svg>
);

function Login() {
  const [correo, setCorreo] = useState("");
  const [password, setPassword] = useState("");
  const [errores, setErrores] = useState({ correo: "", password: "" });
  const [errorGeneral, setErrorGeneral] = useState("");
  const [mayusculas, setMayusculas] = useState(false);
  const [cargando, setCargando] = useState(false); // ¿estamos esperando al backend?
  const [usuario, setUsuario] = useState(null);    // datos del usuario si el login fue exitoso

  // Valida un campo al salir de él
  const alSalir = (campo, valor) => {
    setErrores((prev) => ({ ...prev, [campo]: validar(campo, valor) }));
  };

  // Si el campo ya tenía error, se revalida mientras el usuario escribe
  const alEscribir = (campo, valor, setter) => {
    setter(valor);
    setErrorGeneral("");
    if (errores[campo]) {
      setErrores((prev) => ({ ...prev, [campo]: validar(campo, valor) }));
    }
  };

  const handleSubmit = async (e) => {
    e.preventDefault();

    const nuevos = {
      correo: validar("correo", correo),
      password: validar("password", password),
    };
    setErrores(nuevos);
    if (nuevos.correo || nuevos.password) return;

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

  // Inicio de sesión exitoso
  if (usuario) {
    return (
      <AuthLayout titulo="¡Bienvenido!" subtitulo="Iniciaste sesión correctamente.">
        <div className="exito" role="status">
          <svg className="exito-check" viewBox="0 0 52 52" aria-hidden="true">
            <circle cx="26" cy="26" r="24" />
            <path d="m15 27 8 8 15-17" />
          </svg>
          <p>
            Sesión iniciada como <strong>{usuario.correo}</strong>
          </p>
          <span className="exito-rol">Rol: {usuario.rol}</span>
        </div>
        <Link to="/" className="login-btn-link">Ir al inicio</Link>
        <button type="button" className="login-btn-secondary" onClick={cerrarSesion}>
          Cerrar sesión
        </button>
      </AuthLayout>
    );
  }

  return (
    <AuthLayout
      titulo="Bienvenido de nuevo"
      subtitulo="Inicia sesión para gestionar tus vehículos."
      pie={<>¿No tienes cuenta? <Link to="/registro">Regístrate</Link></>}
    >
      <form onSubmit={handleSubmit} noValidate>
        {errorGeneral && (
          <p className="login-error" role="alert">{errorGeneral}</p>
        )}

        <Campo
          id="correo"
          label="Correo electrónico"
          tipo="email"
          inputMode="email"
          autoComplete="email"
          autoFocus
          icono="correo"
          placeholder="tucorreo@ejemplo.com"
          valor={correo}
          alCambiar={(v) => alEscribir("correo", v, setCorreo)}
          alSalir={() => alSalir("correo", correo)}
          error={errores.correo}
        />

        <Campo
          id="password"
          label="Contraseña"
          tipo="password"
          autoComplete="current-password"
          icono="candado"
          placeholder="••••••••"
          valor={password}
          alCambiar={(v) => alEscribir("password", v, setPassword)}
          alSalir={() => {
            alSalir("password", password);
            setMayusculas(false);
          }}
          alSoltarTecla={(e) => setMayusculas(e.getModifierState("CapsLock"))}
          error={errores.password}
          extraLabel={<Link to="/recuperar" className="link-olvido">¿Olvidaste tu contraseña?</Link>}
          ayuda={mayusculas && !errores.password && <p className="field-aviso">Las mayúsculas están activadas.</p>}
        />

        <button type="submit" disabled={cargando}>
          {cargando ? <span className="spinner" aria-hidden="true" /> : null}
          {cargando ? "Ingresando..." : <>Iniciar sesión <IconoFlecha /></>}
        </button>
      </form>
    </AuthLayout>
  );
}

export default Login;
