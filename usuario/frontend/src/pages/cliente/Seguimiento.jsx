import { useCallback, useEffect, useRef, useState } from "react";
import { Link, useLocation, useParams } from "react-router-dom";
import {
  obtenerSeguimiento,
  obtenerPresupuesto,
  decidirPresupuesto,
  ESTADOS_PRESUPUESTO,
  TIPOS_ITEM,
  CON_ORDEN,
  EN_CURSO,
} from "../../services/seguimientoService";
import { ESTADOS_CITA_CLIENTE } from "../../services/citasClienteService";
import { SEVERIDADES, textoCategoria, cargarFoto } from "../../services/ordenesService";
import { fechaLarga, hora12 } from "../../services/reservaService";
import { formatoPrecio } from "../../services/serviciosService";
import { fechaHora, hoyLima } from "../../utils/fechas";
import Modal from "../../components/admin/Modal";
import Aviso from "../../components/admin/Aviso";
import "../../components/admin/admin-ui.css";
import "./MisCitas.css";
import "./Seguimiento.css";

/* CLI-05 – Seguimiento de una cita y presupuesto adicional (APIs de T38.1, T47.1 y T48.1).
   - Línea de tiempo: reservada, confirmada, recibida, diagnóstico, presupuesto, reparación, lista y pagada
   - Avance del trabajo según las tareas de la orden; se actualiza sola cada 30 s mientras la atención sigue
   - Presupuesto: lo que encontró el mecánico (con fotos), el detalle de cada costo y el total.
     Si espera respuesta, el cliente lo aprueba o lo rechaza (con confirmación). */

const CADA_ACTUALIZACION = 30 * 1000;

const cuando = (f) => {
  if (!f) return "";
  const h = hora12(f.slice(11, 16));
  return f.slice(0, 10) === hoyLima() ? `Hoy, ${h}` : `${fechaHora(f).slice(0, 10)}, ${h}`;
};

/* ---------- Foto protegida (se descarga con el token) ---------- */
function Foto({ foto, alt }) {
  const [estado, setEstado] = useState({ clave: null, url: "", error: false });
  useEffect(() => {
    let vigente = true;
    cargarFoto(foto)
      .then((url) => vigente && setEstado({ clave: foto.clave, url, error: false }))
      .catch(() => vigente && setEstado({ clave: foto.clave, url: "", error: true }));
    return () => {
      vigente = false;
    };
  }, [foto]);
  if (estado.clave !== foto.clave) return <span className="seg-foto-img esq" aria-hidden="true" />;
  if (estado.error) return <span className="seg-foto-img seg-foto-falla">Sin imagen</span>;
  return <img className="seg-foto-img" src={estado.url} alt={alt} />;
}

/* ---------- Línea de tiempo ---------- */
function LineaDeTiempo({ seguimiento, presupuesto }) {
  const activa = EN_CURSO.includes(seguimiento.estado);
  const actual = activa ? seguimiento.hitos.findIndex((h) => !h.completado) : -1;

  const detalle = (h) => {
    if (h.clave === "RESERVADA") return `Para el ${fechaLarga(seguimiento.fecha)} a las ${hora12(seguimiento.inicio)}`;
    if (h.clave === "PRESUPUESTO" && presupuesto) return `${formatoPrecio(presupuesto.total)} · ${ESTADOS_PRESUPUESTO[presupuesto.estado]}`;
    if ((h.clave === "RECHAZADA" || h.clave === "CANCELADA") && h.detalle) return `Motivo: ${h.detalle}`;
    if (h.clave === "EN_REPARACION" && h.completado) return "";
    return h.detalle;
  };
  const titulo = (h, i) => {
    if (h.clave === "PRESUPUESTO" && !h.completado) return "Presupuesto adicional por aprobar";
    if (h.clave === "EN_REPARACION" && i === actual) return "Reparación en curso";
    if (h.clave === "EN_REPARACION" && h.completado) return "Reparación terminada";
    return h.titulo;
  };

  return (
    <ol className="seg-linea">
      {seguimiento.hitos.map((h, i) => {
        const clase = h.clave === "CANCELADA" || h.clave === "RECHAZADA" ? "anulado" : h.completado ? "hecho" : i === actual ? "actual" : "";
        const texto = detalle(h);
        return (
          <li key={`${h.clave}-${i}`} className={clase} aria-current={i === actual ? "step" : undefined}>
            <span className="seg-punto" aria-hidden="true">
              {clase === "hecho" && "✓"}
              {clase === "anulado" && "×"}
            </span>
            <div>
              <p className="seg-hito">
                {titulo(h, i)}
                <span className="sr-only">{h.completado ? " (completado)" : i === actual ? " (en este paso)" : " (pendiente)"}</span>
              </p>
              {texto && <p className="seg-hito-detalle">{texto}</p>}
              {h.fecha && <p className="seg-hito-fecha">{cuando(h.fecha)}</p>}
            </div>
          </li>
        );
      })}
    </ol>
  );
}

/* ---------- Presupuesto ---------- */
function Presupuesto({ presupuesto, alDecidir, alVerFoto }) {
  const pendiente = presupuesto.estado === "PENDIENTE";
  return (
    <section id="presupuesto" className={pendiente ? "seg-caja seg-pres pendiente" : "seg-caja seg-pres"} aria-labelledby="seg-pres-titulo">
      <div className="seg-caja-cab">
        <h3 id="seg-pres-titulo">Presupuesto adicional</h3>
        <span className={`seg-pres-estado pres-${presupuesto.estado}`}>{ESTADOS_PRESUPUESTO[presupuesto.estado]}</span>
      </div>
      {presupuesto.descripcion && <p className="seg-pres-desc">{presupuesto.descripcion}</p>}

      {presupuesto.deficiencias.length > 0 && (
        <>
          <h4>Lo que encontró el mecánico</h4>
          <ul className="seg-defs">
            {presupuesto.deficiencias.map((d) => (
              <li key={d.id} className={`seg-def sev-${d.severidad}`}>
                <div className="seg-def-cab">
                  <span className="seg-sev">{SEVERIDADES[d.severidad].texto}</span>
                  <strong>{textoCategoria(d.categoria)}</strong>
                </div>
                <p>{d.descripcion}</p>
                {d.fotos.length > 0 && (
                  <div className="seg-fotos">
                    {d.fotos.map((f, i) => (
                      <button key={f.clave} type="button" className="seg-foto" onClick={() => alVerFoto({ foto: f, titulo: `${textoCategoria(d.categoria)}: ${d.descripcion}` })}>
                        <Foto foto={f} alt={`Foto ${i + 1} de ${textoCategoria(d.categoria)}`} />
                      </button>
                    ))}
                  </div>
                )}
              </li>
            ))}
          </ul>
        </>
      )}

      <h4>Detalle del costo</h4>
      <div className="seg-items">
        <table>
          <thead>
            <tr>
              <th scope="col">Concepto</th>
              <th scope="col" className="num seg-col-det">
                Cant.
              </th>
              <th scope="col" className="num seg-col-det">
                Precio
              </th>
              <th scope="col" className="num">
                Subtotal
              </th>
            </tr>
          </thead>
          <tbody>
            {presupuesto.items.map((it, i) => (
              <tr key={i}>
                <td>
                  <span className="seg-tipo">{TIPOS_ITEM[it.tipo]}</span>
                  {it.descripcion}
                  <span className="seg-cant-movil">
                    {it.cantidad} × {formatoPrecio(it.precioUnitario)}
                  </span>
                </td>
                <td className="num seg-col-det">{it.cantidad}</td>
                <td className="num seg-col-det">{formatoPrecio(it.precioUnitario)}</td>
                <td className="num">{formatoPrecio(it.subtotal)}</td>
              </tr>
            ))}
          </tbody>
          <tfoot>
            <tr>
              <th scope="row" colSpan={3} className="seg-total-pc">
                Total adicional
              </th>
              <th scope="row" className="seg-total-movil">
                Total adicional
              </th>
              <td className="num">{formatoPrecio(presupuesto.total)}</td>
            </tr>
          </tfoot>
        </table>
      </div>

      {pendiente ? (
        <>
          <p className="seg-nota">
            Si lo apruebas, el mecánico hará estos trabajos y el monto se sumará al pago final. Si lo rechazas, solo se hará el servicio que
            reservaste.
          </p>
          <div className="form-acciones">
            <button type="button" className="btn btn-sec" onClick={() => alDecidir("RECHAZADO")}>
              Rechazar
            </button>
            <button type="button" className="btn btn-pri" onClick={() => alDecidir("APROBADO")}>
              Aprobar presupuesto
            </button>
          </div>
        </>
      ) : (
        <p className="seg-nota">
          {presupuesto.estado === "APROBADO" ? "Lo aprobaste" : "Lo rechazaste"}
          {presupuesto.fechaDecision && ` el ${fechaHora(presupuesto.fechaDecision)}`}.{" "}
          {presupuesto.estado === "APROBADO" ? "El monto se suma al pago final del servicio." : "El mecánico solo hará el servicio que reservaste."}
        </p>
      )}
    </section>
  );
}

function Seguimiento() {
  const { id } = useParams();
  const { hash } = useLocation();
  const [recarga, setRecarga] = useState(0);
  const [resultado, setResultado] = useState({ clave: null, seguimiento: null, presupuesto: null, error: "", status: 0 });
  const [decision, setDecision] = useState(null); // "APROBADO" | "RECHAZADO"
  const [enviando, setEnviando] = useState(false);
  const [errorDecision, setErrorDecision] = useState("");
  const [visor, setVisor] = useState(null);
  const [aviso, setAviso] = useState(null);

  const contadorAvisos = useRef(0);
  const avisar = (tipo, texto) => {
    contadorAvisos.current += 1;
    setAviso({ tipo, texto, id: contadorAvisos.current });
  };
  const cerrarAviso = useCallback(() => setAviso(null), []);

  const clave = `${id}|${recarga}`;
  const cargando = resultado.clave !== clave;
  const { seguimiento, presupuesto } = resultado;

  useEffect(() => {
    let vigente = true;
    obtenerSeguimiento(id)
      .then(async (s) => {
        const p = CON_ORDEN.includes(s.estado) ? await obtenerPresupuesto(id) : null;
        if (vigente) setResultado({ clave, seguimiento: s, presupuesto: p, error: "", status: 0 });
      })
      .catch((e) => vigente && setResultado({ clave, seguimiento: null, presupuesto: null, error: e.message, status: e.status ?? 0 }));
    return () => {
      vigente = false;
    };
  }, [id, clave]);

  // "Revisar presupuesto" abre la pantalla con #presupuesto: se baja hasta él cuando termina de cargar
  const hayPresupuesto = !!presupuesto;
  useEffect(() => {
    if (hayPresupuesto && hash === "#presupuesto") document.getElementById("presupuesto")?.scrollIntoView({ block: "start" });
  }, [hayPresupuesto, hash]);

  // Mientras la atención sigue, se actualiza sola (sin interrumpir si hay una ventana abierta)
  const enCurso = !!seguimiento && EN_CURSO.includes(seguimiento.estado);
  const ocupado = !!decision || !!visor;
  useEffect(() => {
    if (!enCurso || ocupado) return undefined;
    const t = setInterval(() => !document.hidden && setRecarga((n) => n + 1), CADA_ACTUALIZACION);
    return () => clearInterval(t);
  }, [enCurso, ocupado]);

  const confirmarDecision = async () => {
    setEnviando(true);
    setErrorDecision("");
    try {
      const p = await decidirPresupuesto(presupuesto.id, decision);
      setResultado((r) => ({ ...r, presupuesto: p }));
      setDecision(null);
      setRecarga((n) => n + 1);
      avisar("ok", p.estado === "APROBADO" ? "Aprobaste el presupuesto. El mecánico continuará con la reparación." : "Rechazaste el presupuesto. El mecánico continuará solo con el servicio reservado.");
    } catch (err) {
      setErrorDecision(err.message);
      if (err.status === 409) setRecarga((n) => n + 1);
    } finally {
      setEnviando(false);
    }
  };

  const volver = (
    <Link to="/cliente/citas" className="seg-volver">
      ← Mis citas
    </Link>
  );

  if (resultado.error) {
    const noEsTuya = resultado.status === 403 || resultado.status === 404;
    return (
      <section className="gestion seg">
        {volver}
        <div className="tabla-caja">
          <div className="tabla-estado">
            <p className="tabla-estado-titulo">{noEsTuya ? "No encontramos esta cita" : "No se pudo cargar el seguimiento"}</p>
            <p>{noEsTuya ? "Puede que el enlace esté mal o que la cita no sea de tu cuenta." : resultado.error}</p>
            {!noEsTuya && (
              <button type="button" className="btn btn-sec" onClick={() => setRecarga((n) => n + 1)}>
                Reintentar
              </button>
            )}
          </div>
        </div>
      </section>
    );
  }

  if (!seguimiento) {
    return (
      <section className="gestion seg">
        {volver}
        <div className="tabla-caja">
          <div className="tabla-estado">
            <p>Cargando el seguimiento...</p>
          </div>
        </div>
      </section>
    );
  }

  const estado = ESTADOS_CITA_CLIENTE[seguimiento.estado] ?? ESTADOS_CITA_CLIENTE.PENDIENTE;
  const conAvance = CON_ORDEN.includes(seguimiento.estado);

  return (
    <section className="gestion seg">
      {volver}
      <div className="seg-cabecera">
        <div>
          <p className="seg-ticket">Cita {seguimiento.ticket}</p>
          <h2>{seguimiento.servicio}</h2>
          <p className="seg-sub">
            {fechaLarga(seguimiento.fecha)}, {hora12(seguimiento.inicio)} · {seguimiento.placa} · con {seguimiento.mecanico}
          </p>
        </div>
        <div className="seg-cab-der">
          <span className={`cit-estado est-${seguimiento.estado}`}>{estado.texto}</span>
          <button type="button" className="btn btn-sec" onClick={() => setRecarga((n) => n + 1)} disabled={cargando}>
            {cargando && <span className="btn-spinner" aria-hidden="true" />}
            Actualizar
          </button>
        </div>
      </div>

      {presupuesto?.estado === "PENDIENTE" && (
        <a href="#presupuesto" className="seg-alerta">
          <strong>El mecánico necesita tu aprobación.</strong> Revisa el presupuesto adicional de {formatoPrecio(presupuesto.total)} para que continúe.
        </a>
      )}

      <div className="seg-cuerpo">
        <div className="seg-caja">
          <div className="seg-caja-cab">
            <h3>Estado de la atención</h3>
          </div>
          <p className="seg-estado-texto">{estado.detalle}</p>
          {conAvance && (
            <div className="seg-avance">
              <div className="seg-avance-cab">
                <span>Avance del trabajo</span>
                <strong>{seguimiento.avance} %</strong>
              </div>
              <div className="seg-barra" role="progressbar" aria-valuenow={seguimiento.avance} aria-valuemin={0} aria-valuemax={100} aria-label="Avance del trabajo">
                <span style={{ width: `${seguimiento.avance}%` }} />
              </div>
            </div>
          )}
          <LineaDeTiempo seguimiento={seguimiento} presupuesto={presupuesto} />
          {enCurso && <p className="seg-auto">Esta pantalla se actualiza sola cada 30 segundos.</p>}
        </div>

        {presupuesto && (
          <Presupuesto
            presupuesto={presupuesto}
            alVerFoto={setVisor}
            alDecidir={(d) => {
              setDecision(d);
              setErrorDecision("");
            }}
          />
        )}
      </div>

      <Modal
        abierto={!!decision}
        alCerrar={() => !enviando && setDecision(null)}
        titulo={decision === "APROBADO" ? "Aprobar presupuesto" : "Rechazar presupuesto"}
        pequeno
      >
        {decision && presupuesto && (
          <div className="confirmar">
            <p>
              {decision === "APROBADO" ? (
                <>
                  ¿Apruebas el presupuesto adicional de <strong>{formatoPrecio(presupuesto.total)}</strong>? El mecánico hará los trabajos detallados y
                  ese monto se sumará al pago final.
                </>
              ) : (
                <>
                  ¿Rechazas el presupuesto adicional de <strong>{formatoPrecio(presupuesto.total)}</strong>? El mecánico continuará solo con el servicio que
                  reservaste. Tu respuesta no se puede cambiar después.
                </>
              )}
            </p>
            {errorDecision && (
              <p className="form-error-general" role="alert">
                {errorDecision}
              </p>
            )}
            <div className="form-acciones">
              <button type="button" className="btn btn-sec" onClick={() => setDecision(null)} disabled={enviando}>
                Volver
              </button>
              <button type="button" className={decision === "APROBADO" ? "btn btn-pri" : "btn btn-peligro"} onClick={confirmarDecision} disabled={enviando}>
                {enviando && <span className="btn-spinner" aria-hidden="true" />}
                {enviando ? "Enviando..." : decision === "APROBADO" ? "Sí, aprobar" : "Sí, rechazar"}
              </button>
            </div>
          </div>
        )}
      </Modal>

      <Modal abierto={!!visor} alCerrar={() => setVisor(null)} titulo="Foto del mecánico">
        {visor && (
          <figure className="seg-visor">
            <Foto foto={visor.foto} alt={visor.titulo} />
            <figcaption>{visor.titulo}</figcaption>
          </figure>
        )}
      </Modal>

      <Aviso aviso={aviso} alCerrar={cerrarAviso} />
    </section>
  );
}

export default Seguimiento;
