import { useEffect, useState } from "react";
import { Link, useLocation, useParams } from "react-router-dom";
import { obtenerSeguimiento } from "../../services/seguimientoService";
import { ordenDeCita } from "../../services/pagoClienteService";
import { obtenerDetalle } from "../../services/mantenimientoClienteService";
import { ESTADOS_CITA_CLIENTE } from "../../services/citasClienteService";
import { SEVERIDADES, minutosEntre } from "../../services/ordenesService";
import { ESTADOS_PRESUPUESTO } from "../../services/seguimientoService";
import { formatoPrecio } from "../../services/serviciosService";
import { fechaHora } from "../../utils/fechas";
import FotoCliente from "./FotoCliente";
import CalificarServicio from "./CalificarServicio";
import Modal from "../../components/admin/Modal";
import "../../components/admin/admin-ui.css";
import "./MisCitas.css";
import "./Seguimiento.css";
import "./Pagar.css";
import "./DetalleMantenimiento.css";

/* CLI-07 – Detalle del mantenimiento y calificación (APIs de T66.1 y T65.1).
   Lo que se le hizo al vehículo: diagnóstico, revisión de 5 puntos, problemas encontrados con fotos, trabajos
   realizados, repuestos usados y presupuesto adicional. Si el servicio terminó, el cliente califica al mecánico.
   #calificar baja directo a la calificación. */

const TERMINADA = ["FINALIZADA", "PAGADO"];

function duracion(desde, hasta) {
  const min = minutosEntre(desde, hasta);
  if (!Number.isFinite(min) || min < 0) return "";
  const h = Math.floor(min / 60);
  return h ? `${h} h ${min % 60} min` : `${min} min`;
}

function DetalleMantenimiento() {
  const { id } = useParams();
  const { hash } = useLocation();
  const [recarga, setRecarga] = useState(0);
  const [resultado, setResultado] = useState({ clave: null, cita: null, detalle: null, error: "", status: 0 });
  const [visor, setVisor] = useState(null);

  const clave = `${id}|${recarga}`;

  useEffect(() => {
    let vigente = true;
    (async () => {
      const cita = await obtenerSeguimiento(id);
      const idOrden = await ordenDeCita({ id: cita.id ?? Number(id), placa: cita.placa });
      return { cita, detalle: idOrden ? await obtenerDetalle(idOrden) : null };
    })()
      .then((r) => vigente && setResultado({ clave, ...r, error: "", status: 0 }))
      .catch((e) => vigente && setResultado({ clave, cita: null, detalle: null, error: e.message, status: e.status ?? 0 }));
    return () => {
      vigente = false;
    };
  }, [id, clave]);

  const { cita, detalle } = resultado;
  const listo = !!detalle;
  useEffect(() => {
    if (listo && hash === "#calificar") document.getElementById("calificar")?.scrollIntoView({ block: "center" });
  }, [listo, hash]);

  const volver = (
    <Link to="/cliente/citas" className="seg-volver">
      ← Mis citas
    </Link>
  );

  if (resultado.error || !cita) {
    const noEsTuya = resultado.status === 403 || resultado.status === 404;
    return (
      <section className="gestion det">
        {volver}
        <div className="tabla-caja">
          <div className="tabla-estado">
            {resultado.error ? (
              <>
                <p className="tabla-estado-titulo">{noEsTuya ? "No encontramos esta cita" : "No se pudo cargar el detalle"}</p>
                <p>{noEsTuya ? "Puede que el enlace esté mal o que la cita no sea de tu cuenta." : resultado.error}</p>
                {!noEsTuya && (
                  <button type="button" className="btn btn-sec" onClick={() => setRecarga((n) => n + 1)}>
                    Reintentar
                  </button>
                )}
              </>
            ) : (
              <p>Cargando el detalle del trabajo...</p>
            )}
          </div>
        </div>
      </section>
    );
  }

  if (!detalle) {
    return (
      <section className="gestion det">
        {volver}
        <div className="tabla-caja">
          <div className="tabla-estado">
            <p className="tabla-estado-titulo">Tu vehículo aún no ingresa al taller</p>
            <p>Cuando el mecánico reciba tu vehículo, aquí verás todo lo que se le revisa y se le hace.</p>
            <Link to={`/cliente/citas/${id}`} className="btn btn-sec">
              Ver seguimiento
            </Link>
          </div>
        </div>
      </section>
    );
  }

  const terminada = TERMINADA.includes(detalle.estado);
  const estado = ESTADOS_CITA_CLIENTE[detalle.estado] ?? ESTADOS_CITA_CLIENTE.EN_PROCESO;
  const observados = detalle.checklist.filter((i) => !i.ok).length;

  return (
    <section className="gestion det">
      {volver}
      <div className="seg-cabecera">
        <div>
          <p className="seg-ticket">Cita {detalle.ticket}</p>
          <h2>{detalle.servicio}</h2>
          <p className="seg-sub">
            {detalle.placa}
            {detalle.vehiculo && ` · ${detalle.vehiculo}`} · con {detalle.mecanico}
          </p>
        </div>
        <div className="seg-cab-der">
          <span className={`cit-estado est-${detalle.estado}`}>{estado.texto}</span>
          <Link to={`/cliente/citas/${id}`} className="btn btn-sec">
            Seguimiento
          </Link>
          {detalle.estado === "FINALIZADA" && (
            <Link to={`/cliente/citas/${id}/pago`} className="btn btn-pri">
              Pagar servicio
            </Link>
          )}
          {detalle.estado === "PAGADO" && (
            <Link to={`/cliente/citas/${id}/pago`} className="btn btn-sec">
              Comprobante
            </Link>
          )}
        </div>
      </div>

      <dl className="det-fechas">
        <div>
          <dt>Recibido</dt>
          <dd>{fechaHora(detalle.fechaInicio)}</dd>
        </div>
        <div>
          <dt>Terminado</dt>
          <dd>{detalle.fechaFin ? fechaHora(detalle.fechaFin) : "En curso"}</dd>
        </div>
        {detalle.fechaFin && (
          <div>
            <dt>Tiempo en el taller</dt>
            <dd>{duracion(detalle.fechaInicio, detalle.fechaFin)}</dd>
          </div>
        )}
        <div>
          <dt>Trabajos</dt>
          <dd>
            {detalle.tareas.completadas} de {detalle.tareas.total} hechos
          </dd>
        </div>
      </dl>

      <div className="seg-cuerpo det-cuerpo">
        <div className="det-principal">
          <section className="seg-caja">
            <h3>Diagnóstico del mecánico</h3>
            <p className="det-texto">{detalle.diagnostico || "El mecánico aún no registra el diagnóstico."}</p>
          </section>

          {detalle.checklist.length > 0 && (
            <section className="seg-caja">
              <div className="seg-caja-cab">
                <h3>Revisión general</h3>
                <span className="seg-nota det-resumen">{observados ? `${observados} con observaciones` : "Todo en orden"}</span>
              </div>
              <ul className="det-check">
                {detalle.checklist.map((i) => (
                  <li key={i.elemento} className={i.ok ? "ok" : "obs"}>
                    <span className="det-check-icono" aria-hidden="true">
                      {i.ok ? "✓" : "!"}
                    </span>
                    <div>
                      <strong>{i.elemento}</strong>
                      <span>{i.ok ? "Sin problemas" : i.observacion || "Con observaciones"}</span>
                    </div>
                  </li>
                ))}
              </ul>
            </section>
          )}

          {detalle.deficiencias.length > 0 && (
            <section className="seg-caja">
              <h3>Problemas encontrados</h3>
              <ul className="seg-defs det-defs">
                {detalle.deficiencias.map((d) => (
                  <li key={d.id} className={`seg-def sev-${d.severidad}`}>
                    <div className="seg-def-cab">
                      <span className="seg-sev">{SEVERIDADES[d.severidad].texto}</span>
                      <strong>{d.categoria}</strong>
                    </div>
                    <p>{d.descripcion}</p>
                    {d.fotos.length > 0 && (
                      <div className="seg-fotos">
                        {d.fotos.map((f, i) => (
                          <button key={f.clave} type="button" className="seg-foto" onClick={() => setVisor({ foto: f, titulo: `${d.categoria}: ${d.descripcion}` })}>
                            <FotoCliente foto={f} alt={`Foto ${i + 1} de ${d.categoria}`} />
                          </button>
                        ))}
                      </div>
                    )}
                  </li>
                ))}
              </ul>
            </section>
          )}

          <section className="seg-caja">
            <div className="seg-caja-cab">
              <h3>Trabajos realizados</h3>
              <span className="seg-nota det-resumen">{detalle.tareas.avance} %</span>
            </div>
            {detalle.tareas.lista.length ? (
              <ul className="det-tareas">
                {detalle.tareas.lista.map((t, i) => (
                  <li key={i} className={t.completada ? "hecha" : ""}>
                    <span aria-hidden="true">{t.completada ? "✓" : ""}</span>
                    {t.descripcion}
                    <span className="sr-only">{t.completada ? " (hecho)" : " (pendiente)"}</span>
                  </li>
                ))}
              </ul>
            ) : (
              <p className="det-texto">El mecánico aún no registra los trabajos.</p>
            )}
          </section>
        </div>

        <div className="det-lateral">
          {terminada && <CalificarServicio idOrden={detalle.idOrden} idCita={detalle.idCita ?? Number(id)} mecanico={detalle.mecanico} />}

          <section className="seg-caja">
            <h3>Costos</h3>
            <ul className="pag-lineas det-costos">
              <li>
                <span>{detalle.servicio}</span>
                <strong>{formatoPrecio(detalle.precioServicio)}</strong>
              </li>
              {detalle.presupuesto && (
                <li>
                  <span>
                    Presupuesto adicional
                    <span className={`seg-pres-estado pres-${detalle.presupuesto.estado}`}>{ESTADOS_PRESUPUESTO[detalle.presupuesto.estado] ?? detalle.presupuesto.estado}</span>
                  </span>
                  <strong className={detalle.presupuesto.estado === "RECHAZADO" ? "det-tachado" : detalle.presupuesto.estado === "PENDIENTE" ? "det-pendiente" : ""}>{formatoPrecio(detalle.presupuesto.total)}</strong>
                </li>
              )}
            </ul>
            {detalle.repuestos.length > 0 && (
              <>
                <h4>Repuestos usados</h4>
                <ul className="det-repuestos">
                  {detalle.repuestos.map((r, i) => (
                    <li key={i}>
                      <span>{r.codigo}</span>
                      <span>× {r.cantidad}</span>
                    </li>
                  ))}
                </ul>
              </>
            )}
            <p className="seg-nota">El total final y el pago están en {detalle.estado === "PAGADO" ? "tu comprobante" : "la pantalla de pago"}.</p>
          </section>
        </div>
      </div>

      <Modal abierto={!!visor} alCerrar={() => setVisor(null)} titulo="Foto del mecánico">
        {visor && (
          <figure className="seg-visor">
            <FotoCliente foto={visor.foto} alt={visor.titulo} />
            <figcaption>{visor.titulo}</figcaption>
          </figure>
        )}
      </Modal>
    </section>
  );
}

export default DetalleMantenimiento;
