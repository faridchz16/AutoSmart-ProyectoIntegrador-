import { useEffect, useId, useRef } from "react";
import "./admin-ui.css";

/* Ventana modal accesible (usa <dialog>): Esc y clic fuera la cierran.
   El contenido se monta solo mientras está abierta, así los formularios se reinician. */
function Modal({ abierto, alCerrar, titulo, children, pequeno = false }) {
  const ref = useRef(null);
  const idTitulo = useId();

  useEffect(() => {
    const dialogo = ref.current;
    if (!dialogo) return;
    if (abierto && !dialogo.open) dialogo.showModal();
    if (!abierto && dialogo.open) dialogo.close();
  }, [abierto]);

  return (
    <dialog
      ref={ref}
      className={pequeno ? "modal modal-pequeno" : "modal"}
      aria-labelledby={idTitulo}
      onClose={alCerrar}
      onClick={(e) => e.target === ref.current && alCerrar()}
    >
      {abierto && (
        <div className="modal-caja">
          <header className="modal-cabecera">
            <h2 id={idTitulo}>{titulo}</h2>
            <button type="button" className="modal-cerrar" onClick={alCerrar} aria-label="Cerrar">
              <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" aria-hidden="true">
                <path d="M6 6l12 12M18 6 6 18" />
              </svg>
            </button>
          </header>
          {children}
        </div>
      )}
    </dialog>
  );
}

export default Modal;
