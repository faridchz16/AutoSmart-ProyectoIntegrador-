import { useState } from "react";
import "./Login.css";

function Login() {
  // 1. ESTADOS: "cajitas" que guardan lo que el usuario escribe
  const [correo, setCorreo] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");

  // 2. Se ejecuta al presionar "Iniciar sesión"
  const handleSubmit = (e) => {
    e.preventDefault(); // evita que la página se recargue

    // Validación: campos vacíos
    if (!correo || !password) {
      setError("Completa todos los campos.");
      return;
    }

    // Validación: formato de correo
    const formatoCorreo = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    if (!formatoCorreo.test(correo)) {
      setError("Ingresa un correo válido.");
      return;
    }

    setError("");
    // En el Paso 5 aquí llamaremos al backend (POST /auth/login)
    alert(`Formulario listo para enviar: ${correo}`);
  };

  // 3. Lo que se dibuja en pantalla (JSX)
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

          <button type="submit">Iniciar sesión</button>
        </form>

        <p className="login-footer">
          ¿No tienes cuenta? <a href="#">Regístrate</a>
        </p>
      </div>
    </div>
  );
}

export default Login;