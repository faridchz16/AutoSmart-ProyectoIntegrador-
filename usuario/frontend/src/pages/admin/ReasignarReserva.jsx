import { useEffect, useState } from "react";
import Modal from "../../components/admin/Modal";
import { listarMecanicosLibres, reasignarReserva } from "../../services/reservasService";
import { ESTADOS_CITA } from "../../services/agendaService";
import { fechaLarga } from "../../services/bloqueosService";

/* T39.2 – Reasignar una reserva a otro mecánico.
   Muestra solo los mecánicos libres en ese horario y con la especialidad del servicio
   (GET /admin/appointments/{id}/available-mechanics) y guarda con PUT .../reassign.
   Al reasignar, la cita vuelve a quedar pendiente hasta que el nuevo mecánico la acepte. */

const iniciales = (nombre) =>
  nombre.split(" ").filter(Boolean).slice(0, 2).map((p) => p[0]).join("").toUpperCase() || "?";

// Errores que significan "la cita cambió por otro lado": hay que recargar la tabla
const CITA_DESACTUALIZADA = /cancelada o rechazada|ya paso|no encontrada/i;
// Errores que significan "la lista de mecánicos libres cambió": hay que recargar la lista
const LISTA_DESACTUALIZADA = /no esta libre|ya esta asignada|no encontrado|especialidad/i;

function Contenido({ reserva, alCerrar, alReasignar, alDesactualizar }) {
  const [recarga, setRecarga] = useState(0);
  const [lista, setLista] = useState({ clave: null, mecanicos: [], error: "" });
  const [elegido, setElegido] = useState("");
  const [enviando, setEnviando] = useState(false);
  const [error, setError] = useState("");
  const cargando = lista.clave !== recarga;

  useEffect(() => {
    let vigente = true;
    listarMecanicosLibres(reserva.id)
      .then((mecanicos) => vigente && setLista({ clave: recarga, mecanicos, error: "" }))
      .catch((e) => vigente && setLista({ clave: recarga, mecanicos: [], error: e.message }));
    return () => {
      vigente = false;
    };
  }, [reserva.id, recarga]);

  const nuevo = lista.mecanicos.find((m) => String(m.id) === String(elegido));

  const confirmar = async () => {
    if (!nuevo || enviando) return;
    setEnviando(true);
    setError("");
    try {
      const actualizada = await reasignarReserva(reserva.id, nuevo.id);
      alReasignar({ ...reserva, ...actualizada, mecanico: actualizada.mecanico || nuevo.nombre });
    } catch (e) {
      setEnviando(false);
      setError(e.message);
      if (CITA_DESACTUALIZADA.test(e.message)) alDesactualizar();
      else if (LISTA_DESACTUALIZADA.test(e.message) || e.status === 409) {
        setElegido("");
        setRecarga((n) => n + 1);
      }
    }
  };

  return (
    <div className="rr">
      {/* ---------- Resumen de la reserva ---------- */}
      <div className="rr-cita">
        <div className="rr-cita-fila">
          <span className={`insignia rs-est-${reserva.estado}`}>{ESTADOS_CITA[reserva.estado] ?? reserva.estado}</span>
          <span className="rs-ticket">{reserva.ticket}</span>
        </div>
        <strong>{reserva.servicio}</strong>
        <span className="rr-cuando">
          <span className="rr-fecha">{fechaLarga(reserva.fecha)}</span> · {reserva.horaInicio} – {reserva.horaFin}
        </span>
        <span className="rr-cliente">
          {reserva.cliente}
          {reserva.placa && <span className="rs-placa">{reserva.placa}</span>}
        </span>
        <p className="rr-actual">
          Mecánico actual: <strong>{reserva.mecanico || "—"}</strong>
        </p>
      </div>

      {/* ---------- Mecánicos libres ---------- */}
      <fieldset className="rr-lista" aria-busy={cargando} disabled={enviando}>
        <legend>Elige al nuevo mecánico</legend>
        {cargando && lista.clave === null ? (
          <div className="rr-cargando">
            <span className="esq esq-largo" />
            <span className="esq esq-largo" />
          </div>
        ) : lista.error ? (
          <div className="rr-vacio rr-error" role="alert">
            <p>No se pudo cargar la lista de mecánicos: {lista.error}</p>
            <button type="button" className="btn btn-sec" onClick={() => setRecarga((n) => n + 1)}>
              Reintentar
            </button>
          </div>
        ) : lista.mecanicos.length === 0 ? (
          <div className="rr-vacio">
            <p className="rr-vacio-titulo">No hay otros mecánicos libres en ese horario</p>
            <p>
              Solo aparecen los mecánicos activos que atienden ese día, no tienen otra cita a esa hora y tienen la
              especialidad que pide el servicio.
            </p>
          </div>
        ) : (
          <div className={cargando ? "rr-opciones actualizando" : "rr-opciones"}>
            {lista.mecanicos.map((m) => (
              <label key={m.id} className={String(elegido) === String(m.id) ? "rr-opcion elegida" : "rr-opcion"}>
                <input
                  type="radio"
                  name="rr-mecanico"
                  value={m.id}
                  checked={String(elegido) === String(m.id)}
                  onChange={() => {
                    setElegido(m.id);
                    setError("");
                  }}
                />
                <span className="rr-avatar" aria-hidden="true">{iniciales(m.nombre)}</span>
                <span className="rr-datos">
                  <strong>{m.nombre}</strong>
                  {m.especialidades.length > 0 ? (
                    <span className="rr-esps">
                      {m.especialidades.map((e) => (
                        <span key={e}>{e}</span>
                      ))}
                    </span>
                  ) : (
                    <small>Sin especialidades registradas</small>
                  )}
                </span>
              </label>
            ))}
          </div>
        )}
      </fieldset>

      {nuevo && (
        <p className="rr-nota">
          La cita pasará a <strong>{nuevo.nombre}</strong> y quedará <strong>pendiente</strong> hasta que la acepte.
        </p>
      )}
      {error && <p className="form-error-general" role="alert">{error}</p>}

      <div className="form-acciones">
        <button type="button" className="btn btn-sec" onClick={alCerrar} disabled={enviando}>
          Cancelar
        </button>
        <button type="button" className="btn btn-pri" onClick={confirmar} disabled={!nuevo || enviando}>
          {enviando && <span className="btn-spinner" aria-hidden="true" />}
          {enviando ? "Reasignando..." : "Reasignar"}
        </button>
      </div>
    </div>
  );
}

function ReasignarReserva({ reserva, alCerrar, alReasignar, alDesactualizar }) {
  return (
    <Modal abierto={!!reserva} alCerrar={alCerrar} titulo="Reasignar reserva">
      {reserva && (
        <Contenido
          key={reserva.id}
          reserva={reserva}
          alCerrar={alCerrar}
          alReasignar={alReasignar}
          alDesactualizar={alDesactualizar}
        />
      )}
    </Modal>
  );
}

export default ReasignarReserva;
