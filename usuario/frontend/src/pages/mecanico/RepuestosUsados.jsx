import { useEffect, useState } from "react";
import { registrarConsumo, listarMaterialesTaller, formatoSoles, MAX_REFERENCIA } from "../../services/ordenesService";
import { BuscadorAlmacen } from "./PresupuestoOrden";

/* T51.2 – Pantalla de repuestos usados en la orden (API de T51.1).
   - Lista las salidas de stock de la orden: las solicitudes aprobadas por el almacén (T50/T59)
     y el uso directo que registra el mecánico
   - Muestra el costo de repuestos que el backend acumula en la orden
   - "Registrar uso" descuenta el stock al instante (409 si no alcanza) */

function RepuestosUsados({ idOrden, resumen, editable, estado, momento, alCambiar, alConflicto }) {
  const [nombres, setNombres] = useState({}); // código -> nombre (la API de consumos solo trae el código)
  const [buscando, setBuscando] = useState(false);
  const [elegido, setElegido] = useState(null);
  const [cantidad, setCantidad] = useState("1");
  const [referencia, setReferencia] = useState("");
  const [error, setError] = useState("");
  const [enviando, setEnviando] = useState(false);
  const [ok, setOk] = useState("");

  const { consumos, costoRepuestos } = resumen;
  const hayConsumos = consumos.length > 0;

  useEffect(() => {
    if (!hayConsumos) return undefined;
    let vigente = true;
    listarMaterialesTaller()
      .then((lista) => vigente && setNombres(Object.fromEntries(lista.map((m) => [m.codigo, m.nombre]))))
      .catch(() => {});
    return () => {
      vigente = false;
    };
  }, [hayConsumos]);

  const limpiar = () => {
    setElegido(null);
    setCantidad("1");
    setReferencia("");
    setError("");
  };

  const elegir = (m) => {
    setElegido(m);
    setBuscando(false);
    setCantidad("1");
    setOk("");
    setError(m.stockActual === 0 ? `No hay stock de ${m.nombre}.` : "");
  };

  const registrar = async (e) => {
    e.preventDefault();
    const n = Number(cantidad);
    if (!Number.isInteger(n) || n < 1) return setError("La cantidad debe ser un número entero desde 1.");
    if (n > elegido.stockActual) return setError(`Solo hay ${elegido.stockActual} en el almacén.`);
    if (referencia.trim().length > MAX_REFERENCIA) return setError(`La nota puede tener hasta ${MAX_REFERENCIA} caracteres.`);
    setEnviando(true);
    setError("");
    try {
      alCambiar(await registrarConsumo(idOrden, { idMaterial: elegido.id, cantidad: n, referencia }));
      setNombres((nm) => ({ ...nm, [elegido.codigo]: elegido.nombre }));
      setOk(`Se registró ${n} × ${elegido.nombre} y se descontó del almacén.`);
      limpiar();
    } catch (err) {
      setError(err.status === 409 && err.estado ? `${err.message} Se actualizó la orden.` : err.message);
      if (err.status === 409 && err.estado) alConflicto();
    } finally {
      setEnviando(false);
    }
  };

  return (
    <div className="ot-usados">
      {hayConsumos ? (
        <>
          <ul className="ot-usados-lista">
            {consumos.map((c) => (
              <li key={c.id} className="ot-usado">
                <div className="ot-usado-cab">
                  <span className="ot-almacen-cod">{c.codigo}</span>
                  <strong>
                    {c.cantidad} × {nombres[c.codigo] ?? "Repuesto"}
                  </strong>
                  <span className="ot-usado-sub">{formatoSoles(c.subtotal)}</span>
                </div>
                <p className="ot-sol-meta">
                  {c.idSolicitud ? (
                    <span className="ot-usado-origen sol">Solicitud #{c.idSolicitud}</span>
                  ) : (
                    <span className="ot-usado-origen">Uso directo</span>
                  )}
                  {formatoSoles(c.costoUnitario)} c/u · {momento(c.fecha)}
                  {!c.idSolicitud && c.referencia ? ` · ${c.referencia}` : ""}
                </p>
              </li>
            ))}
          </ul>
          <div className="ot-usados-total">
            <span>Costo de repuestos</span>
            <strong>{formatoSoles(costoRepuestos)}</strong>
          </div>
        </>
      ) : (
        <p className="ot-diag-vacio">
          {estado === "FINALIZADA"
            ? "No se usaron repuestos del almacén en esta orden."
            : "Aún no hay repuestos usados. Aparecerán aquí cuando el almacén apruebe una solicitud o registres un uso directo."}
        </p>
      )}

      {editable &&
        (elegido ? (
          <form className="ot-sol-form" onSubmit={registrar} noValidate>
            <div className="ot-sol-elegido">
              <span className="ot-almacen-cod">{elegido.codigo}</span>
              <strong>{elegido.nombre}</strong>
              <span className={elegido.stockActual === 0 ? "ot-almacen-stock agotado" : "ot-almacen-stock"}>
                Disponible: {elegido.stockActual}
              </span>
              <button type="button" className="ot-ver-mas" onClick={() => setBuscando(true)}>
                Cambiar
              </button>
            </div>
            <div className="ot-sol-campos">
              <div className="campo">
                <label htmlFor="ot-uso-cant">Cantidad</label>
                <input
                  id="ot-uso-cant"
                  type="number"
                  min="1"
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
                <label htmlFor="ot-uso-ref">
                  Nota <span className="opcional">(opcional)</span>
                </label>
                <input
                  id="ot-uso-ref"
                  maxLength={MAX_REFERENCIA}
                  placeholder="Ej. Limpieza de frenos"
                  value={referencia}
                  onChange={(e) => setReferencia(e.target.value)}
                  disabled={elegido.stockActual === 0}
                />
              </div>
            </div>
            <p className="campo-ayuda">
              Costo: {formatoSoles(elegido.costoUnitario * (Number(cantidad) || 0))}. Se descuenta del stock al registrarlo.
            </p>
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
                {enviando ? "Registrando..." : "Registrar uso"}
              </button>
            </div>
          </form>
        ) : (
          !buscando && (
            <button type="button" className="ot-diag-base ot-usados-btn" onClick={() => setBuscando(true)}>
              + Registrar uso directo del almacén
            </button>
          )
        ))}
      {editable && buscando && <BuscadorAlmacen alElegir={elegir} alCerrar={() => setBuscando(false)} />}
      {ok && (
        <p className="ot-sol-ok" role="status">
          ✓ {ok}
        </p>
      )}
    </div>
  );
}

export default RepuestosUsados;
