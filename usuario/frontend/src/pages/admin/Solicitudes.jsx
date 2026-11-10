import { useCallback, useEffect, useRef, useState } from "react";
import {
  listarSolicitudesAlmacen,
  decidirSolicitud,
  ESTADOS_SOLICITUD,
  MAX_OBSERVACION,
  fechaHora,
} from "../../services/inventarioService";
import Modal from "../../components/admin/Modal";
import Aviso from "../../components/admin/Aviso";
import "../../components/admin/admin-ui.css";
import "./Inventario.css";

/* T59.2 – Bandeja de solicitudes de repuestos del administrador (API de T59.1).
   - Pendientes: de la más antigua a la más nueva (orden de atención); Todas: de la más nueva a la más antigua
   - Aprobar descuenta el stock (salida en el kardex) y suma el repuesto al costo de la orden
   - Rechazar pide una observación para el mecánico
   - El backend responde 409 si ya fue atendida, si la orden ya no está activa o si no hay stock
   Se actualiza sola cada minuto mientras se ven las pendientes. */

const PESTANAS = [
  { valor: "PENDIENTE", texto: "Pendientes" },
  { valor: "", texto: "Todas" },
];
const CADA_ACTUALIZACION = 60 * 1000;

const hoyLima = () => new Intl.DateTimeFormat("sv-SE", { timeZone: "America/Lima" }).format(new Date());

// "hoy, 09:12" o "08/11/2026 09:12"
const cuando = (f) => (!f ? "—" : f.slice(0, 10) === hoyLima() ? `hoy, ${f.slice(11, 16)}` : fechaHora(f));

function Solicitudes() {
  const [pestana, setPestana] = useState("PENDIENTE");
  const [recarga, setRecarga] = useState(0);
  const [resultado, setResultado] = useState({ clave: null, lista: [], error: "" });
  const [decision, setDecision] = useState(null); // { solicitud, tipo: "APROBADA" | "RECHAZADA" }
  const [observacion, setObservacion] = useState("");
  const [errorDecision, setErrorDecision] = useState("");
  const [enviando, setEnviando] = useState(false);
  const [aviso, setAviso] = useState(null);

  const clave = `${pestana}|${recarga}`;
  const cargando = resultado.clave !== clave;
  const primeraCarga = resultado.clave === null;

  const contadorAvisos = useRef(0);
  const avisar = (tipo, texto) => {
    contadorAvisos.current += 1;
    setAviso({ tipo, texto, id: contadorAvisos.current });
  };
  const cerrarAviso = useCallback(() => setAviso(null), []);

  useEffect(() => {
    let vigente = true;
    listarSolicitudesAlmacen(pestana)
      .then((lista) => vigente && setResultado({ clave, lista, error: "" }))
      .catch((e) => vigente && setResultado({ clave, lista: [], error: e.message }));
    return () => {
      vigente = false;
    };
  }, [clave, pestana]);

  useEffect(() => {
    if (pestana !== "PENDIENTE") return undefined;
    const id = setInterval(() => setRecarga((n) => n + 1), CADA_ACTUALIZACION);
    return () => clearInterval(id);
  }, [pestana]);

  const abrir = (solicitud, tipo) => {
    setDecision({ solicitud, tipo });
    setObservacion("");
    setErrorDecision("");
  };

  const confirmar = async (e) => {
    e.preventDefault();
    const { solicitud, tipo } = decision;
    if (tipo === "RECHAZADA" && !observacion.trim()) return setErrorDecision("Explica al mecánico por qué se rechaza.");
    if (observacion.trim().length > MAX_OBSERVACION) return setErrorDecision(`Máximo ${MAX_OBSERVACION} caracteres.`);
    setEnviando(true);
    setErrorDecision("");
    try {
      const r = await decidirSolicitud(solicitud.id, tipo, observacion);
      setDecision(null);
      setResultado((res) => ({
        ...res,
        lista: pestana === "PENDIENTE" ? res.lista.filter((s) => s.id !== r.id) : res.lista.map((s) => (s.id === r.id ? r : s)),
      }));
      avisar(
        "ok",
        tipo === "APROBADA"
          ? `Aprobada: salieron ${r.cantidad} × ${r.nombre} hacia la orden N.º ${r.idOrden}. Stock: ${r.stockDisponible}.`
          : `Rechazaste la solicitud de ${r.nombre}. El mecánico verá tu observación.`
      );
    } catch (err) {
      setErrorDecision(err.message);
      if (err.status === 409) setRecarga((n) => n + 1);
    } finally {
      setEnviando(false);
    }
  };

  const { lista } = resultado;
  const pendientes = lista.filter((s) => s.estado === "PENDIENTE").length;

  return (
    <section className="gestion sol">
      <div className="gestion-cabecera">
        <div>
          <h2>Solicitudes de repuestos</h2>
          <p>
            Pedidos de los mecánicos para sus órdenes de trabajo.
            {!primeraCarga && !resultado.error && pestana === "PENDIENTE" && (
              <>
                {" "}
                <strong>{pendientes}</strong> {pendientes === 1 ? "pendiente" : "pendientes"} de atención.
              </>
            )}
          </p>
        </div>
        <button type="button" className="btn btn-sec" onClick={() => setRecarga((n) => n + 1)} disabled={cargando}>
          Actualizar
        </button>
      </div>

      <div className="filtros">
        <div className="segmentos" role="group" aria-label="Mostrar solicitudes">
          {PESTANAS.map((p) => (
            <button
              key={p.valor || "todas"}
              type="button"
              className={pestana === p.valor ? "activo" : ""}
              aria-pressed={pestana === p.valor}
              onClick={() => setPestana(p.valor)}
            >
              {p.texto}
            </button>
          ))}
        </div>
      </div>

      <div className={cargando && !primeraCarga ? "tabla-caja actualizando" : "tabla-caja"} aria-busy={cargando}>
        {resultado.error ? (
          <div className="tabla-estado">
            <p className="tabla-estado-titulo">No se pudieron cargar las solicitudes</p>
            <p>{resultado.error}</p>
            <button type="button" className="btn btn-sec" onClick={() => setRecarga((n) => n + 1)}>
              Reintentar
            </button>
          </div>
        ) : primeraCarga ? (
          <div className="tabla-estado">
            <p>Cargando solicitudes...</p>
          </div>
        ) : lista.length === 0 ? (
          <div className="tabla-estado">
            <p className="tabla-estado-titulo">{pestana === "PENDIENTE" ? "No hay solicitudes pendientes" : "Aún no hay solicitudes"}</p>
            <p>Cuando un mecánico pida un repuesto desde su orden de trabajo, aparecerá aquí.</p>
          </div>
        ) : (
          <ul className="sol-lista">
            {lista.map((s) => {
              const alcanza = s.stockDisponible >= s.cantidad;
              return (
                <li key={s.id} className={`sol-item sol-${s.estado}`}>
                  <div className="sol-cab">
                    <span className="inv-codigo">{s.codigo}</span>
                    <strong>
                      {s.cantidad} × {s.nombre}
                    </strong>
                    {pestana === "" && <span className={`sol-estado sol-${s.estado}`}>{ESTADOS_SOLICITUD[s.estado]}</span>}
                  </div>
                  <p className="sol-meta">
                    Orden N.º {s.idOrden} · {s.solicitadoPor} · {cuando(s.fechaSolicitud)}
                  </p>
                  {s.motivo && <p className="sol-motivo">«{s.motivo}»</p>}
                  {s.estado === "PENDIENTE" ? (
                    <div className="sol-pie">
                      <span className={alcanza ? "sol-stock" : "sol-stock falta"}>
                        {alcanza
                          ? `Stock: ${s.stockDisponible} → quedarán ${s.stockDisponible - s.cantidad}`
                          : `Stock insuficiente: hay ${s.stockDisponible}, se piden ${s.cantidad}`}
                      </span>
                      <div className="sol-acciones">
                        <button type="button" className="btn btn-sec sol-rechazar" onClick={() => abrir(s, "RECHAZADA")}>
                          Rechazar
                        </button>
                        <button type="button" className="btn btn-pri" onClick={() => abrir(s, "APROBADA")} disabled={!alcanza}>
                          Aprobar
                        </button>
                      </div>
                    </div>
                  ) : (
                    <p className="sol-atendida">
                      {s.estado === "APROBADA" ? "Aprobada" : "Rechazada"} por {s.atendidoPor || "—"} · {cuando(s.fechaAtencion)}
                      {s.observacion && <> · «{s.observacion}»</>}
                    </p>
                  )}
                </li>
              );
            })}
          </ul>
        )}
      </div>

      <Modal
        abierto={!!decision}
        alCerrar={() => !enviando && setDecision(null)}
        titulo={decision?.tipo === "APROBADA" ? "¿Aprobar la solicitud?" : "¿Rechazar la solicitud?"}
        pequeno
      >
        {decision && (
          <form className="confirmar" onSubmit={confirmar} noValidate>
            <p>
              <strong>
                {decision.solicitud.cantidad} × {decision.solicitud.nombre}
              </strong>{" "}
              para la orden N.º {decision.solicitud.idOrden}.{" "}
              {decision.tipo === "APROBADA"
                ? `Se descontarán del almacén (quedarán ${decision.solicitud.stockDisponible - decision.solicitud.cantidad}) y se sumarán al costo de la orden.`
                : "El mecánico verá tu observación en su orden de trabajo."}
            </p>
            <div className="campo">
              <label htmlFor="sol-obs">
                Observación {decision.tipo === "APROBADA" && <span className="opcional">(opcional)</span>}
              </label>
              <textarea
                id="sol-obs"
                rows={2}
                maxLength={MAX_OBSERVACION}
                autoFocus
                placeholder={decision.tipo === "APROBADA" ? "Ej. Recógelo en el estante B" : "Ej. Usa los del proveedor; llegan mañana"}
                value={observacion}
                onChange={(e) => {
                  setObservacion(e.target.value);
                  setErrorDecision("");
                }}
              />
            </div>
            {errorDecision && (
              <p className="form-error-general" role="alert">
                {errorDecision}
              </p>
            )}
            <div className="form-acciones">
              <button type="button" className="btn btn-sec" onClick={() => setDecision(null)} disabled={enviando}>
                Cancelar
              </button>
              <button type="submit" className={decision.tipo === "APROBADA" ? "btn btn-pri" : "btn btn-peligro"} disabled={enviando}>
                {enviando && <span className="btn-spinner" aria-hidden="true" />}
                {enviando ? "Guardando..." : decision.tipo === "APROBADA" ? "Aprobar" : "Rechazar"}
              </button>
            </div>
          </form>
        )}
      </Modal>

      <Aviso aviso={aviso} alCerrar={cerrarAviso} />
    </section>
  );
}

export default Solicitudes;
