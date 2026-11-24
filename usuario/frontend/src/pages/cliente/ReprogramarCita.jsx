import { useEffect, useState } from "react";
import { datosParaReprogramar, reprogramarCita } from "../../services/citasClienteService";
import { fechaLarga, hora12 } from "../../services/reservaService";
import { hoyLima } from "../../utils/fechas";
import { PasoHorario } from "./PasosReserva";

/* Cambiar la fecha y hora de una cita (CLI-04). Mismo servicio y mismo mecánico: se muestran sus turnos libres.
   Si no se pueden ubicar el servicio o el mecánico en el catálogo (p. ej., el servicio se desactivó),
   se pide la fecha y la hora a mano y el backend valida si el turno está libre. */

function ReprogramarCita({ cita, alListo, alCancelar }) {
  const hoy = hoyLima();
  const [datos, setDatos] = useState({ cargado: false, valor: null, error: "" });
  const [recarga, setRecarga] = useState(0);
  const [fecha, setFecha] = useState(cita.fecha > hoy ? cita.fecha : hoy);
  const [elegido, setElegido] = useState(null); // { fecha, inicio, fin }
  const [manual, setManual] = useState({ fecha: "", hora: "" });
  const [recargaTurnos, setRecargaTurnos] = useState(0);
  const [enviando, setEnviando] = useState(false);
  const [error, setError] = useState("");

  useEffect(() => {
    let vigente = true;
    datosParaReprogramar(cita)
      .then((valor) => vigente && setDatos({ cargado: true, valor, error: "" }))
      .catch((e) => vigente && setDatos({ cargado: true, valor: null, error: e.message }));
    return () => {
      vigente = false;
    };
  }, [cita, recarga]);

  const conTurnos = datos.cargado && !!datos.valor;
  const sinCatalogo = datos.cargado && !datos.valor && !datos.error;
  const nuevo = conTurnos ? elegido : manual.fecha && manual.hora ? { fecha: manual.fecha, inicio: manual.hora } : null;

  const confirmar = async () => {
    if (!nuevo) return setError(conTurnos ? "Elige el nuevo horario." : "Indica la nueva fecha y hora.");
    if (nuevo.fecha === cita.fecha && nuevo.inicio === cita.inicio) return setError("Ese es el horario que ya tienes. Elige otro.");
    setEnviando(true);
    setError("");
    try {
      const actualizada = await reprogramarCita(cita.id, { fecha: nuevo.fecha, horaInicio: nuevo.inicio });
      alListo(actualizada);
    } catch (err) {
      if (err.status === 409 && conTurnos) {
        setElegido(null);
        setRecargaTurnos((n) => n + 1);
      }
      setError(err.status === 409 && /disponible/i.test(err.message) ? "Ese horario acaba de ocuparse. Elige otro." : err.message);
    } finally {
      setEnviando(false);
    }
  };

  return (
    <div className="cit-reprogramar">
      <p className="cit-actual">
        <span>Horario actual</span>
        <strong>
          {fechaLarga(cita.fecha)}, {hora12(cita.inicio)}
        </strong>
        <span>
          {cita.servicio} con {cita.mecanico}
        </span>
      </p>

      {!datos.cargado ? (
        <p className="res-nota">Buscando los horarios de {cita.mecanico}...</p>
      ) : datos.error ? (
        <div className="form-error-general" role="alert">
          {datos.error}{" "}
          <button type="button" className="res-enlace" onClick={() => setRecarga((n) => n + 1)}>
            Reintentar
          </button>
        </div>
      ) : conTurnos ? (
        <PasoHorario
          servicio={datos.valor.servicio}
          mecanico={datos.valor.mecanico}
          fecha={fecha}
          alCambiarFecha={(f) => {
            setFecha(f);
            setError("");
          }}
          bloqueo={elegido && { ...elegido, idMecanico: datos.valor.mecanico.id }}
          bloqueando=""
          alElegir={(t) => {
            setElegido({ fecha, inicio: t.inicio, fin: t.fin });
            setError("");
          }}
          recargaExterna={recargaTurnos}
          nota={`Tu cita sigue con ${cita.mecanico}.`}
        />
      ) : (
        sinCatalogo && (
          <>
            <p className="res-nota">No pudimos mostrar los horarios libres de este servicio. Indica la fecha y hora que prefieres y verificaremos si está libre.</p>
            <div className="campo-doble">
              <div className="campo">
                <label htmlFor="rep-fecha">Nueva fecha</label>
                <input id="rep-fecha" type="date" min={hoy} value={manual.fecha} onChange={(e) => setManual((m) => ({ ...m, fecha: e.target.value }))} />
              </div>
              <div className="campo">
                <label htmlFor="rep-hora">Hora de inicio</label>
                <input id="rep-hora" type="time" step={900} value={manual.hora} onChange={(e) => setManual((m) => ({ ...m, hora: e.target.value }))} />
              </div>
            </div>
          </>
        )
      )}

      {nuevo && (
        <p className="cit-nuevo">
          Nuevo horario:{" "}
          <strong>
            {fechaLarga(nuevo.fecha)}, {hora12(nuevo.inicio)}
          </strong>{" "}
          (quedará <strong>por confirmar</strong> hasta que el mecánico lo acepte)
        </p>
      )}
      {error && (
        <p className="form-error-general" role="alert">
          {error}
        </p>
      )}
      <div className="form-acciones">
        <button type="button" className="btn btn-sec" onClick={alCancelar} disabled={enviando}>
          Cancelar
        </button>
        <button type="button" className="btn btn-pri" onClick={confirmar} disabled={enviando || !datos.cargado}>
          {enviando && <span className="btn-spinner" aria-hidden="true" />}
          {enviando ? "Guardando..." : "Confirmar cambio"}
        </button>
      </div>
    </div>
  );
}

export default ReprogramarCita;
