import { useCallback, useEffect, useRef, useState } from "react";
import { Link, useSearchParams } from "react-router-dom";
import {
  listarMisCitas,
  cancelarCita,
  ESTADOS_CITA_CLIENTE,
  ESTADOS_ACTIVOS,
  HORAS_CANCELAR,
  HORAS_REPROGRAMAR,
  puedeCancelar,
  puedeReprogramar,
  vencida,
  minutosPara,
} from "../../services/citasClienteService";
import { fechaLarga, hora12, diaSemana, mesCorto } from "../../services/reservaService";
import { formatoPrecio } from "../../services/serviciosService";
import { hoyLima } from "../../utils/fechas";
import ReprogramarCita from "./ReprogramarCita";
import Modal from "../../components/admin/Modal";
import Aviso from "../../components/admin/Aviso";
import "../../components/admin/admin-ui.css";
import "./Cliente.css";
import "./Reservar.css";
import "./MisCitas.css";

/* CLI-04 – Mis citas del cliente (APIs de T30.1, T31.1 y T32.1).
   - Próximas y en curso (de la más cercana a la más lejana) e Historial (de la más reciente a la más antigua)
   - Cada cita muestra su estado explicado para el cliente
   - Cancelar (hasta 2 horas antes) y reprogramar con los turnos libres del mismo mecánico (hasta 24 horas antes)
   - "Ver seguimiento" / "Revisar presupuesto" abren el detalle de la atención (CLI-05)
   - "Pagar servicio" y "Comprobante de pago" abren el pago (CLI-06)
   ?nueva=ID resalta la cita recién reservada. */

const PESTANAS = [
  { valor: "proximas", texto: "Próximas y en curso" },
  { valor: "historial", texto: "Historial" },
];
const CADA_ACTUALIZACION = 60 * 1000;
const clave = (c) => `${c.fecha}${c.inicio}`;

function separar(lista) {
  const proximas = lista.filter((c) => ESTADOS_ACTIVOS.includes(c.estado) && !vencida(c)).sort((a, b) => clave(a).localeCompare(clave(b)));
  const historial = lista.filter((c) => !proximas.includes(c)).sort((a, b) => clave(b).localeCompare(clave(a)));
  return { proximas, historial };
}

// Texto de cuánto falta: "Hoy, en 3 h", "Mañana", "En 5 días"
function cuandoEs(c) {
  const min = minutosPara(c);
  if (min < 0) return "";
  if (min < 60) return `En ${min} min`;
  const horas = Math.floor(min / 60);
  const dias = Math.round((new Date(`${c.fecha}T12:00:00Z`) - new Date(`${hoyLima()}T12:00:00Z`)) / 864e5);
  if (dias === 0) return `Hoy, en ${horas} h`;
  if (dias === 1) return "Mañana";
  return `En ${dias} días`;
}

function TarjetaCita({ cita, nueva, alCancelar, alReprogramar }) {
  const sinAtender = vencida(cita);
  const estado = ESTADOS_CITA_CLIENTE[cita.estado];
  const activa = cita.estado === "PENDIENTE" || cita.estado === "CONFIRMADA";
  const cancelable = puedeCancelar(cita);
  const reprogramable = puedeReprogramar(cita);
  const falta = activa && !sinAtender ? cuandoEs(cita) : "";

  return (
    <li id={`cita-${cita.id}`} className={`cit-item cit-${sinAtender ? "VENCIDA" : cita.estado}${nueva ? " nueva" : ""}`}>
      <div className="cit-fecha" aria-hidden="true">
        <span>{diaSemana(cita.fecha).slice(0, 3)}</span>
        <strong>{Number(cita.fecha.slice(8, 10))}</strong>
        <span>{mesCorto(cita.fecha)}</span>
      </div>

      <div className="cit-cuerpo">
        <div className="cit-cab">
          <h3>{cita.servicio}</h3>
          <span className={`cit-estado est-${sinAtender ? "VENCIDA" : cita.estado}`}>{sinAtender ? "Sin atender" : estado.texto}</span>
          {nueva && <span className="cit-nueva">Nueva</span>}
        </div>
        <p className="cit-cuando">
          <span className="sr-only">{fechaLarga(cita.fecha)}, </span>
          <strong>
            {hora12(cita.inicio)} a {hora12(cita.fin)}
          </strong>
          {falta && <span className="cit-falta">{falta}</span>}
        </p>
        <dl className="cit-datos">
          <div>
            <dt>Mecánico</dt>
            <dd>{cita.mecanico}</dd>
          </div>
          <div>
            <dt>Vehículo</dt>
            <dd>{cita.placa}</dd>
          </div>
          <div>
            <dt>Código</dt>
            <dd>{cita.ticket}</dd>
          </div>
          {cita.precio !== null && (
            <div>
              <dt>Precio base</dt>
              <dd>{formatoPrecio(cita.precio)}</dd>
            </div>
          )}
        </dl>
        <p className="cit-detalle">
          {sinAtender ? "La hora de esta cita ya pasó y no llegó a atenderse. Si aún lo necesitas, reserva una nueva." : estado.detalle}
        </p>

        <div className="cit-acciones">
          {cita.estado === "FINALIZADA" && (
            <Link to={`/cliente/citas/${cita.id}/pago`} className="btn btn-pri">
              Pagar servicio
            </Link>
          )}
          {cita.estado === "PAGADO" && (
            <Link to={`/cliente/citas/${cita.id}/pago`} className="btn btn-sec">
              Comprobante de pago
            </Link>
          )}
          {cita.estado === "ESPERA_APROBACION" && (
            <Link to={`/cliente/citas/${cita.id}#presupuesto`} className="btn btn-pri">
              Revisar presupuesto
            </Link>
          )}
          <Link to={`/cliente/citas/${cita.id}`} className="btn btn-sec">
            {cita.estado === "EN_PROCESO" || cita.estado === "ESPERA_APROBACION" ? "Ver avance" : "Ver seguimiento"}
          </Link>
          {activa && !sinAtender && (
            <>
              {reprogramable && (
                <button type="button" className="btn btn-sec" onClick={() => alReprogramar(cita)}>
                  Cambiar fecha u hora
                </button>
              )}
              {cancelable && (
                <button type="button" className="btn btn-sec cit-cancelar" onClick={() => alCancelar(cita)}>
                  Cancelar cita
                </button>
              )}
              {!reprogramable && (
                <p className="cit-regla">
                  {cancelable
                    ? `Faltan menos de ${HORAS_REPROGRAMAR} horas: ya no se puede cambiar el horario, pero aún puedes cancelarla.`
                    : `Faltan menos de ${HORAS_CANCELAR} horas: ya no se puede cancelar ni cambiar. Si no puedes venir, avisa al taller.`}
                </p>
              )}
            </>
          )}
        </div>
      </div>
    </li>
  );
}

function MisCitas() {
  const [params] = useSearchParams();
  const idNueva = Number(params.get("nueva")) || null;
  const [pestana, setPestana] = useState("proximas");
  const [recarga, setRecarga] = useState(0);
  const [resultado, setResultado] = useState({ clave: null, lista: [], error: "" });
  const [porCancelar, setPorCancelar] = useState(null);
  const [cancelando, setCancelando] = useState(false);
  const [errorCancelar, setErrorCancelar] = useState("");
  const [porReprogramar, setPorReprogramar] = useState(null);
  const [aviso, setAviso] = useState(null);

  const contadorAvisos = useRef(0);
  const avisar = (tipo, texto) => {
    contadorAvisos.current += 1;
    setAviso({ tipo, texto, id: contadorAvisos.current });
  };
  const cerrarAviso = useCallback(() => setAviso(null), []);
  const recargar = () => setRecarga((n) => n + 1);

  const primeraCarga = resultado.clave === null;
  const cargando = resultado.clave !== recarga;

  useEffect(() => {
    let vigente = true;
    listarMisCitas()
      .then((lista) => vigente && setResultado({ clave: recarga, lista, error: "" }))
      .catch((e) => vigente && setResultado({ clave: recarga, lista: [], error: e.message }));
    return () => {
      vigente = false;
    };
  }, [recarga]);

  // El estado cambia cuando el mecánico confirma o avanza la atención: se actualiza cada minuto
  useEffect(() => {
    const id = setInterval(() => !document.hidden && setRecarga((n) => n + 1), CADA_ACTUALIZACION);
    return () => clearInterval(id);
  }, []);

  // Lleva la vista a la cita recién reservada
  useEffect(() => {
    if (!primeraCarga && idNueva) document.getElementById(`cita-${idNueva}`)?.scrollIntoView({ block: "center" });
  }, [primeraCarga, idNueva]);

  const reemplazar = (c) => setResultado((r) => ({ ...r, lista: r.lista.map((x) => (x.id === c.id ? c : x)) }));

  const confirmarCancelacion = async () => {
    setCancelando(true);
    setErrorCancelar("");
    try {
      const c = await cancelarCita(porCancelar.id);
      reemplazar(c);
      setPorCancelar(null);
      avisar("ok", `Cancelaste tu cita ${c.ticket}. El turno quedó libre.`);
    } catch (err) {
      setErrorCancelar(err.message);
      if (err.status === 409) recargar();
    } finally {
      setCancelando(false);
    }
  };

  const { proximas, historial } = separar(resultado.lista);
  const lista = pestana === "proximas" ? proximas : historial;
  const contador = { proximas: proximas.length, historial: historial.length };

  return (
    <section className="gestion cit">
      <div className="gestion-cabecera">
        <div>
          <h2>Mis citas</h2>
          <p>Sigue el estado de tus reservas, cámbialas de horario o cancélalas a tiempo.</p>
        </div>
        <div className="cit-cab-acciones">
          <button type="button" className="btn btn-sec" onClick={recargar} disabled={cargando}>
            Actualizar
          </button>
          <Link to="/cliente/reservar" className="btn btn-pri">
            <span aria-hidden="true">+</span> Reservar cita
          </Link>
        </div>
      </div>

      <div className="filtros">
        <div className="segmentos" role="group" aria-label="Mostrar citas">
          {PESTANAS.map((p) => (
            <button key={p.valor} type="button" className={pestana === p.valor ? "activo" : ""} aria-pressed={pestana === p.valor} onClick={() => setPestana(p.valor)}>
              {p.texto}
              {!primeraCarga && !resultado.error && <span className="cit-contador">{contador[p.valor]}</span>}
            </button>
          ))}
        </div>
      </div>

      {resultado.error ? (
        <div className="tabla-caja">
          <div className="tabla-estado">
            <p className="tabla-estado-titulo">No se pudieron cargar tus citas</p>
            <p>{resultado.error}</p>
            <button type="button" className="btn btn-sec" onClick={recargar}>
              Reintentar
            </button>
          </div>
        </div>
      ) : primeraCarga ? (
        <div className="tabla-caja">
          <div className="tabla-estado">
            <p>Cargando tus citas...</p>
          </div>
        </div>
      ) : lista.length === 0 ? (
        <div className="tabla-caja">
          <div className="tabla-estado">
            <p className="tabla-estado-titulo">{pestana === "proximas" ? "No tienes citas próximas" : "Aún no tienes citas anteriores"}</p>
            <p>{pestana === "proximas" ? "Reserva una cita y aquí verás su estado hasta que recojas tu vehículo." : "Aquí quedarán las citas pagadas, canceladas o no aceptadas."}</p>
            {pestana === "proximas" && (
              <Link to="/cliente/reservar" className="btn btn-pri">
                Reservar una cita
              </Link>
            )}
          </div>
        </div>
      ) : (
        <ul className={cargando ? "cit-lista actualizando" : "cit-lista"} aria-busy={cargando}>
          {lista.map((c) => (
            <TarjetaCita
              key={c.id}
              cita={c}
              nueva={c.id === idNueva}
              alCancelar={(x) => {
                setPorCancelar(x);
                setErrorCancelar("");
              }}
              alReprogramar={setPorReprogramar}
            />
          ))}
        </ul>
      )}

      <Modal abierto={!!porCancelar} alCerrar={() => !cancelando && setPorCancelar(null)} titulo="Cancelar cita" pequeno>
        {porCancelar && (
          <div className="confirmar">
            <p>
              ¿Cancelar tu cita de <strong>{porCancelar.servicio}</strong> del {fechaLarga(porCancelar.fecha)} a las {hora12(porCancelar.inicio)} (
              {porCancelar.placa})? El turno quedará libre para otros clientes y no se puede deshacer.
            </p>
            {errorCancelar && (
              <p className="form-error-general" role="alert">
                {errorCancelar}
              </p>
            )}
            <div className="form-acciones">
              <button type="button" className="btn btn-sec" onClick={() => setPorCancelar(null)} disabled={cancelando}>
                Volver
              </button>
              <button type="button" className="btn btn-peligro" onClick={confirmarCancelacion} disabled={cancelando}>
                {cancelando && <span className="btn-spinner" aria-hidden="true" />}
                {cancelando ? "Cancelando..." : "Sí, cancelar cita"}
              </button>
            </div>
          </div>
        )}
      </Modal>

      <Modal abierto={!!porReprogramar} alCerrar={() => setPorReprogramar(null)} titulo="Cambiar fecha u hora">
        {porReprogramar && (
          <ReprogramarCita
            cita={porReprogramar}
            alCancelar={() => setPorReprogramar(null)}
            alListo={(c) => {
              reemplazar(c);
              setPorReprogramar(null);
              avisar("ok", `Tu cita ahora es el ${fechaLarga(c.fecha)} a las ${hora12(c.inicio)}; el mecánico debe confirmarla de nuevo.`);
            }}
          />
        )}
      </Modal>

      <Aviso aviso={aviso} alCerrar={cerrarAviso} />
    </section>
  );
}

export default MisCitas;
