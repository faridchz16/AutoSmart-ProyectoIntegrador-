import { useState } from "react";
import {
  crearMaterial,
  editarMaterial,
  normalizarCodigo,
  UNIDADES,
  MAX_CODIGO,
  MAX_NOMBRE,
  textoUnidad,
  formatoSoles,
  formatoCantidad,
} from "../../services/inventarioService";

/* Formulario de material (T57.2): sirve para registrar (material = null) y para editar.
   Mismas reglas que el backend (T57.1): código único, costo ≥ 0 y stock mínimo entero.
   El stock actual no se edita aquí. */

const PATRON_CODIGO = /^[A-Z0-9][A-Z0-9-]*$/;

function validar(datos, codigosUsados, codigoOriginal) {
  const errores = {};

  const codigo = normalizarCodigo(datos.codigo);
  if (!codigo) errores.codigo = "Ingresa el código del material.";
  else if (codigo.length < 3 || codigo.length > MAX_CODIGO) errores.codigo = `Entre 3 y ${MAX_CODIGO} caracteres.`;
  else if (!PATRON_CODIGO.test(codigo)) errores.codigo = "Usa solo letras, números y guiones (ej. FIL-001).";
  else if (codigo !== codigoOriginal && codigosUsados.includes(codigo)) errores.codigo = `El código ${codigo} ya está registrado.`;

  const nombre = datos.nombre.trim();
  if (!nombre) errores.nombre = "Ingresa el nombre del material.";
  else if (nombre.length < 3) errores.nombre = "El nombre debe tener al menos 3 caracteres.";
  else if (nombre.length > MAX_NOMBRE) errores.nombre = `El nombre no puede pasar de ${MAX_NOMBRE} caracteres.`;

  if (!datos.unidad.trim()) errores.unidad = "Elige la unidad de medida.";

  const costoTexto = String(datos.costo).replace(",", ".");
  const costo = Number(costoTexto);
  if (datos.costo === "") errores.costo = "Ingresa el costo unitario.";
  else if (!Number.isFinite(costo) || costo < 0) errores.costo = "El costo no puede ser negativo.";
  else if (costo > 99999999) errores.costo = "El costo es demasiado alto.";
  else if (!/^\d+(\.\d{1,2})?$/.test(costoTexto)) errores.costo = "Usa como máximo 2 decimales.";

  const minimo = Number(datos.minimo);
  if (datos.minimo === "") errores.minimo = "Ingresa el stock mínimo.";
  else if (!Number.isInteger(minimo) || minimo < 0) errores.minimo = "Usa un número entero de 0 en adelante.";
  else if (minimo > 99999) errores.minimo = "El stock mínimo es demasiado alto.";

  return errores;
}

function FormMaterial({ material, codigosUsados = [], alGuardar, alCerrar }) {
  const editando = !!material;
  const inicial = {
    codigo: material?.codigo ?? "",
    nombre: material?.nombre ?? "",
    unidad: material?.unidad ?? "unidad",
    costo: material ? String(material.costoUnitario) : "",
    minimo: material ? String(material.stockMinimo) : "",
  };
  const [datos, setDatos] = useState(inicial);
  const [errores, setErrores] = useState({});
  const [errorGeneral, setErrorGeneral] = useState("");
  const [enviando, setEnviando] = useState(false);

  const sinCambios = editando && Object.keys(inicial).every((k) => inicial[k] === datos[k]);
  // Si el material tiene una unidad que no está en la lista (registrada desde otro cliente), se ofrece igual
  const unidades = UNIDADES.some(([uno]) => uno === inicial.unidad) ? UNIDADES : [...UNIDADES, [inicial.unidad]];

  const actualizar = (campo, valor) => {
    setDatos((d) => ({ ...d, [campo]: valor }));
    setErrorGeneral("");
    if (errores[campo]) setErrores((er) => ({ ...er, [campo]: "" }));
  };
  const cambiar = (campo) => (e) => actualizar(campo, e.target.value);

  const enviar = async (e) => {
    e.preventDefault();
    const nuevos = validar(datos, codigosUsados, material?.codigo);
    setErrores(nuevos);
    if (Object.keys(nuevos).length) return;

    const cuerpo = {
      codigo: normalizarCodigo(datos.codigo),
      nombre: datos.nombre.trim(),
      unidad: datos.unidad,
      costoUnitario: Number(String(datos.costo).replace(",", ".")),
      stockMinimo: Number(datos.minimo),
    };

    setEnviando(true);
    try {
      const guardado = editando ? await editarMaterial(material.id, cuerpo) : await crearMaterial(cuerpo);
      alGuardar(guardado, editando);
    } catch (err) {
      // 409: código repetido -> se marca en el campo
      if (err.status === 409) setErrores((er) => ({ ...er, codigo: err.message }));
      else setErrorGeneral(err.message);
      setEnviando(false);
    }
  };

  return (
    <form className="adm-form inv-form" onSubmit={enviar} noValidate>
      {errorGeneral && <p className="form-error-general" role="alert">{errorGeneral}</p>}

      <div className="campo-doble inv-cod-nom">
        <div className="campo">
          <label htmlFor="mat-codigo">Código</label>
          <input
            id="mat-codigo"
            className="inv-input-codigo"
            autoFocus={!editando}
            autoComplete="off"
            maxLength={MAX_CODIGO}
            placeholder="FIL-001"
            value={datos.codigo}
            onChange={(e) => actualizar("codigo", e.target.value.toUpperCase())}
            aria-invalid={!!errores.codigo}
            aria-describedby="mat-codigo-ayuda"
          />
          <p id="mat-codigo-ayuda" className={errores.codigo ? "campo-error" : "campo-ayuda"}>
            {errores.codigo || "Único. Ej. FIL-001"}
          </p>
        </div>

        <div className="campo">
          <label htmlFor="mat-nombre">Nombre</label>
          <input
            id="mat-nombre"
            autoFocus={editando}
            autoComplete="off"
            maxLength={MAX_NOMBRE}
            placeholder="Ej. Filtro de aceite"
            value={datos.nombre}
            onChange={cambiar("nombre")}
            aria-invalid={!!errores.nombre}
            aria-describedby={errores.nombre ? "mat-nombre-error" : undefined}
          />
          {errores.nombre && <p id="mat-nombre-error" className="campo-error">{errores.nombre}</p>}
        </div>
      </div>

      <div className="inv-triple">
        <div className="campo">
          <label htmlFor="mat-unidad">Unidad</label>
          <select
            id="mat-unidad"
            value={datos.unidad}
            onChange={cambiar("unidad")}
            aria-invalid={!!errores.unidad}
            aria-describedby="mat-unidad-ayuda"
          >
            {unidades.map(([uno]) => (
              <option key={uno} value={uno}>
                {uno.charAt(0).toUpperCase() + uno.slice(1)}
              </option>
            ))}
          </select>
          <p id="mat-unidad-ayuda" className={errores.unidad ? "campo-error" : "campo-ayuda"}>
            {errores.unidad || "Cómo se cuenta."}
          </p>
        </div>

        <div className="campo">
          <label htmlFor="mat-costo">Costo unitario</label>
          <div className="campo-prefijo">
            <span aria-hidden="true">S/</span>
            <input
              id="mat-costo"
              type="number"
              inputMode="decimal"
              min="0"
              step="0.01"
              placeholder="0.00"
              value={datos.costo}
              onChange={cambiar("costo")}
              aria-invalid={!!errores.costo}
              aria-describedby="mat-costo-ayuda"
            />
          </div>
          <p id="mat-costo-ayuda" className={errores.costo ? "campo-error" : "campo-ayuda"}>
            {errores.costo || `Por ${textoUnidad(datos.unidad, 1)}.`}
          </p>
        </div>

        <div className="campo">
          <label htmlFor="mat-minimo">Stock mínimo</label>
          <input
            id="mat-minimo"
            type="number"
            inputMode="numeric"
            min="0"
            step="1"
            placeholder="5"
            value={datos.minimo}
            onChange={cambiar("minimo")}
            aria-invalid={!!errores.minimo}
            aria-describedby="mat-minimo-ayuda"
          />
          <p id="mat-minimo-ayuda" className={errores.minimo ? "campo-error" : "campo-ayuda"}>
            {errores.minimo || "Debajo, toca reponer."}
          </p>
        </div>
      </div>

      <div className="inv-nota" role="note">
        {editando ? (
          <>
            <strong>
              Stock actual: {formatoCantidad(material.stockActual)} {textoUnidad(material.unidad, material.stockActual)}
            </strong>
            <span>
              Valor en almacén: {formatoSoles(material.stockActual * material.costoUnitario)}. El stock no se edita aquí:
              cambia con los ingresos y las salidas del almacén.
            </span>
          </>
        ) : (
          <>
            <strong>El material se crea con stock 0.</strong>
            <span>Luego registra un ingreso de mercadería para sumar las unidades que lleguen.</span>
          </>
        )}
      </div>

      <div className="form-acciones">
        <button type="button" className="btn btn-sec" onClick={alCerrar} disabled={enviando}>
          Cancelar
        </button>
        <button type="submit" className="btn btn-pri" disabled={enviando || sinCambios}>
          {enviando && <span className="btn-spinner" aria-hidden="true" />}
          {enviando ? "Guardando..." : editando ? "Guardar cambios" : "Registrar material"}
        </button>
      </div>
    </form>
  );
}

export default FormMaterial;
