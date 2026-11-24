import { useState } from "react";
import {
  pagar,
  TARJETAS_PRUEBA,
  CODIGO_YAPE_PRUEBA,
  formatearTarjeta,
  marcaTarjeta,
  luhnValido,
  tokenDeTarjeta,
  soloDigitos,
} from "../../services/pagoClienteService";
import { formatoPrecio } from "../../services/serviciosService";
import { hoyLima } from "../../utils/fechas";

/* Formulario de pago (CLI-06): tarjeta, Yape/Plin o efectivo en el taller.
   La pasarela del backend es de pruebas (sandbox): la tarjeta se convierte en el token de prueba que le
   corresponde y el número nunca se envía al servidor. Yape/Plin se paga con el código de aprobación de 6 dígitos. */

const VACIO = { numero: "", titular: "", vence: "", cvv: "", celular: "", codigo: "" };
const ICONOS = {
  TARJETA: (
    <svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" aria-hidden="true">
      <rect x="3" y="5.5" width="18" height="13" rx="2.5" />
      <path d="M3 10h18M7 15h4" />
    </svg>
  ),
  YAPE_PLIN: (
    <svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" aria-hidden="true">
      <rect x="7" y="2.5" width="10" height="19" rx="2.2" />
      <path d="M11 18.5h2M10 7.5h4" />
    </svg>
  ),
  EFECTIVO: (
    <svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" aria-hidden="true">
      <rect x="2.5" y="6.5" width="19" height="11" rx="2" />
      <circle cx="12" cy="12" r="2.5" />
      <path d="M6 9.5v5M18 9.5v5" />
    </svg>
  ),
};
const AYUDA = {
  TARJETA: "Crédito o débito",
  YAPE_PLIN: "Con código de aprobación",
  EFECTIVO: "Al recoger tu vehículo",
};

// "1229" -> "12/29"
const formatearVence = (t) => {
  const d = soloDigitos(t).slice(0, 4);
  return d.length > 2 ? `${d.slice(0, 2)}/${d.slice(2)}` : d;
};

function validar(metodo, d) {
  const e = {};
  if (metodo === "TARJETA") {
    const num = soloDigitos(d.numero);
    if (!num) e.numero = "Escribe el número de la tarjeta.";
    else if (num.length < 13 || !luhnValido(num)) e.numero = "El número de tarjeta no es válido. Revísalo.";
    else if (!tokenDeTarjeta(num)) e.numero = "Estamos en modo de pruebas: usa una de las tarjetas de prueba de abajo.";
    if (!d.titular.trim()) e.titular = "Escribe el nombre como aparece en la tarjeta.";
    const [mm, aa] = d.vence.split("/");
    const hoy = hoyLima();
    if (!/^\d{2}\/\d{2}$/.test(d.vence) || Number(mm) < 1 || Number(mm) > 12) e.vence = "Usa el formato MM/AA.";
    else if (`20${aa}-${mm}` < hoy.slice(0, 7)) e.vence = "La tarjeta está vencida.";
    const largoCvv = marcaTarjeta(num) === "American Express" ? 4 : 3;
    if (soloDigitos(d.cvv).length !== largoCvv) e.cvv = `Son los ${largoCvv} dígitos del reverso.`;
  }
  if (metodo === "YAPE_PLIN") {
    if (!/^9\d{8}$/.test(soloDigitos(d.celular))) e.celular = "Escribe tu celular de 9 dígitos (empieza con 9).";
    if (!/^\d{6}$/.test(soloDigitos(d.codigo))) e.codigo = "El código de aprobación tiene 6 dígitos.";
  }
  return e;
}

// Mensajes de la pasarela en palabras para el cliente
const motivoClaro = (m) =>
  ({
    "La tarjeta fue rechazada": "Tu banco rechazó la tarjeta.",
    "Fondos insuficientes": "La tarjeta no tiene saldo suficiente.",
    "La tarjeta esta vencida": "La tarjeta está vencida.",
    "Codigo de aprobacion invalido": "El código de aprobación no es correcto o ya venció. Genera uno nuevo en tu app.",
  })[m] ?? m;

function FormPago({ idOrden, total, metodos, alPagado, alCambioDeEstado }) {
  const [metodo, setMetodo] = useState(metodos.find((m) => m.enLinea)?.codigo ?? metodos[0]?.codigo ?? "TARJETA");
  const [datos, setDatos] = useState(VACIO);
  const [errores, setErrores] = useState({});
  const [rechazo, setRechazo] = useState("");
  const [enviando, setEnviando] = useState(false);

  const enLinea = metodos.find((m) => m.codigo === metodo)?.enLinea ?? false;
  const marca = marcaTarjeta(datos.numero);

  const cambiar = (campo, formato = (t) => t) => (e) => {
    setDatos((d) => ({ ...d, [campo]: formato(e.target.value) }));
    setErrores((x) => ({ ...x, [campo]: undefined }));
    setRechazo("");
  };

  const usarTarjeta = (t) => {
    setDatos((d) => ({ ...d, numero: formatearTarjeta(t.numero), titular: d.titular || "LUCIA FERNANDEZ", vence: d.vence || "12/30", cvv: d.cvv || "123" }));
    setErrores({});
    setRechazo("");
  };

  const enviar = async (ev) => {
    ev.preventDefault();
    const e = validar(metodo, datos);
    setErrores(e);
    if (Object.keys(e).length) {
      document.getElementById(`pago-${Object.keys(e)[0]}`)?.focus();
      return;
    }
    setEnviando(true);
    setRechazo("");
    try {
      const tokenPago = metodo === "TARJETA" ? tokenDeTarjeta(datos.numero) : soloDigitos(datos.codigo);
      const pago = await pagar({ idOrden, metodo, tokenPago });
      if (pago.estado === "APROBADO") return alPagado(pago);
      setRechazo(`${motivoClaro(pago.motivoRechazo)} No se hizo ningún cobro; puedes intentar de nuevo o usar otro método.`);
      setDatos((d) => ({ ...d, cvv: "", codigo: "" }));
    } catch (err) {
      setRechazo(err.message);
      if (err.status === 409) alCambioDeEstado();
    } finally {
      setEnviando(false);
    }
  };

  const campo = (id, etiqueta, props, ayuda) => (
    <div className="campo">
      <label htmlFor={`pago-${id}`}>{etiqueta}</label>
      <input
        id={`pago-${id}`}
        value={datos[id]}
        aria-invalid={!!errores[id]}
        aria-describedby={errores[id] ? `pago-${id}-error` : undefined}
        disabled={enviando}
        {...props}
      />
      {errores[id] ? (
        <p className="campo-error" id={`pago-${id}-error`}>
          {errores[id]}
        </p>
      ) : (
        ayuda && <span className="campo-ayuda">{ayuda}</span>
      )}
    </div>
  );

  return (
    <form className="pag-form" onSubmit={enviar} noValidate>
      <fieldset className="pag-metodos">
        <legend>¿Cómo quieres pagar?</legend>
        {metodos.map((m) => (
          <label key={m.codigo} className={metodo === m.codigo ? "pag-metodo elegido" : "pag-metodo"}>
            <input
              type="radio"
              name="metodo"
              value={m.codigo}
              checked={metodo === m.codigo}
              disabled={enviando}
              onChange={() => {
                setMetodo(m.codigo);
                setErrores({});
                setRechazo("");
              }}
            />
            <span className="pag-metodo-icono">{ICONOS[m.codigo] ?? ICONOS.TARJETA}</span>
            <span>
              <strong>{m.nombre}</strong>
              <span>{AYUDA[m.codigo] ?? (m.enLinea ? "En línea" : "En el taller")}</span>
            </span>
          </label>
        ))}
      </fieldset>

      {metodo === "TARJETA" && (
        <div className="pag-campos">
          {campo(
            "numero",
            <>
              Número de tarjeta {marca && <span className="pag-marca">{marca}</span>}
            </>,
            {
              inputMode: "numeric",
              autoComplete: "cc-number",
              placeholder: "1234 5678 9012 3456",
              maxLength: 23,
              onChange: cambiar("numero", formatearTarjeta),
            },
          )}
          {campo("titular", "Nombre del titular", {
            autoComplete: "cc-name",
            placeholder: "Como aparece en la tarjeta",
            maxLength: 60,
            onChange: cambiar("titular", (t) => t.toUpperCase()),
          })}
          <div className="campo-doble">
            {campo("vence", "Vence", { inputMode: "numeric", autoComplete: "cc-exp", placeholder: "MM/AA", maxLength: 5, onChange: cambiar("vence", formatearVence) })}
            {campo("cvv", "CVV", {
              inputMode: "numeric",
              autoComplete: "cc-csc",
              placeholder: marca === "American Express" ? "1234" : "123",
              maxLength: 4,
              onChange: cambiar("cvv", (t) => soloDigitos(t).slice(0, 4)),
            })}
          </div>
          <details className="pag-prueba">
            <summary>Modo de pruebas: no se hacen cobros reales. Ver tarjetas de prueba</summary>
            <ul>
              {TARJETAS_PRUEBA.map((t) => (
                <li key={t.numero}>
                  <code>{formatearTarjeta(t.numero)}</code>
                  <span>{t.texto}</span>
                  <button type="button" className="res-enlace" onClick={() => usarTarjeta(t)} disabled={enviando}>
                    Usar
                  </button>
                </li>
              ))}
            </ul>
          </details>
        </div>
      )}

      {metodo === "YAPE_PLIN" && (
        <div className="pag-campos">
          {campo(
            "celular",
            "Celular de tu Yape o Plin",
            { inputMode: "numeric", autoComplete: "tel-national", placeholder: "987654321", maxLength: 9, onChange: cambiar("celular", (t) => soloDigitos(t).slice(0, 9)) },
          )}
          {campo(
            "codigo",
            "Código de aprobación",
            { inputMode: "numeric", autoComplete: "one-time-code", placeholder: "6 dígitos", maxLength: 6, onChange: cambiar("codigo", (t) => soloDigitos(t).slice(0, 6)) },
            "Ábrelo en tu app: Menú → Código de aprobación. Vence en unos minutos.",
          )}
          <p className="pag-prueba-nota">
            Modo de pruebas: el código <code>{CODIGO_YAPE_PRUEBA}</code> se aprueba; cualquier otro se rechaza.
          </p>
        </div>
      )}

      {!enLinea && (
        <div className="pag-efectivo">
          <p>
            <strong>Paga en la caja del taller cuando recojas tu vehículo.</strong> El administrador registrará tu pago y aquí podrás descargar tu
            comprobante.
          </p>
          <p>Monto a pagar: {formatoPrecio(total)}</p>
        </div>
      )}

      {rechazo && (
        <p className="form-error-general" role="alert">
          {rechazo}
        </p>
      )}

      {enLinea && (
        <button type="submit" className="btn btn-pri pag-boton" disabled={enviando}>
          {enviando && <span className="btn-spinner" aria-hidden="true" />}
          {enviando ? "Procesando pago..." : `Pagar ${formatoPrecio(total)}`}
        </button>
      )}
      {enLinea && (
        <p className="pag-seguro">
          <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" aria-hidden="true">
            <rect x="5" y="10.5" width="14" height="10" rx="2" />
            <path d="M8 10.5V8a4 4 0 0 1 8 0v2.5" />
          </svg>
          Los datos de tu tarjeta no se guardan en AutoSmart. El monto lo calcula el taller con tu servicio y los adicionales aprobados.
        </p>
      )}
    </form>
  );
}

export default FormPago;
