import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { solicitarCodigo } from "../services/authService";
import AuthLayout from "../components/AuthLayout";
import Campo from "../components/Campo";
import "./Login.css";
import "./Recuperar.css";

/* T04.3 – Paso 1: "¿Olvidaste tu contraseña?"
   El usuario escribe su correo y se le envía un código de 6 dígitos. */

const FORMATO_CORREO = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

function Recuperar() {
  const navigate = useNavigate();
  const [correo, setCorreo] = useState("");
  const [error, setError] = useState("");
  const [errorGeneral, setErrorGeneral] = useState("");
  const [enviando, setEnviando] = useState(false);

  const enviar = async (e) => {
    e.preventDefault();
    const limpio = correo.trim().toLowerCase();

    if (!limpio) return setError("Ingresa tu correo electrónico.");
    if (!FORMATO_CORREO.test(limpio)) return setError("Ingresa un correo válido.");

    setEnviando(true);
    try {
      await solicitarCodigo(limpio);
      navigate("/recuperar/nueva", { state: { correo: limpio, enviadoEn: Date.now() } });
    } catch (err) {
      setErrorGeneral(err.message);
      setEnviando(false);
    }
  };

  return (
    <AuthLayout
      titulo="¿Olvidaste tu contraseña?"
      subtitulo="Escribe el correo de tu cuenta y te enviaremos un código para crear una nueva."
      pie={<>¿La recordaste? <Link to="/login">Inicia sesión</Link></>}
    >
      <form onSubmit={enviar} noValidate>
        {errorGeneral && <p className="login-error" role="alert">{errorGeneral}</p>}

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
          alCambiar={(v) => {
            setCorreo(v);
            setError("");
            setErrorGeneral("");
          }}
          error={error}
        />

        <button type="submit" disabled={enviando}>
          {enviando ? <span className="spinner" aria-hidden="true" /> : null}
          {enviando ? "Enviando código..." : "Enviar código"}
        </button>

        <p className="recuperar-nota">
          El código llega en unos segundos y vence en 15 minutos. Revisa también tu carpeta de spam.
        </p>
      </form>
    </AuthLayout>
  );
}

export default Recuperar;
