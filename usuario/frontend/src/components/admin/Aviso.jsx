import { useEffect } from "react";
import "./admin-ui.css";

/* Mensaje flotante breve (éxito o error) que se oculta solo. */
function Aviso({ aviso, alCerrar }) {
  useEffect(() => {
    if (!aviso) return;
    const t = setTimeout(alCerrar, 3800);
    return () => clearTimeout(t);
  }, [aviso, alCerrar]);

  return (
    <div className="aviso-zona" role="status" aria-live="polite">
      {aviso && (
        <div key={aviso.id} className={`aviso aviso-${aviso.tipo}`}>
          <span className="aviso-icono" aria-hidden="true">{aviso.tipo === "error" ? "!" : "✓"}</span>
          <p>{aviso.texto}</p>
          <button type="button" onClick={alCerrar} aria-label="Cerrar aviso">×</button>
        </div>
      )}
    </div>
  );
}

export default Aviso;
