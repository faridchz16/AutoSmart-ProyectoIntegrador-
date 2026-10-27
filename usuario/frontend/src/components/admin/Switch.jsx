import "./admin-ui.css";

/* Interruptor activo / inactivo (se usa en usuarios, servicios y el horario del taller).
   textoSi / textoNo cambian lo que dice al lado (por defecto "Activo" / "Inactivo"). */
function Switch({ activo, alCambiar, deshabilitado, cargando, etiqueta, titulo, textoSi = "Activo", textoNo = "Inactivo" }) {
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
      <span className="switch-texto">{activo ? textoSi : textoNo}</span>
    </button>
  );
}

export default Switch;
