import "./admin-ui.css";

/* Interruptor activo / inactivo (se usa en usuarios y en servicios). */
function Switch({ activo, alCambiar, deshabilitado, cargando, etiqueta, titulo }) {
  return (
    <button
      type="button"
      role="switch"
      aria-checked={activo}
      aria-label={etiqueta}
      title={titulo}
      className={`switch${activo ? " on" : ""}${cargando ? " cargando" : ""}`}
      onClick={() => alCambiar(!activo)}
      disabled={deshabilitado || cargando}
    >
      <span className="switch-pista" aria-hidden="true">
        <span className="switch-bola" />
      </span>
      <span className="switch-texto">{activo ? "Activo" : "Inactivo"}</span>
    </button>
  );
}

export default Switch;
