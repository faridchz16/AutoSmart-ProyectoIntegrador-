import { useCallback, useEffect, useRef, useState } from "react";
import { Link, useSearchParams } from "react-router-dom";
import { bloquearTurno, liberarTurno, reservarCita, fechaLarga, hora12 } from "../../services/reservaService";
import { listarVehiculos } from "../../services/vehiculosService";
import { formatoPrecio, formatoDuracion } from "../../services/serviciosService";
import { hoyLima } from "../../utils/fechas";
import { PasoServicio, PasoMecanico, PasoHorario } from "./PasosReserva";
import FormVehiculo from "./FormVehiculo";
import Modal from "../../components/admin/Modal";
import Aviso from "../../components/admin/Aviso";
import "../../components/admin/admin-ui.css";
import "./Cliente.css";
import "./Reservar.css";

/* CLI-03 – Reservar una cita (APIs de T10.1, T30.1, T31.1, T33.1 y T34.1).
   1. Servicio del catálogo (buscador y tipo)       3. Día y turno libre: se bloquea 10 minutos en el servidor
   2. Mecánico con la especialidad, con estrellas   4. Vehículo y confirmación -> ticket de la cita (PENDIENTE)
   El turno bloqueado se libera al cambiar de mecánico o servicio, al salir de la pantalla o al cerrar la pestaña.
   ?vehiculo=ID deja elegido ese vehículo (botón "Reservar cita" de Mis vehículos). */

const PASOS = ["Servicio", "Mecánico", "Fecha y hora", "Confirmar"];
const TITULOS = {
  1: ["¿Qué servicio necesitas?", "Elige uno del catálogo; el precio es el base del taller."],
  2: ["Elige a tu mecánico", "Puedes ver su experiencia y cómo lo calificaron otros clientes."],
  3: ["¿Cuándo quieres venir?", "Elige el día y la hora de inicio que te acomoden."],
  4: ["Revisa y confirma", "Elige el vehículo que traerás y confirma la reserva."],
};

function Temporizador({ expira, alVencer }) {
  const [ahora, setAhora] = useState(() => Date.now());
  const restante = Math.max(0, Math.ceil((expira - ahora) / 1000));
  const avisado = useRef(false);

  useEffect(() => {
    const id = setInterval(() => {
      const t = Date.now();
      setAhora(t);
      if (t >= expira && !avisado.current) {
        avisado.current = true;
        alVencer();
      }
    }, 1000);
    return () => clearInterval(id);
  }, [expira, alVencer]);

  const mm = Math.floor(restante / 60);
  const ss = String(restante % 60).padStart(2, "0");
  return (
    <p className={restante <= 60 ? "res-reloj urgente" : "res-reloj"} role="timer" aria-live={restante <= 60 ? "polite" : "off"}>
      <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" aria-hidden="true">
        <circle cx="12" cy="13" r="8" />
        <path d="M12 9v4l2.5 2M9 2h6" />
      </svg>
      <span>
        Turno separado por <strong>{mm}:{ss}</strong>
      </span>
    </p>
  );
}

function Resumen({ servicio, mecanico, bloqueo, vehiculo }) {
  const filas = [
    ["Servicio", servicio?.nombre],
    ["Mecánico", mecanico?.nombre],
    ["Fecha", bloqueo && fechaLarga(bloqueo.fecha)],
    ["Hora", bloqueo && `${hora12(bloqueo.inicio)} a ${hora12(bloqueo.fin)}`],
    ["Vehículo", vehiculo && `${vehiculo.placa} · ${vehiculo.marca} ${vehiculo.modelo}`],
  ];
  return (
    <dl className="res-resumen">
      {filas.map(([t, v]) => (
        <div key={t}>
          <dt>{t}</dt>
          <dd>{v || <span className="cli-vacio">Por elegir</span>}</dd>
        </div>
      ))}
      {servicio && (
        <div className="res-resumen-total">
          <dt>Precio base</dt>
          <dd>{formatoPrecio(servicio.precio)}</dd>
        </div>
      )}
    </dl>
  );
}

function Exito({ cita, alReservarOtra }) {
  const titulo = useRef(null);
  useEffect(() => titulo.current?.focus(), []);
  return (
    <section className="gestion res-exito">
      <span className="res-exito-check" aria-hidden="true">
        <svg viewBox="0 0 24 24" width="34" height="34" fill="none" stroke="currentColor" strokeWidth="2.4" strokeLinecap="round" strokeLinejoin="round">
          <path d="m5 12.5 4.5 4.5L19 7.5" />
        </svg>
      </span>
      <h2 ref={titulo} tabIndex={-1}>
        ¡Tu cita quedó reservada!
      </h2>
      <p>
        Tu código de reserva es <strong className="res-ticket">{cita.ticket}</strong>
      </p>
      <dl className="res-resumen">
        <div>
          <dt>Servicio</dt>
          <dd>{cita.servicio}</dd>
        </div>
        <div>
          <dt>Mecánico</dt>
          <dd>{cita.mecanico}</dd>
        </div>
        <div>
          <dt>Fecha</dt>
          <dd>{fechaLarga(cita.fecha)}</dd>
        </div>
        <div>
          <dt>Hora</dt>
          <dd>
            {hora12(cita.inicio)} a {hora12(cita.fin)}
          </dd>
        </div>
        <div>
          <dt>Vehículo</dt>
          <dd>{cita.placa}</dd>
        </div>
        <div>
          <dt>Estado</dt>
          <dd>
            <span className="res-pendiente">Pendiente de confirmación</span>
          </dd>
        </div>
        {cita.precio !== null && (
          <div className="res-resumen-total">
            <dt>Precio base</dt>
            <dd>{formatoPrecio(cita.precio)}</dd>
          </div>
        )}
      </dl>
      <p className="res-exito-nota">
        Te enviamos el detalle a tu correo. El mecánico revisará tu reserva y te avisaremos cuando la confirme. Llega unos minutos antes con tu
        vehículo.
      </p>
      <div className="form-acciones">
        <button type="button" className="btn btn-sec" onClick={alReservarOtra}>
          Reservar otra cita
        </button>
        <Link to="/cliente" className="btn btn-pri">
          Ir al inicio
        </Link>
      </div>
    </section>
  );
}

function Reservar() {
  const [params] = useSearchParams();
  const [paso, setPaso] = useState(1);
  const [servicio, setServicio] = useState(null);
  const [mecanico, setMecanico] = useState(null);
  const [fecha, setFecha] = useState(hoyLima);
  const [bloqueo, setBloqueo] = useState(null);
  const [bloqueando, setBloqueando] = useState("");
  const [recargaTurnos, setRecargaTurnos] = useState(0);
  const [vehiculos, setVehiculos] = useState({ cargado: false, lista: [], error: "" });
  const [idVehiculo, setIdVehiculo] = useState(() => Number(params.get("vehiculo")) || null);
  const [nuevoVehiculo, setNuevoVehiculo] = useState(false);
  const [enviando, setEnviando] = useState(false);
  const [errorEnvio, setErrorEnvio] = useState("");
  const [cita, setCita] = useState(null);
  const [aviso, setAviso] = useState(null);
  const [recargaVehiculos, setRecargaVehiculos] = useState(0);

  const titulo = useRef(null);
  const montado = useRef(false);
  const bloqueoActual = useRef(null);
  const contadorAvisos = useRef(0);

  const avisar = useCallback((tipo, texto) => {
    contadorAvisos.current += 1;
    setAviso({ tipo, texto, id: contadorAvisos.current });
  }, []);
  const cerrarAviso = useCallback(() => setAviso(null), []);

  // Siempre se sabe qué bloqueo liberar al salir
  useEffect(() => {
    bloqueoActual.current = bloqueo;
  }, [bloqueo]);
  useEffect(() => {
    const alSalir = () => liberarTurno(bloqueoActual.current?.id);
    window.addEventListener("pagehide", alSalir);
    return () => {
      window.removeEventListener("pagehide", alSalir);
      alSalir();
    };
  }, []);

  // Al cambiar de paso, el foco va al título (lectores de pantalla y teclado)
  useEffect(() => {
    if (montado.current) titulo.current?.focus();
    montado.current = true;
  }, [paso]);

  // Vehículos del cliente (para el paso 4)
  useEffect(() => {
    let vigente = true;
    listarVehiculos()
      .then((lista) => vigente && setVehiculos({ cargado: true, lista, error: "" }))
      .catch((e) => vigente && setVehiculos({ cargado: true, lista: [], error: e.message }));
    return () => {
      vigente = false;
    };
  }, [recargaVehiculos]);

  const soltarBloqueo = () => {
    liberarTurno(bloqueo?.id);
    setBloqueo(null);
  };

  const elegirServicio = (s) => {
    if (s.id !== servicio?.id) {
      soltarBloqueo();
      setMecanico(null);
      setServicio(s);
    }
    setPaso(2);
  };

  const elegirMecanico = (m) => {
    if (m.id !== mecanico?.id) {
      soltarBloqueo();
      setMecanico(m);
    }
    setPaso(3);
  };

  const elegirTurno = async (t) => {
    if (bloqueo && bloqueo.fecha === fecha && bloqueo.inicio === t.inicio && bloqueo.idMecanico === mecanico.id) return setPaso(4);
    setBloqueando(t.inicio);
    try {
      // El backend reemplaza el bloqueo anterior del cliente por el nuevo
      const nuevo = await bloquearTurno({ idServicio: servicio.id, idMecanico: mecanico.id, fecha, horaInicio: t.inicio });
      setBloqueo(nuevo);
      setErrorEnvio("");
      setPaso(4);
    } catch (err) {
      soltarBloqueo();
      setRecargaTurnos((n) => n + 1);
      avisar("error", err.status === 409 ? "Ese turno acaba de ser tomado por otra persona. Elige otro horario." : err.message);
    } finally {
      setBloqueando("");
    }
  };

  const alVencer = useCallback(() => {
    if (!bloqueoActual.current) return; // los dos relojes (lateral y móvil) avisan una sola vez
    bloqueoActual.current = null;
    setBloqueo(null);
    setPaso((p) => (p === 4 ? 3 : p));
    setRecargaTurnos((n) => n + 1);
    avisar("error", "Pasaron los 10 minutos y el turno se liberó. Elige la hora de nuevo.");
  }, [avisar]);

  const irAPaso = (n) => {
    if (n === 4 && !bloqueo) return;
    setPaso(n);
  };

  const confirmar = async () => {
    if (!idVehiculo) return setErrorEnvio("Elige el vehículo que traerás al taller.");
    setEnviando(true);
    setErrorEnvio("");
    try {
      const nueva = await reservarCita({ idServicio: servicio.id, idMecanico: mecanico.id, idVehiculo, fecha: bloqueo.fecha, horaInicio: bloqueo.inicio });
      setBloqueo(null); // el backend ya liberó el bloqueo
      setCita(nueva);
    } catch (err) {
      if (err.status === 409 || (err.status === 400 && /fecha|turno/i.test(err.message))) {
        setBloqueo(null);
        setRecargaTurnos((n) => n + 1);
        setPaso(3);
        avisar("error", "El turno ya no está disponible. Elige otra hora.");
      } else if (err.status === 404 && /vehiculo/i.test(err.message)) {
        setIdVehiculo(null);
        setRecargaVehiculos((n) => n + 1);
        setErrorEnvio("Ese vehículo ya no está en tu lista. Elige otro.");
      } else setErrorEnvio(err.message);
    } finally {
      setEnviando(false);
    }
  };

  const reiniciar = () => {
    setCita(null);
    setServicio(null);
    setMecanico(null);
    setBloqueo(null);
    setFecha(hoyLima());
    setErrorEnvio("");
    setPaso(1);
  };

  if (cita) return <Exito cita={cita} alReservarOtra={reiniciar} />;

  const vehiculo = vehiculos.lista.find((v) => v.id === idVehiculo) ?? null;
  const [tituloPaso, ayudaPaso] = TITULOS[paso];
  const habilitado = (n) => n === 1 || (n === 2 && servicio) || (n === 3 && mecanico) || (n === 4 && bloqueo);

  return (
    <section className="gestion res">
      <div className="gestion-cabecera">
        <div>
          <h2>Reservar cita</h2>
          <p>Agenda la atención de tu vehículo en cuatro pasos.</p>
        </div>
      </div>

      <ol className="res-pasos">
        {PASOS.map((nombre, i) => {
          const n = i + 1;
          const estado = n === paso ? "actual" : n < paso ? "hecho" : "";
          return (
            <li key={nombre} className={estado}>
              <button type="button" onClick={() => irAPaso(n)} disabled={!habilitado(n) || n === paso} aria-current={n === paso ? "step" : undefined}>
                <span className="res-paso-num" aria-hidden="true">
                  {n < paso ? "✓" : n}
                </span>
                <span className="res-paso-nombre">
                  <span className="sr-only">Paso {n}: </span>
                  {nombre}
                </span>
              </button>
            </li>
          );
        })}
      </ol>

      {bloqueo && (
        <div className="res-reloj-movil">
          <Temporizador key={bloqueo.id} expira={bloqueo.expira} alVencer={alVencer} />
        </div>
      )}

      <div className="res-cuerpo">
        <div className="res-principal">
          <div className="res-titulo">
            <h3 ref={titulo} tabIndex={-1}>
              {tituloPaso}
            </h3>
            <p>{ayudaPaso}</p>
          </div>

          {paso === 1 && <PasoServicio elegido={servicio} alElegir={elegirServicio} />}
          {paso === 2 && <PasoMecanico servicio={servicio} elegido={mecanico} alElegir={elegirMecanico} alCambiarServicio={() => setPaso(1)} />}
          {paso === 3 && (
            <PasoHorario
              servicio={servicio}
              mecanico={mecanico}
              fecha={fecha}
              alCambiarFecha={setFecha}
              bloqueo={bloqueo}
              bloqueando={bloqueando}
              alElegir={elegirTurno}
              recargaExterna={recargaTurnos}
            />
          )}
          {paso === 4 && bloqueo && (
            <div className="res-confirmar">
              <fieldset className="res-vehiculos">
                <legend>¿Qué vehículo traerás?</legend>
                {!vehiculos.cargado ? (
                  <p className="res-nota">Cargando tus vehículos...</p>
                ) : vehiculos.error ? (
                  <p className="form-error-general">
                    {vehiculos.error}{" "}
                    <button type="button" className="res-enlace" onClick={() => setRecargaVehiculos((n) => n + 1)}>
                      Reintentar
                    </button>
                  </p>
                ) : (
                  <>
                    {vehiculos.lista.length === 0 && <p className="res-nota">Aún no registras vehículos. Agrega el que traerás para continuar.</p>}
                    <div className="res-vehiculos-lista">
                      {vehiculos.lista.map((v) => (
                        <label key={v.id} className={v.id === idVehiculo ? "res-vehiculo elegido" : "res-vehiculo"}>
                          <input
                            type="radio"
                            name="vehiculo"
                            value={v.id}
                            checked={v.id === idVehiculo}
                            onChange={() => {
                              setIdVehiculo(v.id);
                              setErrorEnvio("");
                            }}
                          />
                          <span className="res-vehiculo-placa">{v.placa}</span>
                          <span>
                            {v.marca} {v.modelo} {v.anio ? `· ${v.anio}` : ""}
                          </span>
                        </label>
                      ))}
                      <button type="button" className="res-vehiculo res-vehiculo-nuevo" onClick={() => setNuevoVehiculo(true)}>
                        <span aria-hidden="true">+</span> {vehiculos.lista.length ? "Agregar otro vehículo" : "Agregar vehículo"}
                      </button>
                    </div>
                  </>
                )}
              </fieldset>

              <div className="res-confirmar-resumen">
                <h4>Tu reserva</h4>
                <Resumen servicio={servicio} mecanico={mecanico} bloqueo={bloqueo} vehiculo={vehiculo} />
                <p className="res-nota">
                  Duración aproximada: {formatoDuracion(servicio.duracion)}. Pagas al terminar el servicio. Si el mecánico encuentra algo más que
                  reparar, te enviará un presupuesto para que lo apruebes antes de hacerlo.
                </p>
              </div>

              {errorEnvio && (
                <p className="form-error-general" role="alert">
                  {errorEnvio}
                </p>
              )}
              <div className="form-acciones">
                <button type="button" className="btn btn-sec" onClick={() => setPaso(3)} disabled={enviando}>
                  Cambiar horario
                </button>
                <button type="button" className="btn btn-pri" onClick={confirmar} disabled={enviando}>
                  {enviando && <span className="btn-spinner" aria-hidden="true" />}
                  {enviando ? "Reservando..." : "Confirmar reserva"}
                </button>
              </div>
            </div>
          )}

          {paso > 1 && paso < 4 && (
            <div className="res-volver">
              <button type="button" className="btn btn-sec" onClick={() => setPaso(paso - 1)}>
                ← Volver
              </button>
            </div>
          )}
        </div>

        <aside className="res-lateral" aria-label="Resumen de tu reserva">
          <h4>Tu reserva</h4>
          <Resumen servicio={servicio} mecanico={mecanico} bloqueo={bloqueo} vehiculo={vehiculo} />
          {bloqueo && <Temporizador key={bloqueo.id} expira={bloqueo.expira} alVencer={alVencer} />}
        </aside>
      </div>

      <Modal abierto={nuevoVehiculo} alCerrar={() => setNuevoVehiculo(false)} titulo="Agregar vehículo">
        {nuevoVehiculo && (
          <FormVehiculo
            vehiculo={null}
            alCancelar={() => setNuevoVehiculo(false)}
            alGuardar={(v) => {
              setNuevoVehiculo(false);
              if (!v) return;
              setVehiculos((x) => ({ ...x, lista: [...x.lista, v].sort((a, b) => a.placa.localeCompare(b.placa)) }));
              setIdVehiculo(v.id);
              setErrorEnvio("");
              avisar("ok", `Registraste ${v.placa} y lo elegimos para esta cita.`);
            }}
          />
        )}
      </Modal>

      <Aviso aviso={aviso} alCerrar={cerrarAviso} />
    </section>
  );
}

export default Reservar;
