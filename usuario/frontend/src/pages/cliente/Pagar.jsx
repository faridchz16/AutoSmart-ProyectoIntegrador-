import { useEffect, useRef, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { obtenerSeguimiento } from "../../services/seguimientoService";
import { ordenDeCita, obtenerCosto, listarMetodos, descargarComprobante } from "../../services/pagoClienteService";
import { ESTADOS_CITA_CLIENTE } from "../../services/citasClienteService";
import { nombreMetodo } from "../../services/indicadoresService";
import { fechaLarga, hora12 } from "../../services/reservaService";
import { formatoPrecio } from "../../services/serviciosService";
import { fechaHora } from "../../utils/fechas";
import { calificacionGuardada } from "../../services/mantenimientoClienteService";
import FormPago from "./FormPago";
import "../../components/admin/admin-ui.css";
import "./MisCitas.css";
import "./Seguimiento.css";
import "./Pagar.css";

/* CLI-06 – Pago del servicio (APIs de T60.1, T61.1, T62.1, T63.1 y T64.1).
   - Detalle de la cuenta: servicio reservado, adicionales del presupuesto aprobado, repuestos usados y total
   - Pago con tarjeta o Yape/Plin (pasarela sandbox); efectivo se paga en el taller
   - Pago realizado: código, método, fecha y descarga del comprobante en PDF */

const TIPOS = { MANO_OBRA: "Mano de obra", REPUESTO: "Repuesto" };

function Cuenta({ costo }) {
  return (
    <section className="seg-caja pag-cuenta" aria-labelledby="pag-cuenta-titulo">
      <div className="seg-caja-cab">
        <h3 id="pag-cuenta-titulo">Detalle de tu cuenta</h3>
        <span className="pag-ticket">{costo.ticket}</span>
      </div>
      <ul className="pag-lineas">
        <li>
          <span>
            <span className="seg-tipo">Servicio reservado</span>
            {costo.servicio.nombre}
          </span>
          <strong>{formatoPrecio(costo.servicio.precio)}</strong>
        </li>
        {costo.adicionales.map((a, i) => (
          <li key={i}>
            <span>
              <span className="seg-tipo">Adicional · {TIPOS[a.tipo] ?? "Otro"}</span>
              {a.descripcion}
              {a.cantidad > 1 && (
                <span className="pag-cant">
                  {a.cantidad} × {formatoPrecio(a.precioUnitario)}
                </span>
              )}
            </span>
            <strong>{formatoPrecio(a.subtotal)}</strong>
          </li>
        ))}
      </ul>
      <div className="pag-total">
        <span>Total a pagar</span>
        <strong>{formatoPrecio(costo.total)}</strong>
      </div>
      {costo.repuestos.length > 0 && (
        <div className="pag-repuestos">
          <h4>Repuestos que se usaron</h4>
          <ul>
            {costo.repuestos.map((r, i) => (
              <li key={i}>
                {r.material} · {r.cantidad} {r.unidad}
              </li>
            ))}
          </ul>
          <p>Ya están incluidos en el servicio y en los adicionales que aprobaste.</p>
        </div>
      )}
    </section>
  );
}

function Pagado({ pago, monto, titulo, alDescargar, descargando, errorDescarga, calificar }) {
  const ref = useRef(null);
  useEffect(() => {
    if (titulo) ref.current?.focus();
  }, [titulo]);
  return (
    <section className="seg-caja pag-hecho" aria-labelledby="pag-hecho-titulo">
      <span className="res-exito-check" aria-hidden="true">
        <svg viewBox="0 0 24 24" width="30" height="30" fill="none" stroke="currentColor" strokeWidth="2.4" strokeLinecap="round" strokeLinejoin="round">
          <path d="m5 12.5 4.5 4.5L19 7.5" />
        </svg>
      </span>
      <h3 id="pag-hecho-titulo" ref={ref} tabIndex={-1}>
        {titulo ? "¡Pago realizado!" : "Este servicio ya está pagado"}
      </h3>
      <dl className="res-resumen">
        <div>
          <dt>Código de pago</dt>
          <dd>{pago.codigo}</dd>
        </div>
        <div>
          <dt>Método</dt>
          <dd>{nombreMetodo(pago.metodo)}</dd>
        </div>
        <div>
          <dt>Fecha</dt>
          <dd>{fechaHora(pago.fecha)}</dd>
        </div>
        <div className="res-resumen-total">
          <dt>Monto</dt>
          <dd>{formatoPrecio(monto)}</dd>
        </div>
      </dl>
      {errorDescarga && (
        <p className="form-error-general" role="alert">
          {errorDescarga}
        </p>
      )}
      <button type="button" className="btn btn-pri" onClick={alDescargar} disabled={descargando}>
        {descargando && <span className="btn-spinner" aria-hidden="true" />}
        {descargando ? "Descargando..." : "Descargar comprobante (PDF)"}
      </button>
      <p className="pag-gracias">Gracias por confiar en AutoSmart. Ya puedes recoger tu vehículo.</p>
      {calificar && (
        <Link to={calificar} className="pag-calificar">
          ★ Califica la atención que recibiste →
        </Link>
      )}
    </section>
  );
}

function Pagar() {
  const { id } = useParams();
  const [recarga, setRecarga] = useState(0);
  const [resultado, setResultado] = useState({ clave: null, cita: null, costo: null, metodos: [], error: "", status: 0 });
  const [pagoNuevo, setPagoNuevo] = useState(null);
  const [descargando, setDescargando] = useState(false);
  const [errorDescarga, setErrorDescarga] = useState("");

  const clave = `${id}|${recarga}`;

  useEffect(() => {
    let vigente = true;
    (async () => {
      const cita = await obtenerSeguimiento(id);
      const idOrden = await ordenDeCita({ id: cita.id ?? Number(id), placa: cita.placa });
      const [costo, metodos] = await Promise.all([idOrden ? obtenerCosto(idOrden) : null, listarMetodos()]);
      return { cita, costo, metodos };
    })()
      .then((r) => vigente && setResultado({ clave, ...r, error: "", status: 0 }))
      .catch((e) => vigente && setResultado({ clave, cita: null, costo: null, metodos: [], error: e.message, status: e.status ?? 0 }));
    return () => {
      vigente = false;
    };
  }, [id, clave]);

  const { cita, costo, metodos } = resultado;
  const pago = pagoNuevo ?? costo?.pago ?? null;

  const descargar = async () => {
    setDescargando(true);
    setErrorDescarga("");
    try {
      await descargarComprobante({ id: pago.id, codigo: pago.codigo, monto: pagoNuevo?.monto ?? costo.total });
    } catch (e) {
      setErrorDescarga(e.message);
    } finally {
      setDescargando(false);
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
      <section className="gestion pag">
        {volver}
        <div className="tabla-caja">
          <div className="tabla-estado">
            <p className="tabla-estado-titulo">{noEsTuya ? "No encontramos esta cita" : "No se pudo cargar el pago"}</p>
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

  if (!cita) {
    return (
      <section className="gestion pag">
        {volver}
        <div className="tabla-caja">
          <div className="tabla-estado">
            <p>Calculando tu cuenta...</p>
          </div>
        </div>
      </section>
    );
  }

  const estadoCita = pagoNuevo ? "PAGADO" : cita.estado;

  return (
    <section className="gestion pag">
      {volver}
      <div className="seg-cabecera">
        <div>
          <p className="seg-ticket">Cita {cita.ticket}</p>
          <h2>{pago ? "Pago del servicio" : "Pagar servicio"}</h2>
          <p className="seg-sub">
            {cita.servicio} · {fechaLarga(cita.fecha)}, {hora12(cita.inicio)} · {cita.placa}
          </p>
        </div>
        <div className="seg-cab-der">
          <span className={`cit-estado est-${estadoCita}`}>{ESTADOS_CITA_CLIENTE[estadoCita]?.texto}</span>
        </div>
      </div>

      {!costo ? (
        <div className="tabla-caja">
          <div className="tabla-estado">
            <p className="tabla-estado-titulo">Aún no hay nada que pagar</p>
            <p>Podrás pagar cuando el mecánico termine el servicio. Mientras tanto, revisa el avance de tu cita.</p>
            <Link to={`/cliente/citas/${id}`} className="btn btn-sec">
              Ver seguimiento
            </Link>
          </div>
        </div>
      ) : (
        <div className="seg-cuerpo pag-cuerpo">
          <Cuenta costo={costo} />
          {pago ? (
            <Pagado
              pago={pago}
              monto={pagoNuevo?.monto ?? costo.total}
              titulo={!!pagoNuevo}
              alDescargar={descargar}
              descargando={descargando}
              errorDescarga={errorDescarga}
              calificar={calificacionGuardada(cita.id ?? Number(id)) ? "" : `/cliente/citas/${id}/detalle#calificar`}
            />
          ) : costo.puedePagar ? (
            <section className="seg-caja" aria-labelledby="pag-metodo-titulo">
              <div className="seg-caja-cab">
                <h3 id="pag-metodo-titulo">Pago</h3>
              </div>
              <FormPago
                idOrden={costo.idOrden}
                total={costo.total}
                metodos={metodos}
                alPagado={setPagoNuevo}
                alCambioDeEstado={() => setRecarga((n) => n + 1)}
              />
            </section>
          ) : (
            <section className="seg-caja pag-espera">
              <h3>El servicio aún no termina</h3>
              <p>
                Este es el costo hasta ahora. Podrás pagar cuando el mecánico marque tu vehículo como listo; si aprueba un presupuesto adicional, se sumará
                al total.
              </p>
              <Link to={`/cliente/citas/${id}`} className="btn btn-sec">
                Ver avance
              </Link>
            </section>
          )}
        </div>
      )}
    </section>
  );
}

export default Pagar;
