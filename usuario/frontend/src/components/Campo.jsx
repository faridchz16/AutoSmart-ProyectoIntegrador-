import { useState } from "react";

/* Campo de formulario reutilizable (login y registro):
   etiqueta, icono, mensaje de error y botón de ver/ocultar contraseña. */

const Icono = ({ children }) => (
  <svg className="input-icon" viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
    {children}
  </svg>
);

const ICONOS = {
  correo: (
    <Icono>
      <rect x="3" y="5" width="18" height="14" rx="2" />
      <path d="m3 7 9 6 9-6" />
    </Icono>
  ),
  candado: (
    <Icono>
      <rect x="4" y="11" width="16" height="10" rx="2" />
      <path d="M8 11V7a4 4 0 0 1 8 0v4" />
    </Icono>
  ),
  usuario: (
    <Icono>
      <circle cx="12" cy="8" r="3.6" />
      <path d="M4.5 20c.8-3.6 3.7-5.5 7.5-5.5s6.7 1.9 7.5 5.5" />
    </Icono>
  ),
};

const Ojo = ({ tachado }) => (
  <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
    <path d="M2 12s3.5-7 10-7 10 7 10 7-3.5 7-10 7S2 12 2 12Z" />
    <circle cx="12" cy="12" r="3" />
    {tachado && <path d="m3 3 18 18" />}
  </svg>
);

function Campo({
  id,
  label,
  tipo = "text",
  valor,
  alCambiar,
  alSalir,
  alSoltarTecla,
  error,
  icono,
  placeholder,
  autoComplete,
  autoFocus,
  inputMode,
  extraLabel,
  ayuda,
}) {
  const [ver, setVer] = useState(false);
  const esPassword = tipo === "password";

  return (
    <div className="field">
      <div className="label-row">
        <label htmlFor={id}>{label}</label>
        {extraLabel}
      </div>

      <div className={error ? "input-wrap has-error" : "input-wrap"}>
        {ICONOS[icono]}
        <input
          id={id}
          type={esPassword && ver ? "text" : tipo}
          inputMode={inputMode}
          autoComplete={autoComplete}
          autoFocus={autoFocus}
          placeholder={placeholder}
          value={valor}
          onChange={(e) => alCambiar(e.target.value)}
          onBlur={alSalir}
          onKeyUp={alSoltarTecla}
          aria-invalid={!!error}
          aria-describedby={error ? `${id}-error` : undefined}
        />
        {esPassword && (
          <button
            type="button"
            className="toggle-password"
            onClick={() => setVer((v) => !v)}
            aria-label={ver ? "Ocultar contraseña" : "Mostrar contraseña"}
            aria-pressed={ver}
          >
            <Ojo tachado={ver} />
          </button>
        )}
      </div>

      {error && (
        <p id={`${id}-error`} className="field-error" role="alert">
          {error}
        </p>
      )}
      {ayuda}
    </div>
  );
}

export default Campo;
