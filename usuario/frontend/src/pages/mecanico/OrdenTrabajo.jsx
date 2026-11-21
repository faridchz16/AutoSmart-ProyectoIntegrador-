import { useEffect, useRef, useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import { obtenerCita, enlaceWhatsApp, formatoKm, horaDe } from "../../services/citasService";
import {
  obtenerOrdenDeCita,
  obtenerChecklist,
  listarDeficiencias,
  obtenerDiagnostico,
  listarTareas,
  TAREAS_VACIAS,
  obtenerPresupuesto,
  listarSolicitudes,
  resumenSolicitudes,
  listarConsumos,
  CONSUMOS_VACIOS,
  formatoSoles,
  ESTADOS_PRESUPUESTO,
  cargarFoto,
  textoCategoria,
  minutosEntre,
  fechaDe,
  ELEMENTOS_CHECKLIST,
  ESTADOS_ITEM,
  SEVERIDADES,
} from "../../services/ordenesService";
import { ESTADOS_CITA } from "../../services/agendaService";
import { hoyLima, horaLima, fechaCorta, fechaLarga, aFecha, isoDe } from "../../services/bloqueosService";
import { formatoHoras } from "../../services/horariosService";
import DiagnosticoOrden from "./DiagnosticoOrden";
import TareasOrden from "./TareasOrden";
import PresupuestoOrden from "./PresupuestoOrden";
import SolicitudesRepuestos from "./SolicitudesRepuestos";
import RepuestosUsados from "./RepuestosUsados";
import CompletarOrden from "./CompletarOrden";
import "../../components/admin/admin-ui.css";
import "./Agenda.css";
import "./OrdenTrabajo.css";

/* TEC3.2 – Vista web de la orden de trabajo: datos de la atención y la inspección de recepción
   que el mecánico registra desde la app (checklist, deficiencias y fotos).
   Se abre desde el detalle de una cita en proceso, en espera de aprobación o finalizada.
   T45.2: área de diagnóstico técnico (se registra y edita aquí mientras la orden está EN_PROCESO).
   T49.2: tareas con casilla y porcentaje de avance.
   T46.2: presupuesto con mano de obra, repuestos y total (se envía al cliente para su aprobación).
   T50.2: solicitud de repuestos al almacén.
   T51.2: repuestos usados en la orden y su costo.
   T52.2: completar el mantenimiento con validaciones previas (la orden queda cerrada).
   Se actualiza sola cada minuto mientras la orden está abierta y al volver a la pestaña. */

const CADA_RELOJ = 30 * 1000;
const CADA_ACTUALIZACION = 60 * 1000;
const marcaAhora = () => Date.now();
const plural = (n, uno, varios) => `${n} ${n === 1 ? uno : varios}`;

// "hoy, 12:04", "ayer, 10:02" o "lun 2 nov, 12:04"
const momento = (fechaHora) => {
  if (!fechaHora) return "—";
  const fecha = fechaDe(fechaHora);
  const hoy = hoyLima();
  const ayer = aFecha(hoy);
  ayer.setDate(ayer.getDate() - 1);
  const dia = fecha === hoy ? "hoy" : fecha === isoDe(ayer.getFullYear(), ayer.getMonth(), ayer.getDate()) ? "ayer" : fechaCorta(fecha);
  return `${dia}, ${horaDe(fechaHora)}`;
};

const Icono = ({ children, tam = 18 }) => (
  <svg viewBox="0 0 24 24" width={tam} height={tam} fill="none" stroke="currentColor" strokeWidth="1.9" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
    {children}
  </svg>
);

const ICONOS_CHECKLIST = {
  LUCES: (
    <Icono>
      <path d="M9 18h6M10 21h4" />
      <path d="M12 3a6 6 0 0 0-3.5 10.9c.6.5 1 1.2 1 2.1h5c0-.9.4-1.6 1-2.1A6 6 0 0 0 12 3z" />
    </Icono>
  ),
  FRENOS: (
    <Icono>
      <circle cx="12" cy="12" r="8.5" />
      <circle cx="12" cy="12" r="3" />
      <path d="M5.5 6.5a9 9 0 0 0 0 11" />
    </Icono>
  ),
  NEUMATICOS: (
    <Icono>
      <circle cx="12" cy="12" r="8.5" />
      <circle cx="12" cy="12" r="4" />
      <path d="M12 3.5V8M12 16v4.5M3.5 12H8M16 12h4.5" />
    </Icono>
  ),
  FLUIDOS: (
    <Icono>
      <path d="M12 3.5s6 6.4 6 10.5a6 6 0 0 1-12 0c0-4.1 6-10.5 6-10.5z" />
    </Icono>
  ),
  CARROCERIA: (
    <Icono>
      <path d="M5 16V11l2-5h10l2 5v5" />
      <path d="M3 16h18v3H3z" />
      <circle cx="7.5" cy="13.5" r="0.8" />
      <circle cx="16.5" cy="13.5" r="0.8" />
    </Icono>
  ),
};

const IconoActualizar = ({ girando }) => (
  <svg className={girando ? "ot-girando" : undefined} viewBox="0 0 24 24" width="17" height="17" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
    <path d="M20 11a8 8 0 1 0-2.3 5.7" />
    <path d="M20 4v7h-7" />
  </svg>
);

const IconoCelular = () => (
  <Icono tam={22}>
    <rect x="7" y="2.5" width="10" height="19" rx="2.2" />
    <path d="M11 18.5h2" />
  </Icono>
);

/* ---------- Foto protegida: se descarga con el token y se muestra al terminar ---------- */
function Foto({ foto, alt = "", className = "" }) {
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

  const lista = estado.clave === foto.clave;
  if (lista && estado.error) {
    return (
      <span className={`ot-foto-img ot-foto-falla ${className}`} role="img" aria-label="No se pudo cargar la foto">
        <Icono>
          <rect x="3.5" y="5" width="17" height="14" rx="2" />
          <path d="M4 5l16 14" />
        </Icono>
      </span>
    );
  }
  if (!lista) return <span className={`ot-foto-img esq ${className}`} aria-hidden="true" />;
  return <img className={`ot-foto-img ${className}`} src={estado.url} alt={alt} />;
}

/* ---------- Visor de fotos a pantalla completa (flechas del teclado y Esc) ---------- */
function Visor({ deficiencia, indice, alCambiar, alCerrar }) {
  const ref = useRef(null);
  const total = deficiencia.fotos.length;
  const mover = (paso) => alCambiar((indice + paso + total) % total);
  const sev = SEVERIDADES[deficiencia.severidad];

  useEffect(() => {
    const dialogo = ref.current;
    if (dialogo && !dialogo.open) dialogo.showModal();
  }, []);

  return (
    <dialog
      ref={ref}
      className="ot-visor"
      aria-label={`Fotos de la deficiencia: ${textoCategoria(deficiencia.categoria)}`}
      onClose={alCerrar}
      onClick={(e) => e.target === ref.current && alCerrar()}
      onKeyDown={(e) => {
        if (total > 1 && e.key === "ArrowRight") mover(1);
        if (total > 1 && e.key === "ArrowLeft") mover(-1);
      }}
    >
      <figure className="ot-visor-figura">
        <Foto
          key={deficiencia.fotos[indice].clave}
          foto={deficiencia.fotos[indice]}
          alt={`Foto ${indice + 1} de ${total}: ${deficiencia.descripcion}`}
          className="ot-visor-img"
        />
        <figcaption>
          <div className="ot-visor-cab">
            <span className={`ot-sev sev-${deficiencia.severidad}`}>{sev.texto}</span>
            <strong>{textoCategoria(deficiencia.categoria)}</strong>
            {total > 1 && <span className="ot-visor-num">Foto {indice + 1} de {total}</span>}
          </div>
          <p>{deficiencia.descripcion}</p>
        </figcaption>
      </figure>
      {total > 1 && (
        <>
          <button type="button" className="ot-visor-flecha ant" onClick={() => mover(-1)} aria-label="Foto anterior">
            <Icono tam={22}><path d="M15 5l-7 7 7 7" /></Icono>
          </button>
          <button type="button" className="ot-visor-flecha sig" onClick={() => mover(1)} aria-label="Foto siguiente">
            <Icono tam={22}><path d="M9 5l7 7-7 7" /></Icono>
          </button>
        </>
      )}
      <button type="button" className="ot-visor-cerrar" onClick={alCerrar} aria-label="Cerrar fotos" autoFocus>
        <Icono tam={20}><path d="M6 6l12 12M18 6 6 18" /></Icono>
      </button>
    </dialog>
  );
}

/* ---------- Aviso cuando la app aún no envía datos ---------- */
function Pendiente({ titulo, texto }) {
  return (
    <div className="ot-pendiente">
      <span className="ot-pendiente-icono"><IconoCelular /></span>
      <div>
        <p className="ot-pendiente-titulo">{titulo}</p>
        {texto && <p>{texto}</p>}
      </div>
    </div>
  );
}

/* ---------- Checklist de recepción (T42.1 / T42.2) ---------- */
function Checklist({ checklist, abierta }) {
  if (!checklist) {
    return (
      <Pendiente
        titulo="Aún no se registra la inspección de recepción."
        texto={
          abierta
            ? "Regístrala desde la app AutoSmart en tu celular, junto al vehículo. Luego pulsa Actualizar."
            : "Esta orden se cerró sin checklist de recepción."
        }
      />
    );
  }

  return (
    <>
      <ul className="ot-checklist">
        {ELEMENTOS_CHECKLIST.map((el) => {
          const item = checklist.items.find((i) => i.elemento === el.clave);
          const estado = item?.estado ?? "SIN_REVISAR";
          return (
            <li key={el.clave} className={`ot-item est-${estado}`}>
              <span className="ot-item-icono">{ICONOS_CHECKLIST[el.clave]}</span>
              <div className="ot-item-texto">
                <strong>{el.texto}</strong>
                {item?.observacion && <p>{item.observacion}</p>}
              </div>
              <span className="ot-item-estado">{ESTADOS_ITEM[estado]}</span>
            </li>
          );
        })}
      </ul>
      {checklist.observacionGeneral && (
        <div className="ot-obs">
          <span>Observación general</span>
          <p>{checklist.observacionGeneral}</p>
        </div>
      )}
      <p className="ot-registro">Registrado desde la app {momento(checklist.fechaRegistro)}</p>
    </>
  );
}

/* ---------- Deficiencias con fotos (T43.1 y T44.1 / T43.2 y T44.2) ---------- */
function Deficiencias({ deficiencias, abierta, alVerFoto }) {
  if (!deficiencias.length) {
    return (
      <Pendiente
        titulo="No hay deficiencias registradas."
        texto={abierta ? "Si encuentras alguna durante la inspección, regístrala desde la app con sus fotos." : ""}
      />
    );
  }

  return (
    <ol className="ot-deficiencias">
      {deficiencias.map((d) => {
        const categoria = textoCategoria(d.categoria);
        return (
          <li key={d.id} className={`ot-def sev-${d.severidad}`}>
            <div className="ot-def-cab">
              <span className={`ot-sev sev-${d.severidad}`}>{SEVERIDADES[d.severidad].texto}</span>
              <strong>{categoria}</strong>
              {d.fechaRegistro && <time className="ot-def-hora">{momento(d.fechaRegistro)}</time>}
            </div>
            <p className="ot-def-texto">{d.descripcion}</p>
            {d.fotos.length > 0 ? (
              <ul className="ot-fotos" aria-label={`Fotos de evidencia: ${categoria}`}>
                {d.fotos.map((f, i) => (
                  <li key={f.clave}>
                    <button
                      type="button"
                      className="ot-foto"
                      onClick={() => alVerFoto(d, i)}
                      aria-label={`Ver foto ${i + 1} de ${d.fotos.length}: ${categoria}`}
                    >
                      <Foto foto={f} />
                    </button>
                  </li>
                ))}
              </ul>
            ) : (
              <p className="ot-sin-fotos">Sin fotos de evidencia</p>
            )}
          </li>
        );
      })}
    </ol>
  );
}

/* ---------- Pantalla ---------- */
function OrdenTrabajo() {
  const { idCita } = useParams();
  const navigate = useNavigate();
  const [recarga, setRecarga] = useState(0);
  const [resultado, setResultado] = useState({ clave: null, datosDe: null, error: "" });
  const [ahora, setAhora] = useState(marcaAhora);
  const [visor, setVisor] = useState(null); // { deficiencia, indice }
  const [completada, setCompletada] = useState(null); // { fechaFin, costoRepuestos } al completar el mantenimiento

  const clave = `${idCita}|${recarga}`;
  const cargando = resultado.clave !== clave;
  const datos = resultado.datosDe === idCita ? resultado : null;
  const abierta = !!datos?.orden && !datos.orden.fechaFin && datos.orden.estado !== "FINALIZADA";

  useEffect(() => {
    let vigente = true;
    Promise.all([obtenerCita(idCita), obtenerOrdenDeCita(idCita)])
      .then(async ([cita, orden]) => {
        if (!orden) return { cita, orden: null, checklist: null, deficiencias: [], diagnostico: null, tareas: TAREAS_VACIAS, presupuesto: null, solicitudes: [], consumos: CONSUMOS_VACIOS };
        const [checklist, deficiencias, diagnostico, tareas, presupuesto, solicitudes, consumos] = await Promise.all([
          obtenerChecklist(orden.idOrden),
          listarDeficiencias(orden.idOrden),
          obtenerDiagnostico(orden.idOrden),
          listarTareas(orden.idOrden),
          obtenerPresupuesto(orden.idOrden),
          listarSolicitudes(orden.idOrden),
          listarConsumos(orden.idOrden),
        ]);
        return { cita, orden, checklist, deficiencias, diagnostico, tareas, presupuesto, solicitudes, consumos };
      })
      .then((d) => {
        if (vigente) setResultado({ clave, datosDe: idCita, ...d, error: "", fallo: "", actualizado: horaLima() });
      })
      .catch((e) => {
        if (!vigente) return;
        // Si ya se mostraba la orden, se mantiene y solo se avisa del fallo
        setResultado((r) =>
          r.datosDe === idCita ? { ...r, clave, fallo: e.message } : { clave, datosDe: null, error: e.message }
        );
      });
    return () => {
      vigente = false;
    };
  }, [clave, idCita]);

  // Reloj para el tiempo en atención
  useEffect(() => {
    const id = setInterval(() => setAhora(marcaAhora()), CADA_RELOJ);
    return () => clearInterval(id);
  }, []);

  // Mientras la orden está abierta, se actualiza cada minuto y al volver a la pestaña
  // (el mecánico registra la inspección desde el celular)
  useEffect(() => {
    if (!abierta) return;
    const actualizar = () => setRecarga((n) => n + 1);
    const id = setInterval(actualizar, CADA_ACTUALIZACION);
    const alVolver = () => {
      if (document.visibilityState === "visible") actualizar();
    };
    document.addEventListener("visibilitychange", alVolver);
    return () => {
      clearInterval(id);
      document.removeEventListener("visibilitychange", alVolver);
    };
  }, [abierta]);

  const volver = () => (window.history.state?.idx > 0 ? navigate(-1) : navigate("/mecanico/citas-hoy"));

  const botonVolver = (
    <button type="button" className="ot-volver" onClick={volver}>
      <Icono tam={16}><path d="M15 5l-7 7 7 7" /></Icono>
      Volver
    </button>
  );

  /* ----- Error al cargar ----- */
  if (!datos && resultado.error && !cargando) {
    return (
      <section className="gestion ot">
        {botonVolver}
        <div className="tabla-caja">
          <div className="tabla-estado" role="alert">
            <p className="tabla-estado-titulo">No se pudo cargar la orden de trabajo</p>
            <p>{resultado.error}</p>
            <button type="button" className="btn btn-sec" onClick={() => setRecarga((n) => n + 1)}>
              Reintentar
            </button>
          </div>
        </div>
      </section>
    );
  }

  /* ----- Primera carga ----- */
  if (!datos) {
    return (
      <section className="gestion ot" aria-busy="true">
        {botonVolver}
        <div className="gestion-cabecera">
          <div>
            <h2>Orden de trabajo</h2>
            <span className="esq esq-largo" />
          </div>
        </div>
        <div className="ot-ficha">
          {[0, 1, 2, 3].map((i) => (
            <div key={i} className="ot-dato"><span className="esq" /><span className="esq esq-largo" /></div>
          ))}
        </div>
      </section>
    );
  }

  const { cita, orden, checklist, deficiencias, diagnostico, tareas, presupuesto, solicitudes, consumos, fallo, actualizado } = datos;
  const recargar = () => setRecarga((n) => n + 1);
  const cambiar = (campo) => (valor) => setResultado((r) => ({ ...r, [campo]: valor }));
  const estado = orden?.estado ?? cita.estado;

  /* ----- La cita aún no tiene orden (o no se encontró) ----- */
  if (!orden) {
    const iniciada = ["EN_PROCESO", "ESPERA_APROBACION", "FINALIZADA"].includes(cita.estado);
    return (
      <section className="gestion ot">
        {botonVolver}
        <div className="tabla-caja">
          <div className="tabla-estado">
            <p className="tabla-estado-titulo">
              {iniciada ? "No se encontró la orden de trabajo de esta cita" : "Esta cita aún no tiene orden de trabajo"}
            </p>
            <p>
              {cita.servicio} · {cita.cliente} · {cita.placa}
            </p>
            <p>
              {iniciada
                ? `La cita figura como «${ESTADOS_CITA[cita.estado]}», pero el servidor no devolvió su orden. Inténtalo más tarde.`
                : "La orden se crea cuando pulsas «Iniciar atención» en el detalle de una cita confirmada."}
            </p>
            <Link to="/mecanico/citas-hoy" className="btn btn-sec">Ir a citas de hoy</Link>
          </div>
        </div>
      </section>
    );
  }

  const vehiculo = [cita.marca, cita.modelo, cita.anio].filter(Boolean).join(" ");
  const whatsapp = enlaceWhatsApp(cita.telefono);
  const enAtencion = minutosEntre(orden.fechaInicio, orden.fechaFin || ahora);
  const observados = checklist ? checklist.items.filter((i) => i.estado === "OBSERVADO").length : 0;
  const correctos = checklist ? checklist.items.filter((i) => i.estado === "OK").length : 0;
  const totalFotos = deficiencias.reduce((n, d) => n + d.fotos.length, 0);
  const porSeveridad = Object.keys(SEVERIDADES)
    .map((s) => ({ s, n: deficiencias.filter((d) => d.severidad === s).length }))
    .filter((x) => x.n > 0);

  return (
    <section className="gestion ot">
      {botonVolver}

      <div className="gestion-cabecera">
        <div>
          <h2>Orden de trabajo N.º {orden.idOrden}</h2>
          <p>
            {cita.ticket && <span className="ot-ticket">Ticket {cita.ticket}</span>}
            {cita.servicio}
          </p>
        </div>
        <div className="ot-actualizar">
          {actualizado && <span>Actualizado a las {actualizado}</span>}
          <button type="button" className="btn btn-sec" onClick={() => setRecarga((n) => n + 1)} disabled={cargando}>
            <IconoActualizar girando={cargando} />
            Actualizar
          </button>
        </div>
      </div>

      {completada && (
        <div className="ot-completada" role="status">
          <strong>✓ Mantenimiento completado {momento(completada.fechaFin)}</strong>
          <span>
            La orden quedó cerrada{completada.costoRepuestos > 0 ? ` · repuestos ${formatoSoles(completada.costoRepuestos)}` : ""}. El
            cliente ya lo ve como finalizado en la app.
          </span>
          <Link to="/mecanico/citas-hoy" className="btn btn-sec">
            Ir a citas de hoy
          </Link>
        </div>
      )}

      {fallo && (
        <p className="aviso-fijo" role="alert">
          No se pudo actualizar: {fallo} Se muestran los datos cargados a las {actualizado}.
        </p>
      )}

      {/* ---------- Ficha de la atención ---------- */}
      <div className={`ot-ficha est-${estado}`}>
        <div className="ot-dato ot-dato-estado">
          <span className="ot-etq">Estado</span>
          <span className={`insignia-estado est-${estado}`}>{ESTADOS_CITA[estado] ?? estado}</span>
          {estado === "ESPERA_APROBACION" && <span className="ot-sub">El cliente revisa el presupuesto en la app.</span>}
          {!abierta && <span className="ot-sub">Orden cerrada: solo lectura.</span>}
        </div>

        <div className="ot-dato">
          <span className="ot-etq">Vehículo</span>
          <span className="ag-placa ot-placa">{cita.placa}</span>
          <span className="ot-sub">
            {vehiculo || "—"} · {formatoKm(cita.kilometraje)}
          </span>
        </div>

        <div className="ot-dato">
          <span className="ot-etq">Cliente</span>
          <strong>{cita.cliente}</strong>
          {cita.telefono ? (
            <span className="ot-contacto">
              <a href={`tel:${cita.telefono.replace(/\s/g, "")}`}>{cita.telefono}</a>
              {whatsapp && (
                <a href={whatsapp} target="_blank" rel="noreferrer">WhatsApp</a>
              )}
            </span>
          ) : (
            <span className="ot-sub">Sin teléfono registrado</span>
          )}
        </div>

        <div className="ot-dato">
          <span className="ot-etq">Atención</span>
          <strong>Inicio {momento(orden.fechaInicio)}</strong>
          {orden.fechaFin ? (
            <span className="ot-sub">
              Fin {momento(orden.fechaFin)}
              {enAtencion !== null && enAtencion >= 0 ? ` · ${formatoHoras(enAtencion)}` : ""}
            </span>
          ) : (
            enAtencion !== null &&
            enAtencion >= 0 && (
              <span className="ot-sub ot-en-vivo">
                <span className="ot-pulso" aria-hidden="true" />
                En atención hace {formatoHoras(enAtencion)}
              </span>
            )
          )}
          <span className="ot-sub">
            Programada: {fechaLarga(cita.fecha)}, {cita.horaInicio} – {cita.horaFin}
          </span>
        </div>
      </div>

      {/* ---------- Resumen de la inspección ---------- */}
      <nav className="ot-resumen" aria-label="Resumen de la inspección">
        <a href="#ot-checklist" className={checklist ? "ot-res hecho" : "ot-res"}>
          <span className="ot-res-num">{checklist ? `${correctos}/${ELEMENTOS_CHECKLIST.length}` : "—"}</span>
          <span>
            <strong>Checklist</strong>
            {checklist ? (observados ? plural(observados, "punto observado", "puntos observados") : "Todo OK") : "Pendiente"}
          </span>
        </a>
        <a href="#ot-deficiencias" className={deficiencias.some((d) => d.severidad === "CRITICA") ? "ot-res alerta" : "ot-res"}>
          <span className="ot-res-num">{deficiencias.length}</span>
          <span>
            <strong>{deficiencias.length === 1 ? "Deficiencia" : "Deficiencias"}</strong>
            {porSeveridad.length
              ? porSeveridad.map(({ s, n }) => `${n} ${n === 1 ? SEVERIDADES[s].texto.toLowerCase() : SEVERIDADES[s].plural}`).join(" · ")
              : "Ninguna registrada"}
          </span>
        </a>
        <a href="#ot-diagnostico" className={diagnostico ? "ot-res hecho" : "ot-res"}>
          <span className="ot-res-num" aria-hidden="true">{diagnostico ? "✓" : "—"}</span>
          <span>
            <strong>Diagnóstico</strong>
            {diagnostico ? "Registrado" : estado === "EN_PROCESO" ? "Por registrar" : "Sin registrar"}
          </span>
        </a>
        <a href="#ot-tareas" className={tareas.total && tareas.porcentaje === 100 ? "ot-res hecho" : "ot-res"}>
          <span className="ot-res-num">{tareas.total ? `${tareas.porcentaje}%` : "—"}</span>
          <span>
            <strong>Avance</strong>
            {tareas.total ? `${tareas.completadas} de ${plural(tareas.total, "tarea", "tareas")}` : "Sin tareas"}
          </span>
        </a>
        <a
          href="#ot-presupuesto"
          className={
            presupuesto?.estado === "APROBADO" ? "ot-res hecho" : presupuesto?.estado === "RECHAZADO" ? "ot-res alerta" : "ot-res"
          }
        >
          <span className="ot-res-num ot-res-monto">{presupuesto ? formatoSoles(presupuesto.total) : "—"}</span>
          <span>
            <strong>Presupuesto</strong>
            {presupuesto ? ESTADOS_PRESUPUESTO[presupuesto.estado] : "Sin armar"}
          </span>
        </a>
        <a href="#ot-repuestos" className={solicitudes.some((s) => s.estado === "PENDIENTE") ? "ot-res pendiente" : "ot-res"}>
          <span className="ot-res-num ot-res-monto">{formatoSoles(consumos.costoRepuestos)}</span>
          <span>
            <strong>Repuestos</strong>
            {consumos.consumos.length
              ? plural(consumos.consumos.length, "usado", "usados")
              : solicitudes.length
                ? resumenSolicitudes(solicitudes)
                : "Ninguno usado"}
            {consumos.consumos.length > 0 && solicitudes.some((s) => s.estado === "PENDIENTE") ? " · hay solicitudes pendientes" : ""}
          </span>
        </a>
      </nav>

      {/* ---------- Inspección registrada desde la app ---------- */}
      <div className="ot-cuerpo">
        <section className="ot-panel" id="ot-checklist" aria-labelledby="ot-checklist-titulo">
          <header className="ot-panel-cab">
            <h3 id="ot-checklist-titulo">Checklist de recepción</h3>
            {checklist && (
              <span className="ot-panel-extra">
                {correctos} OK · {observados} {observados === 1 ? "observado" : "observados"}
              </span>
            )}
          </header>
          <Checklist checklist={checklist} abierta={abierta} />
        </section>

        <section className="ot-panel" id="ot-deficiencias" aria-labelledby="ot-def-titulo">
          <header className="ot-panel-cab">
            <h3 id="ot-def-titulo">Deficiencias detectadas</h3>
            {deficiencias.length > 0 && (
              <span className="ot-panel-extra">
                {plural(deficiencias.length, "registrada", "registradas")} · {plural(totalFotos, "foto", "fotos")}
              </span>
            )}
          </header>
          <Deficiencias
            deficiencias={deficiencias}
            abierta={abierta}
            alVerFoto={(deficiencia, indice) => setVisor({ deficiencia, indice })}
          />
        </section>
      </div>

      {/* ---------- Trabajo del mecánico: diagnóstico (T45.2) y tareas (T49.2) ---------- */}
      <div className="ot-cuerpo ot-trabajo">
        <section className="ot-panel" id="ot-diagnostico" aria-labelledby="ot-diag-titulo">
          <header className="ot-panel-cab">
            <h3 id="ot-diag-titulo">Diagnóstico técnico</h3>
            <span className="ot-panel-extra">Lo verá el cliente en la app</span>
          </header>
          <DiagnosticoOrden
            idOrden={orden.idOrden}
            diagnostico={diagnostico}
            editable={estado === "EN_PROCESO"}
            estado={estado}
            deficiencias={deficiencias}
            momento={momento}
            alGuardar={cambiar("diagnostico")}
            alConflicto={recargar}
          />
        </section>

        <section className="ot-panel" id="ot-tareas" aria-labelledby="ot-tareas-titulo">
          <header className="ot-panel-cab">
            <h3 id="ot-tareas-titulo">Tareas</h3>
            {tareas.total > 0 && <span className="ot-panel-extra">{tareas.completadas}/{tareas.total} completadas</span>}
          </header>
          <TareasOrden
            idOrden={orden.idOrden}
            resumen={tareas}
            editable={estado === "EN_PROCESO"}
            estado={estado}
            momento={momento}
            alCambiar={cambiar("tareas")}
            alConflicto={recargar}
          />
        </section>
      </div>

      {/* ---------- Presupuesto (T46.2) ---------- */}
      <section className="ot-panel ot-seccion" id="ot-presupuesto" aria-labelledby="ot-pres-titulo">
        <header className="ot-panel-cab">
          <h3 id="ot-pres-titulo">Presupuesto</h3>
          <span className="ot-panel-extra">El cliente lo aprueba desde la app</span>
        </header>
        <PresupuestoOrden
          idOrden={orden.idOrden}
          presupuesto={presupuesto}
          diagnostico={diagnostico}
          cita={cita}
          editable={estado === "EN_PROCESO"}
          estado={estado}
          momento={momento}
          alEnviado={(p) => {
            cambiar("presupuesto")(p);
            recargar();
          }}
          alConflicto={recargar}
        />
      </section>

      {/* ---------- Repuestos: solicitudes (T50.2) y usados (T51.2) ---------- */}
      <section className="ot-panel ot-seccion" id="ot-repuestos" aria-labelledby="ot-rep-titulo">
        <header className="ot-panel-cab">
          <h3 id="ot-rep-titulo">Repuestos</h3>
          {solicitudes.length > 0 && <span className="ot-panel-extra">{resumenSolicitudes(solicitudes)}</span>}
        </header>
        <div className="ot-rep-grid">
          <div className="ot-rep-col">
            <h4 className="ot-sub-titulo">Solicitudes al almacén</h4>
            <SolicitudesRepuestos
              idOrden={orden.idOrden}
              solicitudes={solicitudes}
              editable={estado === "EN_PROCESO"}
              estado={estado}
              momento={momento}
              alCambiar={cambiar("solicitudes")}
              alConflicto={recargar}
            />
          </div>
          <div className="ot-rep-col">
            <h4 className="ot-sub-titulo">Repuestos usados en la orden</h4>
            <RepuestosUsados
              idOrden={orden.idOrden}
              resumen={consumos}
              editable={estado === "EN_PROCESO"}
              estado={estado}
              momento={momento}
              alCambiar={cambiar("consumos")}
              alConflicto={recargar}
            />
          </div>
        </div>
      </section>

      {/* ---------- Completar el mantenimiento (T52.2) ---------- */}
      {abierta && (
        <section className="ot-panel ot-seccion ot-fin-panel" id="ot-completar" aria-labelledby="ot-fin-titulo-panel">
          <header className="ot-panel-cab">
            <h3 id="ot-fin-titulo-panel">Completar mantenimiento</h3>
            <span className="ot-panel-extra">Cierra la orden y registra la hora de fin</span>
          </header>
          <CompletarOrden
            idOrden={orden.idOrden}
            estado={estado}
            diagnostico={diagnostico}
            tareas={tareas}
            solicitudes={solicitudes}
            presupuesto={presupuesto}
            consumos={consumos}
            minutos={enAtencion}
            alCompletar={(r) => {
              setCompletada(r);
              setResultado((d) => ({
                ...d,
                orden: { ...d.orden, estado: r.estado, fechaFin: r.fechaFin },
                cita: { ...d.cita, estado: r.estado },
                consumos: { ...d.consumos, costoRepuestos: r.costoRepuestos || d.consumos.costoRepuestos },
              }));
              window.scrollTo({ top: 0, behavior: "smooth" });
            }}
            alConflicto={recargar}
          />
        </section>
      )}

      {visor && (
        <Visor
          deficiencia={visor.deficiencia}
          indice={visor.indice}
          alCambiar={(indice) => setVisor((v) => ({ ...v, indice }))}
          alCerrar={() => setVisor(null)}
        />
      )}
    </section>
  );
}

export default OrdenTrabajo;
