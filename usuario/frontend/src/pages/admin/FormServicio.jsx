import { useState } from "react";
import { crearServicio, editarServicio, formatoDuracion } from "../../services/serviciosService";

/* Formulario de servicio (T18.2) que también sirve para editar (T19.2).
   - servicio = null      -> crea uno nuevo
   - servicio = {...}     -> edita ese servicio */

const MAX_DESCRIPCION = 250;

function validar({ nombre, descripcion, precio, duracion }) {
  const errores = {};
  const n = nombre.trim();
  if (!n) errores.nombre = "Ingresa el nombre del servicio.";
  else if (n.length < 3) errores.nombre = "El nombre debe tener al menos 3 caracteres.";
  else if (n.length > 80) errores.nombre = "El nombre no puede pasar de 80 caracteres.";

  if (descripcion.length > MAX_DESCRIPCION) {
    errores.descripcion = `Máximo ${MAX_DESCRIPCION} caracteres.`;
  }

  const p = Number(precio);
  if (precio === "") errores.precio = "Ingresa el precio base.";
  else if (!Number.isFinite(p) || p <= 0) errores.precio = "El precio debe ser mayor que 0.";
  else if (p > 99999) errores.precio = "El precio es demasiado alto.";
  else if (!/^\d+([.,]\d{1,2})?$/.test(String(precio))) errores.precio = "Usa como máximo 2 decimales.";

  const d = Number(duracion);
  if (duracion === "") errores.duracion = "Ingresa la duración.";
  else if (!Number.isInteger(d)) errores.duracion = "Usa minutos enteros.";
  else if (d < 10 || d > 600) errores.duracion = "Entre 10 y 600 minutos (10 horas).";

  return errores;
}

function FormServicio({ servicio, alGuardar, alCerrar }) {
  const editando = !!servicio;
  const inicial = {
    nombre: servicio?.nombre ?? "",
    descripcion: servicio?.descripcion ?? "",
    precio: servicio ? String(servicio.precioBase) : "",
    duracion: servicio ? String(servicio.duracionMinutos) : "",
  };
  const [datos, setDatos] = useState(inicial);
  const [errores, setErrores] = useState({});
  const [errorGeneral, setErrorGeneral] = useState("");
  const [enviando, setEnviando] = useState(false);

  const sinCambios = editando && Object.keys(inicial).every((k) => inicial[k] === datos[k]);
  const minutos = Number(datos.duracion);

  const cambiar = (campo) => (e) => {
    const valor = e.target.value;
    setDatos((d) => ({ ...d, [campo]: valor }));
    setErrorGeneral("");
    if (errores[campo]) setErrores((er) => ({ ...er, [campo]: "" }));
  };

  const enviar = async (e) => {
    e.preventDefault();
    const nuevos = validar(datos);
    setErrores(nuevos);
    if (Object.keys(nuevos).length) return;

    const cuerpo = {
      nombre: datos.nombre.trim(),
      descripcion: datos.descripcion.trim(),
      precioBase: Number(String(datos.precio).replace(",", ".")),
      duracionMinutos: Number(datos.duracion),
    };

    setEnviando(true);
    try {
      const guardado = editando ? await editarServicio(servicio.id, cuerpo) : await crearServicio(cuerpo);
      alGuardar(guardado, editando);
    } catch (err) {
      setErrorGeneral(err.message);
      setEnviando(false);
    }
  };

  const ayudaDuracion =
    errores.duracion ||
    (Number.isInteger(minutos) && minutos >= 10 ? `Equivale a ${formatoDuracion(minutos)}.` : "Tiempo aproximado en el taller.");

  return (
    <form className="adm-form" onSubmit={enviar} noValidate>
      {errorGeneral && <p className="form-error-general" role="alert">{errorGeneral}</p>}

      <div className="campo">
        <label htmlFor="srv-nombre">Nombre del servicio</label>
        <input
          id="srv-nombre"
          autoFocus
          autoComplete="off"
          maxLength={80}
          placeholder="Ej. Cambio de aceite y filtro"
          value={datos.nombre}
          onChange={cambiar("nombre")}
          aria-invalid={!!errores.nombre}
          aria-describedby={errores.nombre ? "srv-nombre-error" : undefined}
        />
        {errores.nombre && <p id="srv-nombre-error" className="campo-error">{errores.nombre}</p>}
      </div>

      <div className="campo">
        <div className="campo-fila">
          <label htmlFor="srv-descripcion">
            Descripción <span className="opcional">(opcional)</span>
          </label>
          <span className={datos.descripcion.length > MAX_DESCRIPCION ? "contador excedido" : "contador"}>
            {datos.descripcion.length}/{MAX_DESCRIPCION}
          </span>
        </div>
        <textarea
          id="srv-descripcion"
          rows={3}
          placeholder="¿Qué incluye el servicio?"
          value={datos.descripcion}
          onChange={cambiar("descripcion")}
          aria-invalid={!!errores.descripcion}
          aria-describedby={errores.descripcion ? "srv-descripcion-error" : undefined}
        />
        {errores.descripcion && <p id="srv-descripcion-error" className="campo-error">{errores.descripcion}</p>}
      </div>

      <div className="campo-doble">
        <div className="campo">
          <label htmlFor="srv-precio">Precio base</label>
          <div className="campo-prefijo">
            <span aria-hidden="true">S/</span>
            <input
              id="srv-precio"
              type="number"
              inputMode="decimal"
              min="0"
              step="0.01"
              placeholder="0.00"
              value={datos.precio}
              onChange={cambiar("precio")}
              aria-invalid={!!errores.precio}
              aria-describedby="srv-precio-ayuda"
            />
          </div>
          <p id="srv-precio-ayuda" className={errores.precio ? "campo-error" : "campo-ayuda"}>
            {errores.precio || "En soles, sin incluir repuestos."}
          </p>
        </div>

        <div className="campo">
          <label htmlFor="srv-duracion">Duración (minutos)</label>
          <input
            id="srv-duracion"
            type="number"
            inputMode="numeric"
            min="10"
            max="600"
            step="5"
            placeholder="60"
            value={datos.duracion}
            onChange={cambiar("duracion")}
            aria-invalid={!!errores.duracion}
            aria-describedby="srv-duracion-ayuda"
          />
          <p id="srv-duracion-ayuda" className={errores.duracion ? "campo-error" : "campo-ayuda"}>
            {ayudaDuracion}
          </p>
        </div>
      </div>

      <div className="form-acciones">
        <button type="button" className="btn btn-sec" onClick={alCerrar} disabled={enviando}>
          Cancelar
        </button>
        <button type="submit" className="btn btn-pri" disabled={enviando || sinCambios}>
          {enviando && <span className="btn-spinner" aria-hidden="true" />}
          {enviando ? "Guardando..." : editando ? "Guardar cambios" : "Crear servicio"}
        </button>
      </div>
    </form>
  );
}

export default FormServicio;
