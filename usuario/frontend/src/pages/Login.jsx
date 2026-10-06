import { useState } from "react";
import { login } from "../services/authService";
import "./Login.css";

function Login() {
  const [correo, setCorreo] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");
  const [cargando, setCargando] = useState(false); // ¿estamos esperando al backend?
  const [usuario, setUsuario] = useState(null);    // datos del usuario si el login fue exitoso

  const handleSubmit = async (e) => {
    e.preventDefault();

    if (!correo || !password) {
      setError("Completa todos los campos.");
      return;
    }

    const formatoCorreo = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    if (!formatoCorreo.test(correo)) {
      setError("Ingresa un correo válido.");
      return;
    }

    setError("");
    setCargando(true);

    try {
      const datos = await login(correo, password);

      // Guardamos el token para usarlo en las siguientes pantallas
      localStorage.setItem("token", datos.token);
      localStorage.setItem("usuario", JSON.stringify({ correo: datos.correo, rol: datos.rol }));

      setUsuario(datos);
    } catch (err) {
      setError(err.message);
    } finally {
      setCargando(false);
    }
  };

  // Si el login fue exitoso, mostramos un mensaje de bienvenida
  if (usuario) {
    return (
      <div className="login-page">
        <div className="login-card">
          <h1 className="login-logo">🚗 AutoSmart</h1>
          <p className="login-success">
            ¡Bienvenido! Iniciaste sesión como <strong>{usuario.correo}</strong> (rol: {usuario.rol}).
          </p>
        </div>
      </div>
    );
  }

  return (
    <div className="login-page">
      <div className="login-card">
        <h1 className="login-logo">🚗 AutoSmart</h1>
        <p className="login-subtitle">Inicia sesión para gestionar tus vehículos</p>

        <form onSubmit={handleSubmit} noValidate>
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

          {error && <p className="login-error">{error}</p>}

          <button type="submit" disabled={cargando}>
            {cargando ? "Ingresando..." : "Iniciar sesión"}
          </button>
        </form>

        <p className="login-footer">
          ¿No tienes cuenta? <a href="#">Regístrate</a>
        </p>
      </div>
    </div>
  );
}

export default Login;