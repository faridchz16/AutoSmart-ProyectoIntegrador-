import { useEffect, useState } from "react";
import {
  registrarIngreso,
  obtenerKardex,
  armarReferencia,
  textoUnidad,
  formatoSoles,
  formatoCantidad,
  fechaHora,
  MAX_REFERENCIA,
  MAX_CANTIDAD_INGRESO,
} from "../../services/inventarioService";

/* T58.2 – Formulario de ingreso de mercadería (proveedor, cantidad y fecha) con el kardex del material.
   API de T58.1: POST /admin/materials/{id}/entries y GET /admin/materials/{id}/kardex.
   - El stock sube con la cantidad ingresada y el movimiento queda en el kardex
   - Si cambia el costo unitario, se actualiza el costo del material
   - Proveedor, documento y fecha de recepción se guardan en la referencia del movimiento
     (el servidor registra la fecha y hora en que se ingresa) */

const hoyLima = () => new Intl.DateTimeFormat("sv-SE", { timeZone: "America/Lima" }).format(new Date());

function Kardex({ idMaterial, unidad }) {
  const [estado, setEstado] = useState({ clave: null, movimientos: [], error: "" });

  useEffect(() => {
    let vigente = true;
    obtenerKardex(idMaterial)
      .then((movimientos) => vigente && setEstado({ clave: idMaterial, movimientos, error: "" }))
      .catch((e) => vigente && setEstado({ clave: idMaterial, movimientos: [], error: e.message }));
    return () => {
      vigente = false;
    };
  }, [idMaterial]);

  if (estado.clave === null) return <p className="inv-kardex-msg">Cargando movimientos...</p>;
  if (estado.error) return <p className="inv-kardex-msg error">No se pudo cargar el kardex: {estado.error}</p>;
  if (!estado.movimientos.length) return <p className="inv-kardex-msg">Este material aún no tiene movimientos.</p>;

  return (
    <ol className="inv-kardex">
      {estado.movimientos.slice(0, 8).map((m) => (
        <li key={m.id} className={`inv-mov mov-${m.tipo}`}>
          <span className="inv-mov-cant">
            {m.tipo === "ENTRADA" ? "+" : "−"}
            {formatoCantidad(m.cantidad)}
          </span>
          <div className="inv-mov-texto">
            <strong>
              {m.tipo === "ENTRADA" ? "Ingreso" : m.idOrden ? `Salida a la orden N.º ${m.idOrden}` : "Salida"}
            </strong>
            <span>
              {fechaHora(m.fecha)}
              {m.referencia ? ` · ${m.referencia}` : ""}
            </span>
          </div>
          <span className="inv-mov-stock">
            Stock {formatoCantidad(m.stockResultante)} {textoUnidad(unidad, m.stockResultante)}
          </span>
        </li>
      ))}
    </ol>
  );
}

function FormIngreso({ material, alGuardar, alCerrar }) {
  const hoy = hoyLima();
  const [datos, setDatos] = useState({
    proveedor: "",
    documento: "",
    fecha: hoy,
    cantidad: "",
    costo: material.costoUnitario.toFixed(2),
  });
  const [errores, setErrores] = useState({});
  const [errorGeneral, setErrorGeneral] = useState("");
  const [enviando, setEnviando] = useState(false);

  const cambiar = (campo) => (e) => {
    setDatos((d) => ({ ...d, [campo]: e.target.value }));
    setErrorGeneral("");
    if (errores[campo]) setErrores((er) => ({ ...er, [campo]: "" }));
  };

  const cantidad = Number(datos.cantidad);
  const costo = Number(String(datos.costo).replace(",", "."));
  const costoCambia = datos.costo !== "" && Math.round(costo * 100) !== Math.round(material.costoUnitario * 100);
  const referencia = armarReferencia({ proveedor: datos.proveedor, documento: datos.documento, fechaRecepcion: datos.fecha, hoy });
  const stockFinal = material.stockActual + (Number.isInteger(cantidad) && cantidad > 0 ? cantidad : 0);

  const enviar = async (e) => {
    e.preventDefault();
    const er = {};
    if (!datos.proveedor.trim()) er.proveedor = "Indica el proveedor.";
    else if (datos.proveedor.trim().length > 80) er.proveedor = "Máximo 80 caracteres.";
    if (datos.documento.trim().length > 40) er.documento = "Máximo 40 caracteres.";
    if (!datos.fecha) er.fecha = "Indica la fecha de recepción.";
    else if (datos.fecha > hoy) er.fecha = "La fecha no puede ser futura.";
    if (datos.cantidad === "") er.cantidad = "Ingresa la cantidad recibida.";
    else if (!Number.isInteger(cantidad) || cantidad < 1) er.cantidad = "Usa un número entero desde 1.";
    else if (cantidad > MAX_CANTIDAD_INGRESO) er.cantidad = `Máximo ${MAX_CANTIDAD_INGRESO.toLocaleString("es-PE")}.`;
    if (datos.costo !== "" && (!/^\d+(\.\d{1,2})?$/.test(String(datos.costo).replace(",", ".")) || costo < 0)) {
      er.costo = "Costo inválido (≥ 0, máx. 2 decimales).";
    }
    if (referencia.length > MAX_REFERENCIA) er.proveedor = "Proveedor y documento son demasiado largos.";
    setErrores(er);
    if (Object.keys(er).length) return;

    setEnviando(true);
    try {
      const mov = await registrarIngreso(material.id, { cantidad, costoUnitario: costoCambia ? costo : null, referencia });
      alGuardar({ ...material, stockActual: mov.stockResultante, costoUnitario: mov.costoUnitario }, mov);
    } catch (err) {
      setErrorGeneral(err.message);
      setEnviando(false);
    }
  };

  return (
    <div className="inv-ingreso">
      <div className="inv-ing-material">
        <span className="inv-codigo">{material.codigo}</span>
        <strong>{material.nombre}</strong>
        <span>
          Stock actual: {formatoCantidad(material.stockActual)} {textoUnidad(material.unidad, material.stockActual)} · costo{" "}
          {formatoSoles(material.costoUnitario)}
        </span>
      </div>

      <form className="adm-form inv-form" onSubmit={enviar} noValidate>
        {errorGeneral && (
          <p className="form-error-general" role="alert">
            {errorGeneral}
          </p>
        )}

        <div className="campo-doble">
          <div className="campo">
            <label htmlFor="ing-proveedor">Proveedor</label>
            <input
              id="ing-proveedor"
              autoFocus
              autoComplete="off"
              maxLength={80}
              placeholder="Ej. Repuestos Lima S.A.C."
              value={datos.proveedor}
              onChange={cambiar("proveedor")}
              aria-invalid={!!errores.proveedor}
              aria-describedby={errores.proveedor ? "ing-proveedor-error" : undefined}
            />
            {errores.proveedor && <p id="ing-proveedor-error" className="campo-error">{errores.proveedor}</p>}
          </div>
          <div className="campo">
            <label htmlFor="ing-documento">
              Guía o factura <span className="opcional">(opcional)</span>
            </label>
            <input
              id="ing-documento"
              autoComplete="off"
              maxLength={40}
              placeholder="Ej. F001-000123"
              value={datos.documento}
              onChange={cambiar("documento")}
              aria-invalid={!!errores.documento}
            />
            {errores.documento && <p className="campo-error">{errores.documento}</p>}
          </div>
        </div>

        <div className="inv-triple">
          <div className="campo">
            <label htmlFor="ing-fecha">Fecha de recepción</label>
            <input id="ing-fecha" type="date" max={hoy} value={datos.fecha} onChange={cambiar("fecha")} aria-invalid={!!errores.fecha} />
            {errores.fecha && <p className="campo-error">{errores.fecha}</p>}
          </div>
          <div className="campo">
            <label htmlFor="ing-cantidad">Cantidad</label>
            <input
              id="ing-cantidad"
              type="number"
              inputMode="numeric"
              min="1"
              step="1"
              placeholder="0"
              value={datos.cantidad}
              onChange={cambiar("cantidad")}
              aria-invalid={!!errores.cantidad}
              aria-describedby="ing-cantidad-ayuda"
            />
            <p id="ing-cantidad-ayuda" className={errores.cantidad ? "campo-error" : "campo-ayuda"}>
              {errores.cantidad || `En ${textoUnidad(material.unidad, 2)}.`}
            </p>
          </div>
          <div className="campo">
            <label htmlFor="ing-costo">Costo unitario</label>
            <div className="campo-prefijo">
              <span aria-hidden="true">S/</span>
              <input
                id="ing-costo"
                type="number"
                inputMode="decimal"
                min="0"
                step="0.01"
                value={datos.costo}
                onChange={cambiar("costo")}
                aria-invalid={!!errores.costo}
                aria-describedby="ing-costo-ayuda"
              />
            </div>
            <p id="ing-costo-ayuda" className={errores.costo ? "campo-error" : "campo-ayuda"}>
              {errores.costo || (costoCambia ? "Se actualizará el costo." : "Igual al actual.")}
            </p>
          </div>
        </div>

        <div className="inv-nota" role="note">
          <strong>
            Stock después del ingreso: {formatoCantidad(stockFinal)} {textoUnidad(material.unidad, stockFinal)}
          </strong>
          <span>
            {referencia ? `Quedará registrado como «${referencia}». ` : ""}El sistema guarda la fecha y hora en que registras el
            ingreso.
          </span>
        </div>

        <div className="form-acciones">
          <button type="button" className="btn btn-sec" onClick={alCerrar} disabled={enviando}>
            Cancelar
          </button>
          <button type="submit" className="btn btn-pri" disabled={enviando}>
            {enviando && <span className="btn-spinner" aria-hidden="true" />}
            {enviando ? "Registrando..." : "Registrar ingreso"}
          </button>
        </div>
      </form>

      <section className="inv-ing-kardex" aria-labelledby="ing-kardex-titulo">
        <h4 id="ing-kardex-titulo">Últimos movimientos (kardex)</h4>
        <Kardex idMaterial={material.id} unidad={material.unidad} />
      </section>
    </div>
  );
}

export default FormIngreso;
