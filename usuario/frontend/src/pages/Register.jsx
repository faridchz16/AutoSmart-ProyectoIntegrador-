import { useState } from "react";
import { Link } from "react-router-dom";
import { registrar } from "../services/authService";
import AuthLayout from "../components/AuthLayout";
import Campo from "../components/Campo";
import "./Login.css";

const FORMATO_CORREO = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

// Indicador de seguridad de la contraseña (regla HU-01: 8+ caracteres, mayúscula y número)
function fortaleza(password) {
  const reglas = [password.length >= 8, /[A-Z]/.test(password), /\d/.test(password)];
  const cumplidas = reglas.filter(Boolean).length;
  const nivel = password.length === 0 ? 0 : cumplidas;
  return { nivel, texto: ["", "Débil", "Casi lista", "Segura"][nivel] };
}

function Register() {
  const [nombre, setNombre] = useState("");
  const [correo, setCorreo] = useState("");
  const [password, setPassword] = useState("");
  const [confirmar, setConfirmar] = useState("");
  const [error, setError] = useState("");
  const [cargando, setCargando] = useState(false);
  const [registrado, setRegistrado] = useState(false);

  const seguridad = fortaleza(password);

  const handleSubmit = async (e) => {
    e.preventDefault();

    // 1. Campos obligatorios
    if (!nombre.trim() || !correo || !password || !confirmar) {
      setError("Completa todos los campos.");
      return;
    }

    // 2. Formato de correo
    if (!FORMATO_CORREO.test(correo)) {
      setError("Ingresa un correo válido.");
      return;
    }

    // 3. Regla de contraseña (criterio de aceptación HU-01)
    const formatoPassword = /^(?=.*[A-Z])(?=.*\d).{8,}$/;
    if (!formatoPassword.test(password)) {
      setError("La contraseña debe tener mínimo 8 caracteres, una mayúscula y un número.");
      return;
    }

    // 4. Las dos contraseñas deben coincidir
    if (password !== confirmar) {
      setError("Las contraseñas no coinciden.");
      return;
    }

    setError("");
    setCargando(true);

    try {
      await registrar(nombre.trim(), correo, password);
      setRegistrado(true);
    } catch (err) {
      setError(err.message);
    } finally {
      setCargando(false);
    }
  };

  // Pantalla de éxito
  if (registrado) {
    return (
      <AuthLayout titulo="¡Cuenta creada!" subtitulo="Tu registro se completó con éxito.">
        <div className="exito" role="status">
          <svg className="exito-check" viewBox="0 0 52 52" aria-hidden="true">
            <circle cx="26" cy="26" r="24" />
            <path d="m15 27 8 8 15-17" />
          </svg>
          <p>
            Ya puedes iniciar sesión con <strong>{correo}</strong>
          </p>
        </div>
        <Link to="/login" className="login-btn-link">Ir a iniciar sesión</Link>
      </AuthLayout>
    );
  }

  return (
    <AuthLayout
      titulo="Crea tu cuenta"
      subtitulo="Regístrate como cliente en menos de un minuto."
      pie={<>¿Ya tienes cuenta? <Link to="/login">Inicia sesión</Link></>}
    >
      <form onSubmit={handleSubmit} noValidate>
        {error && <p className="login-error" role="alert">{error}</p>}

        <Campo
          id="nombre"
          label="Nombre completo"
          autoComplete="name"
          autoFocus
          icono="usuario"
          placeholder="Juan Pérez"
          valor={nombre}
          alCambiar={(v) => { setNombre(v); setError(""); }}
        />

        <Campo
          id="correo"
          label="Correo electrónico"
          tipo="email"
          inputMode="email"
          autoComplete="email"
          icono="correo"
          placeholder="tucorreo@ejemplo.com"
          valor={correo}
          alCambiar={(v) => { setCorreo(v); setError(""); }}
        />

        <Campo
          id="password"
          label="Contraseña"
          tipo="password"
          autoComplete="new-password"
          icono="candado"
          placeholder="••••••••"
          valor={password}
          alCambiar={(v) => { setPassword(v); setError(""); }}
          ayuda={
            <div className="fuerza" data-nivel={seguridad.nivel}>
              <div className="fuerza-barras" aria-hidden="true"><i /><i /><i /></div>
              <p className="login-hint">
                {seguridad.texto ? `${seguridad.texto}. ` : ""}Mínimo 8 caracteres, una mayúscula y un número.
              </p>
            </div>
          }
        />

        <Campo
          id="confirmar"
          label="Confirmar contraseña"
          tipo="password"
          autoComplete="new-password"
          icono="candado"
          placeholder="••••••••"
          valor={confirmar}
          alCambiar={(v) => { setConfirmar(v); setError(""); }}
        />

        <button type="submit" disabled={cargando}>
          {cargando ? <span className="spinner" aria-hidden="true" /> : null}
          {cargando ? "Creando cuenta..." : "Crear cuenta"}
        </button>
      </form>
    </AuthLayout>
  );
}

export default Register;
