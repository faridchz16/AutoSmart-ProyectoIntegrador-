import { useState } from "react";
import { crearMecanico } from "../../services/usuariosService";

/* Formulario "Nuevo mecánico" (T15.2).
   El administrador crea la cuenta con una contraseña temporal y se la entrega al mecánico. */

const FORMATO_CORREO = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
const FORMATO_PASSWORD = /^(?=.*[A-Z])(?=.*\d).{8,}$/;

// Contraseña temporal que cumple la regla: 10 caracteres, con mayúscula y número
function generarPassword() {
  const may = "ABCDEFGHJKLMNPQRSTUVWXYZ";
  const min = "abcdefghijkmnpqrstuvwxyz";
  const num = "23456789";
  const azar = (s) => s[crypto.getRandomValues(new Uint32Array(1))[0] % s.length];
  const letras = [azar(may), azar(num), azar(min)];
  while (letras.length < 10) letras.push(azar(may + min + num));
  for (let i = letras.length - 1; i > 0; i--) {
    const j = crypto.getRandomValues(new Uint32Array(1))[0] % (i + 1);
    [letras[i], letras[j]] = [letras[j], letras[i]];
  }
  return letras.join("");
}

function validar({ nombre, correo, password }) {
  const errores = {};
  if (!nombre.trim()) errores.nombre = "Ingresa el nombre completo.";
  else if (nombre.trim().length < 3) errores.nombre = "El nombre es muy corto.";
  if (!correo.trim()) errores.correo = "Ingresa el correo.";
  else if (!FORMATO_CORREO.test(correo.trim())) errores.correo = "Ingresa un correo válido.";
  if (!FORMATO_PASSWORD.test(password)) {
    errores.password = "Mínimo 8 caracteres, una mayúscula y un número.";
  }
  return errores;
}

function NuevoMecanico({ alCrear, alCerrar }) {
  const [datos, setDatos] = useState(() => ({ nombre: "", correo: "", password: generarPassword() }));
  const [errores, setErrores] = useState({});
  const [errorGeneral, setErrorGeneral] = useState("");
  const [enviando, setEnviando] = useState(false);
  const [creado, setCreado] = useState(null);
  const [copiado, setCopiado] = useState(false);

  const cambiar = (campo) => (e) => {
    const valor = e.target.value;
    setDatos((d) => ({ ...d, [campo]: valor }));
    setErrorGeneral("");
    if (errores[campo]) setErrores((er) => ({ ...er, [campo]: "" }));
  };

  const enviar = async (e) => {
    e.preventDefault();
    const nuevos = validar(datos);
    setErrores(nuevos);
    if (Object.keys(nuevos).length) return;

    setEnviando(true);
    try {
      const resultado = await crearMecanico({
        nombre: datos.nombre.trim(),
        correo: datos.correo.trim().toLowerCase(),
        password: datos.password,
      });
      setCreado(resultado);
      alCrear(resultado.usuario);
    } catch (err) {
      setErrorGeneral(err.message);
    } finally {
      setEnviando(false);
    }
  };

  const copiar = async () => {
    const texto = `AutoSmart\nCorreo: ${creado.usuario.correo}\nContraseña temporal: ${creado.passwordTemporal}`;
    try {
      await navigator.clipboard.writeText(texto);
      setCopiado(true);
    } catch {
      setCopiado(false);
    }
  };

  // Cuenta creada: se muestran los datos para entregárselos al mecánico
  if (creado) {
    return (
      <div className="form-exito">
        <span className="form-exito-check" aria-hidden="true">✓</span>
        <p>
          Se creó la cuenta de <strong>{creado.usuario.nombre}</strong>. Entrégale estos datos
          por un medio seguro; esta es la única vez que verás la contraseña.
        </p>
        <dl className="credenciales">
          <dt>Correo</dt>
          <dd>{creado.usuario.correo}</dd>
          <dt>Contraseña temporal</dt>
          <dd><code>{creado.passwordTemporal}</code></dd>
        </dl>
        <div className="form-acciones">
          <button type="button" className="btn btn-sec" onClick={copiar}>
            {copiado ? "¡Copiado!" : "Copiar datos"}
          </button>
          <button type="button" className="btn btn-pri" onClick={alCerrar}>Listo</button>
        </div>
      </div>
    );
  }

  return (
    <form className="adm-form" onSubmit={enviar} noValidate>
      {errorGeneral && <p className="form-error-general" role="alert">{errorGeneral}</p>}

      <div className="campo">
        <label htmlFor="mec-nombre">Nombre completo</label>
        <input
          id="mec-nombre"
          autoComplete="off"
          autoFocus
          placeholder="Ej. Carlos Quispe"
          value={datos.nombre}
          onChange={cambiar("nombre")}
          aria-invalid={!!errores.nombre}
          aria-describedby={errores.nombre ? "mec-nombre-error" : undefined}
        />
        {errores.nombre && <p id="mec-nombre-error" className="campo-error">{errores.nombre}</p>}
      </div>

      <div className="campo">
        <label htmlFor="mec-correo">Correo electrónico</label>
        <input
          id="mec-correo"
          type="email"
          inputMode="email"
          autoComplete="off"
          placeholder="mecanico@correo.com"
          value={datos.correo}
          onChange={cambiar("correo")}
          aria-invalid={!!errores.correo}
          aria-describedby={errores.correo ? "mec-correo-error" : undefined}
        />
        {errores.correo && <p id="mec-correo-error" className="campo-error">{errores.correo}</p>}
      </div>

      <div className="campo">
        <label htmlFor="mec-password">Contraseña temporal</label>
        <div className="campo-con-boton">
          <input
            id="mec-password"
            autoComplete="off"
            spellCheck="false"
            value={datos.password}
            onChange={cambiar("password")}
            aria-invalid={!!errores.password}
            aria-describedby="mec-password-ayuda"
          />
          <button
            type="button"
            className="btn btn-sec"
            onClick={() => {
              setDatos((d) => ({ ...d, password: generarPassword() }));
              setErrores((er) => ({ ...er, password: "" }));
            }}
          >
            Generar otra
          </button>
        </div>
        <p id="mec-password-ayuda" className={errores.password ? "campo-error" : "campo-ayuda"}>
          {errores.password || "Se genera automáticamente. Mínimo 8 caracteres, una mayúscula y un número."}
        </p>
      </div>

      <div className="form-acciones">
        <button type="button" className="btn btn-sec" onClick={alCerrar} disabled={enviando}>
          Cancelar
        </button>
        <button type="submit" className="btn btn-pri" disabled={enviando}>
          {enviando && <span className="btn-spinner" aria-hidden="true" />}
          {enviando ? "Creando..." : "Crear mecánico"}
        </button>
      </div>
    </form>
  );
}

export default NuevoMecanico;
