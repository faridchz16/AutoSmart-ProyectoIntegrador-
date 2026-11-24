import { useState } from "react";
import {
  registrarVehiculo,
  editarVehiculo,
  formatearPlaca,
  formatoKm,
  PATRON_PLACA,
  ANIO_MINIMO,
  KM_MAXIMO,
  MAX_TEXTO,
  MARCAS,
} from "../../services/vehiculosService";
import { hoyLima } from "../../utils/fechas";

/* Formulario para registrar o editar un vehículo (CLI-02). Mismas reglas que VehiculoService:
   - placa ABC-123, única en el sistema y fija una vez registrada
   - marca, modelo y año obligatorios; el kilometraje nunca baja del último registrado */

const ANIO_MAXIMO = Number(hoyLima().slice(0, 4)) + 1;
const soloDigitos = (t) => t.replace(/\D/g, "");

function validar(d, vehiculo) {
  const e = {};
  if (!vehiculo) {
    if (!d.placa) e.placa = "Escribe la placa.";
    else if (!PATRON_PLACA.test(d.placa)) e.placa = "Usa el formato ABC-123: 3 letras o números, guion y 3 números.";
  }
  if (!d.marca.trim()) e.marca = "Escribe la marca.";
  else if (d.marca.trim().length > MAX_TEXTO) e.marca = `Máximo ${MAX_TEXTO} caracteres.`;
  if (!d.modelo.trim()) e.modelo = "Escribe el modelo.";
  else if (d.modelo.trim().length > MAX_TEXTO) e.modelo = `Máximo ${MAX_TEXTO} caracteres.`;
  const anio = Number(d.anio);
  if (!d.anio) e.anio = "Escribe el año.";
  else if (anio < ANIO_MINIMO || anio > ANIO_MAXIMO) e.anio = `Debe estar entre ${ANIO_MINIMO} y ${ANIO_MAXIMO}.`;
  const km = Number(d.kilometraje || 0);
  if (km > KM_MAXIMO) e.kilometraje = `Revisa el valor: máximo ${formatoKm(KM_MAXIMO)}.`;
  else if (vehiculo && km < vehiculo.kilometraje) e.kilometraje = `No puede ser menor al último registrado (${formatoKm(vehiculo.kilometraje)}).`;
  return e;
}

// Ubica el mensaje del backend en su campo
function campoDelError(err) {
  const m = err.message.toLowerCase();
  if (err.status === 409 || m.includes("placa")) return "placa";
  if (m.includes("kilometraje")) return "kilometraje";
  if (m.includes("marca")) return "marca";
  if (m.includes("modelo")) return "modelo";
  if (m.includes("año")) return "anio";
  return null;
}

function FormVehiculo({ vehiculo, alGuardar, alCancelar }) {
  const editando = !!vehiculo;
  const [datos, setDatos] = useState({
    placa: vehiculo?.placa ?? "",
    marca: vehiculo?.marca ?? "",
    modelo: vehiculo?.modelo ?? "",
    anio: vehiculo?.anio ? String(vehiculo.anio) : "",
    kilometraje: vehiculo ? String(vehiculo.kilometraje) : "",
  });
  const [errores, setErrores] = useState({});
  const [errorGeneral, setErrorGeneral] = useState("");
  const [enviando, setEnviando] = useState(false);

  const cambiar = (campo, filtro = (t) => t) => (e) => {
    setDatos((d) => ({ ...d, [campo]: filtro(e.target.value) }));
    setErrores((x) => ({ ...x, [campo]: undefined }));
    setErrorGeneral("");
  };

  const enviar = async (ev) => {
    ev.preventDefault();
    const e = validar(datos, vehiculo);
    setErrores(e);
    if (Object.keys(e).length) {
      document.getElementById(`veh-${Object.keys(e)[0]}`)?.focus();
      return;
    }
    setEnviando(true);
    setErrorGeneral("");
    try {
      const guardado = editando ? await editarVehiculo(vehiculo.id, datos) : await registrarVehiculo(datos);
      alGuardar(guardado);
    } catch (err) {
      const campo = err.status === 404 ? null : campoDelError(err);
      if (campo) setErrores({ [campo]: err.message });
      else setErrorGeneral(err.message);
      if (err.status === 404) alGuardar(null);
    } finally {
      setEnviando(false);
    }
  };

  const ayuda = (campo, texto) =>
    errores[campo] ? (
      <p className="campo-error" id={`veh-${campo}-error`}>
        {errores[campo]}
      </p>
    ) : (
      texto && <span className="campo-ayuda">{texto}</span>
    );

  const props = (campo) => ({
    id: `veh-${campo}`,
    value: datos[campo],
    "aria-invalid": !!errores[campo],
    "aria-describedby": errores[campo] ? `veh-${campo}-error` : undefined,
  });

  return (
    <form className="adm-form" onSubmit={enviar} noValidate>
      <div className="campo">
        <label htmlFor="veh-placa">Placa</label>
        <input
          {...props("placa")}
          className="veh-input-placa"
          autoComplete="off"
          autoCapitalize="characters"
          placeholder="ABC-123"
          maxLength={7}
          disabled={editando}
          autoFocus={!editando}
          onChange={cambiar("placa", formatearPlaca)}
        />
        {ayuda("placa", editando ? "La placa no se puede cambiar." : "Tal como aparece en la placa del vehículo. El guion se agrega solo.")}
      </div>

      <div className="campo-doble">
        <div className="campo">
          <label htmlFor="veh-marca">Marca</label>
          <input {...props("marca")} list="veh-marcas" autoComplete="off" maxLength={MAX_TEXTO} placeholder="Ej. Toyota" autoFocus={editando} onChange={cambiar("marca")} />
          <datalist id="veh-marcas">
            {MARCAS.map((m) => (
              <option key={m} value={m} />
            ))}
          </datalist>
          {ayuda("marca")}
        </div>
        <div className="campo">
          <label htmlFor="veh-modelo">Modelo</label>
          <input {...props("modelo")} autoComplete="off" maxLength={MAX_TEXTO} placeholder="Ej. Yaris" onChange={cambiar("modelo")} />
          {ayuda("modelo")}
        </div>
      </div>

      <div className="campo-doble">
        <div className="campo">
          <label htmlFor="veh-anio">Año</label>
          <input {...props("anio")} inputMode="numeric" autoComplete="off" maxLength={4} placeholder={String(ANIO_MAXIMO - 3)} onChange={cambiar("anio", soloDigitos)} />
          {ayuda("anio")}
        </div>
        <div className="campo">
          <label htmlFor="veh-kilometraje">
            Kilometraje {!editando && <span className="opcional">(opcional)</span>}
          </label>
          <div className="campo-prefijo veh-km">
            <input {...props("kilometraje")} inputMode="numeric" autoComplete="off" maxLength={7} placeholder="0" onChange={cambiar("kilometraje", soloDigitos)} />
            <span>km</span>
          </div>
          {ayuda("kilometraje", editando ? `Último registrado: ${formatoKm(vehiculo.kilometraje)}.` : "Lo que marca el tablero hoy.")}
        </div>
      </div>

      {errorGeneral && (
        <p className="form-error-general" role="alert">
          {errorGeneral}
        </p>
      )}
      <div className="form-acciones">
        <button type="button" className="btn btn-sec" onClick={alCancelar} disabled={enviando}>
          Cancelar
        </button>
        <button type="submit" className="btn btn-pri" disabled={enviando}>
          {enviando && <span className="btn-spinner" aria-hidden="true" />}
          {enviando ? "Guardando..." : editando ? "Guardar cambios" : "Registrar vehículo"}
        </button>
      </div>
    </form>
  );
}

export default FormVehiculo;
