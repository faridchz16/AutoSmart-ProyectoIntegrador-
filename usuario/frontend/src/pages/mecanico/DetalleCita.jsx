import { useEffect, useState } from "react";
import Modal from "../../components/admin/Modal";
import { obtenerCita, enlaceWhatsApp, formatoKm } from "../../services/citasService";
import { ESTADOS_CITA } from "../../services/agendaService";
import { fechaLarga } from "../../services/bloqueosService";
import { formatoPrecio } from "../../services/serviciosService";
import { formatoHoras } from "../../services/horariosService";
import "./Agenda.css";
import "./DetalleCita.css";

/* T35.2 – Detalle de una cita: servicio, cliente (con teléfono), vehículo y precio.
   Se abre desde "Citas de hoy" y desde la agenda semanal (GET /appointments/{id}).
   `cita` es el resumen de la lista: se muestra al instante mientras llega el detalle. */

const Icono = ({ children }) => (
  <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
    {children}
  </svg>
);

function Contenido({ resumen, alCerrar }) {
  const [recarga, setRecarga] = useState(0);
  const [resultado, setResultado] = useState({ clave: null, detalle: null, error: "" });
  const cargando = resultado.clave !== recarga;

  useEffect(() => {
    let vigente = true;
    obtenerCita(resumen.id)
      .then((detalle) => vigente && setResultado({ clave: recarga, detalle, error: "" }))
      .catch((e) => vigente && setResultado({ clave: recarga, detalle: null, error: e.message }));
    return () => {
      vigente = false;
    };
  }, [resumen.id, recarga]);

  // Mientras carga se usan los datos del resumen; luego, los del detalle
  const c = resultado.detalle ?? resumen;
  const listo = !!resultado.detalle;
  const whatsapp = listo ? enlaceWhatsApp(c.telefono) : null;
  const vehiculo = [c.marca, c.modelo, c.anio].filter(Boolean).join(" ");

  return (
    <div className="dc">
      {/* ---------- Cabecera ---------- */}
      <div className="dc-cabeza">
        <div className="dc-cabeza-fila">
          <span className={`insignia-estado est-${c.estado}`}>{ESTADOS_CITA[c.estado] ?? c.estado}</span>
          {c.ticket && <span className="dc-ticket">Ticket {c.ticket}</span>}
        </div>
        <h3>{c.servicio}</h3>
        <p className="dc-cuando">
          <span className="dc-fecha">{fechaLarga(c.fecha)}</span>
          <span>
            {c.horaInicio} – {c.horaFin}
            {listo && c.duracionMinutos ? ` · ${formatoHoras(c.duracionMinutos)}` : ""}
          </span>
        </p>
        {c.estado === "PENDIENTE" && (
          <p className="dc-nota">Esta cita está pendiente: el cliente espera tu confirmación.</p>
        )}
      </div>

      {resultado.error ? (
        <div className="dc-error" role="alert">
          <p>No se pudo cargar el detalle: {resultado.error}</p>
          <button type="button" className="btn btn-sec" onClick={() => setRecarga((n) => n + 1)}>
            Reintentar
          </button>
        </div>
      ) : (
        <div className="dc-secciones" aria-busy={cargando}>
          {/* ---------- Cliente ---------- */}
          <section className="dc-seccion">
            <h4>
              <Icono>
                <circle cx="12" cy="8" r="3.5" />
                <path d="M5 20a7 7 0 0 1 14 0" />
              </Icono>
              Cliente
            </h4>
            <p className="dc-valor">{c.cliente}</p>
            {!listo ? (
              <span className="esq" aria-hidden="true" />
            ) : c.telefono ? (
              <div className="dc-contacto">
                <a className="dc-tel" href={`tel:${c.telefono.replace(/\s/g, "")}`}>
                  <Icono>
                    <path d="M5 4h4l2 5-2.5 1.5a11 11 0 0 0 5 5L15 13l5 2v4a2 2 0 0 1-2 2A16 16 0 0 1 3 6a2 2 0 0 1 2-2" />
                  </Icono>
                  {c.telefono}
                </a>
                {whatsapp && (
                  <a className="dc-wsp" href={whatsapp} target="_blank" rel="noreferrer">
                    WhatsApp
                  </a>
                )}
              </div>
            ) : (
              <p className="dc-sin">Sin teléfono registrado</p>
            )}
          </section>

          {/* ---------- Vehículo ---------- */}
          <section className="dc-seccion">
            <h4>
              <Icono>
                <path d="M5 16V11l2-5h10l2 5v5" />
                <path d="M3 16h18v3H3z" />
                <circle cx="7.5" cy="13.5" r="0.8" />
                <circle cx="16.5" cy="13.5" r="0.8" />
              </Icono>
              Vehículo
            </h4>
            <span className="ag-placa dc-placa">{c.placa}</span>
            {!listo ? (
              <span className="esq" aria-hidden="true" />
            ) : (
              <dl className="dc-datos">
                <div>
                  <dt>Modelo</dt>
                  <dd>{vehiculo || "—"}</dd>
                </div>
                <div>
                  <dt>Kilometraje</dt>
                  <dd>{formatoKm(c.kilometraje)}</dd>
                </div>
              </dl>
            )}
          </section>

          {/* ---------- Servicio y precio ---------- */}
          <section className="dc-seccion dc-precio">
            <h4>
              <Icono>
                <path d="M4 7h16v10H4z" />
                <circle cx="12" cy="12" r="2.5" />
              </Icono>
              Precio del servicio
            </h4>
            {!listo ? (
              <span className="esq" aria-hidden="true" />
            ) : (
              <>
                <p className="dc-monto">{c.precio === null ? "—" : formatoPrecio(c.precio)}</p>
                <p className="dc-sin">Precio vigente al reservar, sin incluir repuestos.</p>
              </>
            )}
          </section>
        </div>
      )}

      {/* T36.2: aquí irán los botones Aceptar / Rechazar para las citas pendientes */}
      <div className="form-acciones">
        <button type="button" className="btn btn-sec" onClick={alCerrar}>
          Cerrar
        </button>
      </div>
    </div>
  );
}

function DetalleCita({ cita, alCerrar }) {
  return (
    <Modal abierto={!!cita} alCerrar={alCerrar} titulo="Detalle de la cita">
      {cita && <Contenido key={cita.id} resumen={cita} alCerrar={alCerrar} />}
    </Modal>
  );
}

export default DetalleCita;
