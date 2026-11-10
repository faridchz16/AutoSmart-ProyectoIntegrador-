import { useEffect, useMemo, useState } from "react";
import {
  enviarPresupuesto,
  listarMaterialesTaller,
  formatoSoles,
  TIPOS_ITEM,
  ESTADOS_PRESUPUESTO,
  MAX_DESC_PRESUPUESTO,
  MAX_DESC_ITEM,
  MAX_ITEMS,
} from "../../services/ordenesService";

/* T46.2 – Pantalla para armar el presupuesto de la orden: mano de obra, repuestos y total (API de T46.1).
   - Requiere el diagnóstico registrado (el backend responde 409 si falta)
   - Los repuestos se pueden elegir del almacén (GET /materials) con su costo como precio sugerido
   - Al enviarlo, la orden pasa a "Espera aprobación" hasta que el cliente decide en la app
   - Si el cliente lo rechaza, se puede armar uno nuevo partiendo del anterior */

const centimos = (n) => Math.round(Number(n || 0) * 100);
const sinTildes = (s) => s.normalize("NFD").replace(/[̀-ͯ]/g, "").toLowerCase();
let contadorFilas = 0;
const fila = (tipo, descripcion = "", cantidad = "1", precio = "", idMaterial = null) => ({
  clave: ++contadorFilas,
  tipo,
  descripcion,
  cantidad: String(cantidad),
  precio: precio === "" ? "" : String(precio),
  idMaterial,
});

function validar(borrador) {
  const errores = { filas: {} };
  const desc = borrador.descripcion.trim();
  if (!desc) errores.descripcion = "Describe en una frase el trabajo que se va a realizar.";
  else if (desc.length > MAX_DESC_PRESUPUESTO) errores.descripcion = `Máximo ${MAX_DESC_PRESUPUESTO} caracteres.`;
  if (!borrador.items.length) errores.general = "Agrega al menos una línea de mano de obra o un repuesto.";
  if (borrador.items.length > MAX_ITEMS) errores.general = `El presupuesto puede tener hasta ${MAX_ITEMS} líneas.`;
  for (const i of borrador.items) {
    const e = {};
    if (!i.descripcion.trim()) e.descripcion = "Falta la descripción.";
    else if (i.descripcion.trim().length > MAX_DESC_ITEM) e.descripcion = `Máximo ${MAX_DESC_ITEM} caracteres.`;
    const cant = Number(i.cantidad);
    if (!Number.isInteger(cant) || cant < 1) e.cantidad = "Entero desde 1.";
    else if (cant > 999) e.cantidad = "Máximo 999.";
    const precio = String(i.precio).replace(",", ".");
    if (precio === "") e.precio = "Falta el precio.";
    else if (!/^\d+(\.\d{1,2})?$/.test(precio)) e.precio = Number(precio) < 0 ? "No puede ser negativo." : "Máx. 2 decimales.";
    else if (Number(precio) > 99999) e.precio = "Demasiado alto.";
    if (Object.keys(e).length) errores.filas[i.clave] = e;
  }
  const hay = errores.descripcion || errores.general || Object.keys(errores.filas).length;
  return hay ? errores : null;
}

/* ---------- Vista de un presupuesto enviado ---------- */
function VistaPresupuesto({ presupuesto, momento, cerrada }) {
  const porTipo = (tipo) => presupuesto.items.filter((i) => i.tipo === tipo).reduce((s, i) => s + i.subtotal, 0);
  return (
    <div className="ot-pres-vista">
      <div className={`ot-pres-estado pres-${presupuesto.estado}`}>
        <strong>{ESTADOS_PRESUPUESTO[presupuesto.estado]}</strong>
        <span>
          {presupuesto.estado === "PENDIENTE"
            ? `Enviado ${momento(presupuesto.fechaCreacion)}. El cliente lo revisa en la app; mientras tanto la orden no se modifica.`
            : presupuesto.estado === "APROBADO"
              ? `El cliente lo aprobó ${momento(presupuesto.fechaDecision)}.${cerrada ? "" : " Puedes continuar con el trabajo."}`
              : `El cliente lo rechazó ${momento(presupuesto.fechaDecision)}.`}
        </span>
      </div>
      {presupuesto.descripcion && <p className="ot-pres-desc">{presupuesto.descripcion}</p>}
      <div className="ot-pres-tabla-caja">
        <table className="ot-pres-tabla">
          <thead>
            <tr>
              <th scope="col">Concepto</th>
              <th scope="col" className="num">Cant.</th>
              <th scope="col" className="num">P. unit.</th>
              <th scope="col" className="num">Subtotal</th>
            </tr>
          </thead>
          <tbody>
            {presupuesto.items.map((i, n) => (
              <tr key={n}>
                <td>
                  <span className={`ot-pres-tipo tipo-${i.tipo}`}>{TIPOS_ITEM[i.tipo]}</span>
                  {i.descripcion}
                </td>
                <td className="num">{i.cantidad}</td>
                <td className="num">{formatoSoles(i.precioUnitario)}</td>
                <td className="num">{formatoSoles(i.subtotal)}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      <dl className="ot-pres-totales">
        <div>
          <dt>Mano de obra</dt>
          <dd>{formatoSoles(porTipo("MANO_OBRA"))}</dd>
        </div>
        <div>
          <dt>Repuestos</dt>
          <dd>{formatoSoles(porTipo("REPUESTO"))}</dd>
        </div>
        <div className="total">
          <dt>Total</dt>
          <dd>{formatoSoles(presupuesto.total)}</dd>
        </div>
      </dl>
    </div>
  );
}

/* ---------- Buscador de repuestos del almacén ---------- */
function BuscadorAlmacen({ alElegir, alCerrar }) {
  const [estado, setEstado] = useState({ cargando: true, materiales: [], error: "" });
  const [texto, setTexto] = useState("");

  useEffect(() => {
    let vigente = true;
    listarMaterialesTaller()
      .then((materiales) => vigente && setEstado({ cargando: false, materiales, error: "" }))
      .catch((e) => vigente && setEstado({ cargando: false, materiales: [], error: e.message }));
    return () => {
      vigente = false;
    };
  }, []);

  const q = sinTildes(texto.trim());
  const lista = estado.materiales.filter((m) => !q || sinTildes(`${m.codigo} ${m.nombre}`).includes(q)).slice(0, 8);

  return (
    <div className="ot-almacen" role="region" aria-label="Repuestos del almacén">
      <div className="ot-almacen-cab">
        <input
          type="search"
          autoFocus
          placeholder="Buscar repuesto por código o nombre"
          value={texto}
          onChange={(e) => setTexto(e.target.value)}
          onKeyDown={(e) => e.key === "Escape" && alCerrar()}
          aria-label="Buscar repuesto por código o nombre"
        />
        <button type="button" className="btn btn-sec" onClick={alCerrar}>
          Cerrar
        </button>
      </div>
      {estado.cargando ? (
        <p className="ot-almacen-msg">Cargando el almacén...</p>
      ) : estado.error ? (
        <p className="ot-almacen-msg error">{estado.error}</p>
      ) : !lista.length ? (
        <p className="ot-almacen-msg">Ningún repuesto coincide con «{texto.trim()}». Puedes agregarlo como línea libre.</p>
      ) : (
        <ul className="ot-almacen-lista">
          {lista.map((m) => (
            <li key={m.id}>
              <button type="button" onClick={() => alElegir(m)}>
                <span className="ot-almacen-cod">{m.codigo}</span>
                <span className="ot-almacen-nom">{m.nombre}</span>
                <span className={m.stockActual === 0 ? "ot-almacen-stock agotado" : m.bajoStock ? "ot-almacen-stock bajo" : "ot-almacen-stock"}>
                  {m.stockActual === 0 ? "Sin stock" : `Stock ${m.stockActual}`}
                </span>
                <span className="ot-almacen-precio">{formatoSoles(m.costoUnitario)}</span>
              </button>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}

/* ---------- Editor ---------- */
function EditorPresupuesto({ idOrden, inicial, alEnviado, alCancelar, alConflicto }) {
  const [borrador, setBorrador] = useState(inicial);
  const [errores, setErrores] = useState(null);
  const [errorGeneral, setErrorGeneral] = useState("");
  const [almacen, setAlmacen] = useState(false);
  const [confirmar, setConfirmar] = useState(false);
  const [enviando, setEnviando] = useState(false);

  const totales = useMemo(() => {
    const sub = (i) => (Number.isInteger(Number(i.cantidad)) ? centimos(String(i.precio).replace(",", ".")) * Number(i.cantidad) : 0);
    const mano = borrador.items.filter((i) => i.tipo === "MANO_OBRA").reduce((s, i) => s + sub(i), 0);
    const rep = borrador.items.filter((i) => i.tipo === "REPUESTO").reduce((s, i) => s + sub(i), 0);
    return { mano: mano / 100, rep: rep / 100, total: (mano + rep) / 100, sub };
  }, [borrador.items]);

  const cambiarFila = (clave, campo, valor) => {
    setBorrador((b) => ({ ...b, items: b.items.map((i) => (i.clave === clave ? { ...i, [campo]: valor } : i)) }));
    setConfirmar(false);
    if (errores?.filas?.[clave]?.[campo]) {
      setErrores((er) => ({ ...er, filas: { ...er.filas, [clave]: { ...er.filas[clave], [campo]: "" } } }));
    }
  };
  const agregarFila = (nueva) => {
    setBorrador((b) => ({ ...b, items: [...b.items, nueva] }));
    setConfirmar(false);
    setErrores((er) => (er ? { ...er, general: "" } : er));
  };
  const quitarFila = (clave) => {
    setBorrador((b) => ({ ...b, items: b.items.filter((i) => i.clave !== clave) }));
    setConfirmar(false);
  };
  const elegirMaterial = (m) => {
    const ya = borrador.items.find((i) => i.idMaterial === m.id);
    if (ya) cambiarFila(ya.clave, "cantidad", String((Number(ya.cantidad) || 0) + 1));
    else agregarFila(fila("REPUESTO", `${m.codigo} · ${m.nombre}`, 1, m.costoUnitario.toFixed(2), m.id));
    setAlmacen(false);
  };

  const revisar = (e) => {
    e.preventDefault();
    const er = validar(borrador);
    setErrores(er);
    setErrorGeneral("");
    if (!er) setConfirmar(true);
  };

  const enviar = async () => {
    setEnviando(true);
    setErrorGeneral("");
    try {
      const p = await enviarPresupuesto(idOrden, {
        descripcion: borrador.descripcion,
        items: borrador.items.map((i) => ({ ...i, precioUnitario: String(i.precio).replace(",", ".") })),
      });
      alEnviado(p);
    } catch (err) {
      setConfirmar(false);
      if (err.status === 409 && err.estado) {
        setErrorGeneral(`${err.message} Se actualizó la orden.`);
        alConflicto();
      } else {
        setErrorGeneral(err.message);
      }
    } finally {
      setEnviando(false);
    }
  };

  const errFila = (clave, campo) => errores?.filas?.[clave]?.[campo];

  return (
    <form className="ot-pres-form" onSubmit={revisar} noValidate>
      {errorGeneral && (
        <p className="form-error-general" role="alert">
          {errorGeneral}
        </p>
      )}

      <div className="campo">
        <div className="campo-fila">
          <label htmlFor="ot-pres-desc">Trabajo a realizar</label>
          <span className={borrador.descripcion.trim().length > MAX_DESC_PRESUPUESTO ? "contador excedido" : "contador"}>
            {borrador.descripcion.trim().length}/{MAX_DESC_PRESUPUESTO}
          </span>
        </div>
        <textarea
          id="ot-pres-desc"
          rows={2}
          value={borrador.descripcion}
          placeholder="Ej. Cambio de amortiguadores delanteros y pastillas de freno"
          onChange={(e) => {
            setBorrador((b) => ({ ...b, descripcion: e.target.value }));
            setConfirmar(false);
            if (errores?.descripcion) setErrores((er) => ({ ...er, descripcion: "" }));
          }}
          aria-invalid={!!errores?.descripcion}
          aria-describedby="ot-pres-desc-ayuda"
        />
        <p id="ot-pres-desc-ayuda" className={errores?.descripcion ? "campo-error" : "campo-ayuda"}>
          {errores?.descripcion || "El cliente lo leerá junto con el detalle y el total."}
        </p>
      </div>

      {/* ----- Líneas ----- */}
      <div className="ot-pres-lineas" role="group" aria-label="Líneas del presupuesto">
        <div className="ot-pres-linea cab" aria-hidden="true">
          <span>Tipo</span>
          <span>Descripción</span>
          <span>Cant.</span>
          <span>P. unit. (S/)</span>
          <span className="num">Subtotal</span>
          <span />
        </div>
        {borrador.items.map((i, n) => (
          <div key={i.clave} className="ot-pres-linea">
            <select
              value={i.tipo}
              onChange={(e) => cambiarFila(i.clave, "tipo", e.target.value)}
              aria-label={`Tipo de la línea ${n + 1}`}
            >
              {Object.entries(TIPOS_ITEM).map(([v, t]) => (
                <option key={v} value={v}>
                  {t}
                </option>
              ))}
            </select>
            <div className="ot-pres-celda">
              <input
                value={i.descripcion}
                maxLength={MAX_DESC_ITEM}
                placeholder={i.tipo === "MANO_OBRA" ? "Ej. Cambio de amortiguadores" : "Ej. Amortiguador delantero"}
                onChange={(e) => cambiarFila(i.clave, "descripcion", e.target.value)}
                aria-label={`Descripción de la línea ${n + 1}`}
                aria-invalid={!!errFila(i.clave, "descripcion")}
              />
              {errFila(i.clave, "descripcion") && <span className="campo-error">{errFila(i.clave, "descripcion")}</span>}
            </div>
            <div className="ot-pres-celda" data-etq="Cantidad">
              <input
                type="number"
                min="1"
                step="1"
                inputMode="numeric"
                value={i.cantidad}
                onChange={(e) => cambiarFila(i.clave, "cantidad", e.target.value)}
                aria-label={`Cantidad de la línea ${n + 1}`}
                aria-invalid={!!errFila(i.clave, "cantidad")}
              />
              {errFila(i.clave, "cantidad") && <span className="campo-error">{errFila(i.clave, "cantidad")}</span>}
            </div>
            <div className="ot-pres-celda" data-etq="Precio unitario (S/)">
              <input
                type="number"
                min="0"
                step="0.01"
                inputMode="decimal"
                placeholder="0.00"
                value={i.precio}
                onChange={(e) => cambiarFila(i.clave, "precio", e.target.value)}
                aria-label={`Precio unitario de la línea ${n + 1}`}
                aria-invalid={!!errFila(i.clave, "precio")}
              />
              {errFila(i.clave, "precio") && <span className="campo-error">{errFila(i.clave, "precio")}</span>}
            </div>
            <span className="ot-pres-sub num">{formatoSoles(totales.sub(i) / 100)}</span>
            <button
              type="button"
              className="ot-pres-quitar"
              onClick={() => quitarFila(i.clave)}
              aria-label={`Quitar la línea ${n + 1}`}
              title="Quitar línea"
            >
              ×
            </button>
          </div>
        ))}
        {errores?.general && (
          <p className="campo-error" role="alert">
            {errores.general}
          </p>
        )}
      </div>

      <div className="ot-pres-agregar">
        <button type="button" className="ot-diag-base" onClick={() => agregarFila(fila("MANO_OBRA"))} disabled={borrador.items.length >= MAX_ITEMS}>
          + Mano de obra
        </button>
        <button type="button" className="ot-diag-base" onClick={() => setAlmacen(true)} disabled={borrador.items.length >= MAX_ITEMS}>
          + Repuesto del almacén
        </button>
        <button type="button" className="ot-diag-base" onClick={() => agregarFila(fila("REPUESTO"))} disabled={borrador.items.length >= MAX_ITEMS}>
          + Repuesto libre
        </button>
      </div>
      {almacen && <BuscadorAlmacen alElegir={elegirMaterial} alCerrar={() => setAlmacen(false)} />}

      <dl className="ot-pres-totales">
        <div>
          <dt>Mano de obra</dt>
          <dd>{formatoSoles(totales.mano)}</dd>
        </div>
        <div>
          <dt>Repuestos</dt>
          <dd>{formatoSoles(totales.rep)}</dd>
        </div>
        <div className="total">
          <dt>Total</dt>
          <dd>{formatoSoles(totales.total)}</dd>
        </div>
      </dl>

      {confirmar ? (
        <div className="ot-pres-confirmar" role="alertdialog" aria-labelledby="ot-pres-conf-titulo">
          <p id="ot-pres-conf-titulo">
            <strong>¿Enviar el presupuesto por {formatoSoles(totales.total)} al cliente?</strong>
          </p>
          <p>Lo recibirá en la app. Mientras decide, la orden queda en «Espera aprobación» y no podrás modificarla.</p>
          <div className="form-acciones">
            <button type="button" className="btn btn-sec" onClick={() => setConfirmar(false)} disabled={enviando}>
              Volver
            </button>
            <button type="button" className="btn btn-pri" onClick={enviar} disabled={enviando} autoFocus>
              {enviando && <span className="btn-spinner" aria-hidden="true" />}
              {enviando ? "Enviando..." : "Sí, enviar al cliente"}
            </button>
          </div>
        </div>
      ) : (
        <div className="form-acciones">
          {alCancelar && (
            <button type="button" className="btn btn-sec" onClick={alCancelar}>
              Cancelar
            </button>
          )}
          <button type="submit" className="btn btn-pri">
            Revisar y enviar
          </button>
        </div>
      )}
    </form>
  );
}

/* ---------- Panel ---------- */
function PresupuestoOrden({ idOrden, presupuesto, diagnostico, cita, editable, estado, momento, alEnviado, alConflicto }) {
  const [armando, setArmando] = useState(false);

  const desdeCero = () => ({
    descripcion: (diagnostico?.texto ?? "").split("\n")[0].slice(0, MAX_DESC_PRESUPUESTO),
    items: [fila("MANO_OBRA", cita.servicio, 1, cita.precio != null ? Number(cita.precio).toFixed(2) : "")],
  });
  const desdeRechazado = () => ({
    descripcion: presupuesto.descripcion,
    items: presupuesto.items.map((i) => fila(i.tipo, i.descripcion, i.cantidad, i.precioUnitario.toFixed(2))),
  });

  const alTerminar = (p) => {
    setArmando(false);
    alEnviado(p);
  };

  // Armar: sin presupuesto (con diagnóstico) o tras un rechazo
  if (editable && armando) {
    return (
      <EditorPresupuesto
        idOrden={idOrden}
        inicial={presupuesto?.estado === "RECHAZADO" ? desdeRechazado() : desdeCero()}
        alEnviado={alTerminar}
        alCancelar={() => setArmando(false)}
        alConflicto={alConflicto}
      />
    );
  }

  if (!presupuesto) {
    if (!editable) {
      return (
        <p className="ot-diag-vacio">
          {estado === "FINALIZADA" ? "Esta orden se cerró sin presupuesto." : "Aún no se arma el presupuesto."}
        </p>
      );
    }
    return (
      <div className="ot-pres-inicio">
        {diagnostico ? (
          <>
            <p>Arma el presupuesto con la mano de obra y los repuestos; el cliente lo aprobará o rechazará desde la app.</p>
            <button type="button" className="btn btn-pri" onClick={() => setArmando(true)}>
              Armar presupuesto
            </button>
          </>
        ) : (
          <p>
            <strong>Primero registra el diagnóstico.</strong> El presupuesto se arma a partir de lo que encontraste en el
            vehículo.
          </p>
        )}
      </div>
    );
  }

  return (
    <>
      <VistaPresupuesto presupuesto={presupuesto} momento={momento} cerrada={estado === "FINALIZADA"} />
      {presupuesto.estado === "RECHAZADO" && editable && (
        <div className="ot-pres-nuevo">
          <p>Habla con el cliente y arma un presupuesto nuevo; partirá de las líneas del anterior.</p>
          <button type="button" className="btn btn-pri" onClick={() => setArmando(true)}>
            Armar nuevo presupuesto
          </button>
        </div>
      )}
    </>
  );
}

export default PresupuestoOrden;
