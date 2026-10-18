import { useState } from "react";
import {
  DIAS,
  crearHorario,
  editarHorario,
  duracionMinutos,
  formatoHoras,
} from "../../services/horariosService";

/* Formulario para agregar o editar un rango de atención (T21.2).
   - horario = null  -> nuevo (diaInicial indica el día elegido)
   - horario = {...} -> edita ese rango
   "otros" son los rangos ya registrados, para avisar si se cruzan antes de enviar. */

function validar({ diaSemana, horaInicio, horaFin }, otros) {
  const errores = {};
  if (!horaInicio) errores.horaInicio = "Indica la hora de inicio.";
  if (!horaFin) errores.horaFin = "Indica la hora de fin.";
  if (horaInicio && horaFin && horaFin <= horaInicio) {
    errores.horaFin = "La hora de fin debe ser posterior a la de inicio.";
  }
  if (!errores.horaInicio && !errores.horaFin) {
    const cruce = otros.find(
      (h) => h.diaSemana === diaSemana && horaInicio < h.horaFin && h.horaInicio < horaFin
    );
    if (cruce) {
      errores.horaFin = `Se cruza con el rango de ${cruce.horaInicio} a ${cruce.horaFin} del mismo día.`;
    }
  }
  return errores;
}

function FormHorario({ horario, diaInicial = 1, otros, alGuardar, alCerrar }) {
  const editando = !!horario;
  const inicial = {
    diaSemana: horario?.diaSemana ?? diaInicial,
    horaInicio: horario?.horaInicio ?? "08:00",
    horaFin: horario?.horaFin ?? "13:00",
  };
  const [datos, setDatos] = useState(inicial);
  const [errores, setErrores] = useState({});
  const [errorGeneral, setErrorGeneral] = useState("");
  const [enviando, setEnviando] = useState(false);

  const sinCambios = editando && Object.keys(inicial).every((k) => inicial[k] === datos[k]);
  const minutos =
    datos.horaInicio && datos.horaFin && datos.horaFin > datos.horaInicio
      ? duracionMinutos(datos.horaInicio, datos.horaFin)
      : 0;

  const cambiar = (campo, valor) => {
    setDatos((d) => ({ ...d, [campo]: valor }));
    setErrorGeneral("");
    setErrores({});
  };

  const enviar = async (e) => {
    e.preventDefault();
    const nuevos = validar(datos, otros);
    setErrores(nuevos);
    if (Object.keys(nuevos).length) return;

    setEnviando(true);
    try {
      const guardado = editando ? await editarHorario(horario.id, datos) : await crearHorario(datos);
      alGuardar(guardado, editando);
    } catch (err) {
      setErrorGeneral(err.message);
      setEnviando(false);
    }
  };

  return (
    <form className="adm-form" onSubmit={enviar} noValidate>
      {errorGeneral && <p className="form-error-general" role="alert">{errorGeneral}</p>}

      <div className="campo">
        <label htmlFor="hor-dia">Día de la semana</label>
        <select
          id="hor-dia"
          value={datos.diaSemana}
          onChange={(e) => cambiar("diaSemana", Number(e.target.value))}
        >
          {DIAS.map((d, i) => (
            <option key={d} value={i + 1}>{d}</option>
          ))}
        </select>
      </div>

      <div className="campo-doble">
        <div className="campo">
          <label htmlFor="hor-inicio">Desde</label>
          <input
            id="hor-inicio"
            type="time"
            step="900"
            autoFocus
            value={datos.horaInicio}
            onChange={(e) => cambiar("horaInicio", e.target.value)}
            aria-invalid={!!errores.horaInicio}
            aria-describedby={errores.horaInicio ? "hor-inicio-error" : undefined}
          />
          {errores.horaInicio && <p id="hor-inicio-error" className="campo-error">{errores.horaInicio}</p>}
        </div>

        <div className="campo">
          <label htmlFor="hor-fin">Hasta</label>
          <input
            id="hor-fin"
            type="time"
            step="900"
            value={datos.horaFin}
            onChange={(e) => cambiar("horaFin", e.target.value)}
            aria-invalid={!!errores.horaFin}
            aria-describedby="hor-fin-ayuda"
          />
        </div>
      </div>

      <p id="hor-fin-ayuda" className={errores.horaFin ? "campo-error" : "campo-ayuda"}>
        {errores.horaFin ||
          (minutos ? `Atenderás ${formatoHoras(minutos)} en este rango.` : "Elige la hora de inicio y de fin.")}
      </p>

      <div className="form-acciones">
        <button type="button" className="btn btn-sec" onClick={alCerrar} disabled={enviando}>
          Cancelar
        </button>
        <button type="submit" className="btn btn-pri" disabled={enviando || sinCambios}>
          {enviando && <span className="btn-spinner" aria-hidden="true" />}
          {enviando ? "Guardando..." : editando ? "Guardar cambios" : "Agregar horario"}
        </button>
      </div>
    </form>
  );
}

export default FormHorario;
