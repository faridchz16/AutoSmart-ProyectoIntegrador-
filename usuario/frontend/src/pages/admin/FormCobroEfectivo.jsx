import { useEffect, useState } from "react";
import {
  listarPorCobrar,
  buscarOrdenDeCita,
  registrarCobroEfectivo,
  descargarComprobante,
  totalDelError,
  MAX_OBSERVACION_COBRO,
} from "../../services/pagosService";
import { formatoSoles } from "../../services/indicadoresService";
import { fechaCorta, fechaHora } from "../../utils/fechas";

/* T69.2 – Formulario de cobro en efectivo (API de T69.1).
   1) El administrador elige la cita FINALIZADA que se paga en el taller (se busca por ticket, cliente o placa).
   2) Escribe lo que recibió, la fecha y hora del cobro y una observación opcional.
      El backend calcula el total real (servicio + adicionales aprobados + repuestos); si lo recibido no alcanza,
      responde con el total y se ofrece usarlo.
   3) Se muestra el código del pago, el total, el vuelto y la descarga del comprobante. La cita pasa a PAGADO. */

const ahoraLima = () =>
  new Intl.DateTimeFormat("sv-SE", { timeZone: "America/Lima", dateStyle: "short", timeStyle: "short" })
    .format(new Date())
    .replace(" ", "T");

const sinTildes = (s) => s.normalize("NFD").replace(/[̀-ͯ]/g, "").toLowerCase();

function FormCobroEfectivo({ alRegistrar, alCerrar }) {
  const [lista, setLista] = useState({ cargando: true, citas: [], error: "" });
  const [recargaLista, setRecargaLista] = useState(0);
  const [busqueda, setBusqueda] = useState("");
  const [cita, setCita] = useState(null);
  const [orden, setOrden] = useState({ cargando: false, datos: null, error: "" });
  const [monto, setMonto] = useState("");
  const [fecha, setFecha] = useState(ahoraLima);
  const [observacion, setObservacion] = useState("");
  const [errores, setErrores] = useState({});
  const [errorGeneral, setErrorGeneral] = useState({ texto: "", total: null });
  const [enviando, setEnviando] = useState(false);
  const [registrado, setRegistrado] = useState(null);
  const [descargando, setDescargando] = useState(false);

  useEffect(() => {
    let vigente = true;
    listarPorCobrar()
      .then((citas) => vigente && setLista({ cargando: false, citas, error: "" }))
      .catch((e) => vigente && setLista({ cargando: false, citas: [], error: e.message }));
    return () => {
      vigente = false;
    };
  }, [recargaLista]);

  const recargar = () => {
    setLista((l) => ({ ...l, cargando: true }));
    setRecargaLista((n) => n + 1);
  };

  const elegir = async (c) => {
    setCita(c);
    setMonto("");
    setErrores({});
    setErrorGeneral({ texto: "", total: null });
    setOrden({ cargando: true, datos: null, error: "" });
    try {
      setOrden({ cargando: false, datos: await buscarOrdenDeCita(c), error: "" });
    } catch (e) {
      setOrden({ cargando: false, datos: null, error: e.message });
    }
  };

  const volver = () => {
    setCita(null);
    setOrden({ cargando: false, datos: null, error: "" });
  };

  const validar = () => {
    const e = {};
    const valor = Number(monto);
    if (!monto.trim()) e.monto = "Escribe cuánto dinero recibiste.";
    else if (!Number.isFinite(valor) || valor <= 0) e.monto = "El monto debe ser mayor a 0.";
    else if (!/^\d+(\.\d{1,2})?$/.test(monto.trim())) e.monto = "Usa como máximo 2 decimales (ej. 230.50).";
    if (!fecha) e.fecha = "Indica la fecha y hora del cobro.";
    else if (fecha > ahoraLima()) e.fecha = "La fecha del cobro no puede ser futura.";
    if (observacion.trim().length > MAX_OBSERVACION_COBRO) e.observacion = `Máximo ${MAX_OBSERVACION_COBRO} caracteres.`;
    return e;
  };

  const enviar = async (ev) => {
    ev.preventDefault();
    const e = validar();
    setErrores(e);
    setErrorGeneral({ texto: "", total: null });
    if (Object.keys(e).length) return;
    setEnviando(true);
    try {
      const pago = await registrarCobroEfectivo({ idOrden: orden.datos.idOrden, monto: monto.trim(), fecha, observacion });
      setRegistrado({ ...pago, recibido: Number(monto), cita });
      alRegistrar(pago);
    } catch (err) {
      const total = err.status === 400 ? totalDelError(err.message) : null;
      setErrorGeneral({
        texto: total !== null ? `Lo recibido no alcanza: el total de la orden es ${formatoSoles(total)}.` : err.message,
        total,
      });
      if (err.status === 409) recargar();
    } finally {
      setEnviando(false);
    }
  };

  const bajarComprobante = async () => {
    setDescargando(true);
    try {
      await descargarComprobante({ id: registrado.id, codigo: registrado.codigo, cliente: registrado.cita.cliente, monto: registrado.monto });
    } catch (e) {
      setErrorGeneral({ texto: `No se pudo descargar el comprobante: ${e.message}`, total: null });
    } finally {
      setDescargando(false);
    }
  };

  /* ---------- 3) Cobro registrado ---------- */
  if (registrado) {
    return (
      <div className="form-exito cobro-exito">
        <span className="form-exito-check" aria-hidden="true">✓</span>
        <p>
          Se registró el pago <strong>{registrado.codigo}</strong> de {registrado.cita.cliente} ({registrado.cita.ticket}). La
          cita quedó como <strong>Pagado</strong>.
        </p>
        <dl className="cobro-cuenta">
          <dt>Total de la orden</dt>
          <dd>{formatoSoles(registrado.monto)}</dd>
          <dt>Recibido</dt>
          <dd>{formatoSoles(registrado.recibido)}</dd>
          <dt>Vuelto</dt>
          <dd className="cobro-vuelto">{formatoSoles(registrado.vuelto)}</dd>
        </dl>
        {errorGeneral.texto && (
          <p className="form-error-general" role="alert">
            {errorGeneral.texto}
          </p>
        )}
        <div className="form-acciones">
          <button type="button" className="btn btn-sec" onClick={bajarComprobante} disabled={descargando}>
            {descargando ? "Descargando..." : "Descargar comprobante"}
          </button>
          <button type="button" className="btn btn-pri" onClick={alCerrar}>
            Listo
          </button>
        </div>
      </div>
    );
  }

  /* ---------- 2) Datos del cobro ---------- */
  if (cita) {
    const referencia = (cita.precio ?? 0) + (orden.datos?.costoRepuestos ?? 0);
    return (
      <form className="adm-form cobro" onSubmit={enviar} noValidate>
        <div className="cobro-cita">
          <div>
            <strong>
              {cita.ticket} · {cita.servicio}
            </strong>
            <span>
              {cita.cliente} · <span className="pg-placa">{cita.placa}</span> · {fechaCorta(cita.fecha)}
            </span>
          </div>
          <button type="button" className="btn-icono" onClick={volver} disabled={enviando}>
            Cambiar
          </button>
        </div>

        {orden.cargando ? (
          <p className="cobro-info">Buscando la orden de trabajo...</p>
        ) : orden.error ? (
          <p className="form-error-general" role="alert">
            {orden.error}
          </p>
        ) : (
          <>
            <p className="cobro-info">
              Orden N.º <strong>{orden.datos.idOrden}</strong> · servicio {formatoSoles(cita.precio ?? 0)} + repuestos{" "}
              {formatoSoles(orden.datos.costoRepuestos)} = <strong>{formatoSoles(referencia)}</strong> de referencia. El total
              final lo calcula el sistema e incluye los trabajos adicionales aprobados en el presupuesto.
            </p>

            <div className="campo-doble">
              <div className="campo">
                <label htmlFor="cobro-monto">Monto recibido</label>
                <div className="campo-prefijo">
                  <span>S/</span>
                  <input
                    id="cobro-monto"
                    inputMode="decimal"
                    autoFocus
                    placeholder={referencia ? referencia.toFixed(2) : "0.00"}
                    value={monto}
                    aria-invalid={!!errores.monto}
                    aria-describedby={errores.monto ? "cobro-monto-error" : undefined}
                    onChange={(e) => {
                      setMonto(e.target.value.replace(",", "."));
                      setErrores((x) => ({ ...x, monto: undefined }));
                    }}
                  />
                </div>
                {errores.monto && (
                  <p className="campo-error" id="cobro-monto-error">
                    {errores.monto}
                  </p>
                )}
              </div>
              <div className="campo">
                <label htmlFor="cobro-fecha">Fecha y hora del cobro</label>
                <input
                  id="cobro-fecha"
                  type="datetime-local"
                  value={fecha}
                  max={ahoraLima()}
                  aria-invalid={!!errores.fecha}
                  onChange={(e) => {
                    setFecha(e.target.value);
                    setErrores((x) => ({ ...x, fecha: undefined }));
                  }}
                />
                {errores.fecha && <p className="campo-error">{errores.fecha}</p>}
              </div>
            </div>

            <div className="campo">
              <label htmlFor="cobro-obs">
                Observación <span className="opcional">(opcional)</span>
              </label>
              <textarea
                id="cobro-obs"
                rows={2}
                maxLength={MAX_OBSERVACION_COBRO}
                placeholder="Ej. Pagó con un billete de S/ 200"
                value={observacion}
                onChange={(e) => setObservacion(e.target.value)}
              />
              <span className="campo-ayuda">
                Si hay vuelto, el sistema lo anota solo. {observacion.length}/{MAX_OBSERVACION_COBRO}
              </span>
            </div>

            {errorGeneral.texto && (
              <div className="form-error-general cobro-error" role="alert">
                <span>{errorGeneral.texto}</span>
                {errorGeneral.total !== null && (
                  <button
                    type="button"
                    className="btn btn-sec"
                    onClick={() => {
                      setMonto(errorGeneral.total.toFixed(2));
                      setErrorGeneral({ texto: "", total: null });
                    }}
                  >
                    Usar {formatoSoles(errorGeneral.total)}
                  </button>
                )}
              </div>
            )}
          </>
        )}

        <div className="form-acciones">
          <button type="button" className="btn btn-sec" onClick={alCerrar} disabled={enviando}>
            Cancelar
          </button>
          <button type="submit" className="btn btn-pri" disabled={enviando || orden.cargando || !orden.datos}>
            {enviando && <span className="btn-spinner" aria-hidden="true" />}
            {enviando ? "Registrando..." : "Registrar cobro"}
          </button>
        </div>
      </form>
    );
  }

  /* ---------- 1) Elegir la cita ---------- */
  const texto = sinTildes(busqueda.trim());
  const visibles = texto
    ? lista.citas.filter((c) => sinTildes(`${c.ticket} ${c.cliente} ${c.placa} ${c.servicio}`).includes(texto))
    : lista.citas;

  return (
    <div className="cobro">
      <p className="cobro-info">Elige el servicio finalizado que el cliente está pagando en el taller.</p>
      <label className="buscador">
        <span className="sr-only">Buscar por ticket, cliente o placa</span>
        <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" aria-hidden="true">
          <circle cx="11" cy="11" r="7" />
          <path d="m20 20-3.5-3.5" />
        </svg>
        <input
          type="search"
          autoFocus
          placeholder="Buscar por ticket, cliente o placa"
          value={busqueda}
          onChange={(e) => setBusqueda(e.target.value)}
        />
      </label>

      {lista.error ? (
        <div className="cobro-vacio">
          <p className="form-error-general">{lista.error}</p>
          <button type="button" className="btn btn-sec" onClick={recargar}>
            Reintentar
          </button>
        </div>
      ) : lista.cargando ? (
        <p className="cobro-vacio">Buscando servicios por cobrar...</p>
      ) : visibles.length === 0 ? (
        <p className="cobro-vacio">
          {lista.citas.length ? `Ningún servicio coincide con «${busqueda.trim()}».` : "No hay servicios finalizados pendientes de pago."}
        </p>
      ) : (
        <ul className="cobro-lista">
          {visibles.map((c) => (
            <li key={c.id}>
              <button type="button" onClick={() => elegir(c)}>
                <span className="cobro-lista-cab">
                  <strong>{c.cliente}</strong>
                  <span className="pg-placa">{c.placa}</span>
                </span>
                <span>
                  {c.ticket} · {c.servicio}
                </span>
                <small>
                  Atendido el {fechaHora(`${c.fecha}T${c.horaInicio || "00:00"}`)}
                  {c.mecanico && ` por ${c.mecanico}`}
                </small>
              </button>
            </li>
          ))}
        </ul>
      )}

      <div className="form-acciones">
        <button type="button" className="btn btn-sec" onClick={alCerrar}>
          Cancelar
        </button>
      </div>
    </div>
  );
}

export default FormCobroEfectivo;
