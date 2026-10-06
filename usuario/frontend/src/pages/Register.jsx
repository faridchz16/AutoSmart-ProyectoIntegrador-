import { useState } from "react";
import { Link } from "react-router-dom";
import { registrar } from "../services/authService";
import "./Login.css";

function Register() {
  const [nombre, setNombre] = useState("");
  const [correo, setCorreo] = useState("");
  const [password, setPassword] = useState("");
  const [confirmar, setConfirmar] = useState("");
  const [error, setError] = useState("");
  const [cargando, setCargando] = useState(false);
  const [registrado, setRegistrado] = useState(false);

  const handleSubmit = async (e) => {
    e.preventDefault();

    // 1. Campos obligatorios
    if (!nombre.trim() || !correo || !password || !confirmar) {
      setError("Completa todos los campos.");
      return;
    }

    // 2. Formato de correo
    const formatoCorreo = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    if (!formatoCorreo.test(correo)) {
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
      <div className="login-page">
        <div className="login-card">
          <h1 className="login-logo">🚗 AutoSmart</h1>
          <p className="login-success">
            ¡Cuenta creada con éxito! Ya puedes iniciar sesión con <strong>{correo}</strong>.
          </p>
          <p className="login-footer">
            <Link to="/login">Ir a iniciar sesión</Link>
          </p>
        </div>
      </div>
    );
  }

  return (
    <div className="login-page">
      <div className="login-card">
        <h1 className="login-logo">🚗 AutoSmart</h1>
        <p className="login-subtitle">Crea tu cuenta de cliente</p>

        <form onSubmit={handleSubmit} noValidate>
          <label htmlFor="nombre">Nombre completo</label>
          <input
            id="nombre"
            type="text"
            placeholder="Juan Pérez"
            value={nombre}
            onChange={(e) => setNombre(e.target.value)}
          />

          <label htmlFor="correo">Correo electrónico</label>
          <input
            id="correo"
            type="email"
            placeholder="tucorreo@ejemplo.com"
            value={correo}
            onChange={(e) => setCorreo(e.target.value)}
          />

          <label htmlFor="password">Contraseña</label>
          <input
            id="password"
            type="password"
            placeholder="••••••••"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
          />
          <p className="login-hint">Mínimo 8 caracteres, una mayúscula y un número.</p>

          <label htmlFor="confirmar">Confirmar contraseña</label>
          <input
            id="confirmar"
            type="password"
            placeholder="••••••••"
            value={confirmar}
            onChange={(e) => setConfirmar(e.target.value)}
          />

          {error && <p className="login-error">{error}</p>}

          <button type="submit" disabled={cargando}>
            {cargando ? "Creando cuenta..." : "Crear cuenta"}
          </button>
        </form>

        <p className="login-footer">
          ¿Ya tienes cuenta? <Link to="/login">Inicia sesión</Link>
        </p>
      </div>
    </div>
  );
}

export default Register;