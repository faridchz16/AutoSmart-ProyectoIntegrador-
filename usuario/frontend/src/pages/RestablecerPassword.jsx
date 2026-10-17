import { useEffect, useState } from "react";
import { Link, useLocation } from "react-router-dom";
import { solicitarCodigo, restablecerPassword } from "../services/authService";
import { DATOS_PRUEBA, CODIGO_PRUEBA } from "../services/datosPrueba";
import AuthLayout from "../components/AuthLayout";
import Campo from "../components/Campo";
import "./Login.css";
import "./Recuperar.css";

/* T04.3 – Paso 2: "Nueva contraseña"
   El usuario escribe el código que recibió y su nueva contraseña. */

const FORMATO_CORREO = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
const FORMATO_PASSWORD = /^(?=.*[A-Z])(?=.*\d).{8,}$/;
const VIGENCIA = 15 * 60; // segundos que dura el código
const ESPERA_REENVIO = 60; // segundos antes de poder pedir otro código

function fortaleza(password) {
  const cumplidas = [password.length >= 8, /[A-Z]/.test(password), /\d/.test(password)].filter(Boolean).length;
  const nivel = password.length === 0 ? 0 : cumplidas;
  return { nivel, texto: ["", "Débil", "Casi lista", "Segura"][nivel] };
}

const reloj = (s) => `${String(Math.floor(s / 60)).padStart(2, "0")}:${String(s % 60).padStart(2, "0")}`;

function RestablecerPassword() {
  const { state } = useLocation();
  const correoRecibido = state?.correo ?? "";

  const [correo, setCorreo] = useState(correoRecibido);
  const [codigo, setCodigo] = useState("");
  const [password, setPassword] = useState("");
  const [confirmar, setConfirmar] = useState("");
  const [errores, setErrores] = useState({});
  const [errorGeneral, setErrorGeneral] = useState("");
  const [enviando, setEnviando] = useState(false);
  const [listo, setListo] = useState(false);

  // Cuenta regresiva del código (solo si sabemos cuándo se envió)
  const [enviadoEn, setEnviadoEn] = useState(state?.enviadoEn ?? null);
  const [restante, setRestante] = useState(VIGENCIA);
  const [reenviando, setReenviando] = useState(false);
  const [reenviado, setReenviado] = useState(false);

  useEffect(() => {
    if (!enviadoEn) return;
    const fin = enviadoEn + VIGENCIA * 1000;
    const t = setInterval(() => setRestante(Math.max(0, Math.round((fin - Date.now()) / 1000))), 1000);
    return () => clearInterval(t);
  }, [enviadoEn]);

  const vencido = enviadoEn && restante === 0;
  const esperaReenvio = enviadoEn ? Math.max(0, restante - (VIGENCIA - ESPERA_REENVIO)) : 0;
  const seguridad = fortaleza(password);

  const limpiar = (campo) => {
    setErrorGeneral("");
    if (errores[campo]) setErrores((er) => ({ ...er, [campo]: "" }));
  };

  const reenviar = async () => {
    const limpio = correo.trim().toLowerCase();
    if (!FORMATO_CORREO.test(limpio)) {
      setErrores((er) => ({ ...er, correo: "Ingresa un correo válido para reenviar el código." }));
      return;
    }
    setReenviando(true);
    setErrorGeneral("");
    try {
      await solicitarCodigo(limpio);
      setEnviadoEn(Date.now());
      setRestante(VIGENCIA);
      setCodigo("");
      setReenviado(true);
    } catch (err) {
      setErrorGeneral(err.message);
    } finally {
      setReenviando(false);
    }
  };

  const enviar = async (e) => {
    e.preventDefault();
    const limpio = correo.trim().toLowerCase();
    const nuevos = {};
    if (!FORMATO_CORREO.test(limpio)) nuevos.correo = "Ingresa un correo válido.";
    if (!/^\d{6}$/.test(codigo)) nuevos.codigo = "El código tiene 6 dígitos.";
    if (!FORMATO_PASSWORD.test(password)) nuevos.password = "Mínimo 8 caracteres, una mayúscula y un número.";
    if (!nuevos.password && password !== confirmar) nuevos.confirmar = "Las contraseñas no coinciden.";
    setErrores(nuevos);
    if (Object.keys(nuevos).length) return;

    if (vencido) {
      setErrorGeneral("El código venció. Pide uno nuevo con «Reenviar código».");
      return;
    }

    setEnviando(true);
    try {
      await restablecerPassword(limpio, codigo, password);
      setListo(true);
    } catch (err) {
      setErrorGeneral(err.message);
    } finally {
      setEnviando(false);
    }
  };

  if (listo) {
    return (
      <AuthLayout titulo="¡Contraseña actualizada!" subtitulo="Ya puedes ingresar con tu nueva contraseña.">
        <div className="exito" role="status">
          <svg className="exito-check" viewBox="0 0 52 52" aria-hidden="true">
            <circle cx="26" cy="26" r="24" />
            <path d="m15 27 8 8 15-17" />
          </svg>
          <p>
            Cambiaste la contraseña de <strong>{correo.trim().toLowerCase()}</strong>
          </p>
        </div>
        <Link to="/login" className="login-btn-link">Ir a iniciar sesión</Link>
      </AuthLayout>
    );
  }

  return (
    <AuthLayout
      titulo="Crea una nueva contraseña"
      subtitulo="Ingresa el código que te enviamos y elige tu nueva contraseña."
      pie={<>¿No era tu correo? <Link to="/recuperar">Usar otro correo</Link></>}
    >
      <form onSubmit={enviar} noValidate>
        {errorGeneral && <p className="login-error" role="alert">{errorGeneral}</p>}

        {correoRecibido ? (
          <div className="recuperar-info">
            <p>
              Enviamos un código a <strong>{correoRecibido}</strong>
            </p>
            {enviadoEn && (
              <span className={vencido ? "reloj vencido" : restante <= 60 ? "reloj poco" : "reloj"} aria-live="off">
                {vencido ? "Código vencido" : `Vence en ${reloj(restante)}`}
              </span>
            )}
          </div>
        ) : (
          <Campo
            id="correo"
            label="Correo electrónico"
            tipo="email"
            inputMode="email"
            autoComplete="email"
            icono="correo"
            placeholder="tucorreo@ejemplo.com"
            valor={correo}
            alCambiar={(v) => {
              setCorreo(v);
              limpiar("correo");
            }}
            error={errores.correo}
          />
        )}

        <div className="field">
          <div className="label-row">
            <label htmlFor="codigo">Código de verificación</label>
            <button
              type="button"
              className="link-reenviar"
              onClick={reenviar}
              disabled={reenviando || esperaReenvio > 0}
            >
              {reenviando ? "Enviando..." : esperaReenvio > 0 ? `Reenviar en ${esperaReenvio} s` : "Reenviar código"}
            </button>
          </div>
          <div className={errores.codigo ? "codigo-wrap has-error" : "codigo-wrap"}>
            <input
              id="codigo"
              inputMode="numeric"
              autoComplete="one-time-code"
              autoFocus={!!correoRecibido}
              maxLength={6}
              placeholder="000000"
              value={codigo}
              onChange={(e) => {
                setCodigo(e.target.value.replace(/\D/g, "").slice(0, 6));
                limpiar("codigo");
              }}
              aria-invalid={!!errores.codigo}
              aria-describedby={errores.codigo ? "codigo-error" : undefined}
            />
          </div>
          {errores.codigo && <p id="codigo-error" className="field-error" role="alert">{errores.codigo}</p>}
          {reenviado && !errores.codigo && <p className="field-ok">Te enviamos un código nuevo.</p>}
          {DATOS_PRUEBA && (
            <p className="nota-prueba">Modo de prueba: usa el código <strong>{CODIGO_PRUEBA}</strong>.</p>
          )}
        </div>

        <Campo
          id="password"
          label="Nueva contraseña"
          tipo="password"
          autoComplete="new-password"
          icono="candado"
          placeholder="••••••••"
          valor={password}
          alCambiar={(v) => {
            setPassword(v);
            limpiar("password");
          }}
          error={errores.password}
          ayuda={
            !errores.password && (
              <div className="fuerza" data-nivel={seguridad.nivel}>
                <div className="fuerza-barras" aria-hidden="true"><i /><i /><i /></div>
                <p className="login-hint">
                  {seguridad.texto ? `${seguridad.texto}. ` : ""}Mínimo 8 caracteres, una mayúscula y un número.
                </p>
              </div>
            )
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
          alCambiar={(v) => {
            setConfirmar(v);
            limpiar("confirmar");
          }}
          error={errores.confirmar}
        />

        <button type="submit" disabled={enviando}>
          {enviando ? <span className="spinner" aria-hidden="true" /> : null}
          {enviando ? "Guardando..." : "Guardar nueva contraseña"}
        </button>
      </form>
    </AuthLayout>
  );
}

export default RestablecerPassword;
