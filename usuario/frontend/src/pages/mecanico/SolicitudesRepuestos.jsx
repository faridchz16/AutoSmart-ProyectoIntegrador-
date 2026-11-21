import { useState } from "react";
import { solicitarRepuesto, ESTADOS_SOLICITUD, MAX_MOTIVO } from "../../services/ordenesService";
import { BuscadorAlmacen } from "./PresupuestoOrden";

/* T50.2 – Buscador y formulario de solicitud de repuestos al almacén (API de T50.1).
   - El mecánico busca el repuesto, indica la cantidad y, si quiere, el motivo
   - El backend rechaza (409) si no hay stock suficiente
   - El administrador la aprueba o rechaza en su bandeja (T59); al aprobarla se descuenta el stock
     y el repuesto pasa a los repuestos usados de la orden (T51) */

function SolicitudesRepuestos({ idOrden, solicitudes, editable, estado, momento, alCambiar, alConflicto }) {
  const [buscando, setBuscando] = useState(false);
  const [elegido, setElegido] = useState(null);
  const [cantidad, setCantidad] = useState("1");
  const [motivo, setMotivo] = useState("");
  const [error, setError] = useState("");
  const [enviando, setEnviando] = useState(false);
  const [enviada, setEnviada] = useState("");

  const limpiar = () => {
    setElegido(null);
    setCantidad("1");
    setMotivo("");
    setError("");
  };

  const elegir = (m) => {
    setElegido(m);
    setBuscando(false);
    setCantidad("1");
    setError(m.stockActual === 0 ? `No hay stock de ${m.nombre}. Avisa al administrador para que lo reponga.` : "");
    setEnviada("");
  };

  const enviar = async (e) => {
    e.preventDefault();
    const n = Number(cantidad);
    if (!Number.isInteger(n) || n < 1) return setError("La cantidad debe ser un número entero desde 1.");
    if (n > elegido.stockActual) return setError(`Solo hay ${elegido.stockActual} en el almacén.`);
    if (motivo.trim().length > MAX_MOTIVO) return setError(`El motivo puede tener hasta ${MAX_MOTIVO} caracteres.`);
    setEnviando(true);
    setError("");
    try {
      const nueva = await solicitarRepuesto(idOrden, { idMaterial: elegido.id, cantidad: n, motivo });
      alCambiar([...solicitudes, nueva]);
      setEnviada(`Se envió la solicitud de ${n} × ${elegido.nombre} al almacén.`);
      limpiar();
    } catch (err) {
      setError(err.status === 409 && err.estado ? `${err.message} Se actualizó la orden.` : err.message);
      if (err.status === 409 && err.estado) alConflicto();
    } finally {
      setEnviando(false);
    }
  };

  const lista = [...solicitudes].sort((a, b) => b.fechaSolicitud.localeCompare(a.fechaSolicitud));

  return (
    <div className="ot-sol">
      {/* ----- Nueva solicitud ----- */}
      {editable &&
        (elegido ? (
          <form className="ot-sol-form" onSubmit={enviar} noValidate>
            <div className="ot-sol-elegido">
              <span className="ot-almacen-cod">{elegido.codigo}</span>
              <strong>{elegido.nombre}</strong>
              <span className={elegido.stockActual === 0 ? "ot-almacen-stock agotado" : elegido.bajoStock ? "ot-almacen-stock bajo" : "ot-almacen-stock"}>
                Disponible: {elegido.stockActual}
              </span>
              <button type="button" className="ot-ver-mas" onClick={() => setBuscando(true)}>
                Cambiar
              </button>
            </div>
            <div className="ot-sol-campos">
              <div className="campo">
                <label htmlFor="ot-sol-cant">Cantidad</label>
                <input
                  id="ot-sol-cant"
                  type="number"
                  min="1"
                  max={elegido.stockActual || undefined}
                  step="1"
                  inputMode="numeric"
                  value={cantidad}
                  onChange={(e) => {
                    setCantidad(e.target.value);
                    setError("");
                  }}
                  disabled={elegido.stockActual === 0}
                />
              </div>
              <div className="campo">
                <label htmlFor="ot-sol-motivo">
                  Motivo <span className="opcional">(opcional)</span>
                </label>
                <input
                  id="ot-sol-motivo"
                  maxLength={MAX_MOTIVO}
                  placeholder="Ej. Cambio de amortiguadores aprobado por el cliente"
                  value={motivo}
                  onChange={(e) => setMotivo(e.target.value)}
                  disabled={elegido.stockActual === 0}
                />
              </div>
            </div>
            {error && (
              <p className="campo-error" role="alert">
                {error}
              </p>
            )}
            <div className="form-acciones">
              <button type="button" className="btn btn-sec" onClick={limpiar} disabled={enviando}>
                Cancelar
              </button>
              <button type="submit" className="btn btn-pri" disabled={enviando || elegido.stockActual === 0}>
                {enviando && <span className="btn-spinner" aria-hidden="true" />}
                {enviando ? "Enviando..." : "Enviar solicitud"}
              </button>
            </div>
          </form>
        ) : (
          !buscando && (
            <div className="ot-sol-inicio">
              <p>¿Necesitas un repuesto? Pídelo al almacén; el administrador lo aprueba y se descuenta del stock.</p>
              <button type="button" className="btn btn-pri" onClick={() => setBuscando(true)}>
                Solicitar repuesto
              </button>
            </div>
          )
        ))}
      {editable && buscando && <BuscadorAlmacen alElegir={elegir} alCerrar={() => setBuscando(false)} />}
      {enviada && (
        <p className="ot-sol-ok" role="status">
          ✓ {enviada}
        </p>
      )}

      {/* ----- Solicitudes de la orden ----- */}
      {lista.length === 0 ? (
        !editable && (
          <p className="ot-diag-vacio">
            {estado === "FINALIZADA" ? "No se solicitaron repuestos en esta orden." : "Aún no hay solicitudes de repuestos."}
          </p>
        )
      ) : (
        <ul className="ot-sol-lista">
          {lista.map((s) => (
            <li key={s.id} className={`ot-sol-item sol-${s.estado}`}>
              <div className="ot-sol-cab">
                <span className="ot-almacen-cod">{s.codigo}</span>
                <strong>
                  {s.cantidad} × {s.nombre}
                </strong>
                <span className={`ot-sol-estado sol-${s.estado}`}>{ESTADOS_SOLICITUD[s.estado]}</span>
              </div>
              {s.motivo && <p className="ot-sol-motivo">{s.motivo}</p>}
              <p className="ot-sol-meta">
                Solicitada {momento(s.fechaSolicitud)}
                {s.estado !== "PENDIENTE" && s.fechaAtencion && ` · ${s.estado === "APROBADA" ? "aprobada" : "rechazada"} ${momento(s.fechaAtencion)}`}
                {s.estado === "PENDIENTE" && s.stockDisponible !== null && ` · stock actual ${s.stockDisponible}`}
              </p>
              {s.observacion && <p className="ot-sol-obs">Almacén: {s.observacion}</p>}
            </li>
          ))}
        </ul>
      )}
      {!editable && estado === "ESPERA_APROBACION" && (
        <p className="ot-diag-bloqueo">No se piden repuestos mientras el cliente revisa el presupuesto.</p>
      )}
    </div>
  );
}

export default SolicitudesRepuestos;
