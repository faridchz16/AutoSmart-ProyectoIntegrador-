import { useState } from "react";
import { agregarTarea, marcarTarea, mensajeConflicto, MAX_TAREA } from "../../services/ordenesService";

/* T49.2 – Lista de tareas de la orden con casilla y porcentaje de avance (API de T49.1).
   - El mecánico agrega las tareas del trabajo y las marca al terminarlas
   - El avance (completadas / total) lo calcula el backend y es lo que verá el cliente
   - Solo se edita con la orden EN_PROCESO; para finalizar, todas deben estar completas (T52) */

function TareasOrden({ idOrden, resumen, editable, estado, momento, alCambiar, alConflicto }) {
  const [texto, setTexto] = useState("");
  const [error, setError] = useState("");
  const [agregando, setAgregando] = useState(false);
  const [marcando, setMarcando] = useState([]); // ids que se están guardando
  const [errorLista, setErrorLista] = useState("");

  const { tareas, total, completadas, porcentaje } = resumen;
  const pendientes = total - completadas;

  const fallo = (e, poner) => {
    const err = mensajeConflicto(e);
    poner(err.status === 409 && err.estado ? `${err.message} Se actualizó la orden.` : err.message);
    if (err.status === 409 && err.estado) alConflicto();
  };

  const agregar = async (e) => {
    e.preventDefault();
    const descripcion = texto.trim();
    if (!descripcion) return setError("Escribe la tarea antes de agregarla.");
    if (descripcion.length > MAX_TAREA) return setError(`Máximo ${MAX_TAREA} caracteres.`);
    if (tareas.some((t) => t.descripcion.toLowerCase() === descripcion.toLowerCase())) {
      return setError("Esa tarea ya está en la lista.");
    }
    setAgregando(true);
    setError("");
    try {
      alCambiar(await agregarTarea(idOrden, descripcion));
      setTexto("");
    } catch (err) {
      fallo(err, setError);
    } finally {
      setAgregando(false);
    }
  };

  const marcar = async (tarea) => {
    setMarcando((m) => [...m, tarea.id]);
    setErrorLista("");
    try {
      alCambiar(await marcarTarea(idOrden, tarea.id, !tarea.completada));
    } catch (err) {
      fallo(err, setErrorLista);
    } finally {
      setMarcando((m) => m.filter((id) => id !== tarea.id));
    }
  };

  return (
    <div className="ot-tareas">
      {/* ----- Avance ----- */}
      <div className="ot-avance">
        <div className="ot-avance-cab">
          <strong>{porcentaje} %</strong>
          <span>
            {total === 0
              ? "Sin tareas registradas"
              : pendientes === 0
                ? `Las ${total === 1 ? "única tarea está completa" : `${total} tareas están completas`}`
                : `${completadas} de ${total} completadas · ${pendientes} ${pendientes === 1 ? "pendiente" : "pendientes"}`}
          </span>
        </div>
        <div
          className={porcentaje === 100 ? "ot-avance-barra completa" : "ot-avance-barra"}
          role="progressbar"
          aria-valuemin={0}
          aria-valuemax={100}
          aria-valuenow={porcentaje}
          aria-label="Avance de la orden"
        >
          <span style={{ width: `${porcentaje}%` }} />
        </div>
      </div>

      {/* ----- Lista ----- */}
      {total === 0 ? (
        <p className="ot-tareas-vacio">
          {editable
            ? "Agrega las tareas del trabajo (por ejemplo, «Cambiar pastillas delanteras») y márcalas al terminarlas. El cliente verá el avance en la app."
            : estado === "FINALIZADA"
              ? "Esta orden se cerró sin tareas registradas."
              : "Aún no hay tareas registradas."}
        </p>
      ) : (
        <ul className="ot-tareas-lista">
          {tareas.map((t) => {
            const guardando = marcando.includes(t.id);
            return (
              <li key={t.id} className={t.completada ? "ot-tarea hecha" : "ot-tarea"}>
                <label className={editable ? "ot-tarea-check" : "ot-tarea-check bloqueada"}>
                  <input
                    type="checkbox"
                    checked={t.completada}
                    disabled={!editable || guardando}
                    onChange={() => marcar(t)}
                  />
                  <span className="ot-tarea-caja" aria-hidden="true">
                    {guardando ? <span className="btn-spinner" /> : "✓"}
                  </span>
                  <span className="ot-tarea-texto">{t.descripcion}</span>
                </label>
                <span className="ot-tarea-hora">
                  {t.completada && t.fechaCompletada ? `Hecha ${momento(t.fechaCompletada)}` : "Pendiente"}
                </span>
              </li>
            );
          })}
        </ul>
      )}
      {errorLista && (
        <p className="campo-error" role="alert">
          {errorLista}
        </p>
      )}

      {/* ----- Agregar ----- */}
      {editable ? (
        <form className="ot-tarea-nueva" onSubmit={agregar} noValidate>
          <label htmlFor="ot-tarea-texto" className="sr-only">
            Nueva tarea
          </label>
          <input
            id="ot-tarea-texto"
            autoComplete="off"
            maxLength={MAX_TAREA}
            placeholder="Nueva tarea, p. ej. Cambiar amortiguadores delanteros"
            value={texto}
            onChange={(e) => {
              setTexto(e.target.value);
              if (error) setError("");
            }}
            aria-invalid={!!error}
            aria-describedby={error ? "ot-tarea-error" : undefined}
          />
          <button type="submit" className="btn btn-pri" disabled={agregando}>
            {agregando && <span className="btn-spinner" aria-hidden="true" />}
            {agregando ? "Agregando..." : "Agregar"}
          </button>
          {error && (
            <p id="ot-tarea-error" className="campo-error ot-tarea-error" role="alert">
              {error}
            </p>
          )}
        </form>
      ) : (
        total > 0 && (
          <p className="ot-diag-bloqueo">
            {estado === "ESPERA_APROBACION"
              ? "Las tareas no se editan mientras el cliente revisa el presupuesto."
              : "Orden cerrada: solo lectura."}
          </p>
        )
      )}
    </div>
  );
}

export default TareasOrden;
