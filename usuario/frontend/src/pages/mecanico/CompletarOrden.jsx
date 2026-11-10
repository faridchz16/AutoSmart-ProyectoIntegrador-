import { useState } from "react";
import { completarMantenimiento, formatoSoles } from "../../services/ordenesService";
import { formatoHoras } from "../../services/horariosService";

/* T52.2 – Botón "Completar mantenimiento" con validaciones previas (API de T52.1).
   Antes de habilitarlo se revisa lo mismo que exige el backend:
   - diagnóstico registrado
   - al menos una tarea y todas completadas
   - ninguna solicitud de repuestos pendiente
   Al completarlo se registra la hora de fin y la orden queda cerrada (solo lectura). */

const Marca = ({ ok }) => (
  <span className={ok ? "ot-req-marca ok" : "ot-req-marca"} aria-hidden="true">
    {ok ? "✓" : "!"}
  </span>
);

function CompletarOrden({ idOrden, estado, diagnostico, tareas, solicitudes, presupuesto, consumos, minutos, alCompletar, alConflicto }) {
  const [confirmar, setConfirmar] = useState(false);
  const [enviando, setEnviando] = useState(false);
  const [error, setError] = useState("");

  const pendientesTareas = tareas.total - tareas.completadas;
  const solicitudesPendientes = solicitudes.filter((s) => s.estado === "PENDIENTE").length;
  const requisitos = [
    {
      ok: !!diagnostico,
      texto: diagnostico ? "Diagnóstico registrado" : "Falta registrar el diagnóstico",
      ancla: "#ot-diagnostico",
    },
    {
      ok: tareas.total > 0 && pendientesTareas === 0,
      texto:
        tareas.total === 0
          ? "Registra al menos una tarea"
          : pendientesTareas === 0
            ? `Las ${tareas.total} tareas están completas`
            : `Faltan ${pendientesTareas} de ${tareas.total} tareas por completar`,
      ancla: "#ot-tareas",
    },
    {
      ok: solicitudesPendientes === 0,
      texto:
        solicitudesPendientes === 0
          ? "Sin solicitudes de repuestos pendientes"
          : `${solicitudesPendientes} ${solicitudesPendientes === 1 ? "solicitud de repuesto espera" : "solicitudes de repuestos esperan"} respuesta del almacén`,
      ancla: "#ot-repuestos",
    },
  ];
  const listo = estado === "EN_PROCESO" && requisitos.every((r) => r.ok);

  const completar = async () => {
    setEnviando(true);
    setError("");
    try {
      alCompletar(await completarMantenimiento(idOrden));
    } catch (err) {
      setConfirmar(false);
      setError(err.message);
      if (err.status === 409) alConflicto();
    } finally {
      setEnviando(false);
    }
  };

  if (estado === "ESPERA_APROBACION") {
    return <p className="ot-diag-bloqueo">Podrás completar el mantenimiento cuando el cliente responda el presupuesto.</p>;
  }

  return (
    <div className="ot-completar">
      <ul className="ot-req">
        {requisitos.map((r) => (
          <li key={r.ancla} className={r.ok ? "ok" : ""}>
            <Marca ok={r.ok} />
            {r.ok ? r.texto : <a href={r.ancla}>{r.texto}</a>}
          </li>
        ))}
        <li className="info">
          <span className="ot-req-marca info" aria-hidden="true">i</span>
          {presupuesto
            ? presupuesto.estado === "APROBADO"
              ? `Presupuesto aprobado por ${formatoSoles(presupuesto.total)}`
              : "El último presupuesto fue rechazado por el cliente"
            : "Sin presupuesto: solo se cobrará el servicio reservado"}
          {consumos.costoRepuestos > 0 && ` · repuestos usados ${formatoSoles(consumos.costoRepuestos)}`}
        </li>
      </ul>

      {error && (
        <p className="form-error-general" role="alert">
          {error}
        </p>
      )}

      {confirmar ? (
        <div className="ot-pres-confirmar" role="alertdialog" aria-labelledby="ot-fin-titulo">
          <p id="ot-fin-titulo">
            <strong>¿Completar el mantenimiento?</strong>
          </p>
          <p>
            Se registrará la hora de fin{minutos !== null && minutos >= 0 ? ` (en atención ${formatoHoras(minutos)})` : ""} y la orden
            quedará cerrada: ya no podrás modificar el diagnóstico, las tareas ni los repuestos. El cliente lo verá como finalizado en la app.
          </p>
          <div className="form-acciones">
            <button type="button" className="btn btn-sec" onClick={() => setConfirmar(false)} disabled={enviando}>
              Volver
            </button>
            <button type="button" className="btn btn-exito" onClick={completar} disabled={enviando} autoFocus>
              {enviando && <span className="btn-spinner" aria-hidden="true" />}
              {enviando ? "Completando..." : "Sí, completar"}
            </button>
          </div>
        </div>
      ) : (
        <div className="ot-completar-pie">
          <p>{listo ? "Todo listo. Revisa que el vehículo esté en orden antes de entregarlo." : "Completa los puntos pendientes para cerrar la orden."}</p>
          <button type="button" className="btn btn-exito" onClick={() => setConfirmar(true)} disabled={!listo}>
            <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" strokeWidth="2.4" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
              <path d="M5 12.5l4.5 4.5L19 7.5" />
            </svg>
            Completar mantenimiento
          </button>
        </div>
      )}
    </div>
  );
}

export default CompletarOrden;
